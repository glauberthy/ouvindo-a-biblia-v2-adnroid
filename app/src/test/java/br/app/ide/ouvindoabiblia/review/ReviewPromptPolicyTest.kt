package br.app.ide.ouvindoabiblia.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trava as regras de [shouldAskForReview].
 *
 * Este é o único teste possível da avaliação in-app: o cartão do Play não renderiza em build que
 * não veio da loja, e a API não informa se apareceu. Ver `InAppReviewManager`.
 */
class ReviewPromptPolicyTest {

    private val today = 20_000L

    private fun state(
        appOpenCount: Int = MIN_APP_OPENS,
        hasPlayedAudio: Boolean = true,
        lastPromptEpochDay: Long = ReviewPromptState.NEVER_PROMPTED,
        promptCount: Int = 0
    ) = ReviewPromptState(appOpenCount, hasPlayedAudio, lastPromptEpochDay, promptCount)

    @Test
    fun `pede quando engajou, abriu o suficiente e nunca pediu`() {
        assertTrue(shouldAskForReview(state(), today))
    }

    @Test
    fun `nao pede a quem nunca tocou audio`() {
        // Abrir o app 50 vezes sem ouvir nada não gera opinião sobre um app de áudio.
        assertFalse(shouldAskForReview(state(appOpenCount = 50, hasPlayedAudio = false), today))
    }

    @Test
    fun `nao pede antes do minimo de aberturas`() {
        assertFalse(shouldAskForReview(state(appOpenCount = MIN_APP_OPENS - 1), today))
        assertTrue(shouldAskForReview(state(appOpenCount = MIN_APP_OPENS), today))
    }

    @Test
    fun `nao pede antes do intervalo minimo entre tentativas`() {
        val ontem = today - 1
        assertFalse(shouldAskForReview(state(lastPromptEpochDay = ontem, promptCount = 1), today))

        val faltandoUmDia = today - (MIN_DAYS_BETWEEN_PROMPTS - 1)
        assertFalse(
            shouldAskForReview(state(lastPromptEpochDay = faltandoUmDia, promptCount = 1), today)
        )

        val noLimite = today - MIN_DAYS_BETWEEN_PROMPTS
        assertTrue(
            shouldAskForReview(state(lastPromptEpochDay = noLimite, promptCount = 1), today)
        )
    }

    @Test
    fun `desiste depois do teto de tentativas`() {
        val antigo = today - (MIN_DAYS_BETWEEN_PROMPTS * 10)
        assertTrue(
            shouldAskForReview(
                state(lastPromptEpochDay = antigo, promptCount = MAX_PROMPTS - 1), today
            )
        )
        assertFalse(
            shouldAskForReview(
                state(lastPromptEpochDay = antigo, promptCount = MAX_PROMPTS), today
            )
        )
    }

    @Test
    fun `relogio andando para tras nao libera o pedido`() {
        // Data do aparelho recuada (fuso, viagem, usuário mexendo no relógio) deixa `elapsed`
        // negativo. Uma comparação mal escrita liberaria o cartão toda vez que isso acontecesse.
        val futuro = today + 500
        assertFalse(shouldAskForReview(state(lastPromptEpochDay = futuro, promptCount = 1), today))
    }

    @Test
    fun `o gate de engajamento vence os demais criterios`() {
        // Mesmo com tudo o resto favorável, sem áudio tocado não pede.
        val antigo = today - (MIN_DAYS_BETWEEN_PROMPTS * 10)
        assertFalse(
            shouldAskForReview(
                state(appOpenCount = 100, hasPlayedAudio = false, lastPromptEpochDay = antigo),
                today
            )
        )
    }
}
