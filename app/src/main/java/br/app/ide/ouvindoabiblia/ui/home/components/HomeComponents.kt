package br.app.ide.ouvindoabiblia.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.app.ide.ouvindoabiblia.ui.home.BookSummary
import br.app.ide.ouvindoabiblia.ui.home.TestamentFilter
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar() {
    CenterAlignedTopAppBar(
        title = { Text("Ouvindo a Bíblia", style = MaterialTheme.typography.headlineMedium) }
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Tentar Novamente") }
    }
}

// --- COMPONENTE OTIMIZADO: Continuar Ouvindo ---
@Composable
fun ContinueListeningCard(book: BookSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Capa do Livro Pequena (Otimizada)
        Box(
            modifier = Modifier
                .width(50.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val context = LocalContext.current
            // Cache da Request para performance
            val model = remember(book.imageUrl) {
                ImageRequest.Builder(context)
                    .data(book.imageUrl)
                    .size(150, 150)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false)
                    .build()
            }

            if (book.imageUrl != null) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = ColorPainter(Color.Gray),
                    placeholder = ColorPainter(Color.Transparent)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Continuar de onde parou", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// --- COMPONENTE OTIMIZADO: Item Favorito (Avatar Circular) ---
@Composable
fun FavoriteBookItem(book: BookSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp) // Largura fixa
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar Circular
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val context = LocalContext.current
            val model = remember(book.imageUrl) {
                ImageRequest.Builder(context)
                    .data(book.imageUrl)
                    .size(200, 200)
                    .precision(Precision.INEXACT)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false)
                    .build()
            }

            if (book.imageUrl == null) {
                Box(Modifier
                    .fillMaxSize()
                    .background(Color.LightGray))
            } else {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = ColorPainter(Color.Gray),
                    placeholder = ColorPainter(Color.Transparent)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = book.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestamentFilterRow(selected: TestamentFilter, onSelect: (TestamentFilter) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TestamentFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.name) }
            )
        }
    }
}