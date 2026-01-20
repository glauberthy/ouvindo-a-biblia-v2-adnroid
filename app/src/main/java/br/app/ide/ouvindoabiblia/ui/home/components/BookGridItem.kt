package br.app.ide.ouvindoabiblia.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.app.ide.ouvindoabiblia.ui.home.BookSummary
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision

@Composable
fun BookGridItem(
    book: BookSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // OTIMIZAÇÃO DE MEMÓRIA E CPU
    // Usamos remember para não recriar o Builder a cada frame.
    val imageRequest = remember(book.imageUrl) {
        ImageRequest.Builder(context)
            .data(book.imageUrl)
            .size(width = 400, height = 600) // Perfeito: Tamanho nativo da sua imagem
            .precision(Precision.INEXACT) // Mantém INEXACT para ser flexível em telas menores
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
//            .aspectRatio(1f)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(8.dp))
            // Usar uma cor estática é mais leve que desenhar um placeholder complexo
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        if (book.imageUrl != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null, // Null é melhor para performance em listas decorativas
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                // OTIMIZAÇÃO: ColorPainter é desenhado na CPU e é ultra leve
                // Evita carregar drawables XML complexos para placeholder/erro
                placeholder = ColorPainter(Color.Transparent),
                error = ColorPainter(Color.DarkGray)
            )
        }
    }
}