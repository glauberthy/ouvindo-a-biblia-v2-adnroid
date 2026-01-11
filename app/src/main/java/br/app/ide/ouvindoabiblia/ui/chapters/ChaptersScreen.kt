package br.app.ide.ouvindoabiblia.ui.chapters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersScreen(
    onBackClick: () -> Unit,
    
    onNavigateToPlayer: (Int, String) -> Unit,
    viewModel: ChaptersViewModel = hiltViewModel()
) {
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.bookName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(chapters, key = { it.id }) { chapter ->
                ChapterItem(
                    number = chapter.number,
                    onClick = {
                        // Toca o áudio
                        viewModel.playChapter(chapter)
                        onNavigateToPlayer(
                            chapter.number,
                            chapter.coverUrl ?: ""
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ChapterItem(number: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}