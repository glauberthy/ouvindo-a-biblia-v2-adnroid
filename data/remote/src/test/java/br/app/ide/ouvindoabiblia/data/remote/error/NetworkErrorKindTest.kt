package br.app.ide.ouvindoabiblia.data.remote.error

import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Trava a classificação de erro de rede que alimenta a mensagem do usuário.
 *
 * Regressão coberta: o repositório expunha `localizedMessage` cru, então numa 1ª
 * instalação com rate limit o usuário lia "HTTP 429 Too Many Requests" na tela.
 */
class NetworkErrorKindTest {

    private fun httpError(code: Int): HttpException = HttpException(
        Response.error<Any>(code, "".toResponseBody("application/json".toMediaType()))
    )

    @Test
    fun rateLimit() {
        assertEquals(NetworkErrorKind.RATE_LIMITED, networkErrorKind(httpError(429)))
    }

    @Test
    fun erroDeServidor() {
        for (code in listOf(500, 502, 503)) {
            assertEquals("HTTP $code", NetworkErrorKind.SERVER_ERROR, networkErrorKind(httpError(code)))
        }
    }

    @Test
    fun semConexao() {
        assertEquals(NetworkErrorKind.NO_CONNECTION, networkErrorKind(UnknownHostException()))
        assertEquals(NetworkErrorKind.NO_CONNECTION, networkErrorKind(SocketTimeoutException()))
        assertEquals(NetworkErrorKind.NO_CONNECTION, networkErrorKind(IOException("conexão recusada")))
    }

    @Test
    fun conteudoInvalido() {
        assertEquals(
            NetworkErrorKind.INVALID_CONTENT,
            networkErrorKind(SerializationException("token inesperado"))
        )
    }

    @Test
    fun percorreACadeiaDeCausas() {
        // O caso real: o Retrofit/coroutines embrulha a exceção original.
        val embrulhado = RuntimeException("falhou o sync", IllegalStateException("meio", httpError(429)))
        assertEquals(NetworkErrorKind.RATE_LIMITED, networkErrorKind(embrulhado))
    }

    @Test
    fun naoEntraEmLacoComCausaCircular() {
        // Defensivo: cadeia que aponta para si não pode travar o app.
        class Circular : Exception() {
            override val cause: Throwable get() = this
        }
        assertEquals(NetworkErrorKind.UNKNOWN, networkErrorKind(Circular()))
    }

    @Test
    fun desconhecidos() {
        assertEquals(NetworkErrorKind.UNKNOWN, networkErrorKind(null))
        assertEquals(NetworkErrorKind.UNKNOWN, networkErrorKind(IllegalArgumentException()))
        // 4xx que não é 429 não vira "sem conexão" nem "servidor fora".
        assertEquals(NetworkErrorKind.UNKNOWN, networkErrorKind(httpError(404)))
    }
}
