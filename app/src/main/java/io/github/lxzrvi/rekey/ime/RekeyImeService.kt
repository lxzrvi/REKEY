package io.github.lxzrvi.rekey.ime

import android.content.ClipboardManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.WindowManager
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
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
import io.github.lxzrvi.rekey.data.RekeyPreferences
import io.github.lxzrvi.rekey.data.ThemeMode
import io.github.lxzrvi.rekey.ui.keyboard.KeyAction
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
    private var systemDark by mutableStateOf(false)
    private var suggestions by mutableStateOf(Words.pick())
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
        WindowCompat.setDecorFitsSystemWindows(w, false)
        w.navigationBarColor = AndroidColor.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) w.isNavigationBarContrastEnforced = false
        if (Build.VERSION.SDK_INT >= 28) {
            val attrs = w.attributes
            attrs.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            w.attributes = attrs
        }
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

    override fun onCreateInputView(): View {
        reloadTheme()
        setUpEdgeToEdge()
        val view = ComposeView(this).apply {
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
        suggestions = Words.pick()
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
    }

    private fun reloadTheme() {
        settings = preferences.loadAppearance()
        systemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val active = preferences.loadActivePresetId()
        palette = preferences.loadPresets().firstOrNull { it.id == active }?.palette
            ?: KeyboardPalettes.Paper
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
                w.navigationBarColor = palette.canvas.toArgb()
                if (Build.VERSION.SDK_INT >= 26) {
                    WindowCompat.getInsetsController(w, w.decorView)
                        .isAppearanceLightNavigationBars = palette.canvas.luminance() > 0.5f
                }
            }
            RekeyKeyboard(
                palette = palette,
                modifier = Modifier.fillMaxWidth(),
                bottomInsetPx = navBottomPx,
                leftInsetPx = navLeftPx,
                rightInsetPx = navRightPx,
                suggestions = suggestions,
                onKey = ::handleKey,
            )
        }
    }

    private fun handleKey(action: KeyAction, text: String) {
        when (action) {
            KeyAction.LANGUAGE -> { switchToNextInputMethod(false); return }
            KeyAction.CYCLE_THEME -> { cycleTheme(); return }
            else -> Unit
        }
        val ic: InputConnection = currentInputConnection ?: return
        when (action) {
            KeyAction.TEXT, KeyAction.SPACE -> ic.commitText(text, 1)
            KeyAction.BACKSPACE -> {
                val selected = ic.getSelectedText(0)
                if (!selected.isNullOrEmpty()) ic.commitText("", 1) else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
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
            KeyAction.PASTE -> pasteClipboard()
            KeyAction.LANGUAGE, KeyAction.CYCLE_THEME -> Unit
        }
    }

    /** The palette tool walks the saved layouts and remembers the choice. */
    private fun cycleTheme() {
        val all = preferences.loadPresets()
        if (all.isEmpty()) return
        val i = all.indexOfFirst { it.id == preferences.loadActivePresetId() }
        val next = all[(i + 1) % all.size]
        preferences.saveActivePresetId(next.id)
        palette = next.palette
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
private object Words {
    private val all = listOf(
        "Hello", "Beautiful", "Today", "Amazing", "Dream",
        "Creative", "Perfect", "Wonderful", "Design", "Explore",
        "Lovely", "Awesome", "Music", "Style", "Welcome",
        "Incredible", "Happy", "Forever", "Inspire", "Magic",
    )
    fun pick(): List<String> = all.shuffled().take(3)
}
