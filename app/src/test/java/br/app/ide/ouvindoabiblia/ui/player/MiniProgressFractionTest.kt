package br.app.ide.ouvindoabiblia.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ISSUE 9.D — [miniProgressFraction] alimenta a barra só-leitura do mini player.
 * Guarda da 1.B: duração desconhecida (<= 0) → 0f, nunca barra cheia falsa.
 */
class MiniProgressFractionTest {

    @Test
    fun `duracao desconhecida rende barra vazia`() {
        assertEquals(0f, miniProgressFraction(158_204L, 0L), 0f)
        assertEquals(0f, miniProgressFraction(158_204L, -1L), 0f)
    }

    @Test
    fun `progresso normal e a fracao posicao sobre duracao`() {
        assertEquals(0.5f, miniProgressFraction(30_000L, 60_000L), 0.0001f)
        assertEquals(0f, miniProgressFraction(0L, 60_000L), 0f)
    }

    @Test
    fun `posicao alem da duracao satura em 1`() {
        assertEquals(1f, miniProgressFraction(61_000L, 60_000L), 0f)
    }

    @Test
    fun `posicao negativa satura em 0`() {
        assertEquals(0f, miniProgressFraction(-500L, 60_000L), 0f)
    }
}
