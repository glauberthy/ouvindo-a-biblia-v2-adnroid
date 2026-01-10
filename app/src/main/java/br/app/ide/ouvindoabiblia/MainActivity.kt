package br.app.ide.ouvindoabiblia

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import br.app.ide.ouvindoabiblia.ui.chapters.ChaptersScreen
import br.app.ide.ouvindoabiblia.ui.home.HomeScreen
import br.app.ide.ouvindoabiblia.ui.more.MoreScreen
import br.app.ide.ouvindoabiblia.ui.navigation.Screen
import br.app.ide.ouvindoabiblia.ui.player.PlayerScreen
import br.app.ide.ouvindoabiblia.ui.player.PlayerViewModel
import br.app.ide.ouvindoabiblia.ui.theme.OuvindoABibliaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var shouldNavigateToPlayer by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkIntentForNotification(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val navController = rememberNavController()

            LaunchedEffect(shouldNavigateToPlayer) {
                if (shouldNavigateToPlayer) {
                    shouldNavigateToPlayer = false
                    navController.navigate(Screen.Player("RESUME", "Carregando...", ""))
                }
            }

            val items = listOf(
                BottomNavItem("Início", Icons.Default.Home, Screen.Home),
                BottomNavItem("Favoritos", Icons.Default.Favorite, Screen.Favorites),
                BottomNavItem("Busca", Icons.Default.Search, Screen.Search),
                BottomNavItem("Histórico", Icons.Default.History, Screen.History),
                BottomNavItem("Mais", Icons.Default.Menu, Screen.More),
            )

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.hasRoute<Screen.Player>() == false

            OuvindoABibliaTheme {
                val view = LocalView.current
                val darkTheme = isSystemInDarkTheme()

                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        // Define a cor dos ícones:
                        // Se o tema for claro, os ícones ficam escuros.
                        // Se o tema for escuro, os ícones ficam brancos.
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                            !darkTheme
                    }
                }
                
                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                items.forEach { item ->
                                    val isSelected = currentDestination?.hierarchy?.any {
                                        it.hasRoute(item.screen::class)
                                    } == true

                                    NavigationBarItem(
                                        icon = { Icon(item.icon, contentDescription = item.title) },
                                        label = { Text(item.title) },
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(item.screen) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        composable<Screen.Home> {
                            HomeScreen(
                                windowSizeClass = windowSizeClass,
                                onNavigateToBook = { id, name, cover ->
                                    navController.navigate(Screen.Player(id, name, cover))
                                }
                            )
                        }

                        composable<Screen.Favorites> { PlaceholderScreen("Favoritos") }
                        composable<Screen.Search> { PlaceholderScreen("Busca") }
                        composable<Screen.History> { PlaceholderScreen("Histórico") }

                        composable<Screen.More> {
                            MoreScreen(onNavigateToItem = { /* Navegação futura */ })
                        }

                        composable<Screen.Player> { backStackEntry ->
                            val args = backStackEntry.toRoute<Screen.Player>()
                            val viewModel = hiltViewModel<PlayerViewModel>()

                            LaunchedEffect(args.bookId) {
                                viewModel.loadBookPlaylist(
                                    args.bookId,
                                    args.bookTitle,
                                    args.coverUrl
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

// DEFINIÇÕES FORA DA CLASSE PARA EVITAR ERROS DE ESCOPO
data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val screen: Screen
)

@Composable
fun PlaceholderScreen(title: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            Text("Tela de $title em construção", style = MaterialTheme.typography.headlineSmall)
        }
    }
}