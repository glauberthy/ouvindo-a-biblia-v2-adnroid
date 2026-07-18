package br.app.ide.ouvindoabiblia.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 10,
    exportSchema = true // Exporta schema p/ migrações testáveis (DIAGNOSTICO_01 §4b)
)
abstract class BibleDatabase : RoomDatabase() {

    // Expõe o DAO para ser usado
    abstract fun bibleDao(): BibleDao

    companion object {
        /**
         * Migração 8 → 9.
         *
         * Não há mudança de schema entre 8 e 9: esta migração existe para
         * SUBSTITUIR o fallbackToDestructiveMigration() (que apagava favoritos e a
         * posição de "continuar ouvindo" a cada bump de versão — DIAGNOSTICO_01 §4b).
         *
         * A partir daqui, todo novo schema deve declarar sua própria migração
         * (MIGRATION_9_10, ...). Sem migração, o Room passa a LANÇAR em vez de
         * apagar os dados silenciosamente.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Schema idêntico ao da v8 — nada a alterar; dados preservados.
            }
        }

        /**
         * Migração 9 → 10 (ISSUE 9.A): descrição por aula de estudo.
         * Coluna nullable sem default → aulas existentes ficam com NULL até o
         * próximo sync com bump de version do estudos.json regravar os metadados.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE study_lessons ADD COLUMN description TEXT")
            }
        }
    }
}