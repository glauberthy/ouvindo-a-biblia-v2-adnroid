package br.app.ide.ouvindoabiblia.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import br.app.ide.ouvindoabiblia.data.local.dao.BibleDao
import br.app.ide.ouvindoabiblia.data.local.entity.BookEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ChapterEntity
import br.app.ide.ouvindoabiblia.data.local.entity.MomentEntity
import br.app.ide.ouvindoabiblia.data.local.entity.MoreContentEntity
import br.app.ide.ouvindoabiblia.data.local.entity.PlaybackStateEntity
import br.app.ide.ouvindoabiblia.data.local.entity.StudyEntity
import br.app.ide.ouvindoabiblia.data.local.entity.StudyLessonEntity
import br.app.ide.ouvindoabiblia.data.local.entity.ThemeEntity

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        PlaybackStateEntity::class,
        ThemeEntity::class,
        MomentEntity::class,
        StudyEntity::class,
        StudyLessonEntity::class,
        MoreContentEntity::class
    ],
    version = 8,
    exportSchema = false // Para desenvolvimento inicial, deixamos falso
)
abstract class BibleDatabase : RoomDatabase() {

    // Expõe o DAO para ser usado
    abstract fun bibleDao(): BibleDao
}