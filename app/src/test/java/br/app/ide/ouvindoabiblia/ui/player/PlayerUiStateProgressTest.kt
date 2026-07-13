package br.app.ide.ouvindoabiblia.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Trava a regressão da ISSUE 1.B: no cold start, quando a duração salva é
 * desconhecida (<=0, ex.: save durante o buffering), a barra de progresso NÃO
 * pode aparecer cheia.
 *
 * Cobre [sliderProgressValueMs], a função que o Slider de [PlayerProgressBar]
 * realmente usa para o valor exibido. Com duração desconhecida o Slider fica com
 * range 0f..1f; devolver a posição real (ex.: 158204) estouraria esse range e
 * saturaria o thumb em "cheio". A função devolve 0f nesse caso (barra vazia).
 */
class PlayerUiStateProgressTest {

    @Test
    fun `duracao desconhecida (0) com posicao real nao enche a barra`() {
        // Cenário exato do cold start com duration=0 salvo (posição real de ~2:38).
        assertEquals(0f, sliderProgressValueMs(currentPositionMs = 158_204L, durationMs = 0L), 0f)
    }

    @Test
    fun `duracao negativa nao enche a barra`() {
        assertEquals(0f, sliderProgressValueMs(currentPositionMs = 158_204L, durationMs = -1L), 0f)
    }

    @Test
    fun `duracao conhecida usa a posicao real`() {
        // Caso normal (regressão inversa): o valor do slider é a posição em ms,
        // e o range 0f..duration faz a fração correta (158204/181792 ≈ 0.87).
        assertEquals(
            158_204f,
            sliderProgressValueMs(currentPositionMs = 158_204L, durationMs = 181_792L),
            0f
        )
    }
}
