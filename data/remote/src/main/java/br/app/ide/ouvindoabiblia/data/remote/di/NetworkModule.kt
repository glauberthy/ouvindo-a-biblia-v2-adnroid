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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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

        return OkHttpClient.Builder()
            .addInterceptor(logging)
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