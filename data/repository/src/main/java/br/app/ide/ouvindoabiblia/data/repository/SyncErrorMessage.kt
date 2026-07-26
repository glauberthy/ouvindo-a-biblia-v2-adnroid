package br.app.ide.ouvindoabiblia.data.repository

import br.app.ide.ouvindoabiblia.data.remote.error.NetworkErrorKind
import br.app.ide.ouvindoabiblia.data.remote.error.networkErrorKind

/**
 * Mensagem do `Resource.Error` quando o sync falha E não há cache para mostrar
 * (na prática: 1ª instalação).
 *
 * Antes era `exceptionOrNull()?.localizedMessage`, que joga texto de biblioteca na cara do
 * usuário — num rate limit ele lia algo como "HTTP 429 Too Many Requests", e num payload
 * inválido, um erro de parser do kotlinx-serialization. Aqui a classificação vem de
 * [networkErrorKind] (que fica no módulo que fala HTTP) e só o texto mora nesta camada,
 * que é quem já montava a mensagem.
 */
internal fun syncErrorMessage(throwable: Throwable?): String =
    when (networkErrorKind(throwable)) {
        NetworkErrorKind.RATE_LIMITED ->
            "O servidor está recebendo muitos acessos agora. Aguarde alguns segundos e tente novamente."

        NetworkErrorKind.SERVER_ERROR ->
            "O servidor está indisponível neste momento. Tente novamente em alguns minutos."

        NetworkErrorKind.NO_CONNECTION ->
            "Sem conexão com a internet. Verifique sua rede e tente novamente."

        NetworkErrorKind.INVALID_CONTENT ->
            "Não foi possível ler o conteúdo recebido. Tente novamente."

        NetworkErrorKind.UNKNOWN ->
            "Não foi possível carregar o conteúdo. Tente novamente."
    }
