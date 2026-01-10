package br.app.ide.ouvindoabiblia.ui.home.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import br.app.ide.ouvindoabiblia.R
import br.app.ide.ouvindoabiblia.ui.home.BookSummary
import coil.compose.AsyncImage
import coil.request.ImageRequest


@Composable
fun BookGridItem(
    book: BookSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = MaterialTheme.shapes.medium,
        // Dica Pro: Desligar a sombra padrão em listas longas ajuda a GPU
        // elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        // Mas se quiser manter a sombra, tudo bem, o impacto maior é a imagem.
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(book.imageUrl)
                .crossfade(true)
                // 400px é suficiente para um grid de 3 colunas em telas HD/FullHD
                .size(400, 400)
                // Permite usar uma imagem um pouco maior/menor se já estiver em cache
                .precision(coil.size.Precision.INEXACT)
                // Removemos o listener de LOG (Debug pesa no scroll)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .build(),
            contentDescription = "Capa de ${book.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            // Dica Pro 2: FilterQuality.Low é mais rápido e imperceptível em imagens pequenas
            filterQuality = FilterQuality.Low
        )
    }
}