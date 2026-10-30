package io.github.lxzrvi.rekey

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import io.github.lxzrvi.rekey.ui.foundation.FoundationScreen
import io.github.lxzrvi.rekey.ui.theme.RekeyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            val darkTheme = isSystemInDarkTheme()
            RekeyTheme(darkTheme = darkTheme) {
                FoundationScreen()
            }
        }
    }
}
