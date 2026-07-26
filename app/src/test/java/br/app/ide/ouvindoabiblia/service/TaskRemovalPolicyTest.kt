package br.app.ide.ouvindoabiblia.service

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Trava a tabela de decisão do swipe (remover dos recentes).
 *
 * Regressão coberta: com o áudio TOCANDO, o swipe precisa encerrar. Antes o serviço
 * era mantido vivo (modelo persistente) e o áudio continuava tocando depois de o
 * usuário fechar o app.
 */
class TaskRemovalPolicyTest {

    @Test
    fun tocando_encerra() {
        assertEquals(
            "swipe com áudio tocando deve encerrar o serviço",
            TaskRemovalDecision.STOP_SERVICE,
            decideOnTaskRemoval(isPlaying = true, hasStartedPlaybackThisProcess = true)
        )
    }

    @Test
    fun pausadoAposTocar_mantemNotificacao() {
        assertEquals(
            "pausado após tocar deve manter a notificação dismissível (4.A)",
            TaskRemovalDecision.KEEP_PAUSED_NOTIFICATION,
            decideOnTaskRemoval(isPlaying = false, hasStartedPlaybackThisProcess = true)
        )
    }

    @Test
    fun sessaoRestauradaNuncaTocada_encerra() {
        assertEquals(
            "sessão só restaurada não tem notificação a preservar (BUG B)",
            TaskRemovalDecision.STOP_SERVICE,
            decideOnTaskRemoval(isPlaying = false, hasStartedPlaybackThisProcess = false)
        )
    }

    @Test
    fun tocandoSemFlag_encerra() {
        // Combinação que não deveria ocorrer (onIsPlayingChanged marca o flag), mas se
        // ocorrer o desfecho seguro é encerrar, nunca deixar áudio órfão.
        assertEquals(
            TaskRemovalDecision.STOP_SERVICE,
            decideOnTaskRemoval(isPlaying = true, hasStartedPlaybackThisProcess = false)
        )
    }
}
