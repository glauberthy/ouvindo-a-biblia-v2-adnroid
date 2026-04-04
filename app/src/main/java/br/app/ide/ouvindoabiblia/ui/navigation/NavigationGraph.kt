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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.app.ide.ouvindoabiblia.data.local.model.MomentWithAudio
import br.app.ide.ouvindoabiblia.ui.chapters.ChaptersScreen
import br.app.ide.ouvindoabiblia.ui.favorites.FavoritesScreen
import br.app.ide.ouvindoabiblia.ui.home.HomeScreen
import br.app.ide.ouvindoabiblia.ui.more.MoreScreen
import br.app.ide.ouvindoabiblia.ui.studies.StudiesScreen
import br.app.ide.ouvindoabiblia.ui.studies.StudyDetailsScreen
import br.app.ide.ouvindoabiblia.ui.themas.ThemeDetailsScreen
import br.app.ide.ouvindoabiblia.ui.themas.ThemesScreen

@Composable
fun NavigationGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    onPlayBook: (Int, String, String, Int, Long, Long) -> Unit,
    onPlayTheme: (String, String, List<MomentWithAudio>, Int) -> Unit,
    onPlayStudy: (Int, String, String, Int) -> Unit,
    bottomContentPadding: Dp = 0.dp,
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
                    onPlayBook(numericId, name, cover, 0, 0L, 0L)
                }
            )
        }

        // --- FAVORITOS ---
        composable<Screen.Favorites> {
            FavoritesScreen(
                onPlayChapter = { numericId, name, cover, index ->
                    onPlayBook(numericId, name, cover, index, 0L, 0L)
                },
                // Agora os tipos batem: (Int, String, String, Int)
                onPlayStudy = { studyId, title, cover, index ->
                    onPlayStudy(studyId, title, cover, index)
                }
            )
        }
        composable<Screen.Themes> {
            ThemesScreen(
                bottomContentPadding = bottomContentPadding,
                onThemeClick = { id, title ->
                    navController.navigate(Screen.ThemeDetails(id, title))
                }
            )
        }

        // --- DETALHES DO TEMA ---
        composable<Screen.ThemeDetails> {
            ThemeDetailsScreen(
                bottomContentPadding = bottomContentPadding,
                onBackClick = { navController.popBackStack() },
                onPlayTheme = { momentsList, startIndex, themeTitle ->
                    // Aciona o novo callback repassando a fila inteira
                    onPlayTheme(
                        themeTitle,
                        "", // URL da capa do tema, se aplicável, ou string vazia
                        momentsList,
                        startIndex
                    )
                }
            )
        }


        // --- ESTUDOS (MASTER) ---
        composable<Screen.Estudos> {
            StudiesScreen(
                onStudyClick = { id, title ->
                    navController.navigate(Screen.StudyDetails(id, title))
                },
                bottomContentPadding = bottomContentPadding
            )
        }


        // --- DETALHES DO ESTUDO ---
        composable<Screen.StudyDetails> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.StudyDetails>()
            StudyDetailsScreen(
                onBackClick = { navController.popBackStack() },
                onPlayStudy = { title, cover, _, index ->
                    // Usamos o args.id que veio da navegação!
                    onPlayStudy(args.studyId, title, cover, index)
                },
                bottomContentPadding = bottomContentPadding
            )
        }

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