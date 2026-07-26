package br.app.ide.ouvindoabiblia.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trava a política de re-tentativa do áudio.
 *
 * Regressão coberta: com o backoff antigo (1s/2s/4s ≈ 7s no total) o rate limit do WAF
 * derrubava a reprodução — reproduzido com 6 trocas de faixa em 18s, que resultavam em
 * "Source error" (HTTP 429).
 */
class AudioRetryPolicyTest {

    @Test
    fun rateLimit_usaEscadaLonga() {
        assertEquals(2_000L, audioRetryBaseDelayMs(429, attempt = 1))
        assertEquals(5_000L, audioRetryBaseDelayMs(429, attempt = 2))
        assertEquals(12_000L, audioRetryBaseDelayMs(429, attempt = 3))
    }

    @Test
    fun rateLimit_esperaMaisQueOBackoffAntigo() {
        // O ponto da mudança: a soma tem de cobrir uma janela maior que os 7s de antes.
        val total = (1..3).sumOf { audioRetryBaseDelayMs(429, attempt = it) }
        assertTrue("total=$total deveria passar de 7s", total > 7_000L)
    }

    @Test
    fun rateLimit_respeitaRetryAfterDoServidor() {
        assertEquals(3_000L, audioRetryBaseDelayMs(429, attempt = 1, retryAfterSeconds = 3))
    }

    @Test
    fun rateLimit_limitaRetryAfterExagerado() {
        // Servidor pedindo 60s: travar a reprodução tanto tempo é pior que falhar.
        assertEquals(
            MAX_RATE_LIMIT_BACKOFF_MS,
            audioRetryBaseDelayMs(429, attempt = 1, retryAfterSeconds = 60)
        )
    }

    @Test
    fun rateLimit_ignoraRetryAfterInvalido() {
        assertEquals(2_000L, audioRetryBaseDelayMs(429, attempt = 1, retryAfterSeconds = 0))
        assertEquals(2_000L, audioRetryBaseDelayMs(429, attempt = 1, retryAfterSeconds = -5))
    }

    @Test
    fun errosDefinitivos_naoRetentam() {
        // Antes qualquer erro ganhava delay: um 404 custava ~7s para falhar do mesmo jeito.
        for (code in listOf(400, 401, 403, 404, 410, 416, 501)) {
            assertEquals(
                "HTTP $code não deveria ser re-tentado",
                AUDIO_NO_RETRY,
                audioRetryBaseDelayMs(code, attempt = 1)
            )
        }
    }

    @Test
    fun errosTransitorios_mantemEscadaCurta() {
        // Sem código HTTP (timeout, rede oscilando) e 5xx recuperável.
        assertEquals(1_000L, audioRetryBaseDelayMs(null, attempt = 1))
        assertEquals(2_000L, audioRetryBaseDelayMs(null, attempt = 2))
        assertEquals(4_000L, audioRetryBaseDelayMs(null, attempt = 3))
        assertEquals(1_000L, audioRetryBaseDelayMs(503, attempt = 1))
    }

    @Test
    fun attemptForaDeFaixa_naoQuebra() {
        assertEquals(1_000L, audioRetryBaseDelayMs(null, attempt = 0))
        assertEquals(4_000L, audioRetryBaseDelayMs(null, attempt = 99))
        assertEquals(12_000L, audioRetryBaseDelayMs(429, attempt = 99))
    }
}
