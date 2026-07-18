package br.app.ide.ouvindoabiblia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground

/**
 * Scrim de proteção da status bar sobre imagens edge-to-edge (headers de Tema/Estudo).
 *
 * Por quê: os ícones da status bar nas telas de detalhe são ESCUROS (decisão global no
 * MainScreen, pensada para o CreamBackground) — sobre imagem escura eles somem. A
 * proteção correta é um véu CLARO, curto e com curva suave: forte apenas na faixa dos
 * ícones (altura real da status bar, que o app conhece e a imagem não) e morrendo ~24dp
 * abaixo dela. Substitui o degradê que era assado nas próprias imagens do servidor —
 * imagens ficam limpas (sem mancha nos thumbs/cards) e o ajuste fino mora aqui, num
 * lugar só, valendo para QUALQUER imagem.
 *
 * Curva (3 stops, easing aproximado): 0.85 → 0.35 → 0. Com alpha 0.85 sobre imagem
 * PRETA, o fundo atrás dos ícones fica com luminância ~200 (creme) — ícones escuros
 * sempre legíveis; sobre imagem clara o véu praticamente desaparece.
 */
@Composable
fun StatusBarScrim(modifier: Modifier = Modifier) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight + 24.dp)
            .background(
                Brush.verticalGradient(
                    0f to CreamBackground.copy(alpha = 0.85f),
                    0.55f to CreamBackground.copy(alpha = 0.35f),
                    1f to Color.Transparent
                )
            )
    )
}
