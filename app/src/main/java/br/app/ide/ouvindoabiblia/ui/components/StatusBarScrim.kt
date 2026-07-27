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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import br.app.ide.ouvindoabiblia.ui.theme.AppColors

/**
 * Scrim ÚNICO de proteção da status bar. Fica no `MainScreen`, sobre todas as telas.
 *
 * Por quê: os ícones da status bar são pintados pelo SISTEMA, e o app só escolhe entre
 * claros ou escuros (`isAppearanceLightStatusBars` no MainScreen) — sobre o conteúdo
 * errado (capa de livro rolando por baixo, header de Tema/Estudo edge-to-edge) eles
 * somem. A proteção correta é um véu da cor do FUNDO da página, curto e com curva suave:
 * presente na faixa dos ícones (altura real da status bar, que o app conhece e a imagem
 * não) e morrendo pouco abaixo dela. Substitui o degradê que era assado nas próprias
 * imagens do servidor — imagens ficam limpas (sem mancha nos thumbs/cards) e o ajuste
 * fino mora aqui, num lugar só, valendo para QUALQUER conteúdo.
 *
 * **Só pode existir UMA instância na árvore.** Antes havia duas empilhadas: este
 * componente (ISSUE 9.G, nos headers de Tema/Estudo) MAIS um degradê inline no
 * `MainScreen` que começava em alpha 1,0 e valia para o app todo. Somados davam
 * 14,5:1 medidos sobre a imagem mais escura do acervo — 3× o necessário — e é por isso
 * que sobre foto escura o topo virava uma faixa clara "viva". Com dois véus não existe
 * calibração possível: baixar um não muda o pior caso, que é ditado pelo outro.
 *
 * Curva: smootherstep (quíntica, 11 stops) do pico até 0 — 1ª e 2ª derivadas zero nas
 * pontas, então o véu "nasce" e "morre" sem degrau perceptível.
 *
 * **O pico está calibrado no LIMITE de legibilidade, não acima dele** — ver
 * [SCRIM_PEAK_ALPHA_LIGHT]/[SCRIM_PEAK_ALPHA_DARK] e `StatusBarScrimContrastTest`.
 *
 * @param color cor do véu. O padrão acompanha o fundo da página (creme no tema claro,
 *   quase-preto no escuro); o `MainScreen` passa [br.app.ide.ouvindoabiblia.ui.theme.BrandNavy]
 *   no modo economia de bateria, que é quando ele força ícones claros.
 */
@Composable
fun StatusBarScrim(
    modifier: Modifier = Modifier,
    color: Color = AppColors.background
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // O pico necessário NÃO é simétrico entre os temas (ver constantes). Decide pela
    // luminância do próprio véu em vez de reler o DARK_THEME_ENABLED/isSystemInDarkTheme:
    // assim a escolha acompanha automaticamente a cor recebida — inclusive a de economia
    // de bateria, onde o véu é escuro e os ícones são claros.
    val peakAlpha = if (color.luminance() < 0.5f) {
        SCRIM_PEAK_ALPHA_DARK
    } else {
        SCRIM_PEAK_ALPHA_LIGHT
    }

    val stops = (0..SCRIM_STOP_COUNT).map { i ->
        val t = i / SCRIM_STOP_COUNT.toFloat()
        t to color.copy(alpha = scrimAlphaAt(t, peakAlpha))
    }.toTypedArray()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight + SCRIM_FADE_TAIL)
            .background(Brush.verticalGradient(*stops))
    )
}

/**
 * Pico do véu no tema CLARO (véu creme, ícones escuros). Pior caso possível = imagem
 * PRETA sob os ícones: 0,52 deixa o fundo composto em luminância ~0,19, ou **4,9:1**
 * contra o glifo preto — acima do mínimo de 4,5:1 do WCAG para texto pequeno (o relógio).
 *
 * Era 0,72, que rendia 8,9:1 no pior caso teórico e **13,6:1 medido** na imagem mais
 * escura do acervo (tema "Guerra Espiritual"): o dobro do necessário, e é por isso que
 * sobre foto escura o véu aparecia como uma faixa clara "viva" em cima da imagem.
 * Abaixar até aqui corta ~28% do véu sem tocar no piso de legibilidade.
 *
 * **Não abaixe sem medir**: 0,50 já é 4,58:1 (no fio) e 0,45 cai para 3,86:1, abaixo do
 * mínimo. O `StatusBarScrimContrastTest` trava esse piso.
 */
internal const val SCRIM_PEAK_ALPHA_LIGHT = 0.52f

/**
 * Pico do véu no tema ESCURO (véu quase-preto, ícones claros). Precisa ser MAIOR que o
 * do claro: escurecer um fundo branco o suficiente para um glifo branco aparecer custa
 * mais alpha do que clarear um fundo preto para um glifo preto. Pior caso = imagem
 * BRANCA sob os ícones: 0,62 dá **4,9:1**; 0,58 é o fio de 4,5:1.
 */
internal const val SCRIM_PEAK_ALPHA_DARK = 0.62f

internal const val SCRIM_STOP_COUNT = 10

/**
 * alpha(t) = pico × (1 − smootherstep(t)). Função pura para o teste poder travar a curva
 * (monotônica, começa no pico, termina em 0) sem subir Compose.
 */
internal fun scrimAlphaAt(t: Float, peak: Float): Float {
    val x = t.coerceIn(0f, 1f)
    val eased = x * x * x * (x * (x * 6f - 15f) + 10f) // smootherstep (quíntica)
    return peak * (1f - eased)
}

private val SCRIM_FADE_TAIL = 56.dp
