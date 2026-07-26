package br.app.ide.ouvindoabiblia.playback

/** Sentinela de "não re-tentar". O chamador traduz para `C.TIME_UNSET` do Media3. */
internal const val AUDIO_NO_RETRY = -1L

internal const val HTTP_TOO_MANY_REQUESTS = 429

/**
 * Teto para o `Retry-After` do servidor: se ele pedir 60s, não vale travar a reprodução
 * esperando — melhor falhar e deixar o usuário tocar play de novo.
 */
internal const val MAX_RATE_LIMIT_BACKOFF_MS = 15_000L

/** Códigos em que re-tentar é inútil: o servidor está respondendo de forma definitiva. */
internal val UNRECOVERABLE_HTTP_CODES = setOf(400, 401, 403, 404, 410, 416, 501)

/**
 * Espera BASE antes de re-tentar o carregamento de um áudio. Função pura para poder ser
 * travada por teste; o jitter é somado por quem chama (ver MediaModule), porque jitter e
 * asserção não combinam.
 *
 * Três comportamentos, e o motivo de cada um:
 *  - **Definitivo** (404, 403, …): não re-tenta. Antes qualquer erro ganhava delay, então
 *    um áudio removido do ar custava ~7s de espera para falhar do mesmo jeito.
 *  - **429 (rate limit do WAF)**: escada longa (2s/5s/12s), porque a janela do WAF é maior
 *    que os 7s do backoff antigo (1+2+4) — reproduzido: 6 trocas de faixa em 18s
 *    derrubavam a reprodução. Respeita `Retry-After` quando o servidor manda, com teto.
 *  - **Resto** (rede oscilando, timeout): escada curta original, 1s/2s/4s.
 */
internal fun audioRetryBaseDelayMs(
    httpCode: Int?,
    attempt: Int,
    retryAfterSeconds: Long? = null
): Long {
    if (httpCode != null && httpCode in UNRECOVERABLE_HTTP_CODES) return AUDIO_NO_RETRY

    val safeAttempt = attempt.coerceAtLeast(1)

    if (httpCode == HTTP_TOO_MANY_REQUESTS) {
        val fromServer = retryAfterSeconds
            ?.takeIf { it > 0 }
            ?.times(1_000L)
            ?.coerceAtMost(MAX_RATE_LIMIT_BACKOFF_MS)
        if (fromServer != null) return fromServer

        return when (safeAttempt) {
            1 -> 2_000L
            2 -> 5_000L
            else -> 12_000L
        }
    }

    return when (safeAttempt) {
        1 -> 1_000L
        2 -> 2_000L
        else -> 4_000L
    }
}
