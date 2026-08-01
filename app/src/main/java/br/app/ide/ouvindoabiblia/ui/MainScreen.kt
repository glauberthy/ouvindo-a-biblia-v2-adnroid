package br.app.ide.ouvindoabiblia.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Spa
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.app.ide.ouvindoabiblia.R
import br.app.ide.ouvindoabiblia.ui.components.StatusBarScrim
import br.app.ide.ouvindoabiblia.ui.navigation.NavigationGraph
import br.app.ide.ouvindoabiblia.ui.navigation.Screen
import br.app.ide.ouvindoabiblia.review.ReviewPromptEffect
import br.app.ide.ouvindoabiblia.ui.player.PlayerViewModel
import br.app.ide.ouvindoabiblia.ui.player.SharedPlayerScreen
import br.app.ide.ouvindoabiblia.ui.theme.AppColors
import br.app.ide.ouvindoabiblia.ui.theme.BrandNavy
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.OnBrandNavy
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray
import br.app.ide.ouvindoabiblia.ui.theme.OuvindoABibliaTheme
import br.app.ide.ouvindoabiblia.ui.theme.SlateBlue
import br.app.ide.ouvindoabiblia.ui.theme.extractDominantColorFromUrl
import br.app.ide.ouvindoabiblia.ui.theme.isDark
import br.app.ide.ouvindoabiblia.util.ShareUtils
import kotlin.math.roundToInt

// Padrão Material 3: contorno quando inativo, preenchido quando ativo.
// `iconSelected` == `iconUnselected` é aceitável para glifos sem variante
// preenchida (Menu), aí só o rótulo em negrito marca a seleção.
data class BottomNavItem(
    val title: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector,
    val screen: Screen
)

@Composable
fun MainScreen(
    windowSizeClass: WindowSizeClass,
    shouldOpenPlayer: Boolean,
    onPlayerOpened: () -> Unit
) {
    val context = LocalContext.current
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }

    // Estado para monitorar se a economia de bateria está ativa
    var isPowerSaveMode by remember { mutableStateOf(powerManager.isPowerSaveMode) }

    // Registra um receiver para ouvir a mudança do sistema em tempo real
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isPowerSaveMode = powerManager.isPowerSaveMode
            }
        }

        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    OuvindoABibliaTheme {
        val navController = rememberNavController()
        val playerViewModel: PlayerViewModel = hiltViewModel()
        val playerUiState by playerViewModel.uiState.collectAsState()

        // Estado de expansão do player
        var isPlayerExpanded by remember { mutableStateOf(false) }
        val hasMedia = playerUiState.title.isNotEmpty()

        // Avaliação in-app. O sinal é "tocou de fato", com trava — NÃO dá para usar `hasMedia`,
        // que fica verdadeiro só por restaurar a sessão salva no cold start, sem o usuário ter
        // ouvido nada. Ver review/InAppReviewManager.kt para por que este caminho não é
        // verificável no emulador.
        var hasPlayedAudioThisSession by remember { mutableStateOf(false) }
        LaunchedEffect(playerUiState.isPlaying) {
            if (playerUiState.isPlaying) hasPlayedAudioThisSession = true
        }
        ReviewPromptEffect(
            hasPlayedAudio = hasPlayedAudioThisSession,
            isPlayerExpanded = isPlayerExpanded
        )

        val miniPlayerHeight = 64.dp
        val playerFloatMargin = 26.dp

        val extraBottomContentPadding by animateDpAsState(
            targetValue = when {
                isPlayerExpanded -> 0.dp
                hasMedia -> miniPlayerHeight + playerFloatMargin
                else -> 0.dp
            },
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "ExtraBottomContentPadding"
        )

        // --- 1. DIMENSÕES E PROGRESSO ---
        val displayMetrics = context.resources.displayMetrics
        val screenHeightPx = displayMetrics.heightPixels
        val screenHeight =
            with(LocalDensity.current) { screenHeightPx.toDp() + 100.dp } // Margem de segurança

        // Altura dinâmica do container do player
        val playerContainerHeight by animateDpAsState(
            targetValue = when {
                isPlayerExpanded -> screenHeight
                hasMedia -> miniPlayerHeight // Altura do Mini Player
                else -> 0.dp
            },
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "PlayerHeight"
        )

        // Calcula de 0.0 a 1.0 quanto o player está expandido
        val expandProgress by remember {
            derivedStateOf {
                val minH = 64f
                val maxH = screenHeight.value
                val currentH = playerContainerHeight.value
                ((currentH - minH) / (maxH - minH)).coerceIn(0f, 1f)
            }
        }

        // --- 2. EXTRAÇÃO DE COR (Dinâmica baseada na capa) ---
        val defaultColor = MaterialTheme.colorScheme.surfaceVariant
        var artworkColor by remember { mutableStateOf(defaultColor) }

        LaunchedEffect(playerUiState.imageUrl) {
            if (playerUiState.imageUrl.isNotEmpty()) {
                val color = extractDominantColorFromUrl(context, playerUiState.imageUrl)
                artworkColor = color ?: defaultColor
            } else {
                artworkColor = defaultColor
            }
        }

        val animatedArtworkColor by animateColorAsState(
            targetValue = artworkColor,
            label = "ColorAnim"
        )

        // --- 3. CONTROLE DA STATUS BAR ---
        val view = LocalView.current
        val isSystemDark = isSystemInDarkTheme()

        // Lido na composição (getter @Composable) para poder ser usado dentro do SideEffect.
        val pageBackgroundIsDark = AppColors.background.isDark()

        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                val controller = WindowCompat.getInsetsController(window, view)

                // Se o player cobrir quase tudo (> 90%), a barra de status deve reagir à cor do player
                val isVisuallyExpanded = expandProgress > 0.90f

                val useDarkIcons = if (isVisuallyExpanded) {
                    !animatedArtworkColor.isDark() // Se a arte for clara, ícones escuros
                } else {
                    // Ícones escuros SÓ quando o fundo da página é claro. Antes era
                    // `!isPowerSaveMode`, que presumia fundo claro sempre — no tema escuro
                    // isso deixava ícones escuros sobre fundo escuro (relógio ilegível).
                    !(isPowerSaveMode || pageBackgroundIsDark)
                }

                controller.isAppearanceLightStatusBars = useDarkIcons
            }
        }

        // --- 4. LÓGICA DE ABERTURA VIA NOTIFICAÇÃO ---
        LaunchedEffect(shouldOpenPlayer) {
            if (shouldOpenPlayer) {
                // Não precisamos carregar nada, o Service já restaurou. Apenas expandimos.
                isPlayerExpanded = true
                onPlayerOpened()
            }
        }

        // --- 5. BACK HANDLER (Fechar player ao voltar) ---
        BackHandler(enabled = isPlayerExpanded) { isPlayerExpanded = false }

        Box(modifier = Modifier.fillMaxSize()) {

            // CAMADA 1: NAVEGAÇÃO PRINCIPAL (Fica por baixo)
            Scaffold(
                contentWindowInsets = WindowInsets.navigationBars,
                bottomBar = {
                    val items = listOf(
                        // Bíblia desenhada à mão (res/drawable/ic_biblia_*): a aba
                        // É a Bíblia, e o Material Symbols não tem esse glifo.
                        // Rótulo "Livros" (não "Início"): é a palavra que o app já usa
                        // para capítulos bíblicos no seletor de Favoritos, e nomeia o
                        // conteúdo em vez de uma posição.
                        BottomNavItem(
                            "Livros",
                            ImageVector.vectorResource(R.drawable.ic_biblia_filled),
                            ImageVector.vectorResource(R.drawable.ic_biblia_outlined),
                            Screen.Home
                        ),
                        BottomNavItem(
                            "Favoritos",
                            Icons.Filled.Favorite,
                            Icons.Outlined.FavoriteBorder,
                            Screen.Favorites
                        ),
                        // Lótus (Spa): escolhido pelo autor entre Category, Style,
                        // Label, Interests, Bookmarks, Topic, Tag, CollectionsBookmark,
                        // Sell, Explore, Lightbulb, GridView e Bookmark, todos
                        // comparados no emulador nos dois estados. O tom de
                        // acolhimento casa com os temas (ansiedade, consolo,
                        // esperança) e o preenchido fica sólido e legível.
                        BottomNavItem(
                            "Temas",
                            Icons.Filled.Spa,
                            Icons.Outlined.Spa,
                            Screen.Themes
                        ),
                        // Livro aberto com linhas de texto (MenuBook): lê como
                        // leitura/exposição. Não colide com Livros, que é um livro
                        // FECHADO — comparado no emulador com AutoStories,
                        // ImportContacts, LibraryBooks e Book; este é o mais nítido
                        // nos dois estados, e Book é justamente o que se confunde
                        // com a Bíblia.
                        BottomNavItem(
                            "Estudos",
                            Icons.Filled.MenuBook,
                            Icons.Outlined.MenuBook,
                            Screen.Estudos
                        ),
                        BottomNavItem(
                            "Mais",
                            Icons.Filled.Menu,
                            Icons.Outlined.Menu,
                            Screen.More
                        ),
                    )
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    // BUG 1: seleção por hasRoute (compara pelo serialName do @Serializable,
                    // imune à ofuscação do R8). Antes usava contains(::class.simpleName), que o
                    // R8 renomeava no release -> match por substring casava vários itens.
                    // Virou função porque agora o indicador deslizante também precisa saber
                    // QUAL aba está ativa, não só cada item sobre si mesmo.
                    fun isSelectedFor(item: BottomNavItem): Boolean =
                        currentDestination?.hierarchy?.any { navDestination ->
                            when (item.screen) {
                                is Screen.Themes ->
                                    navDestination.hasRoute(Screen.Themes::class) ||
                                            navDestination.hasRoute(Screen.ThemeDetails::class)

                                is Screen.Estudos ->
                                    navDestination.hasRoute(Screen.Estudos::class) ||
                                            navDestination.hasRoute(Screen.StudyDetails::class)

                                // Sem isto a aba "Mais" perde o estado selecionado
                                // dentro da sublista de direitos (mesmo BUG 1).
                                is Screen.More ->
                                    navDestination.hasRoute(Screen.More::class) ||
                                            navDestination.hasRoute(Screen.MoreRights::class)

                                else ->
                                    navDestination.hasRoute(item.screen::class)
                            }
                        } == true

                    // Indicador deslizante. A pílula do Material3 some numa aba e
                    // reaparece na outra; aqui ela é desenhada UMA vez atrás da barra e
                    // anima a posição, então corre entre as abas (~230ms, mola).
                    // Para ficar atrás dos ícones, o BrandNavy sai do NavigationBar e vai
                    // para este Box — senão o container pintaria por cima da pílula, e o
                    // indicador nativo é desligado com indicatorColor = Transparent.
                    //
                    // As DUAS coordenadas são medidas via onGloballyPositioned, não
                    // calculadas. Tentei os dois por conta e os dois erraram: o passo real
                    // do NavigationBar é 220px num aparelho de 1080 (não os 216 de
                    // largura/5), o que deixava a pílula 8px fora do ícone nas abas das
                    // pontas; e o topo não é os 16dp que calibrei no olho, que a punham
                    // 4dp abaixo do ícone. Medindo, funciona em qualquer largura e
                    // sobrevive a mudanças de altura da barra ou do tamanho do ícone.
                    val selectedIndex = items.indexOfFirst { isSelectedFor(it) }.coerceAtLeast(0)
                    val pillWidth = 64.dp
                    val pillHeight = 32.dp
                    val itemCenters = remember { mutableStateMapOf<Int, Float>() }
                    var barLeftPx by remember { mutableFloatStateOf(0f) }
                    var barTopPx by remember { mutableFloatStateOf(0f) }
                    var iconCenterYPx by remember { mutableStateOf<Float?>(null) }
                    val density = LocalDensity.current
                    val targetCenterPx = itemCenters[selectedIndex]
                    val indicatorX by animateDpAsState(
                        targetValue = with(density) {
                            ((targetCenterPx ?: 0f).toDp() - pillWidth / 2)
                        },
                        animationSpec = spring(
                            dampingRatio = 0.75f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "IndicatorSlide"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandNavy)
                            .onGloballyPositioned {
                                barLeftPx = it.positionInWindow().x
                                barTopPx = it.positionInWindow().y
                            }
                    ) {
                        val centerY = iconCenterYPx
                        if (targetCenterPx != null && centerY != null) {
                            Box(
                                modifier = Modifier
                                    // Overload de LAMBDA do offset, não o de Dp: aqui o X
                                    // vem de uma animação, e a versão com argumento força
                                    // RECOMPOSIÇÃO a cada quadro do deslize. Com a lambda o
                                    // valor é lido na fase de layout — a barra inteira
                                    // deixa de recompor 60x por segundo durante a troca de
                                    // aba. (lint: UseOfNonLambdaOffsetOverload)
                                    .offset {
                                        IntOffset(
                                            x = indicatorX.roundToPx(),
                                            y = (centerY - pillHeight.toPx() / 2f).roundToInt()
                                        )
                                    }
                                    .size(pillWidth, pillHeight)
                                    .background(SlateBlue, CircleShape)
                            )
                        }

                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        items.forEachIndexed { index, item ->

                            val isSelected = isSelectedFor(item)

                            NavigationBarItem(
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    itemCenters[index] =
                                        coords.positionInWindow().x - barLeftPx +
                                                coords.size.width / 2f
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) {
                                            item.iconSelected
                                        } else {
                                            item.iconUnselected
                                        },
                                        contentDescription = item.title,
                                        modifier = Modifier
                                            .size(26.dp)
                                            // Basta um item medir: as cinco caixas de
                                            // ícone ficam na mesma altura.
                                            .then(
                                                if (index == 0) {
                                                    Modifier.onGloballyPositioned { coords ->
                                                        iconCenterYPx =
                                                            coords.positionInWindow().y -
                                                                    barTopPx +
                                                                    coords.size.height / 2f
                                                    }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
                                // A barra é BrandNavy nos dois temas, então as cores aqui são
                                // de marca (OnBrandNavy/LavenderGray), não do color scheme.
                                // O alpha 0.6f dos inativos foi removido: dava 2,65:1 sobre a
                                // navy, abaixo do mínimo até para ícone (3:1). Em alpha cheio
                                // são 4,84:1, e seguem visivelmente mais apagados que o ativo.
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = OnBrandNavy,
                                    indicatorColor = Color.Transparent,
                                    selectedTextColor = OnBrandNavy,
                                    unselectedIconColor = LavenderGray,
                                    unselectedTextColor = LavenderGray
                                )
                            )
                        }
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
                        onPlayBook = { numericId, name, cover, index ->
                            playerViewModel.playBook(
                                bookId = numericId,
                                bookTitle = name,
                                coverUrl = cover,
                                initialIndex = index
                            )
                        },
                        onPlayTheme = { themeTitle, themeCoverUrl, moments, startIndex ->
                            playerViewModel.playThemePlaylist(
                                themeTitle,
                                themeCoverUrl,
                                moments,
                                startIndex
                            )
                        },
                        onPlayStudy = { studyId, studyTitle, studyCoverUrl, startIndex ->
                            playerViewModel.playStudyById(
                                studyId = studyId,
                                title = studyTitle,
                                cover = studyCoverUrl,
                                startIndex = startIndex
                            )
                        },
                        bottomContentPadding = extraBottomContentPadding
                    )
                }
            }

            // --- SCRIM DA STATUS BAR (único do app) ---
            // Acompanha o fundo da página: no tema escuro o scrim precisa ser escuro,
            // senão sobra uma faixa clara no topo. O modo economia segue forçando escuro
            // (é o mesmo caso em que `useDarkIcons` acima vira false).
            //
            // Era um degradê inline começando em alpha 1,0 aqui, e os headers de Tema/
            // Estudo somavam um SEGUNDO véu (ISSUE 9.G) por cima — daí a faixa clara
            // "viva" sobre foto escura. Agora existe um só, calibrado no limite de
            // legibilidade dentro do próprio componente.
            StatusBarScrim(
                modifier = Modifier.align(Alignment.TopCenter),
                color = if (isPowerSaveMode) BrandNavy else AppColors.background
            )

            // CAMADA 2: PLAYER FLUTUANTE (Persistent Overlay)
            // Só mostra se tiver altura (animação ou conteúdo)
            if (playerContainerHeight > 0.dp) {

                // Calcula o padding inferior dinâmico
                val navBarHeight = 85.dp // Altura padrão da Material 3 NavigationBar
                val floatMargin = playerFloatMargin

                // Anima o padding para subir/descer suavemente
                val animatedBottomPadding by animateDpAsState(
                    targetValue = if (isPlayerExpanded) 0.dp else (navBarHeight + floatMargin),
                    label = "PlayerBottomMargin"
                )

                // Anima as laterais (para dar efeito de "card" flutuante quando mini)
                val animatedSidePadding by animateDpAsState(
                    targetValue = if (isPlayerExpanded) 0.dp else 8.dp,
                    label = "PlayerSideMargin"
                )

                // Anima o arredondamento
                val animatedCorner by animateDpAsState(
                    targetValue = if (isPlayerExpanded) 0.dp else 16.dp,
                    label = "PlayerCorner"
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter) // Alinha no fundo da tela
                        .padding(
                            bottom = animatedBottomPadding,
                            start = animatedSidePadding,
                            end = animatedSidePadding
                        )
                        .height(playerContainerHeight) // Altura controlada pela animação principal
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isPlayerExpanded) 0.dp else 8.dp, // Sombra só quando mini
                            shape = RoundedCornerShape(animatedCorner),
                            clip = false
                        )
                        .draggable(
                            state = rememberDraggableState { delta ->
                                // delta > 0 = Arrastando para BAIXO (Fechar)
                                if (delta > 15 && isPlayerExpanded) {
                                    isPlayerExpanded = false
                                }
                                // delta < 0 = Arrastando para CIMA (Abrir)
                                if (delta < -15 && !isPlayerExpanded) {
                                    isPlayerExpanded = true
                                }
                            },
                            orientation = Orientation.Vertical
                        ),
                    shape = RoundedCornerShape(animatedCorner),
                    color = animatedArtworkColor, // A cor extraída da capa
                    tonalElevation = 0.dp
                ) {
                    SharedPlayerScreen(
                        expandProgress = expandProgress,
                        uiState = playerUiState,
                        backgroundColor = animatedArtworkColor,
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onSkipToNextChapter = { playerViewModel.skipToNextChapter() },
                        onSkipToPreviousChapter = { playerViewModel.skipToPreviousChapter() },
                        onRewind = { playerViewModel.rewind() },
                        onFastForward = { playerViewModel.fastForward() },
                        onSetSleepTimer = { minutes -> playerViewModel.setSleepTimer(minutes) },
                        onSeek = { playerViewModel.seekTo(it) },
                        onShare = {
                            ShareUtils.shareCurrentContent(
                                context = context,
                                title = playerUiState.title,
                                subtitle = playerUiState.subtitle
                            )
                        },
                        onSetSpeed = { speed -> playerViewModel.setPlaybackSpeed(speed) },
                        onChapterSelect = { index -> playerViewModel.onChapterSelected(index) },
                        onToggleFavorite = { playerViewModel.toggleFavorite() },
                        onCollapse = { isPlayerExpanded = false },
                        onOpen = { isPlayerExpanded = true },
                        onConsumePlaybackError = { playerViewModel.consumePlaybackError() }
                    )
                }
            }
        }
    }
}


@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    name = "Degradê Claro (Sobre Conteúdo)"
)
@Composable
fun StatusBarGradientLightPreview() {
    val mockStatusBarPadding = 24.dp
    val baseColor = CreamBackground

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp) // Altura maior para ver o efeito
    ) {
        // 1. Simulando as capas dos livros rolando por baixo
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .background(Color.Gray)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .background(Color.DarkGray)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .background(Color.LightGray)
            )
        }

        // 2. O seu degradê passando por cima de tudo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mockStatusBarPadding + 8.dp)
                .height(mockStatusBarPadding)
                .background(
                    Brush.verticalGradient(
                        0.0f to baseColor.copy(alpha = 1f),
                        0.1f to baseColor.copy(alpha = 0.8f),
                        0.8f to baseColor.copy(alpha = 0.0f),
                    )
                )
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    name = "Degradê Escuro (Sobre Conteúdo)"
)
@Composable
fun StatusBarGradientDarkPreview() {
    val mockStatusBarPadding = 24.dp
    val baseColor = DeepBlueDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        // 1. Simulando as capas dos livros rolando por baixo
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .background(Color.Gray)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .background(Color.DarkGray)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .background(Color.LightGray)
            )
        }

        // 2. O seu degradê passando por cima
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mockStatusBarPadding + 8.dp)
                .background(
                    Brush.verticalGradient(
                        0.0f to baseColor.copy(alpha = 1f),
                        0.1f to baseColor.copy(alpha = 0.8f),
                        0.8f to baseColor.copy(alpha = 0.0f),
                    )
                )
        )
    }
}