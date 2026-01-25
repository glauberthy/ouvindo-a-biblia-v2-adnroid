package br.app.ide.ouvindoabiblia.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    accentColor: Color,
    currentMinutes: Int, // <--- 1. Recebe qual está ativo (ex: 15)
    onTimeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val disableLabel = if (currentMinutes == 0) "Desativado" else "Desativar Timer"
    val options = listOf(
        5 to "5 minutos",
        15 to "15 minutos",
        30 to "30 minutos",
        45 to "45 minutos",
        60 to "1 hora",
        0 to disableLabel
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepBlueDark,
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
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.NightlightRound,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Parar áudio em...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = Color.White.copy(alpha = 0.08f)
            )

            LazyColumn {
                items(options) { (minutes, label) ->
                    // LÓGICA DE SELEÇÃO IGUAL À DE VELOCIDADE
                    val isSelected = minutes == currentMinutes
                    val isDisableOption = minutes == 0
                    val textColor = if (isSelected) {
                        accentColor
                    } else if (isDisableOption) {
                        Color(0xFFEF5350)
                    } else {
                        Color.White
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTimeSelected(minutes)
                                onDismiss()
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween // Espalha texto e check
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )

                        // CHECKMARK SE SELECIONADO (Igual Velocidade)
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = textColor
                            )
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.05f))
                }
            }
        }
    }
}