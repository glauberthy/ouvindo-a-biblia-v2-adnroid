package br.app.ide.ouvindoabiblia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import br.app.ide.ouvindoabiblia.ui.chapters.ChaptersScreen
import br.app.ide.ouvindoabiblia.ui.home.HomeScreen
import br.app.ide.ouvindoabiblia.ui.navigation.Screen
import br.app.ide.ouvindoabiblia.ui.player.PlayerScreen
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

                    // 1. Home
                    composable<Screen.Home> {
                        HomeScreen(
                            windowSizeClass = windowSizeClass,
                            // CORREÇÃO AQUI: Adicionado 'bookName' na assinatura
                            onNavigateToBook = { bookId, bookName ->
                                // Agora passamos o nome real que veio da Home
                                navController.navigate(Screen.Chapters(bookId, bookName))
                            }
                        )
                    }

                    // 2. Capítulos
                    composable<Screen.Chapters> { backStackEntry ->
                        val args = backStackEntry.toRoute<Screen.Chapters>()

                        ChaptersScreen(
                            onBackClick = { navController.popBackStack() },
                            viewModel = hiltViewModel(),
                            onNavigateToPlayer = { chapterNum, coverUrl ->
                                navController.navigate(
                                    Screen.Player(
                                        bookId = args.bookId,
                                        bookTitle = args.bookName,
                                        chapterNumber = chapterNum,
                                        coverUrl = coverUrl
                                    )
                                )
                            }
                        )
                    }

                    // 3. Player
                    composable<Screen.Player> { backStackEntry ->
                        val args = backStackEntry.toRoute<Screen.Player>()
                        PlayerScreen(
                            onBackClick = { navController.popBackStack() },
                            bookTitle = args.bookTitle,
                            chapterNumber = args.chapterNumber,
                            coverUrl = args.coverUrl
                        )
                    }
                }
            }
        }
    }
}