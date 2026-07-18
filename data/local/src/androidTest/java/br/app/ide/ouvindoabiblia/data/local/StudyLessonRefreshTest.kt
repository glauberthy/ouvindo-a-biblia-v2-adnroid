package br.app.ide.ouvindoabiblia.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.app.ide.ouvindoabiblia.data.local.database.BibleDatabase
import br.app.ide.ouvindoabiblia.data.local.entity.StudyEntity
import br.app.ide.ouvindoabiblia.data.local.entity.StudyLessonEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ISSUE 9.A — prova o caminho de UPDATE do sync de Estudos: aula que JÁ existe
 * no Room (insert IGNORE devolve -1) precisa receber a `description` nova via
 * `updateStudyLessonMetadata`, preservando o favorito (dado só-local).
 */
@RunWith(AndroidJUnit4::class)
class StudyLessonRefreshTest {

    private lateinit var db: BibleDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            BibleDatabase::class.java
        ).build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun refreshStudiesData_preencheDescriptionDeAulaExistente_preservandoFavorito() = runBlocking {
        val dao = db.bibleDao()
        val study = StudyEntity(
            id = 1,
            title = "Apocalipse",
            author = "Rev. Leandro",
            description = "Série",
            imageUrl = "https://x/cover.webp"
        )
        val lessonSemDescricao = StudyLessonEntity(
            remoteId = 1,
            studyId = 1,
            title = "Características literárias",
            url = "https://x/01.m4a",
            duration = 5885
        )

        // 1º sync: payload antigo, sem description.
        dao.refreshStudiesData(listOf(study), listOf(lessonSemDescricao))
        val antes = dao.getStudyWithLessons(1).first().lessons.single()
        assertNull("antes do payload novo, description é null", antes.description)

        // Usuário favorita a aula (dado só-local, não pode ser perdido no re-sync).
        dao.updateStudyFavoriteStatus(studyId = 1, lessonId = 1, isFavorite = true)

        // 2º sync: mesmo id, agora com description (payload novo do servidor).
        dao.refreshStudiesData(
            listOf(study),
            listOf(lessonSemDescricao.copy(description = "Aula introdutória que estabelece os fundamentos."))
        )

        val depois = dao.getStudyWithLessons(1).first().lessons.single()
        assertEquals(
            "aula existente deveria receber a description no re-sync",
            "Aula introdutória que estabelece os fundamentos.",
            depois.description
        )
        assertEquals("favorito (só-local) preservado no re-sync", true, depois.isFavorite)
        assertEquals("não deveria duplicar a aula", 1, dao.getStudyWithLessons(1).first().lessons.size)
    }
}
