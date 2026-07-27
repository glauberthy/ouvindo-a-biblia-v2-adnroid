package br.app.ide.ouvindoabiblia.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trava o pico do [StatusBarScrim] no LIMITE de legibilidade.
 *
 * O véu é o único protetor dos ícones da status bar sobre imagem de header, e ele tem dois
 * jeitos de errar em direções opostas:
 *  - alto demais → faixa clara "viva" por cima da foto (era o caso com 0,72: 13,6:1
 *    medidos na imagem mais escura do acervo, contra 4,5:1 necessários);
 *  - baixo demais → relógio e ícones desaparecem sobre a imagem.
 *
 * Por isso o piso é verificado por CÁLCULO no pior caso possível (imagem preta no tema
 * claro, branca no escuro), não por inspeção visual num punhado de imagens: uma imagem
 * nova no servidor não pode invalidar a calibração.
 *
 * Alvo: 4,5:1 (WCAG 2.1 SC 1.4.3, texto pequeno — o relógio da status bar). O mínimo de
 * 3:1 de SC 1.4.11 valeria só para os ícones, então 4,5:1 é o corte mais rigoroso.
 */
class StatusBarScrimContrastTest {

    private companion object {
        const val CREAM_BACKGROUND = 0xF2E9E4 // AppColors.background no tema claro
        const val DARK_BACKGROUND = 0x17161F  // AppColors.background no tema escuro

        const val WCAG_TEXT_MIN = 4.5
        const val BLACK = 0x000000
        const val WHITE = 0xFFFFFF
    }

    private fun channel(rgb: Int, shift: Int) = (rgb shr shift and 0xFF) / 255.0

    /** sRGB → linear (WCAG 2.x). */
    private fun linear(c: Double) =
        if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)

    private fun relativeLuminance(rgb: Int): Double =
        0.2126 * linear(channel(rgb, 16)) +
            0.7152 * linear(channel(rgb, 8)) +
            0.0722 * linear(channel(rgb, 0))

    private fun contrast(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** Composita [scrim] com alpha [alpha] sobre [image] (source-over, sem gamma). */
    private fun composite(scrim: Int, image: Int, alpha: Float): Int {
        fun mix(shift: Int): Int {
            val s = scrim shr shift and 0xFF
            val i = image shr shift and 0xFF
            return (alpha * s + (1 - alpha) * i).toInt().coerceIn(0, 255)
        }
        return (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    @Test
    fun `tema claro protege icone escuro sobre a imagem mais escura possivel`() {
        val behindIcons = composite(CREAM_BACKGROUND, BLACK, SCRIM_PEAK_ALPHA_LIGHT)
        val ratio = contrast(BLACK, behindIcons)
        assertTrue(
            "pico claro ${SCRIM_PEAK_ALPHA_LIGHT} dá ${"%.2f".format(ratio)}:1 " +
                "sobre imagem preta — abaixo de $WCAG_TEXT_MIN:1",
            ratio >= WCAG_TEXT_MIN
        )
    }

    @Test
    fun `tema escuro protege icone claro sobre a imagem mais clara possivel`() {
        val behindIcons = composite(DARK_BACKGROUND, WHITE, SCRIM_PEAK_ALPHA_DARK)
        val ratio = contrast(WHITE, behindIcons)
        assertTrue(
            "pico escuro ${SCRIM_PEAK_ALPHA_DARK} dá ${"%.2f".format(ratio)}:1 " +
                "sobre imagem branca — abaixo de $WCAG_TEXT_MIN:1",
            ratio >= WCAG_TEXT_MIN
        )
    }

    /**
     * Impede o inverso do bug: o véu voltar a ser mais forte do que precisa. Se alguém
     * subir o pico "para garantir", a faixa clara sobre foto escura volta.
     */
    @Test
    fun `pico nao passa do necessario com folga`() {
        val claro = contrast(BLACK, composite(CREAM_BACKGROUND, BLACK, SCRIM_PEAK_ALPHA_LIGHT))
        val escuro = contrast(WHITE, composite(DARK_BACKGROUND, WHITE, SCRIM_PEAK_ALPHA_DARK))
        assertTrue("pico claro alto demais (${"%.2f".format(claro)}:1)", claro <= 6.0)
        assertTrue("pico escuro alto demais (${"%.2f".format(escuro)}:1)", escuro <= 6.0)
    }

    @Test
    fun `curva comeca no pico, morre em zero e nunca sobe`() {
        val peak = SCRIM_PEAK_ALPHA_LIGHT
        assertEquals(peak, scrimAlphaAt(0f, peak), 1e-6f)
        assertEquals(0f, scrimAlphaAt(1f, peak), 1e-6f)

        var previous = Float.MAX_VALUE
        for (i in 0..SCRIM_STOP_COUNT) {
            val alpha = scrimAlphaAt(i / SCRIM_STOP_COUNT.toFloat(), peak)
            assertTrue("curva subiu no stop $i", alpha <= previous)
            previous = alpha
        }
    }

    @Test
    fun `t fora de faixa nao extrapola`() {
        val peak = SCRIM_PEAK_ALPHA_LIGHT
        assertEquals(peak, scrimAlphaAt(-1f, peak), 1e-6f)
        assertEquals(0f, scrimAlphaAt(2f, peak), 1e-6f)
    }
}
