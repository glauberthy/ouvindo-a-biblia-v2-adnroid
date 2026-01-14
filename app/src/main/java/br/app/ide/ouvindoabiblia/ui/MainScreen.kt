package br.app.ide.ouvindoabiblia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.app.ide.ouvindoabiblia.ui.navigation.NavigationGraph
import br.app.ide.ouvindoabiblia.ui.navigation.Screen
import br.app.ide.ouvindoabiblia.ui.player.MiniPlayer
import br.app.ide.ouvindoabiblia.ui.player.PlayerScreen
import br.app.ide.ouvindoabiblia.ui.player.PlayerViewModel
import br.app.ide.ouvindoabiblia.ui.theme.OuvindoABibliaTheme

data class BottomNavItem(val title: String, val icon: ImageVector, val screen: Screen)

@Composable
fun MainScreen(
    windowSizeClass: WindowSizeClass,
    shouldOpenPlayer: Boolean,
    onPlayerOpened: () -> Unit
) {
    OuvindoABibliaTheme {
        val navController = rememberNavController()
        val playerViewModel: PlayerViewModel = hiltViewModel()
        val playerUiState by playerViewModel.uiState.collectAsState()

        var isPlayerExpanded by remember { mutableStateOf(false) }

        LaunchedEffect(shouldOpenPlayer) {
            if (shouldOpenPlayer) {
                playerViewModel.loadBookPlaylist("RESUME", "Retomando...", "")
                isPlayerExpanded = true
                onPlayerOpened()
            }
        }

        BackHandler(enabled = isPlayerExpanded) {
            isPlayerExpanded = false
        }

        // Altura "Nuclear": Garante cobertura total da Status Bar
        val displayMetrics = LocalContext.current.resources.displayMetrics
        val screenHeightPx = displayMetrics.heightPixels
        val screenHeight = with(LocalDensity.current) { screenHeightPx.toDp() + 100.dp }

        // --- ANIMAÇÕES ---
        val bottomPadding by animateDpAsState(
            targetValue = if (isPlayerExpanded) 0.dp else 116.dp, // 116dp (Sua preferência)
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "BottomPadding"
        )

        val sidePadding by animateDpAsState(
            targetValue = if (isPlayerExpanded) 0.dp else 16.dp,
            label = "SidePadding"
        )

        val playerContainerHeight by animateDpAsState(
            targetValue = if (isPlayerExpanded) screenHeight else 64.dp,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "Height"
        )

        val cornerRadius by animateDpAsState(
            targetValue = if (isPlayerExpanded) 0.dp else 12.dp,
            label = "Corner"
        )

        val containerColor by animateColorAsState(
            targetValue = if (isPlayerExpanded) Color.Transparent else Color(0xFFF5F5F5),
            label = "ContainerColor"
        )

        // Sombra DURA: Elevation baixa (2dp) + Cor (Controlada no shadow modifier)
        val elevation by animateDpAsState(
            targetValue = if (isPlayerExpanded) 0.dp else 2.dp,
            label = "Elevation"
        )

        Box(modifier = Modifier.fillMaxSize()) {

            // CAMADA 1: NAVEGAÇÃO
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        val items = listOf(
                            BottomNavItem("Início", Icons.Default.Home, Screen.Home),
                            BottomNavItem("Favoritos", Icons.Default.Favorite, Screen.Favorites),
                            BottomNavItem("Busca", Icons.Default.Search, Screen.Search),
                            BottomNavItem("Histórico", Icons.Default.History, Screen.History),
                            BottomNavItem("Mais", Icons.Default.Menu, Screen.More),
                        )
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        items.forEach { item ->
                            val isSelected = currentDestination?.hierarchy?.any {
                                it.route?.contains(item.screen::class.simpleName ?: "") == true
                            } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(item.screen) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {

                    NavigationGraph(
                        modifier = Modifier.fillMaxSize(),
                        navController = navController,
                        windowSizeClass = windowSizeClass,
                        onPlayBook = { id, name, cover ->
                            playerViewModel.loadBookPlaylist(id, name, cover)
                            isPlayerExpanded = true
                        }
                    )
                }
            }

            // CAMADA 2: PLAYER FLUTUANTE
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding, start = sidePadding, end = sidePadding)
                    .height(playerContainerHeight)
                    .fillMaxWidth()
                    .draggable(
                        state = rememberDraggableState { delta ->
                            if (delta < -20) isPlayerExpanded = true
                            if (delta > 20) isPlayerExpanded = false
                        },
                        orientation = Orientation.Vertical
                    )
                    // SOMBRA DURA: SpotColor Black + Elevation Baixa
                    .shadow(
                        elevation = elevation,
                        shape = RoundedCornerShape(cornerRadius),
                        spotColor = Color.Black,
                        ambientColor = Color.Black
                    ),
                shape = RoundedCornerShape(cornerRadius),
                color = containerColor,
                shadowElevation = 0.dp
            ) {
                AnimatedContent(
                    targetState = isPlayerExpanded,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "PlayerContent"
                ) { expanded ->
                    if (expanded) {
                        PlayerScreen(
                            viewModel = playerViewModel,
                            onCollapse = { isPlayerExpanded = false }
                        )
                    } else {
                        // MINI PLAYER: Sem Box extra bloqueando cliques
                        // O draggable do pai já cuida do swipe.
                        // O clique é tratado internamente no MiniPlayer (Colunas de texto).
                        MiniPlayer(
                            uiState = playerUiState,
                            onPlayPause = { playerViewModel.togglePlayPause() },
                            onOpen = { isPlayerExpanded = true },
                            onSkipNext = { playerViewModel.skipToNextChapter() }
                        )
                    }
                }
            }
        }
    }
}