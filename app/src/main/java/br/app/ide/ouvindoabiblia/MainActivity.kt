package br.app.ide.ouvindoabiblia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import br.app.ide.ouvindoabiblia.ui.chapters.ChaptersScreen
import br.app.ide.ouvindoabiblia.ui.home.HomeScreen
import br.app.ide.ouvindoabiblia.ui.navigation.Screen
import br.app.ide.ouvindoabiblia.ui.player.PlayerScreen
import br.app.ide.ouvindoabiblia.ui.player.PlayerViewModel
import br.app.ide.ouvindoabiblia.ui.theme.OuvindoABibliaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Estado para controlar a navegação vinda da notificação
    private var shouldNavigateToPlayer by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Verifica se abriu "frio" (app estava fechado) pela notificação
        checkIntentForNotification(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val navController = rememberNavController()

            // Efeito que monitora nossa variável de controle
            LaunchedEffect(shouldNavigateToPlayer) {
                if (shouldNavigateToPlayer) {
                    shouldNavigateToPlayer = false // Reseta para não navegar 2x
                    // Navega para o Player com ID mágico "RESUME"
                    navController.navigate(
                        Screen.Player(
                            bookId = "RESUME",
                            bookTitle = "Carregando...",
                            coverUrl = ""
                        )
                    )
                }
            }

            OuvindoABibliaTheme {
                NavHost(navController = navController, startDestination = Screen.Home) {

                    // 1. Home
                    composable<Screen.Home> {
                        HomeScreen(
                            windowSizeClass = windowSizeClass,
                            onNavigateToBook = { bookId, bookName, coverUrl ->
                                navController.navigate(
                                    Screen.Player(
                                        bookId = bookId,
                                        bookTitle = bookName,
                                        coverUrl = coverUrl
                                    )
                                )
                            }
                        )
                    }

                    // 2. Player
                    composable<Screen.Player> { backStackEntry ->
                        val args = backStackEntry.toRoute<Screen.Player>()
                        val viewModel = hiltViewModel<PlayerViewModel>()

                        LaunchedEffect(args.bookId) {
                            viewModel.loadBookPlaylist(
                                bookId = args.bookId,
                                initialBookTitle = args.bookTitle,
                                initialCoverUrl = args.coverUrl
                            )
                        }

                        PlayerScreen(
                            onBackClick = { navController.popBackStack() },
                            bookTitle = args.bookTitle,
                            chapterNumber = 0,
                            coverUrl = args.coverUrl,
                            viewModel = viewModel
                        )
                    }

                    // 3. Capítulos (Rota legado)
                    composable<Screen.Chapters> { backStackEntry ->
                        val args = backStackEntry.toRoute<Screen.Chapters>()
                        ChaptersScreen(
                            onBackClick = { navController.popBackStack() },
                            viewModel = hiltViewModel(),
                            onNavigateToPlayer = { _, _ -> }
                        )
                    }
                }
            }
        }
    }

    // Se o app já estava aberto em background e clicou na notificação
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