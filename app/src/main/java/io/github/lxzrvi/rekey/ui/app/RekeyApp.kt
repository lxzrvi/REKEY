package io.github.lxzrvi.rekey.ui.app

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import io.github.lxzrvi.rekey.data.RekeyPreferences
import io.github.lxzrvi.rekey.data.ThemeMode
import io.github.lxzrvi.rekey.design.MotionContract
import io.github.lxzrvi.rekey.design.TypeContract
import io.github.lxzrvi.rekey.ui.components.SplashOverlay
import io.github.lxzrvi.rekey.ui.home.HomeScreen
import io.github.lxzrvi.rekey.ui.theme.RekeyTheme
import kotlinx.coroutines.delay

@Composable
fun RekeyApp() {
    val context = LocalContext.current
    val preferences = remember(context) { RekeyPreferences(context) }
    var settings by remember { mutableStateOf(preferences.loadAppearance()) }
    var splashDone by rememberSaveable { mutableStateOf(false) }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    LaunchedEffect(Unit) {
        if (!splashDone) {
            delay(MotionContract.SPLASH_HOLD_MILLIS)
            splashDone = true
        }
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

    val transition = updateTransition(targetState = splashDone, label = "splash")
    val progress by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = MotionContract.SPLASH_EXIT_MILLIS, easing = FastOutSlowInEasing)
        },
        label = "splashProgress",
    ) { if (it) 1f else 0f }

    RekeyTheme(settings = settings, darkTheme = darkTheme) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale * TypeContract.TEXT_SCALE),
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                // The splash wordmark leaves completely before the home screen
                // appears, so the big REKEY is never seen sitting on the main page.
                val splashAlpha = 1f - (progress / 0.55f).coerceIn(0f, 1f)
                val homeAlpha = ((progress - 0.55f) / 0.45f).coerceIn(0f, 1f)
                Box(Modifier.fillMaxSize()) {
                    HomeScreen(
                        settings = settings,
                        onSettingsChange = {
                            settings = it
                            preferences.saveAppearance(it)
                        },
                        preferences = preferences,
                        darkTheme = darkTheme,
                        modifier = Modifier.graphicsLayer { alpha = homeAlpha },
                    )
                    if (splashAlpha > 0f) {
                        SplashOverlay(modifier = Modifier.graphicsLayer { alpha = splashAlpha })
                    }
                }
            }
        }
    }
}
