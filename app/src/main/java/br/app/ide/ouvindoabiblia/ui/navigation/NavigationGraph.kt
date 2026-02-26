package br.app.ide.ouvindoabiblia.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.app.ide.ouvindoabiblia.ui.chapters.ChaptersScreen
import br.app.ide.ouvindoabiblia.ui.favorites.FavoritesScreen
import br.app.ide.ouvindoabiblia.ui.home.HomeScreen
import br.app.ide.ouvindoabiblia.ui.more.MoreScreen
import br.app.ide.ouvindoabiblia.ui.themas.ThemesScreen
import br.app.ide.ouvindoabiblia.ui.themes.ThemeDetailsScreen

@Composable
fun NavigationGraph(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    onPlayBook: (Int, String, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home,
        modifier = modifier.fillMaxSize()
    ) {
        // --- HOME ---
        composable<Screen.Home> {
            HomeScreen(
                windowSizeClass = windowSizeClass,
                onNavigateToBook = { numericId, name, cover ->
                    // Ao clicar num livro, não navegamos mais para uma nova tela.
                    // Nós chamamos essa função para abrir o Player (Bottom Sheet) por cima.
                    onPlayBook(numericId, name, cover, 0)
                }
            )
        }

        // --- FAVORITOS ---
        composable<Screen.Favorites> {
            FavoritesScreen(
                onPlayChapter = { numericId, name, cover, index -> // Recebe o index da tela
                    onPlayBook(numericId, name, cover, index)     // Repassa para a Main
                }
            )
        }
        composable<Screen.Themes> {
            ThemesScreen(
                onThemeClick = { id, title ->
                    navController.navigate(Screen.ThemeDetails(id, title))
                }
            )
        }

        composable<Screen.ThemeDetails> {
            ThemeDetailsScreen(
                onBackClick = { navController.popBackStack() }, // Botão voltar
                onPlayMoment = { momentWithAudio ->
                    // AQUI É ONDE A MÁGICA DO CLIPPING VAI ACONTECER NO PRÓXIMO PASSO!
                    // Por enquanto vamos apenas printar no Log ou chamar uma função vazia
                    println("Clicou para tocar: ${momentWithAudio.moment.reference} do tempo ${momentWithAudio.moment.startMs} até ${momentWithAudio.moment.endMs}")
                }
            )
        }
        composable<Screen.History> { PlaceholderScreen("Histórico") }

        composable<Screen.More> {
            MoreScreen(onNavigateToItem = { /* Navegação futura */ })
        }

        // --- CAPÍTULOS (Ainda é uma tela separada) ---
        composable<Screen.Chapters> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Chapters>()
            ChaptersScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = hiltViewModel(),
                onNavigateToPlayer = { _, _ -> /* Opcional */ }
            )
        }

        // NOTA: A rota composable<Screen.Player> foi REMOVIDA daqui propositalmente.
    }
}

// Componente temporário para telas que ainda não existem
@Composable
fun PlaceholderScreen(title: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            Text("Tela de $title", style = MaterialTheme.typography.headlineSmall)
        }
    }
}