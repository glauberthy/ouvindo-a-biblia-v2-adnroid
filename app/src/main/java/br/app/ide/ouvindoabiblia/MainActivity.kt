package br.app.ide.ouvindoabiblia

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import br.app.ide.ouvindoabiblia.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Controle simples: se recebermos uma notificação para abrir o player, mudamos isso para true
    private var shouldNavigateToPlayer by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Verifica se o app já abriu clicando na notificação
        checkIntentForNotification(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            // Configuração para a barra de status (ícones claros/escuros)
            val view = LocalView.current
            val darkTheme = isSystemInDarkTheme()
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                        !darkTheme
                }
            }

            // AQUI ESTÁ A MÁGICA:
            // Chamamos a MainScreen, que contém o Player (Gaveta) e a Navegação (Home/Busca)
            MainScreen(
                windowSizeClass = windowSizeClass,
                shouldOpenPlayer = shouldNavigateToPlayer,
                onPlayerOpened = {
                    // Resetamos o estado após abrir
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
        // Se a notificação mandar um extra "OPEN_PLAYER...", nós avisamos a UI para expandir o player
        if (intent.getBooleanExtra("OPEN_PLAYER_FROM_NOTIF", false)) {
            shouldNavigateToPlayer = true
        }
    }
}