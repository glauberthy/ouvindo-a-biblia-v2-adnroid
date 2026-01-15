package br.app.ide.ouvindoabiblia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.app.ide.ouvindoabiblia.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var shouldNavigateToPlayer by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 1. Isso garante que a barra seja transparente (o conteúdo passa por baixo)
        enableEdgeToEdge()

        checkIntentForNotification(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            MainScreen(
                windowSizeClass = windowSizeClass,
                shouldOpenPlayer = shouldNavigateToPlayer,
                onPlayerOpened = {
                    shouldNavigateToPlayer = false
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkIntentForNotification(intent)
    }

    private fun checkIntentForNotification(intent: Intent) {
        if (intent.getBooleanExtra("OPEN_PLAYER_FROM_NOTIF", false)) {
            shouldNavigateToPlayer = true
        }
    }
}