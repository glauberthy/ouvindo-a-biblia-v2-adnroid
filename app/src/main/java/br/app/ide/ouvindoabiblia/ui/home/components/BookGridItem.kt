package br.app.ide.ouvindoabiblia.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import br.app.ide.ouvindoabiblia.ui.components.AppAsyncImage
import br.app.ide.ouvindoabiblia.ui.home.BookSummary
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark

// Piso da redução automática do título (ver BookTitle). Abaixo disto o nome fica
// pequeno demais em relação aos vizinhos e é melhor truncar com elipse.
private val MIN_TITLE_FONT_SIZE = 10.sp

// Passo de redução por tentativa. Suave o suficiente para não "pular" tamanhos.
private const val TITLE_SHRINK_STEP = 0.94f

@Composable
fun BookGridItem(
    book: BookSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // O item TODO continua clicável (inclusive o texto — área de toque maior), mas o
    // feedback visual é desenhado SÓ na capa: a mesma MutableInteractionSource é
    // compartilhada entre o clickable (sem indication) e o indication() da imagem.
    // Antes o ripple do clickable cobria a Column inteira e pintava o fundo do texto.
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppAsyncImage(
            imageUrl = book.imageUrl,
            contentDescription = "Capa do livro ${book.title}",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                // Depois do clip(): o ripple é desenhado por cima da imagem e recortado
                // pelos cantos arredondados, expandindo em círculo a partir do toque.
                .indication(interactionSource, ripple())
        )

        BookTitle(title = book.title)
    }
}

/**
 * Título do livro em UMA linha, sempre.
 *
 * Se o nome não couber na largura da coluna, a fonte é reduzida em passos até caber
 * (piso em [MIN_TITLE_FONT_SIZE]); a elipse fica como último recurso. Dos 66 livros só
 * "1 Tessalonicenses" e "2 Tessalonicenses" (17 caracteres) precisam da redução — os
 * demais param em 12. A redução também absorve fonte grande do sistema e as colunas
 * mais estreitas de outros WindowSizeClass.
 *
 * Feito à mão porque o `autoSize` nativo do Text só existe a partir do Compose 1.8 e o
 * BOM aqui é 2024.12.01 (1.7.6).
 */
@Composable
private fun BookTitle(title: String) {
    val baseStyle = MaterialTheme.typography.labelLarge
    var style by remember(title, baseStyle) { mutableStateOf(baseStyle) }
    // Só desenha depois de saber que o tamanho cabe — evita o flash de um frame com a
    // fonte grande antes do encolhimento.
    var fits by remember(title, baseStyle) { mutableStateOf(false) }

    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .drawWithContent { if (fits) drawContent() },
        style = style,
        color = DeepBlueDark,
        textAlign = TextAlign.Center,
        softWrap = false,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result ->
            val size = style.fontSize
            if (result.didOverflowWidth && !size.isUnspecified && size > MIN_TITLE_FONT_SIZE) {
                style = style.copy(fontSize = size * TITLE_SHRINK_STEP)
            } else {
                fits = true
            }
        }
    )
}
