package br.app.ide.ouvindoabiblia.data.remote.di

import android.content.Context
import android.content.pm.ApplicationInfo
import br.app.ide.ouvindoabiblia.data.remote.api.BibleApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://ouvindo-a-biblia.ide.app.br/"

    private const val HTTP_TOO_MANY_REQUESTS = 429
    private const val MAX_429_RETRIES = 3

    // Teto do Retry-After: com o app cache-first, esperar muito no sync rende menos que
    // falhar e deixar o próximo load tentar de novo.
    private const val MAX_429_BACKOFF_MS = 5_000L

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
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        // ISSUE PUB-03: log de corpo/headers só em builds debug. Em release fica NONE para
        // não vazar payload no logcat nem custar desempenho. Gate pelo FLAG_DEBUGGABLE do app
        // (mesmo critério do OuvindoBibliaApp.isDebuggable()) — independe de BuildConfig por módulo.
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val logging = HttpLoggingInterceptor().apply {
            level = if (isDebuggable) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // BUG A (cold start lento): cache HTTP para GET condicional. Os JSONs estáticos
        // (biblia_index 230KB, themes, estudos, mais) vêm com ETag/Last-Modified e
        // `cache-control: must-revalidate`. Sem Cache, o OkHttp baixava o payload inteiro
        // (200) a cada abertura; com Cache, ele revalida (If-None-Match) e o servidor
        // responde 304 quando nada mudou — pulando os 230KB. Complementa o version-gate
        // do repositório (que evita reescrever o Room, mas não evitava o download).
        val cache = Cache(
            directory = File(context.cacheDir, "http_cache"),
            maxSize = 10L * 1024 * 1024 // 10 MB — folgado para os poucos JSONs
        )

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(logging)
            // IMPORTANTE: Interceptor do WAF
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "BibliaFaladaApp")
                    .build()
                chain.proceed(request)
            }
            // O mesmo WAF que rate-limita as capas (ver o interceptor irmão no CoilModule)
            // também pode rate-limitar os JSONs — os dois usam OkHttp, mas são clientes
            // separados de propósito (o cache de 10MB aqui é dimensionado para JSON, não
            // para imagens), então o tratamento é duplicado em vez de compartilhado.
            // Sem isto, um 429 no sync falhava de primeira. O impacto era pequeno porque
            // o repositório é cache-first, mas na 1ª instalação (cache vazio) o usuário
            // ficava na tela de erro tendo de tocar "Tentar novamente" na mão.
            // Roda na thread de I/O do OkHttp, então o Thread.sleep é aceitável.
            .addInterceptor { chain ->
                val request = chain.request()
                var response = chain.proceed(request)
                var attempt = 0
                while (response.code == HTTP_TOO_MANY_REQUESTS && attempt < MAX_429_RETRIES) {
                    val retryAfterMs = response.header("Retry-After")
                        ?.toLongOrNull()
                        ?.times(1_000L)
                        ?.coerceAtMost(MAX_429_BACKOFF_MS)
                    response.close()
                    val backoff = retryAfterMs
                        ?: (500L * (attempt + 1) + Random.nextLong(0, 400))
                    try {
                        Thread.sleep(backoff)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                    attempt++
                    response = chain.proceed(request)
                }
                response
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