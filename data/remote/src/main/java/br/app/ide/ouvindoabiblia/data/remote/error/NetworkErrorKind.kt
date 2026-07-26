package br.app.ide.ouvindoabiblia.data.remote.error

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Causa de uma falha de rede, em termos que a camada de cima entende sem conhecer HTTP.
 *
 * Existe porque o conhecimento de HTTP (códigos, exceções do Retrofit) tem de ficar aqui,
 * no módulo que fala HTTP — e a MENSAGEM ao usuário fica no repositório, que é quem já
 * monta o `Resource.Error`. Antes o repositório expunha
 * `exceptionOrNull()?.localizedMessage` cru, então numa 1ª instalação com rate limit o
 * usuário lia literalmente algo como "HTTP 429 Too Many Requests" na tela.
 */
enum class NetworkErrorKind {
    /** WAF/servidor freando o acesso (429). Tentar de novo em seguida costuma resolver. */
    RATE_LIMITED,

    /** Servidor com problema (5xx). Não é culpa do aparelho nem da rede do usuário. */
    SERVER_ERROR,

    /** Sem internet: DNS falhou, conexão recusada ou timeout. */
    NO_CONNECTION,

    /** Respondeu, mas o corpo não é o JSON esperado (payload corrompido, HTML de bloqueio). */
    INVALID_CONTENT,

    /** Não classificado. */
    UNKNOWN
}

private const val HTTP_TOO_MANY_REQUESTS = 429
private const val MAX_CAUSE_DEPTH = 10

/** Classifica [throwable] percorrendo a cadeia de causas. */
fun networkErrorKind(throwable: Throwable?): NetworkErrorKind {
    var cause: Throwable? = throwable
    var depth = 0
    while (cause != null && depth++ < MAX_CAUSE_DEPTH) {
        when {
            cause is HttpException -> return when {
                cause.code() == HTTP_TOO_MANY_REQUESTS -> NetworkErrorKind.RATE_LIMITED
                cause.code() >= 500 -> NetworkErrorKind.SERVER_ERROR
                else -> NetworkErrorKind.UNKNOWN
            }

            cause is UnknownHostException || cause is SocketTimeoutException ->
                return NetworkErrorKind.NO_CONNECTION

            cause is SerializationException -> return NetworkErrorKind.INVALID_CONTENT

            // IOException genérica (conexão recusada, socket fechado) só depois das
            // específicas acima, porque SocketTimeoutException também é IOException.
            cause is IOException -> return NetworkErrorKind.NO_CONNECTION
        }
        cause = cause.cause
    }
    return NetworkErrorKind.UNKNOWN
}
