package br.app.ide.ouvindoabiblia.data.local.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Cria a extensão delegate apenas uma vez (Singleton implícito pelo Kotlin)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bible_settings")

@Module
@InstallIn(SingletonComponent::class) // Vida útil de todo o App
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}