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
import br.app.ide.ouvindoabiblia.ui.theme.AppColors

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
 * Curva: smootherstep (quíntica, 11 stops) do pico 0.72 até 0 — ainda mais plana nas
 * pontas que a smoothstep, sem as "dobras" de um gradiente de poucos stops lineares.
 * Com alpha 0.72 sobre imagem PRETA, o fundo atrás dos ícones fica com luminância
 * ~168 (creme) — ícones escuros legíveis; sobre imagem clara o véu desaparece.
 */
@Composable
fun StatusBarScrim(modifier: Modifier = Modifier) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Acompanha o fundo da página: no tema claro o véu é creme e protege ícones
    // ESCUROS; no escuro ele precisa ser escuro, porque lá os ícones da status bar
    // são claros. Fixo em creme, sobrava uma faixa clara no topo do tema escuro.
    val scrimColor = AppColors.background

    // alpha(t) = PICO × (1 − smootherstep(t)) — 1ª e 2ª derivadas zero nas pontas,
    // então o véu "nasce" e "morre" sem nenhum degrau perceptível.
    val stops = (0..STOP_COUNT).map { i ->
        val t = i / STOP_COUNT.toFloat()
        val eased = t * t * t * (t * (t * 6f - 15f) + 10f) // smootherstep
        t to scrimColor.copy(alpha = PEAK_ALPHA * (1f - eased))
    }.toTypedArray()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight + FADE_TAIL)
            .background(Brush.verticalGradient(*stops))
    )
}

private const val PEAK_ALPHA = 0.72f
private const val STOP_COUNT = 10
private val FADE_TAIL = 56.dp
