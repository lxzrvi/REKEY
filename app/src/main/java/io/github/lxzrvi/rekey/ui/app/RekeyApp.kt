package io.github.lxzrvi.rekey.ui.app

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import io.github.lxzrvi.rekey.data.AppearanceSettings
import io.github.lxzrvi.rekey.data.RekeyPreferences
import io.github.lxzrvi.rekey.data.ThemeMode
import io.github.lxzrvi.rekey.design.MotionContract
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
            delay(if (settings.animationsEnabled) MotionContract.SPLASH_HOLD_MILLIS else 350L)
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
            tween(
                durationMillis = if (settings.animationsEnabled) MotionContract.SPLASH_EXIT_MILLIS else 0,
                easing = FastOutSlowInEasing,
            )
        },
        label = "splashProgress",
    ) { if (it) 1f else 0f }

    RekeyTheme(settings = settings, darkTheme = darkTheme) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale * settings.textScale),
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize()) {
                    HomeScreen(
                        settings = settings,
                        onSettingsChange = {
                            settings = it.normalized()
                            preferences.saveAppearance(it)
                        },
                        preferences = preferences,
                        darkTheme = darkTheme,
                        modifier = Modifier.graphicsLayer {
                            alpha = progress
                            val scale = 0.965f + 0.035f * progress
                            scaleX = scale
                            scaleY = scale
                            translationY = (1f - progress) * 28.dp.toPx()
                        },
                    )
                    if (progress < 1f) {
                        SplashOverlay(
                            modifier = Modifier.graphicsLayer {
                                alpha = 1f - progress
                                val scale = 1f + 0.075f * progress
                                scaleX = scale
                                scaleY = scale
                            },
                        )
                    }
                }
            }
        }
    }
}
