package io.github.lxzrvi.rekey.ui.app

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.RekeyPreferences
import io.github.lxzrvi.rekey.data.ThemeMode
import io.github.lxzrvi.rekey.design.TypeContract
import io.github.lxzrvi.rekey.ui.home.HomeScreen
import io.github.lxzrvi.rekey.ui.onboarding.SetupScreen
import io.github.lxzrvi.rekey.ui.theme.RekeyTheme

/**
 * Startup draws REKEY exactly once — in the platform splash window. There is no
 * Compose wordmark overlay any more, so nothing can appear a second time at a
 * different size.
 */
@Composable
fun RekeyApp() {
    val context = LocalContext.current
    val preferences = remember(context) { RekeyPreferences(context) }
    var settings by remember { mutableStateOf(preferences.loadAppearance()) }
    var setupDone by rememberSaveable { mutableStateOf(preferences.isSetupDone()) }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val activity = context as? Activity
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val onSettingsChange: (AppearanceSettings) -> Unit = {
        settings = it
        preferences.saveAppearance(it)
    }

    RekeyTheme(settings = settings, darkTheme = darkTheme) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale * TypeContract.TEXT_SCALE),
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val pageFade by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    label = "pageFade",
                )
                if (setupDone) {
                    HomeScreen(
                        settings = settings,
                        onSettingsChange = onSettingsChange,
                        preferences = preferences,
                        darkTheme = darkTheme,
                        modifier = Modifier.graphicsLayer { alpha = pageFade },
                    )
                } else {
                    SetupScreen(
                        darkTheme = darkTheme,
                        onFinished = {
                            preferences.setSetupDone(true)
                            setupDone = true
                        },
                        modifier = Modifier.graphicsLayer { alpha = pageFade },
                    )
                }
            }
        }
    }
}
