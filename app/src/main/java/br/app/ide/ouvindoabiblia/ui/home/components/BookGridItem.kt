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
    // OTIMIZAÇÃO 1: 'Box' com 'clip' é mais leve para a GPU que 'Surface' ou 'Card'
    // em listas longas, pois evita passadas extras de sombra/elevação.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)) // Borda arredondada leve
            .background(MaterialTheme.colorScheme.surfaceVariant) // Cor de fundo (placeholder)
            .clickable(onClick = onClick)
    ) {
        val context = LocalContext.current

        // OTIMIZAÇÃO CRÍTICA 2: 'remember'
        // Isso impede que o 'ImageRequest.Builder' seja recriado a cada milissegundo
        // durante a rolagem ou animação de entrada. Economiza MUITA CPU.
        val model = remember(book.imageUrl) {
            ImageRequest.Builder(context)
                .data(book.imageUrl)
                .size(300, 300) // Tamanho fixo ajuda o cache de memória
                .precision(Precision.INEXACT)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(false) // Sem animação para máxima velocidade
                .build()
        }

        if (book.imageUrl == null) {
            // Placeholder estático super leve
            Box(Modifier
                .fillMaxSize()
                .background(Color.LightGray))
        } else {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                // Fallbacks usando ColorPainter (desenhado na CPU, zero risco de resource ID)
                error = ColorPainter(Color.Gray),
                placeholder = ColorPainter(Color.Transparent) // Deixa ver o fundo do Box
            )
        }
    }
}