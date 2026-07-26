package br.app.ide.ouvindoabiblia.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Moment
import br.app.ide.ouvindoabiblia.ui.favorites.FavoritesScreen
import br.app.ide.ouvindoabiblia.ui.home.HomeScreen
import br.app.ide.ouvindoabiblia.ui.more.MoreRightsGroupRoute
import br.app.ide.ouvindoabiblia.ui.more.MoreScreen
import br.app.ide.ouvindoabiblia.ui.more.MoreSectionDetailsRoute
import br.app.ide.ouvindoabiblia.ui.studies.StudiesScreen
import br.app.ide.ouvindoabiblia.ui.studies.StudyDetailsScreen
import br.app.ide.ouvindoabiblia.ui.themas.ThemeDetailsScreen
import br.app.ide.ouvindoabiblia.ui.themas.ThemesScreen

@Composable
fun NavigationGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    onPlayBook: (Int, String, String, Int) -> Unit,
    onPlayTheme: (String, String, List<Moment>, Int) -> Unit,
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
                    onPlayBook(numericId, name, cover, 0)
                },
                bottomContentPadding = bottomContentPadding
            )
        }

        // --- FAVORITOS ---
        composable<Screen.Favorites> {
            FavoritesScreen(
                onPlayChapter = { numericId, name, cover, index ->
                    onPlayBook(numericId, name, cover, index)
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
            MoreScreen(
                bottomContentPadding = bottomContentPadding,
                onRightsGroupClick = {
                    navController.navigate(Screen.MoreRights)
                }
            )
        }

        // --- MAIS > DIREITOS E LICENÇAS (sublista) ---
        composable<Screen.MoreRights> {
            MoreRightsGroupRoute(
                bottomContentPadding = bottomContentPadding,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<Screen.MoreSection> {
            MoreSectionDetailsRoute(
                bottomContentPadding = bottomContentPadding,
                onBackClick = { navController.popBackStack() }
            )
        }

    }
}



