package br.app.ide.ouvindoabiblia.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.app.ide.ouvindoabiblia.data.local.database.BibleDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Garante que a migração 8 → 9 PRESERVA os dados só-locais que o
 * fallbackToDestructiveMigration() apagava: favoritos e a posição de
 * "continuar ouvindo" (DIAGNOSTICO_01 §4b).
 *
 * É a rede de regressão para a troca de migração destrutiva por migração real.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test-db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BibleDatabase::class.java
    )

    @Test
    fun migrate8To9_preservaFavoritosEPosicaoDeRetomada() {
        // 1) Cria o banco na versão 8 e insere dados só-locais.
        helper.createDatabase(testDb, 8).apply {
            execSQL(
                """
                INSERT INTO books (numericId, book_id, name, testament, folder_path, image_url, total_chapters)
                VALUES (1, 'genesis', 'Gênesis', 'at', 'genesis/', NULL, 50)
                """.trimIndent()
            )
            // Capítulo FAVORITADO (is_favorite = 1).
            execSQL(
                """
                INSERT INTO chapters (id, book_id, chapter_number, audio_url, filename, is_favorite)
                VALUES (10, 1, 3, 'https://x/3.ogg', '3.ogg', 1)
                """.trimIndent()
            )
            // Posição de retomada salva.
            execSQL(
                """
                INSERT INTO playback_state (id, mediaId, positionMs, duration, title, subtitle, imageUrl, audioUrl, timestamp)
                VALUES (1, '10', 123456, 600000, 'Gênesis 3', 'Capítulo 3', NULL, 'https://x/3.ogg', 999)
                """.trimIndent()
            )
            close()
        }

        // 2) Roda a migração 8 → 9 e valida que o schema final bate com 9.json.
        helper.runMigrationsAndValidate(testDb, 9, true, BibleDatabase.MIGRATION_8_9)

        // 3) Abre via Room (versão CORRENTE) e confirma que os dados sobreviveram.
        // Registra a cadeia completa de migrações: o Room migra até a versão atual
        // do @Database (hoje 10), então este teste também prova a preservação 8→…→atual.
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            BibleDatabase::class.java,
            testDb
        ).addMigrations(BibleDatabase.MIGRATION_8_9, BibleDatabase.MIGRATION_9_10).build()

        try {
            // Favorito preservado.
            db.query("SELECT is_favorite FROM chapters WHERE id = 10", arrayOf()).use { c ->
                assertTrue("capítulo deveria existir após a migração", c.moveToFirst())
                assertEquals("is_favorite deveria continuar 1", 1, c.getInt(0))
            }
            // Posição de retomada preservada.
            db.query("SELECT positionMs, mediaId FROM playback_state WHERE id = 1", arrayOf()).use { c ->
                assertTrue("playback_state deveria existir após a migração", c.moveToFirst())
                assertEquals("posição deveria ser preservada", 123456L, c.getLong(0))
                assertEquals("mediaId deveria ser preservado", "10", c.getString(1))
            }
        } finally {
            db.close()
        }
    }

    /**
     * Migração 9 → 10 (ISSUE 9.A): `study_lessons` ganha a coluna nullable
     * `description`. Garante que aulas existentes (inclusive favoritadas)
     * sobrevivem com description = NULL até o próximo sync preencher.
     */
    @Test
    fun migrate9To10_adicionaDescriptionPreservandoAulasEFavoritos() {
        // 1) Banco na v9 com um estudo e uma aula FAVORITADA.
        helper.createDatabase(testDb, 9).apply {
            execSQL(
                """
                INSERT INTO studies (id, title, author, image_url, description)
                VALUES (1, 'Apocalipse', 'Rev. Leandro', 'https://x/cover.webp', 'Série de estudos')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO study_lessons (localId, remoteId, studyId, title, url, duration, isFavorite)
                VALUES (1, 1, 1, 'Características literárias', 'https://x/01.m4a', 5885, 1)
                """.trimIndent()
            )
            close()
        }

        // 2) Roda 9 → 10 e valida o schema final contra 10.json.
        helper.runMigrationsAndValidate(testDb, 10, true, BibleDatabase.MIGRATION_9_10)

        // 3) Abre via Room (v10) e confirma dados preservados + coluna nova NULL.
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            BibleDatabase::class.java,
            testDb
        ).addMigrations(BibleDatabase.MIGRATION_8_9, BibleDatabase.MIGRATION_9_10).build()

        try {
            db.query(
                "SELECT title, isFavorite, description FROM study_lessons WHERE localId = 1",
                arrayOf()
            ).use { c ->
                assertTrue("aula deveria existir após a migração", c.moveToFirst())
                assertEquals("título preservado", "Características literárias", c.getString(0))
                assertEquals("favorito preservado", 1, c.getInt(1))
                assertTrue("description deveria nascer NULL", c.isNull(2))
            }
        } finally {
            db.close()
        }
    }
}
