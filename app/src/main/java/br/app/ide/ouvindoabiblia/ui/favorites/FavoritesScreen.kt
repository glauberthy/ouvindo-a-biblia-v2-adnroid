package br.app.ide.ouvindoabiblia.ui.favorites

// Importando sua paleta de cores
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.app.ide.ouvindoabiblia.data.local.model.FavoriteStudyLessonDto
import br.app.ide.ouvindoabiblia.ui.components.AppAsyncImage
import br.app.ide.ouvindoabiblia.ui.theme.Accent2
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray
import br.app.ide.ouvindoabiblia.ui.theme.RosyBeige
import br.app.ide.ouvindoabiblia.ui.theme.SlateBlue
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    onPlayChapter: (Int, String, String, Int) -> Unit,
    onPlayStudy: (Int, String, String, Int) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 1. Criamos o estado do Pager para 2 páginas (Bíblia e Estudos)
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
    // Corrotina necessária para animar o clique da aba
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    FavoritesScreenContent(
        uiState = uiState,
        // O selectedTab agora vem do pagerState
        selectedTab = pagerState.currentPage,
        onTabSelected = { index ->
            // Ao clicar, o pager desliza suavemente até a página
            scope.launch { pagerState.animateScrollToPage(index) }
        },
        // Passamos o pagerState para o conteúdo
        pagerState = pagerState,
        onPlayChapter = onPlayChapter,
        onPlayStudy = onPlayStudy,
        onRemoveChapter = { viewModel.removeFromFavorites(it) },
        onRemoveStudy = { studyId, lessonId ->
            viewModel.removeStudyFromFavorites(
                studyId,
                lessonId
            )
        }
    )
}

@Composable
fun FavoritesScreenContent(
    uiState: FavoritesUiState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState, // Recebe o pagerState
    onPlayChapter: (Int, String, String, Int) -> Unit,
    onPlayStudy: (Int, String, String, Int) -> Unit,
    onRemoveChapter: (Long) -> Unit,
    onRemoveStudy: (Int, Int) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val resolvedBottomPadding = if (uiState.isLoading) 0.dp else navBarPadding + 80.dp

    // O HorizontalPager agora é o dono do conteúdo horizontal
    androidx.compose.foundation.pager.HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground),
        verticalAlignment = Alignment.Top
    ) { pageIndex ->

        // --- ADIÇÃO DO LIMIAR DE SEGURANÇA (Threshold) ---
        // 1. Calculamos a distância bruta como antes
        val rawOffset = remember(pagerState) {
            val distance =
                (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
            kotlin.math.abs(distance)
        }

        // 2. Aplicamos um limiar. Se a distância for muito pequena, forçamos o valor 0.
        // Isso impede que float point inaccuracies deixem o alpha "grudado".
        val pageOffsetForLerp = if (rawOffset < 0.05f) { // Pequeno limiar de segurança
            0f
        } else {
            rawOffset.coerceIn(0f, 1f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // --- EFEITO FADE (Opacidade) CORRIGIDO ---
                    // Agora usamos pageOffsetForLerp, que garante ser 0f quando assentado.
//                    alpha = androidx.compose.ui.util.lerp(
//                        start = 1f,   // Opaque (100%) quando no centro
//                        stop = 1f, // Faint (35%) quando sai da tela
//                        fraction = pageOffsetForLerp
//                    )

//                    // Efeito Scale (permanece o mesmo, mas agora mais fluido)
//                    val scale = androidx.compose.ui.util.lerp(
//                        start = 1f,
//                        stop = 0.90f,
//                        fraction = pageOffsetForLerp
//                    )
//                    scaleX = scale
//                    scaleY = scale
                }
        ) {
            // O seu conteúdo LazyColumn antigo fica aqui dentro do Box com efeitos
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 24.dp, bottom = resolvedBottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ... Todo o conteúdo interno da LazyColumn (Título, Tabs, Listas) permanece igual
                item {
                    Column(
                        modifier = Modifier.padding(
                            top = statusBarPadding + 8.dp,
                            bottom = 8.dp
                        )
                    ) {
                        Text(
                            text = "Meus Favoritos",
                            style = MaterialTheme.typography.headlineLarge,
                            color = DeepBlueDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (pageIndex == 0) "Sua coleção de capítulos bíblicos" else "Suas lições de estudos favoritas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateBlue
                        )
                    }
                }
                item {
                    FavoritesTabSelector(
                        selectedTab = pageIndex,
                        onTabSelected = onTabSelected
                    )
                }
                if (uiState.isLoading) {
                    item {
                        Box(
                            Modifier
                                .fillParentMaxHeight(0.6f)
                                .fillMaxWidth(),
                            Alignment.Center
                        ) { CircularProgressIndicator(color = DeepBlueDark) }
                    }
                } else {
                    if (pageIndex == 0) {
                        if (uiState.bibleFavorites.isEmpty()) item { EmptyFavorites("capítulos bíblicos") }
                        else renderBibleFavorites(
                            uiState.bibleFavorites,
                            onPlayChapter,
                            onRemoveChapter
                        )
                    } else {
                        if (uiState.studyFavorites.isEmpty()) item { EmptyFavorites("lições de estudos") }
                        else renderStudyFavorites(
                            uiState.studyFavorites,
                            onPlayStudy,
                            onRemoveStudy
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterListItem(number: Int, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = SlateBlue // Ícone discreto
            )
            Text(
                text = "Capítulo $number",
                style = MaterialTheme.typography.bodyLarge,
                color = DeepBlueDark,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Accent2,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
fun FavoritesTabSelector(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RosyBeige.copy(alpha = 0.2f))
            .padding(4.dp)
    ) {
        val tabs = listOf("Bíblia", "Estudos")
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) DeepBlueDark else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else DeepBlueDark,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

fun LazyListScope.renderBibleFavorites(
    favorites: List<br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo>,
    onPlayChapter: (Int, String, String, Int) -> Unit,
    onRemove: (Long) -> Unit
) {
    val grouped = favorites.groupBy { it.bookName }

    grouped.forEach { (_, chapters) ->
        item {
            val info = chapters.first()
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppAsyncImage(
                            imageUrl = info.coverUrl,
                            contentDescription = info.bookName,
                            modifier = Modifier
                                .width(60.dp)
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RosyBeige)
                        )
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Surface(
                                color = if (info.testament == "at") RosyBeige else LavenderGray,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (info.testament == "at") "AT" else "NT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = info.bookName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepBlueDark
                            )
                            Text(
                                text = "${info.totalChapters} capítulos",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateBlue.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = CreamBackground)

                    chapters.forEach { item ->
                        ChapterListItem(
                            number = item.chapter.number,
                            onClick = {
                                val index = (item.chapter.number - 1).coerceAtLeast(0)
                                onPlayChapter(
                                    item.chapter.bookId,
                                    item.bookName,
                                    item.coverUrl ?: "",
                                    index
                                )
                            },
                            onRemove = { onRemove(item.chapter.id) }
                        )
                    }
                }
            }
        }
    }
}

fun LazyListScope.renderStudyFavorites(
    favorites: List<FavoriteStudyLessonDto>,
    onPlayStudy: (Int, String, String, Int) -> Unit,
    onRemove: (Int, Int) -> Unit
) {
    val grouped = favorites.groupBy { it.studyTitle }

    grouped.forEach { (studyTitle, lessons) ->
        item {
            val info = lessons.first()
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppAsyncImage(
                            imageUrl = info.studyCoverUrl,
                            contentDescription = studyTitle,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = studyTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepBlueDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = info.studyAuthor,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateBlue
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = CreamBackground)

                    lessons.forEachIndexed { index, item ->
                        StudyLessonFavoriteItem(
                            title = item.lesson.title,
                            onPlay = {
                                onPlayStudy(
                                    item.lesson.studyId,
                                    item.studyTitle,
                                    item.studyCoverUrl,
                                    index
                                )
                            },
                            onRemove = { onRemove(item.lesson.studyId, item.lesson.remoteId) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun StudyLessonFavoriteItem(
    title: String,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = SlateBlue
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = DeepBlueDark,
                modifier = Modifier.padding(start = 12.dp),
                maxLines = 1 // Evita que o texto quebre a linha e empurre o coração
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Accent2,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmptyFavorites(itemType: String) { // Agora aceita o tipo de item
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = RosyBeige
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sua lista de $itemType está vazia", // Texto dinâmico
            style = MaterialTheme.typography.titleMedium,
            color = SlateBlue,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Marque itens como favoritos para\nouvi-los novamente com facilidade.",
            color = LavenderGray,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}