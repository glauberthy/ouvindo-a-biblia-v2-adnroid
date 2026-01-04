package br.app.ide.ouvindoabiblia.data.local.di

import android.content.Context
import androidx.room.Room
import br.app.ide.ouvindoabiblia.data.local.dao.BibleDao
import br.app.ide.ouvindoabiblia.data.local.database.BibleDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Este módulo vive enquanto o App estiver rodando
object DatabaseModule {

    @Provides
    @Singleton // Garante que só existe UMA instância do banco aberta (performance)
    fun provideBibleDatabase(
        @ApplicationContext context: Context
    ): BibleDatabase {
        return Room.databaseBuilder(
            context,
            BibleDatabase::class.java,
            "bible_db"
        )
            // Permite recriar o banco se mudarmos a versão (útil em dev)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideBibleDao(database: BibleDatabase): BibleDao {
        return database.bibleDao()
    }
}