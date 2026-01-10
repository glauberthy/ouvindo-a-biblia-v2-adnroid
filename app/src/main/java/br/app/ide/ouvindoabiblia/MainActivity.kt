package br.app.ide.ouvindoabiblia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
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
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val navController = rememberNavController()

            OuvindoABibliaTheme {
                NavHost(navController = navController, startDestination = Screen.Home) {

                    // 1. Home -> Vai direto para o Player (Playlist Automática)
                    composable<Screen.Home> {
                        HomeScreen(
                            windowSizeClass = windowSizeClass,
                            // MUDANÇA: Agora recebe ID, NOME e CAPA
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

                    // 2. Player (Configurado para Playlist)
                    composable<Screen.Player> { backStackEntry ->
                        val args = backStackEntry.toRoute<Screen.Player>()

                        // Obtemos o ViewModel aqui para poder iniciar a playlist
                        val viewModel = hiltViewModel<PlayerViewModel>()

                        // Carrega a playlist assim que entra na tela
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
                            chapterNumber = 0, // O ViewModel controla isso agora
                            coverUrl = args.coverUrl,
                            viewModel = viewModel
                        )
                    }

                    // 3. Capítulos (Mantido caso queira usar no futuro, mas a Home pula ele)
                    composable<Screen.Chapters> { backStackEntry ->
                        val args = backStackEntry.toRoute<Screen.Chapters>()
                        ChaptersScreen(
                            onBackClick = { navController.popBackStack() },
                            viewModel = hiltViewModel(),
                            onNavigateToPlayer = { _, _ -> } // Não usado neste fluxo novo
                        )
                    }
                }
            }
        }
    }
}