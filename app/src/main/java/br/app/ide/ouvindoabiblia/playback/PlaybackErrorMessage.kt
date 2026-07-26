package br.app.ide.ouvindoabiblia.playback

/** Códigos em que o áudio simplesmente não está lá / não é liberado. */
internal val UNAVAILABLE_HTTP_CODES = setOf(403, 404, 410)

/** Teto da caminhada na cadeia de causas — evita laço se um `cause` apontar para si. */
internal const val MAX_CAUSE_DEPTH = 10

private val RESPONSE_CODE_REGEX = Regex("""Response code:\s*(\d{3})""")

/** Buckets de causa que o usuário precisa distinguir. */
internal enum class PlaybackErrorKind {
    /** WAF do servidor freando o acesso (429). A internet do usuário está boa. */
    SERVER_BUSY,

    /** Áudio inexistente ou bloqueado (403/404/410). Re-tentar não resolve. */
    CONTENT_UNAVAILABLE,

    /** Sem rede: falha de conexão ou timeout. */
    NO_CONNECTION,

    /** Não classificado. */
    UNKNOWN
}

/**
 * Classifica um erro de reprodução.
 *
 * A mensagem antiga era sempre "Verifique sua conexão", que MENTE no caso mais comum:
 * quando o WAF rate-limita (429) a internet do usuário está perfeita, e mandá-lo conferir
 * a rede o faz procurar defeito no lugar errado.
 *
 * Sobre o código HTTP: quem lê o erro é o MediaController, e o erro cruzou o Binder desde
 * o PlaybackService. Nessa travessia o TIPO da causa (InvalidResponseCodeException) é
 * perdido, mas a MENSAGEM sobrevive — então em produção o caminho que vale é a leitura por
 * texto ("Response code: 429"). [typedHttpCode] cobre o caso sem IPC.
 */
internal fun classifyPlaybackError(
    typedHttpCode: Int?,
    messageChain: String?,
    isConnectionError: Boolean,
    isFileNotFound: Boolean
): PlaybackErrorKind {
    val httpCode = typedHttpCode ?: httpCodeFromText(messageChain)
    return when {
        httpCode == HTTP_TOO_MANY_REQUESTS -> PlaybackErrorKind.SERVER_BUSY
        httpCode != null && httpCode in UNAVAILABLE_HTTP_CODES -> PlaybackErrorKind.CONTENT_UNAVAILABLE
        isFileNotFound -> PlaybackErrorKind.CONTENT_UNAVAILABLE
        isConnectionError -> PlaybackErrorKind.NO_CONNECTION
        else -> PlaybackErrorKind.UNKNOWN
    }
}

internal fun httpCodeFromText(text: String?): Int? {
    if (text.isNullOrBlank()) return null
    return RESPONSE_CODE_REGEX.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
}

/** Texto mostrado ao usuário. Nunca expõe stacktrace nem código HTTP. */
internal fun playbackErrorText(kind: PlaybackErrorKind): String = when (kind) {
    PlaybackErrorKind.SERVER_BUSY ->
        "O servidor está recebendo muitos acessos agora. Aguarde alguns segundos e toque em play novamente."

    PlaybackErrorKind.CONTENT_UNAVAILABLE ->
        "Este áudio não está disponível no momento."

    PlaybackErrorKind.NO_CONNECTION ->
        "Sem conexão com a internet. Verifique sua rede e tente novamente."

    PlaybackErrorKind.UNKNOWN ->
        "Não foi possível reproduzir agora. Tente novamente."
}
