package br.app.ide.ouvindoabiblia.data.repository.domain

/**
 * Estado de um carregamento orquestrado pelo repositório (cache + sync).
 *
 * Padrão-alvo da ISSUE 3.B: as ViewModels param de reimplementar a lógica
 * "tem cache? falha de sync é silenciosa?" — o repositório emite [Loading]
 * enquanto não há cache, [Success] quando há dados (mesmo que o sync tenha
 * falhado silenciosamente) e [Error] só quando não há cache e o sync falhou.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: String) : Resource<Nothing>
}
