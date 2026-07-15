package br.app.ide.ouvindoabiblia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.app.ide.ouvindoabiblia.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var shouldNavigateToPlayer by mutableStateOf(false)

    // ISSUE 5.A: launcher do request de POST_NOTIFICATIONS. Registrado como campo (antes de
    // STARTED, como exige a Activity Result API). Concedida ou negada, o áudio funciona igual —
    // negá-la só esconde os controles de mídia na notificação; por isso não tratamos o resultado.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 1. Isso garante que a barra seja transparente (o conteúdo passa por baixo)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()
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

    // ISSUE 5.A: pede POST_NOTIFICATIONS só em Android 13+ (a permissão não existe antes).
    // Se já concedida, não faz nada; o SO só reexibe o diálogo enquanto o usuário não decidir.
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkIntentForNotification(intent: Intent) {
        if (intent.getBooleanExtra("OPEN_PLAYER_FROM_NOTIF", false)) {
            shouldNavigateToPlayer = true
        }
    }
}