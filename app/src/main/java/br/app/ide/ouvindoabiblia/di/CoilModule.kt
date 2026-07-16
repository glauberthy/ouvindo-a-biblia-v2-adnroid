package br.app.ide.ouvindoabiblia.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context
    ): ImageLoader {

        // 1. Criamos o Cliente HTTP EXCLUSIVO para o Coil aqui dentro.
        // Isso evita o conflito "DuplicateBindings" com o NetworkModule.
        val coilOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    // O SEGREDO DO WAF:
                    .addHeader("User-Agent", "BibliaFaladaApp")
                    .build()
                chain.proceed(newRequest)
            }
            // BUG 2: a Home dispara ~66 capas de uma vez na 1ª abertura; o WAF responde HTTP 429
            // (rate limit) para a maioria e as capas ficavam quebradas até revisitar. Este
            // interceptor re-tenta o 429 com backoff + jitter (respeitando Retry-After se vier),
            // espalhando as requisições no tempo até entrarem sob o limite. Roda na thread de I/O
            // do OkHttp (não na main), então o Thread.sleep é aceitável.
            .addInterceptor { chain ->
                val request = chain.request()
                var response = chain.proceed(request)
                var attempt = 0
                while (response.code == 429 && attempt < 3) {
                    val retryAfterMs = response.header("Retry-After")?.toLongOrNull()?.times(1000L)
                    response.close()
                    val backoff = retryAfterMs ?: (500L * (attempt + 1) + Random.nextLong(0, 400))
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
            .build()

        // 2. Construímos o ImageLoader usando esse cliente privado
        return ImageLoader.Builder(context)
            .okHttpClient(coilOkHttpClient) // Usa o cliente com User-Agent
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}