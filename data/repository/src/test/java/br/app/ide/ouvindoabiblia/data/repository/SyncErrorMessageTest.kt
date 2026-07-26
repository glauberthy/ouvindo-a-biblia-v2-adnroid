package br.app.ide.ouvindoabiblia.data.repository

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

/**
 * Trava a mensagem mostrada quando o sync falha sem cache (1ª instalação).
 *
 * O ponto principal: nenhuma mensagem pode carregar texto de biblioteca. Antes era
 * `localizedMessage` cru — o usuário lia "HTTP 429 Too Many Requests".
 */
class SyncErrorMessageTest {

    private val technicalNoise = listOf(
        "HTTP", "Exception", "retrofit", "okhttp", "kotlinx", "429", "500", "null", "java."
    )

    @Test
    fun nenhumaMensagemVazaTermoTecnico() {
        val casos = listOf<Throwable?>(
            null,
            UnknownHostException("ouvindo-a-biblia.ide.app.br"),
            IllegalStateException("boom"),
            RuntimeException("HTTP 429 Too Many Requests")
        )
        for (caso in casos) {
            val msg = syncErrorMessage(caso)
            assertTrue("mensagem vazia para $caso", msg.isNotBlank())
            for (termo in technicalNoise) {
                assertTrue(
                    "mensagem para $caso vaza \"$termo\": $msg",
                    !msg.contains(termo, ignoreCase = true)
                )
            }
        }
    }

    @Test
    fun semConexaoFalaDeRede() {
        val msg = syncErrorMessage(UnknownHostException())
        assertTrue(msg, msg.contains("conexão", ignoreCase = true))
    }

    @Test
    fun todaMensagemConvidaATentarDeNovo() {
        // A tela de erro tem botão de Retry; a mensagem tem de combinar com ele.
        val casos = listOf<Throwable?>(null, UnknownHostException(), IllegalStateException())
        for (caso in casos) {
            val msg = syncErrorMessage(caso)
            assertTrue("sem convite a tentar de novo: $msg", msg.contains("novamente"))
        }
    }
}
