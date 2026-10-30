package io.github.lxzrvi.rekey

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.lxzrvi.rekey.ui.app.RekeyApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 12+ zooms the splash icon as it leaves. That zoom was the
        // "second, bigger REKEY" — kill it and hand straight over to the app.
        splashScreen.setOnExitAnimationListener { view -> view.remove() }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent { RekeyApp() }
    }
}
