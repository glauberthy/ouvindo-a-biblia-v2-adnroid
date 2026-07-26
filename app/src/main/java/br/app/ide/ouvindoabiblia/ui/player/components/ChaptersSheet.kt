package br.app.ide.ouvindoabiblia.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed as itemsIndexedColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.ide.ouvindoabiblia.ui.player.PlayerTimelineItem
import br.app.ide.ouvindoabiblia.ui.theme.BrandNavy
import br.app.ide.ouvindoabiblia.ui.theme.isDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersSheet(
    items: List<PlayerTimelineItem>,
    currentIndex: Int,
    accentColor: Color, // <--- 1. NOVO PARÂMETRO: A cor da capa
    onChapterClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Bíblia -> grid de números; Estudo -> lista de títulos (ISSUE 2.B).
    val numbered = items.firstOrNull()?.numbered != false
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sheetColor = BrandNavy
    val contentColor = Color.White

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.2f), MaterialTheme.shapes.extraLarge)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.width(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = if (numbered) "Escolha o Capítulo" else "Escolha a Aula",
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                thickness = DividerDefaults.Thickness, color = Color.White.copy(alpha = 0.08f)
            )

            if (numbered) {
                // BÍBLIA: grid de números.
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 64.dp),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(items) { index, item ->
                        val isSelected = index == currentIndex

                        // LÓGICA DE COR INTELIGENTE
                        // Se selecionado -> Usa a cor da capa (accentColor)
                        // Se não -> Transparente
                        val cardContainerColor =
                            if (isSelected) accentColor else Color.White.copy(alpha = 0.05f)

                        // LÓGICA DE TEXTO INTELIGENTE
                        val textColor = if (isSelected) {
                            // Se a cor da capa for escura, texto branco. Se for clara, texto preto.
                            if (accentColor.isDark()) Color.White else Color.Black
                        } else {
                            // Texto não selecionado
                            Color.White.copy(alpha = 0.9f)
                        }

                        Card(
                            onClick = {
                                onChapterClick(index)
                                onDismiss()
                            },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = cardContainerColor
                            ),
                            // Borda apenas nos não selecionados
                            border = if (!isSelected)
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = 0.1f)
                                )
                            else null,
                            modifier = Modifier.aspectRatio(1f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor // <--- Cor calculada acima
                                )
                            }
                        }
                    }
                }
            } else {
                // ESTUDO: lista de títulos de aula (não cabem num quadradinho).
                LazyColumn(
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexedColumn(items) { index, item ->
                        val isSelected = index == currentIndex
                        val cardContainerColor =
                            if (isSelected) accentColor else Color.White.copy(alpha = 0.05f)
                        val textColor = if (isSelected) {
                            if (accentColor.isDark()) Color.White else Color.Black
                        } else {
                            Color.White.copy(alpha = 0.9f)
                        }

                        Card(
                            onClick = {
                                onChapterClick(index)
                                onDismiss()
                            },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                            border = if (!isSelected)
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = 0.1f)
                                )
                            else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}