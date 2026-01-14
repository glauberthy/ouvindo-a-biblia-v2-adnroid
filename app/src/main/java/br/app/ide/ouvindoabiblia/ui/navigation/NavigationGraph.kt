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
import br.app.ide.ouvindoabiblia.ui.home.HomeScreen
import br.app.ide.ouvindoabiblia.ui.more.MoreScreen

@Composable
fun NavigationGraph(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    onPlayBook: (String, String, String) -> Unit, // Callback: A Home avisa "Toca isso", e a MainScreen obedece
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
                onNavigateToBook = { id, name, cover ->
                    // Ao clicar num livro, não navegamos mais para uma nova tela.
                    // Nós chamamos essa função para abrir o Player (Bottom Sheet) por cima.
                    onPlayBook(id, name, cover)
                }
            )
        }

        // --- TELAS SECUNDÁRIAS ---
        composable<Screen.Favorites> { PlaceholderScreen("Favoritos") }
        composable<Screen.Search> { PlaceholderScreen("Busca") }
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