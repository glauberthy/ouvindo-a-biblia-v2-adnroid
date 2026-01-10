package br.app.ide.ouvindoabiblia.data.remote.di

import br.app.ide.ouvindoabiblia.data.remote.api.BibleApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://ouvindo-a-biblia.ide.app.br/"

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // Log detalhado para ver headers e resposta do servidor
//        val logging = HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }

        return OkHttpClient.Builder()
//            .addInterceptor(logging)
            // IMPORTANTE: Interceptor do WAF
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "BibliaFaladaApp")
                    .build()
                chain.proceed(request)
            }
            // Reduzindo timeout para falhar rápido se a internet estiver ruim
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideBibleApi(client: OkHttpClient, json: Json): BibleApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BibleApi::class.java)
    }
}