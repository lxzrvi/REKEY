package io.github.lxzrvi.rekey.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.KeyboardPalettes
import io.github.lxzrvi.rekey.data.RekeyPreferences
import io.github.lxzrvi.rekey.data.ThemeMode
import io.github.lxzrvi.rekey.ui.keyboard.KeyAction
import io.github.lxzrvi.rekey.ui.keyboard.ScaledKeyboard
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

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        preferences = RekeyPreferences(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onCreateInputView(): View {
        reloadTheme()
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@RekeyImeService)
            setViewTreeViewModelStoreOwner(this@RekeyImeService)
            setViewTreeSavedStateRegistryOwner(this@RekeyImeService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { ImeContent() }
        }
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        reloadTheme()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /** Never take over the whole screen: the app's own field must stay visible. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onFinishInputView(finishingInput: Boolean) {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        super.onDestroy()
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
            ScaledKeyboard(
                palette = palette,
                modifier = Modifier.fillMaxWidth(),
                onKey = ::handleKey,
            )
        }
    }

    private fun handleKey(action: KeyAction, text: String) {
        val ic: InputConnection = currentInputConnection ?: return
        when (action) {
            KeyAction.TEXT, KeyAction.SPACE -> ic.commitText(text, 1)
            KeyAction.BACKSPACE -> {
                val selected = ic.getSelectedText(0)
                if (!selected.isNullOrEmpty()) ic.commitText("", 1) else ic.deleteSurroundingText(1, 0)
            }
            KeyAction.ENTER -> {
                val info = currentInputEditorInfo
                val actionId = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_UNSPECIFIED
                if (info != null && info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0 &&
                    actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED
                ) {
                    ic.performEditorAction(actionId)
                } else {
                    ic.commitText("\n", 1)
                }
            }
            KeyAction.LANGUAGE -> switchToNextInputMethod(false)
        }
    }
}
