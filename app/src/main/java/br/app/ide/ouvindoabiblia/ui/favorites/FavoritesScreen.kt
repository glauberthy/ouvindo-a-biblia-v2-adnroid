package br.app.ide.ouvindoabiblia.ui.favorites

// Importando sua paleta de cores

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.app.ide.ouvindoabiblia.ui.theme.AppColors
import br.app.ide.ouvindoabiblia.data.repository.domain.model.Chapter
import br.app.ide.ouvindoabiblia.data.repository.domain.model.FavoriteLesson
import br.app.ide.ouvindoabiblia.ui.components.AppAsyncImage
import br.app.ide.ouvindoabiblia.ui.theme.Accent2
import br.app.ide.ouvindoabiblia.ui.theme.BadgeNewTestament
import br.app.ide.ouvindoabiblia.ui.theme.BadgeOldTestament

@Composable
fun FavoritesScreen(
    onPlayChapter: (Int, String, String, Int) -> Unit,
    onPlayStudy: (Int, String, String, Int) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    FavoritesScreenContent(
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onPlayChapter = onPlayChapter,
        onPlayStudy = onPlayStudy,
        onRemoveChapter = { viewModel.handle(FavoritesIntent.RemoveChapter(it)) },
        onRemoveStudy = { studyId, lessonId ->
            viewModel.handle(FavoritesIntent.RemoveStudy(studyId, lessonId))
        }
    )
}

@Composable
fun FavoritesScreenContent(
    uiState: FavoritesUiState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onPlayChapter: (Int, String, String, Int) -> Unit,
    onPlayStudy: (Int, String, String, Int) -> Unit,
    onRemoveChapter: (Long) -> Unit,
    onRemoveStudy: (Int, Int) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Só reserva o espaço da mini-player/nav quando há conteúdo carregado.
    val resolvedBottomPadding =
        if (uiState is FavoritesUiState.Success) navBarPadding + 80.dp else 0.dp

    val subtitle = if (selectedTab == 0) {
        "Sua coleção de capítulos bíblicos"
    } else {
        "Suas lições de estudos favoritas"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 24.dp,
            bottom = resolvedBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    color = AppColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            FavoritesSegmentedSelector(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }

        // Header + seletor ficam sempre visíveis; só a região de conteúdo troca (LCE).
        when (uiState) {
            is FavoritesUiState.Loading -> item {
                Box(
                    modifier = Modifier
                        .fillParentMaxHeight(0.6f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.textPrimary)
                }
            }

            is FavoritesUiState.Error -> item {
                Box(
                    modifier = Modifier
                        .fillParentMaxHeight(0.6f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is FavoritesUiState.Success -> {
                if (selectedTab == 0) {
                    if (uiState.bibleFavorites.isEmpty()) {
                        item { EmptyFavorites("capítulos bíblicos") }
                    } else {
                        renderBibleFavorites(
                            favorites = uiState.bibleFavorites,
                            onPlayChapter = onPlayChapter,
                            onRemove = onRemoveChapter
                        )
                    }
                } else {
                    if (uiState.studyFavorites.isEmpty()) {
                        item { EmptyFavorites("lições de estudos") }
                    } else {
                        renderStudyFavorites(
                            favorites = uiState.studyFavorites,
                            onPlayStudy = onPlayStudy,
                            onRemove = onRemoveStudy
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesSegmentedSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(
                width = 1.dp,
                color = AppColors.textSecondary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(50)
            )
            .clip(RoundedCornerShape(50))
            .background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FavoriteFilterSegment(
            text = "Livros",
            isSelected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            modifier = Modifier.weight(1f)
        )

        FavoriteVerticalDivider()

        FavoriteFilterSegment(
            text = "Estudos",
            isSelected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FavoriteFilterSegment(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.textPrimary else Color.Transparent,
        animationSpec = tween(300),
        label = "FavoriteFilterBg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.background else AppColors.textSecondary,
        animationSpec = tween(300),
        label = "FavoriteFilterText"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun FavoriteVerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight(0.6f)
            .background(AppColors.textSecondary.copy(alpha = 0.2f))
    )
}

@Composable
fun ChapterListItem(number: Int, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            // vertical 4dp (era 12): com o IconButton de volta aos 48dp, isto mantém a
            // linha em ~56dp, que é a altura de list item de uma linha do M3.
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = AppColors.textSecondary // Ícone discreto
            )
            Text(
                text = "Capítulo $number",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.textPrimary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        // O IconButton fica no tamanho padrão (48dp): forçar Modifier.size(24.dp) aqui
        // deixava a ação DESTRUTIVA (desfavoritar) com metade do alvo mínimo de toque.
        // O tamanho visual do coração continua 20dp, no Icon de dentro.
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = "Remover dos favoritos",
                tint = Accent2,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesTabSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Livros", "Estudos")

    SecondaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = AppColors.textPrimary,
        divider = {},
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(
                    selectedTabIndex = selectedTab,
                    matchContentSize = true
                ),
                color = AppColors.textPrimary,
                height = 3.dp
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index

            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                selectedContentColor = AppColors.textPrimary,
                unselectedContentColor = AppColors.textSecondary.copy(alpha = 0.72f),
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            )
        }
    }
}

fun LazyListScope.renderBibleFavorites(
    favorites: List<Chapter>,
    onPlayChapter: (Int, String, String, Int) -> Unit,
    onRemove: (Long) -> Unit
) {
    val grouped = favorites.groupBy { it.bookName }

    grouped.forEach { (_, chapters) ->
        item {
            val info = chapters.first()

            FavoritesGroupCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppAsyncImage(
                        imageUrl = info.coverUrl,
                        contentDescription = info.bookName,
                        modifier = Modifier
                            .width(60.dp)
                            .height(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.outline)
                    )

                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Surface(
                            // Tokens próprios (invariantes ao tema): AppColors.outline/AppColors.textSecondary
                            // davam 2,10:1 e 3,19:1 com o texto branco por cima.
                            color = if (info.testament == "at") BadgeOldTestament else BadgeNewTestament,
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

                        // Respiro entre o selo e o título: sem isto o acento do "Ê" de
                        // "Êxodo" quase toca o selo.
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = info.bookName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.textPrimary
                        )

                        Text(
                            text = "${info.totalChapters} capítulos",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    color = AppColors.outline.copy(alpha = 0.35f)
                )

                chapters.forEach { item ->
                    ChapterListItem(
                        number = item.number,
                        onClick = {
                            val index = (item.number - 1).coerceAtLeast(0)
                            onPlayChapter(
                                item.bookId,
                                item.bookName,
                                item.coverUrl ?: "",
                                index
                            )
                        },
                        onRemove = { onRemove(item.id) }
                    )
                }
            }
        }
    }
}

fun LazyListScope.renderStudyFavorites(
    favorites: List<FavoriteLesson>,
    onPlayStudy: (Int, String, String, Int) -> Unit,
    onRemove: (Int, Int) -> Unit
) {
    val grouped = favorites.groupBy { it.studyTitle }

    grouped.forEach { (studyTitle, lessons) ->
        item {
            val info = lessons.first()

            FavoritesGroupCard {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    // Mesmo tamanho e mesmo recorte da aba Estudos (75dp,
                    // RoundedCornerShape(16.dp)) — era o único lugar do app com capa
                    // circular. Duas razões para sair do círculo:
                    //  - o recorte cortava as PONTAS da arte, e é ali que essas capas põem
                    //    o nome do expositor e o logo (ficavam ilegíveis);
                    //  - o "anel" dourado não era uma borda desenhada: era o fundo Accent
                    //    do Surface aparecendo pela folga de padding(3.dp).
                    Surface(
                        modifier = Modifier.size(75.dp),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        AppAsyncImage(
                            imageUrl = info.studyCoverUrl,
                            contentDescription = studyTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = studyTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            // 2 linhas é TETO, não truncamento por estética: título em 2
                            // linhas (~48dp) + autor (~20dp) ainda cabe na altura da capa
                            // (75dp), que é quem define a altura do card — então a 2ª linha
                            // é de graça e não desalinha a lista. Truncar em 1 linha só
                            // perderia informação ("Ester - Vida Cristã em Cultura Pagã",
                            // 35 caracteres, é o único dos 4 estudos que passa de uma).
                            // O teto existe para um título futuro de 3+ linhas não estourar
                            // a capa.
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = info.studyAuthor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.W400,
                            color = AppColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(
                    color = AppColors.outline.copy(alpha = 0.35f)
                )

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
            // vertical 4dp (era 12): com o IconButton de volta aos 48dp, isto mantém a
            // linha em ~56dp, que é a altura de list item de uma linha do M3.
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = AppColors.textSecondary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.textPrimary,
                modifier = Modifier.padding(start = 12.dp),
                maxLines = 1, // Evita que o texto quebre a linha e empurre o coração
                // Sem elipse o título cortava no meio da palavra ("O Maior De Todos Do
                // Oriente (Jó"), o que parece texto perdido em vez de texto abreviado.
                // Só aparece aqui: na aba Livros a linha é sempre "Capítulo N".
                overflow = TextOverflow.Ellipsis
            )
        }
        // O IconButton fica no tamanho padrão (48dp): forçar Modifier.size(24.dp) aqui
        // deixava a ação DESTRUTIVA (desfavoritar) com metade do alvo mínimo de toque.
        // O tamanho visual do coração continua 20dp, no Icon de dentro.
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = "Remover dos favoritos",
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
            tint = AppColors.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sua lista de $itemType está vazia", // Texto dinâmico
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.textSecondary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Marque itens como favoritos para\nouvi-los novamente com facilidade.",
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FavoritesGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.card
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = AppColors.outline.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}