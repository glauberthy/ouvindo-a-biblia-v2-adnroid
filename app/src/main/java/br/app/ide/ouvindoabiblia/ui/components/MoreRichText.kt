package br.app.ide.ouvindoabiblia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.app.ide.ouvindoabiblia.ui.theme.Accent


/*
*
* Se você quiser, no próximo passo eu posso te entregar uma versão 2 com suporte também a:
títulos (#, ##)
quebra de linha explícita
links clicáveis
versículos destacados com estilo próprio
*
*
* */
@Composable
fun RichTextContent(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        lineHeight = 28.sp
    ),
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    paragraphSpacing: Dp = 16.dp,
    quoteBarColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
    quoteTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    quoteBackgroundColor: Color = Accent.copy(alpha = 0.16f)
) {
    val blocks = remember(text) {
        parseRichTextBlocks(text)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(paragraphSpacing)
    ) {
        blocks.forEach { block ->
            when (block) {
                is RichTextBlock.Paragraph -> {
                    Text(
                        text = remember(block.text) { buildRichAnnotatedString(block.text) },
                        style = textStyle,
                        color = textColor
                    )
                }

                is RichTextBlock.BulletList -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        block.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "•",
                                    style = textStyle,
                                    color = textColor,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = remember(item) { buildRichAnnotatedString(item) },
                                    style = textStyle,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                is RichTextBlock.OrderedList -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        block.items.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = textStyle,
                                    color = textColor,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = remember(item) { buildRichAnnotatedString(item) },
                                    style = textStyle,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                is RichTextBlock.Quote -> {
                    Text(
                        text = remember(block.text) { buildRichAnnotatedString(block.text) },
                        style = textStyle,
                        color = quoteTextColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = quoteBackgroundColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )

                }
            }
        }
    }
}

private sealed interface RichTextBlock {
    data class Paragraph(val text: String) : RichTextBlock
    data class BulletList(val items: List<String>) : RichTextBlock
    data class OrderedList(val items: List<String>) : RichTextBlock
    data class Quote(val text: String) : RichTextBlock
}

private fun parseRichTextBlocks(rawText: String): List<RichTextBlock> {
    val text = rawText
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .trim()

    if (text.isBlank()) return listOf(RichTextBlock.Paragraph(""))

    val lines = text.split("\n")
    val blocks = mutableListOf<RichTextBlock>()

    val paragraphBuffer = mutableListOf<String>()
    val bulletBuffer = mutableListOf<String>()
    val orderedBuffer = mutableListOf<String>()
    val quoteBuffer = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphBuffer.isNotEmpty()) {
            blocks.add(
                RichTextBlock.Paragraph(
                    paragraphBuffer.joinToString(" ").trim()
                )
            )
            paragraphBuffer.clear()
        }
    }

    fun flushBulletList() {
        if (bulletBuffer.isNotEmpty()) {
            blocks.add(RichTextBlock.BulletList(bulletBuffer.toList()))
            bulletBuffer.clear()
        }
    }

    fun flushOrderedList() {
        if (orderedBuffer.isNotEmpty()) {
            blocks.add(RichTextBlock.OrderedList(orderedBuffer.toList()))
            orderedBuffer.clear()
        }
    }

    fun flushQuote() {
        if (quoteBuffer.isNotEmpty()) {
            blocks.add(
                RichTextBlock.Quote(
                    quoteBuffer.joinToString("\n").trim()
                )
            )
            quoteBuffer.clear()
        }
    }

    fun flushAll() {
        flushParagraph()
        flushBulletList()
        flushOrderedList()
        flushQuote()
    }

    val bulletRegex = Regex("""^\s*[-*]\s+(.*)$""")
    val orderedRegex = Regex("""^\s*\d+\.\s+(.*)$""")
    val quoteRegex = Regex("""^\s*>\s?(.*)$""")

    lines.forEach { rawLine ->
        val line = rawLine.trimEnd()

        if (line.isBlank()) {
            flushAll()
            return@forEach
        }

        val bulletMatch = bulletRegex.find(line)
        if (bulletMatch != null) {
            flushParagraph()
            flushOrderedList()
            flushQuote()
            bulletBuffer.add(bulletMatch.groupValues[1].trim())
            return@forEach
        }

        val orderedMatch = orderedRegex.find(line)
        if (orderedMatch != null) {
            flushParagraph()
            flushBulletList()
            flushQuote()
            orderedBuffer.add(orderedMatch.groupValues[1].trim())
            return@forEach
        }

        val quoteMatch = quoteRegex.find(line)
        if (quoteMatch != null) {
            flushParagraph()
            flushBulletList()
            flushOrderedList()
            quoteBuffer.add(quoteMatch.groupValues[1].trim())
            return@forEach
        }

        flushBulletList()
        flushOrderedList()
        flushQuote()
        paragraphBuffer.add(line.trim())
    }

    flushAll()

    return blocks
}

private fun buildRichAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        appendInlineStyledText(text)
    }
}

private fun AnnotatedString.Builder.appendInlineStyledText(text: String) {
    var currentIndex = 0

    while (currentIndex < text.length) {
        val nextToken = findNextInlineToken(text, currentIndex)

        if (nextToken == null) {
            append(text.substring(currentIndex))
            break
        }

        if (nextToken.startIndex > currentIndex) {
            append(text.substring(currentIndex, nextToken.startIndex))
        }

        val contentStart = nextToken.startIndex + nextToken.delimiter.length
        val closingIndex = text.indexOf(nextToken.delimiter, contentStart)

        if (closingIndex == -1) {
            append(text.substring(nextToken.startIndex))
            break
        }

        val innerText = text.substring(contentStart, closingIndex)

        pushStyle(styleForDelimiter(nextToken.delimiter))
        appendInlineStyledText(innerText)
        pop()

        currentIndex = closingIndex + nextToken.delimiter.length
    }
}

private data class InlineToken(
    val delimiter: String,
    val startIndex: Int
)

private fun findNextInlineToken(text: String, fromIndex: Int): InlineToken? {
    val delimiters = listOf("**", "__", "`", "*", "_")

    var bestToken: InlineToken? = null

    delimiters.forEach { delimiter ->
        val start = text.indexOf(delimiter, fromIndex)
        if (start == -1) return@forEach

        val closing = text.indexOf(delimiter, start + delimiter.length)
        if (closing == -1) return@forEach

        if (bestToken == null) {
            bestToken = InlineToken(delimiter, start)
        } else {
            val currentBest = bestToken!!
            if (
                start < currentBest.startIndex ||
                (start == currentBest.startIndex && delimiter.length > currentBest.delimiter.length)
            ) {
                bestToken = InlineToken(delimiter, start)
            }
        }
    }

    return bestToken
}

private fun styleForDelimiter(delimiter: String): SpanStyle {
    return when (delimiter) {
        "**", "__" -> SpanStyle(fontWeight = FontWeight.Bold)
        "*", "_" -> SpanStyle(fontStyle = FontStyle.Italic)
        "`" -> SpanStyle(fontFamily = FontFamily.Monospace)
        else -> SpanStyle()
    }
}