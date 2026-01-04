package br.app.ide.ouvindoabiblia.data.remote.api

import br.app.ide.ouvindoabiblia.data.remote.dto.BibleResponseDto
import retrofit2.http.GET

interface BibleApi {
    // Busca o arquivo estático principal
    @GET("biblia_index.json")
    suspend fun getBibleIndex(): BibleResponseDto
}