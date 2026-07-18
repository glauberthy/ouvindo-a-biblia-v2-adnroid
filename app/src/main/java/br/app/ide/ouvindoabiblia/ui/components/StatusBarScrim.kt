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
 * Curva: smoothstep (7 stops) do pico 0.80 até 0 — easing contínuo, sem as "dobras"
 * visíveis de um gradiente de poucos stops lineares. Com alpha 0.80 sobre imagem
 * PRETA, o fundo atrás dos ícones fica com luminância ~185 (creme) — ícones escuros
 * sempre legíveis; sobre imagem clara o véu praticamente desaparece.
 */
@Composable
fun StatusBarScrim(modifier: Modifier = Modifier) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // alpha(t) = PICO × (1 − smoothstep(t)) — derivada zero nas duas pontas, então o
    // véu "nasce" e "morre" sem degrau perceptível.
    val stops = (0..STOP_COUNT).map { i ->
        val t = i / STOP_COUNT.toFloat()
        val eased = t * t * (3f - 2f * t) // smoothstep
        t to CreamBackground.copy(alpha = PEAK_ALPHA * (1f - eased))
    }.toTypedArray()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight + FADE_TAIL)
            .background(Brush.verticalGradient(*stops))
    )
}

private const val PEAK_ALPHA = 0.80f
private const val STOP_COUNT = 6
private val FADE_TAIL = 36.dp
