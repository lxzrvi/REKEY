package io.github.lxzrvi.rekey.ime

import android.content.ClipboardManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.KeyboardPalettes
import io.github.lxzrvi.rekey.data.KeyboardStyle
import io.github.lxzrvi.rekey.data.RekeyPreferences
import io.github.lxzrvi.rekey.data.ThemeMode
import io.github.lxzrvi.rekey.ui.keyboard.KeyAction
import io.github.lxzrvi.rekey.ui.components.rememberUriImage
import io.github.lxzrvi.rekey.ui.keyboard.RekeyKeyboard
import io.github.lxzrvi.rekey.ui.theme.RekeyTheme

/**
 * The real Android input method.
 *
 * A Service is not a lifecycle owner by itself, so the three Compose hosts
 * (lifecycle, view-model store, saved-state registry) are implemented here and
 * attached to the [ComposeView] before any content is set. Without them Compose
 * throws "ViewTreeLifecycleOwner not found" the moment the keyboard is shown.
 */
class RekeyImeService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)
    private lateinit var preferences: RekeyPreferences

    private var settings by mutableStateOf(AppearanceSettings())
    private var palette by mutableStateOf(KeyboardPalettes.Paper)
    private var style by mutableStateOf(KeyboardStyle())
    private var floating by mutableStateOf(false)
    private var floatY by mutableFloatStateOf(0f)
    private lateinit var feedback: KeyFeedback
    private var systemDark by mutableStateOf(false)
    private var suggestions by mutableStateOf<List<String>>(emptyList())
    private lateinit var predictor: WordPredictor
    private var destroyed = false
    private var navBottomPx by mutableIntStateOf(0)
    private var navLeftPx by mutableIntStateOf(0)
    private var navRightPx by mutableIntStateOf(0)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    /**
     * Every transition goes through here.
     *
     * `InputMethodService.onDestroy()` calls `doFinishInput()`, which calls
     * `finishViews()`, which calls back into [onFinishInputView]. Tearing the
     * registry down before `super.onDestroy()` therefore made that callback try
     * to move a DESTROYED registry back to CREATED and killed the IME process.
     * Once teardown has begun, no upward transition is allowed.
     */
    private fun moveLifecycle(state: Lifecycle.State) {
        if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) return
        if (destroyed && state != Lifecycle.State.DESTROYED) return
        lifecycleRegistry.currentState = state
    }

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        preferences = RekeyPreferences(this)
        feedback = KeyFeedback(this)
        predictor = WordPredictor(this)
        moveLifecycle(Lifecycle.State.CREATED)
    }

    /**
     * Draw behind the system bars and lift the keys above them.
     *
     * Same approach as Unexpected-Keyboard: the window stops fitting insets, the
     * view reads systemBars | displayCutout, the bottom inset is added to the
     * keyboard's own bottom padding, and the navigation bar is painted the
     * keyboard's colour so the two read as one surface.
     */
    private fun setUpEdgeToEdge() {
        val w = window?.window ?: return
        // The board rounds its own corners. An opaque window behind them would
        // fill those corners back in and the radius would never be visible.
        w.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(AndroidColor.TRANSPARENT))
        if (Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            w.navigationBarColor = AndroidColor.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= 29) w.isNavigationBarContrastEnforced = false
        ViewCompat.setOnApplyWindowInsetsListener(w.decorView) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            navBottomPx = bars.bottom
            navLeftPx = bars.left
            navRightPx = bars.right
            insets
        }
        ViewCompat.requestApplyInsets(w.decorView)
    }

    /**
     * The IME window is a Dialog whose input area defaults to filling whatever
     * height it is given. Turning off decor-fits-system-windows without also
     * pinning that area to WRAP_CONTENT let the ComposeView stretch to the status
     * bar, which is the huge empty band above the keys.
     *
     * Same sequence as Unexpected-Keyboard: fix the window, then the input area,
     * then ask for insets. Edge-to-edge is only enabled from API 35 because 30-34
     * draw artifacts with it.
     */
    override fun setInputView(v: View) {
        (v.parent as? ViewGroup)?.removeView(v)
        super.setInputView(v)
        updateWindowLayout()
        v.requestApplyInsets()
    }

    override fun updateFullscreenMode() {
        super.updateFullscreenMode()
        updateWindowLayout()
    }

    // Window.setDecorFitsSystemWindows is deprecated for activities, but an IME
    // window has no WindowInsetsController equivalent that sizes the input area.
    @Suppress("DEPRECATION")
    private fun updateWindowLayout() {
        val w = window?.window ?: return
        if (Build.VERSION.SDK_INT >= 35) {
            val a = w.attributes
            a.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            a.fitInsetsTypes = 0
            w.attributes = a
            w.setDecorFitsSystemWindows(false)
        } else {
            w.setDecorFitsSystemWindows(true)
        }
        // Leave the window height to the framework. Forcing MATCH_PARENT made
        // the IME cover the screen; the board only needs its input area wrapped.
        val inputArea = w.findViewById<View>(android.R.id.inputArea)
        val holder = inputArea?.parent as? View ?: return
        val lp = holder.layoutParams
        lp.height = if (isFullscreenMode) {
            ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }
        // Gravity must be set before the params are handed back, otherwise the
        // change never triggers a relayout.
        (lp as? android.widget.FrameLayout.LayoutParams)?.gravity = Gravity.BOTTOM
        holder.layoutParams = lp
    }

    override fun onCreateInputView(): View {
        reloadTheme()
        setUpEdgeToEdge()
        val view = ComposeView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setViewTreeLifecycleOwner(this@RekeyImeService)
            setViewTreeViewModelStoreOwner(this@RekeyImeService)
            setViewTreeSavedStateRegistryOwner(this@RekeyImeService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { ImeContent() }
        }
        attachOwnersToWindow()
        moveLifecycle(Lifecycle.State.RESUMED)
        return view
    }

    /**
     * Compose resolves its Recomposer from the window's ROOT view, not from the
     * ComposeView it is attached to. In an input method that root is the
     * platform's `parentPanel`, so owners set only on the ComposeView are never
     * found and the keyboard throws "ViewTreeLifecycleOwner not found" the
     * moment the window attaches. The decor view therefore needs them too.
     */
    private fun attachOwnersToWindow() {
        val decor = window?.window?.decorView ?: return
        if (decor.findViewTreeLifecycleOwner() == null) {
            decor.setViewTreeLifecycleOwner(this)
        }
        if (decor.findViewTreeViewModelStoreOwner() == null) {
            decor.setViewTreeViewModelStoreOwner(this)
        }
        if (decor.findViewTreeSavedStateRegistryOwner() == null) {
            decor.setViewTreeSavedStateRegistryOwner(this)
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        setUpEdgeToEdge()
        // The window can be rebuilt between sessions; re-attach every time.
        attachOwnersToWindow()
        moveLifecycle(Lifecycle.State.RESUMED)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        reloadTheme()
        updateSuggestions()
        moveLifecycle(Lifecycle.State.RESUMED)
    }

    /** Never take over the whole screen: the app's own field must stay visible. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onFinishInputView(finishingInput: Boolean) {
        moveLifecycle(Lifecycle.State.CREATED)
        super.onFinishInputView(finishingInput)
    }

    override fun onWindowHidden() {
        moveLifecycle(Lifecycle.State.CREATED)
        super.onWindowHidden()
    }

    override fun onDestroy() {
        // Latch first, let the framework run its finish callbacks, and only then
        // destroy the registry. `super.onDestroy()` calls doFinishInput() ->
        // finishViews() -> onFinishInputView(), so destroying the registry first
        // made that callback attempt DESTROYED -> CREATED and kill the process.
        destroyed = true
        super.onDestroy()
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        store.clear()
        if (::feedback.isInitialized) feedback.release()
        if (::predictor.isInitialized) predictor.flush()
    }

    private fun reloadTheme() {
        settings = preferences.loadAppearance()
        systemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val active = preferences.loadActivePresetId()
        val preset = preferences.loadPresets().firstOrNull { it.id == active }
        palette = preset?.palette ?: KeyboardPalettes.Paper
        style = preset?.style ?: KeyboardStyle()
        if (::feedback.isInitialized) feedback.configure(style.feel)
    }

    @Composable
    private fun ImeContent() {
        val dark = when (settings.themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        RekeyTheme(settings = settings, darkTheme = dark) {
            // Keep the navigation bar the same colour as the tray, with icons
            // that stay legible on it.
            LaunchedEffect(palette.canvas) {
                val w = window?.window ?: return@LaunchedEffect
                if (Build.VERSION.SDK_INT < 35) {
                    @Suppress("DEPRECATION")
                    w.navigationBarColor = palette.canvas.toArgb()
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    WindowCompat.getInsetsController(w, w.decorView)
                        .isAppearanceLightNavigationBars = palette.canvas.luminance() > 0.5f
                }
            }
            val bg = rememberUriImage(
                style.background.imageUri.takeIf { style.background.useImage },
            )
            // Floating mode is a narrower, fully rounded board with a margin,
            // which is as close to a free window as an IME is allowed to get.
            val boardStyle = if (floating) {
                val r = maxOf(style.layout.cornerTopLeft, 18f)
                style.copy(
                    layout = style.layout.copy(
                        widthScale = style.layout.widthScale * 0.88f,
                        cornerTopLeft = r, cornerTopRight = r,
                        cornerBottomLeft = r, cornerBottomRight = r,
                        padBottom = style.layout.padBottom + 4f,
                    ),
                )
            } else style
            Box(
                Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, if (floating) floatY.toInt() else 0) }
                    .padding(bottom = if (floating) 14.dp else 0.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
            RekeyKeyboard(
                palette = palette,
                modifier = Modifier.fillMaxWidth(),
                style = boardStyle,
                backgroundImage = bg,
                bottomInsetPx = navBottomPx,
                leftInsetPx = navLeftPx,
                rightInsetPx = navRightPx,
                systemHaptic = false,
                floating = floating,
                onFloatingDrag = { dy ->
                    floatY = (floatY + dy).coerceIn(-900f, 0f)
                },
                suggestions = suggestions,
                onKey = ::handleKey,
            )
            }
        }
    }

    private fun handleKey(action: KeyAction, text: String) {
        when (action) {
            KeyAction.HIDE -> { requestHideSelf(0); return }
            KeyAction.TOGGLE_FLOAT -> {
                floating = !floating
                floatY = 0f
                return
            }
            KeyAction.LANGUAGE -> { switchToNextInputMethod(false); return }
            KeyAction.CYCLE_THEME -> { cycleTheme(); return }
            else -> Unit
        }
        feedback.tap()
        val ic: InputConnection = currentInputConnection ?: return
        when (action) {
            KeyAction.TEXT, KeyAction.SPACE -> {
                if (action == KeyAction.SPACE) {
                    // The word that just ended is the strongest signal we get.
                    val before = ic.getTextBeforeCursor(48, 0)?.toString().orEmpty()
                    predictor.learn(before.takeLastWhile { it.isLetter() || it == '\'' })
                }
                ic.commitText(text, 1)
                updateSuggestions()
            }
            KeyAction.BACKSPACE -> {
                val selected = ic.getSelectedText(0)
                if (!selected.isNullOrEmpty()) ic.commitText("", 1) else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                updateSuggestions()
            }
            KeyAction.ENTER -> {
                val info = currentInputEditorInfo
                val actionId = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_UNSPECIFIED
                if (info != null && info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0 &&
                    actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED
                ) {
                    ic.performEditorAction(actionId)
                } else {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                }
            }
            KeyAction.SUGGEST -> {
                // Replace the half-typed word instead of appending to it.
                val before = ic.getTextBeforeCursor(48, 0)?.toString().orEmpty()
                val prefix = before.takeLastWhile { it.isLetter() || it == '\'' }
                if (prefix.isNotEmpty()) ic.deleteSurroundingText(prefix.length, 0)
                predictor.learn(text)
                ic.commitText("$text ", 1)
                updateSuggestions()
            }
            KeyAction.PASTE -> pasteClipboard()
            KeyAction.LEFT -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
            KeyAction.RIGHT -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
            KeyAction.UP -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP)
            KeyAction.DOWN -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN)
            KeyAction.HOME -> sendDownUpKeyEvents(KeyEvent.KEYCODE_MOVE_HOME)
            KeyAction.END -> sendDownUpKeyEvents(KeyEvent.KEYCODE_MOVE_END)
            KeyAction.SELECT_ALL -> ic.performContextMenuAction(android.R.id.selectAll)
            KeyAction.COPY -> ic.performContextMenuAction(android.R.id.copy)
            KeyAction.CUT -> ic.performContextMenuAction(android.R.id.cut)
            KeyAction.BOLD -> sendCtrl(KeyEvent.KEYCODE_B)
            KeyAction.ITALIC -> sendCtrl(KeyEvent.KEYCODE_I)
            KeyAction.UNDERLINE -> sendCtrl(KeyEvent.KEYCODE_U)
            KeyAction.LANGUAGE, KeyAction.CYCLE_THEME, KeyAction.HIDE, KeyAction.TOGGLE_FLOAT, KeyAction.SUGGEST -> Unit
        }
    }

    /**
     * Reads back from the editor rather than keeping our own buffer, so moving
     * the caret or switching fields never leaves stale suggestions on screen.
     */
    private fun updateSuggestions() {
        if (!::predictor.isInitialized) return
        val before = currentInputConnection?.getTextBeforeCursor(48, 0)?.toString().orEmpty()
        val prefix = before.takeLastWhile { it.isLetter() || it == '\'' }
        val head = before.dropLast(prefix.length).trimEnd()
        val previous = head.takeLastWhile { it.isLetter() || it == '\'' }
        suggestions = predictor.suggest(prefix, previous, 5)
    }

    /** Formatting shortcuts for editors that understand them. */
    private fun sendCtrl(keyCode: Int) {
        val now = android.os.SystemClock.uptimeMillis()
        val meta = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        currentInputConnection?.sendKeyEvent(
            KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta),
        )
        currentInputConnection?.sendKeyEvent(
            KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta),
        )
    }

    /** The palette tool walks the saved layouts and remembers the choice. */
    private fun cycleTheme() {
        val all = preferences.loadPresets()
        if (all.isEmpty()) return
        val i = all.indexOfFirst { it.id == preferences.loadActivePresetId() }
        val next = all[(i + 1) % all.size]
        preferences.saveActivePresetId(next.id)
        palette = next.palette
        style = next.style
        feedback.configure(style.feel)
    }

    private fun pasteClipboard() {
        val cm = getSystemService(ClipboardManager::class.java) ?: return
        val text = cm.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(this)?.toString()
        if (!text.isNullOrEmpty()) currentInputConnection?.commitText(text, 1)
    }
}

/** Placeholder suggestions shown in the toolbar strip. */
