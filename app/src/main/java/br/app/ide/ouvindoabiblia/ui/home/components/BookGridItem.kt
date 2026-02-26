package br.app.ide.ouvindoabiblia.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.app.ide.ouvindoabiblia.ui.components.AppAsyncImage
import br.app.ide.ouvindoabiblia.ui.home.BookSummary
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark

@Composable
fun BookGridItem(
    book: BookSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Estrutura em Coluna para separar Imagem de Texto
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // SINALIZAÇÃO: Trocamos o bloco gigante do ImageRequest + Box + AsyncImage por isso!
        // O AppAsyncImage já resolve o layout 2:3, o fundo, o loading e o erro nativamente.
        AppAsyncImage(
            imageUrl = book.imageUrl,
            contentDescription = "Capa do livro ${book.title}",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        // TEXTO DO NOME DO LIVRO (Mantido Intacto)
        Text(
            text = book.title,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = DeepBlueDark,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}