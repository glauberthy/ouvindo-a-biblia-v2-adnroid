package br.app.ide.ouvindoabiblia.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.SlateBlue

@Composable
fun BookFilterBar(
    selectedOption: String, // "ALL", "AT", "NT"
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Container Principal (A Cápsula)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp) // Altura confortável para o toque
            .border(
                width = 1.dp,
                color = SlateBlue.copy(alpha = 0.3f), // Borda sutil
                shape = RoundedCornerShape(50) // Totalmente redondo
            )
            .clip(RoundedCornerShape(50))
            .background(Color.Transparent), // Fundo transparente para mostrar o Creme da tela
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Opção 1: Todos
        FilterSegment(
            text = "Todos",
            isSelected = selectedOption == "ALL",
            onClick = { onOptionSelected("ALL") },
            modifier = Modifier.weight(1f)
        )

        // Divisor Vertical Fino
        VerticalDivider()

        // Opção 2: Antigo
        FilterSegment(
            text = "Antigo",
            isSelected = selectedOption == "AT",
            onClick = { onOptionSelected("AT") },
            modifier = Modifier.weight(1f)
        )

        // Divisor Vertical Fino
        VerticalDivider()

        // Opção 3: Novo
        FilterSegment(
            text = "Novo",
            isSelected = selectedOption == "NT",
            onClick = { onOptionSelected("NT") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FilterSegment(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animação suave de cor ao trocar
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) DeepBlueDark else Color.Transparent,
        animationSpec = tween(300),
        label = "BgColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) CreamBackground else SlateBlue,
        animationSpec = tween(300),
        label = "TextColor"
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
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight(0.6f) // O divisor não vai até a borda (estético)
            .background(SlateBlue.copy(alpha = 0.2f))
    )
}