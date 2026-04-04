package br.app.ide.ouvindoabiblia.data.remote.api

import br.app.ide.ouvindoabiblia.data.remote.dto.BibleResponseDto
import br.app.ide.ouvindoabiblia.data.remote.dto.MoreContentDto
import br.app.ide.ouvindoabiblia.data.remote.dto.StudyResponseDto
import br.app.ide.ouvindoabiblia.data.remote.dto.ThemesResponse
import retrofit2.http.GET

interface BibleApi {
    // Busca o arquivo estático principal
    @GET("biblia_index.json")
    suspend fun getBibleIndex(): BibleResponseDto

    @GET("themes.json")
    suspend fun getThemes(): ThemesResponse

    @GET("estudos.json")
    suspend fun getStudies(): StudyResponseDto

    @GET("mais.json")
    suspend fun getMoreContent(): MoreContentDto
}