package br.app.ide.ouvindoabiblia.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.app.ide.ouvindoabiblia.ui.navigation.NavigationGraph
import br.app.ide.ouvindoabiblia.ui.navigation.Screen
import br.app.ide.ouvindoabiblia.ui.player.PlayerViewModel
import br.app.ide.ouvindoabiblia.ui.player.SharedPlayerScreen
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray
import br.app.ide.ouvindoabiblia.ui.theme.OuvindoABibliaTheme
import br.app.ide.ouvindoabiblia.ui.theme.extractDominantColorFromUrl
import br.app.ide.ouvindoabiblia.ui.theme.isDark

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
        val context = LocalContext.current

        var isPlayerExpanded by remember { mutableStateOf(false) }
        val hasMedia = playerUiState.title.isNotEmpty()

        // --- 1. MOVIDO PARA CIMA: Cálculos de Dimensão e Progresso ---
        // (Precisamos saber o progresso ANTES de configurar a Status Bar)
        val displayMetrics = LocalContext.current.resources.displayMetrics
        val screenHeightPx = displayMetrics.heightPixels
        val screenHeight = with(LocalDensity.current) { screenHeightPx.toDp() + 100.dp }

        val playerContainerHeight by animateDpAsState(
            targetValue = when {
                isPlayerExpanded -> screenHeight
                hasMedia -> 64.dp
                else -> 0.dp
            },
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "Height"
        )

        val expandProgress by remember {
            derivedStateOf {
                val minH = 64f
                val maxH = screenHeight.value
                val currentH = playerContainerHeight.value
                ((currentH - minH) / (maxH - minH)).coerceIn(0f, 1f)
            }
        }

        // --- 2. EXTRAÇÃO DE COR ---
        val defaultColor = MaterialTheme.colorScheme.surfaceVariant
        var artworkColor by remember { mutableStateOf(defaultColor) }

        LaunchedEffect(playerUiState.imageUrl) {
            val color = extractDominantColorFromUrl(context, playerUiState.imageUrl)
            if (color != null) artworkColor = color else artworkColor = defaultColor
        }

        val animatedArtworkColor by animateColorAsState(
            targetValue = artworkColor,
            label = "ColorAnim"
        )

        // --- 3. CONTROLE DA BARRA DE STATUS (AGORA SINCRONIZADO) ---
        val view = LocalView.current
        val isSystemDark = isSystemInDarkTheme()

        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                val controller = WindowCompat.getInsetsController(window, view)

                // --- A MUDANÇA SUTIL QUE FAZ TODA DIFERENÇA ---
                // Só consideramos que estamos no "Modo Player" se a animação
                // estiver acima de 95% (quase tocando o topo).
                val isVisuallyExpanded = expandProgress > 0.90f

                val useDarkIcons = if (isVisuallyExpanded) {
                    // Player chegou no topo: Usa a inteligência da cor da capa
                    !animatedArtworkColor.isDark()
                } else {
                    // Player está embaixo ou subindo: Usa o tema da Home
                    !isSystemDark
                }

                controller.isAppearanceLightStatusBars = useDarkIcons
            }
        }

        // --- LÓGICA DE ABERTURA ---
        LaunchedEffect(shouldOpenPlayer) {
            if (shouldOpenPlayer) {
                playerViewModel.loadBookPlaylist("RESUME", "Retomando...", "")
                isPlayerExpanded = true
                onPlayerOpened()
            }
        }

        BackHandler(enabled = isPlayerExpanded) { isPlayerExpanded = false }

        // --- ANIMAÇÕES RESTANTES ---
        val bottomPadding by animateDpAsState(
            targetValue = if (isPlayerExpanded) 0.dp else 116.dp,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "BottomPadding"
        )
        val sidePadding by animateDpAsState(
            targetValue = if (isPlayerExpanded) 0.dp else 16.dp,
            label = "SidePadding"
        )
        val cornerRadius by animateDpAsState(
            targetValue = if (isPlayerExpanded) 0.dp else 12.dp,
            label = "Corner"
        )
        val containerColor by animateColorAsState(
            targetValue = if (isPlayerExpanded) Color.Transparent else animatedArtworkColor,
            label = "ContainerColor"
        )
        val elevation by animateDpAsState(
            targetValue = if (isPlayerExpanded || !hasMedia) 0.dp else 2.dp,
            label = "Elevation"
        )

        Box(modifier = Modifier.fillMaxSize()) {

            // CAMADA 1: NAVEGAÇÃO
            Scaffold(
                contentWindowInsets = WindowInsets.navigationBars,
                bottomBar = {
                    NavigationBar(
                        containerColor = DeepBlueDark,
                        tonalElevation = 0.dp
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
                            val selectedColor = CreamBackground
                            val unselectedColor = LavenderGray

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        item.icon,
                                        item.title,
                                        modifier = Modifier.size(26.dp),
                                        tint = if (isSelected) selectedColor else unselectedColor
                                    )
                                },
                                label = {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) selectedColor else unselectedColor
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
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    NavigationGraph(
                        modifier = Modifier.fillMaxSize(),
                        navController = navController,
                        windowSizeClass = windowSizeClass,
                        onPlayBook = { id, name, cover ->
                            playerViewModel.loadBookPlaylist(id, name, cover)
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
                    .shadow(
                        elevation,
                        RoundedCornerShape(cornerRadius),
                        spotColor = Color.Black,
                        ambientColor = Color.Black
                    ),
                shape = RoundedCornerShape(cornerRadius),
                color = containerColor,
                shadowElevation = 0.dp
            ) {
                if (playerContainerHeight > 0.dp) {
                    SharedPlayerScreen(
                        expandProgress = expandProgress,
                        uiState = playerUiState,
                        backgroundColor = animatedArtworkColor,
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onSkipNext = { playerViewModel.skipToNextChapter() },
                        onSkipPrev = { playerViewModel.skipToPreviousChapter() },
                        onSeek = { playerViewModel.seekTo(it) },
                        onCollapse = { isPlayerExpanded = false },
                        onOpen = { isPlayerExpanded = true }
                    )
                }
            }
        }
    }
}