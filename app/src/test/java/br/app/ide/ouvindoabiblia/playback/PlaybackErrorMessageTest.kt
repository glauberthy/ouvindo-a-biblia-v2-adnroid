package br.app.ide.ouvindoabiblia.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trava a classificação do erro de reprodução.
 *
 * Regressão coberta: a mensagem era sempre "Verifique sua conexão", inclusive quando o WAF
 * do servidor rate-limitava (429) — caso em que a internet do usuário está perfeita e o
 * texto o manda procurar defeito no lugar errado.
 */
class PlaybackErrorMessageTest {

    private fun kind(
        typed: Int? = null,
        message: String? = null,
        connection: Boolean = false,
        notFound: Boolean = false
    ) = classifyPlaybackError(typed, message, connection, notFound)

    @Test
    fun rateLimitPorTipo_viraServidorOcupado() {
        assertEquals(PlaybackErrorKind.SERVER_BUSY, kind(typed = 429))
    }

    @Test
    fun rateLimitPorTexto_viraServidorOcupado() {
        // ESTE é o caminho de produção: o erro cruza o Binder do serviço até o
        // MediaController, e na travessia só a mensagem sobrevive.
        assertEquals(
            PlaybackErrorKind.SERVER_BUSY,
            kind(message = "Source error: Response code: 429")
        )
    }

    @Test
    fun rateLimitTemPrecedenciaSobreErroDeConexao() {
        // Um 429 chega com errorCode de I/O; não pode ser lido como "sem internet".
        assertEquals(
            PlaybackErrorKind.SERVER_BUSY,
            kind(message = "Response code: 429", connection = true)
        )
    }

    @Test
    fun conteudoIndisponivel() {
        for (code in listOf(403, 404, 410)) {
            assertEquals(
                "HTTP $code",
                PlaybackErrorKind.CONTENT_UNAVAILABLE,
                kind(message = "Response code: $code")
            )
        }
        assertEquals(PlaybackErrorKind.CONTENT_UNAVAILABLE, kind(notFound = true))
    }

    @Test
    fun semConexao() {
        assertEquals(PlaybackErrorKind.NO_CONNECTION, kind(connection = true))
    }

    @Test
    fun semSinalNenhum_viraGenerico() {
        assertEquals(PlaybackErrorKind.UNKNOWN, kind())
        assertEquals(PlaybackErrorKind.UNKNOWN, kind(message = "algo inesperado"))
    }

    @Test
    fun textoDoErro_naoExpoeCodigoNemStacktrace() {
        for (k in PlaybackErrorKind.entries) {
            val texto = playbackErrorText(k)
            assertTrue("vazio p/ $k", texto.isNotBlank())
            assertTrue("$k expõe código HTTP: $texto", !texto.contains(Regex("""\d{3}""")))
            assertTrue("$k expõe exceção: $texto", !texto.contains("Exception"))
        }
    }

    @Test
    fun apenasORateLimitFalaDeEspera_eApenasAConexaoFalaDeRede() {
        assertTrue(playbackErrorText(PlaybackErrorKind.SERVER_BUSY).contains("Aguarde"))
        // O ponto do bug: só o caso de rede pode mandar verificar a rede.
        assertTrue(
            !playbackErrorText(PlaybackErrorKind.SERVER_BUSY).contains("rede", ignoreCase = true)
        )
        assertTrue(
            playbackErrorText(PlaybackErrorKind.NO_CONNECTION).contains("rede", ignoreCase = true)
        )
    }

    @Test
    fun parserDeTexto() {
        assertEquals(429, httpCodeFromText("Response code: 429"))
        assertEquals(404, httpCodeFromText("blá Response code:404 blá"))
        assertEquals(null, httpCodeFromText(null))
        assertEquals(null, httpCodeFromText(""))
        assertEquals(null, httpCodeFromText("sem código aqui"))
    }
}
