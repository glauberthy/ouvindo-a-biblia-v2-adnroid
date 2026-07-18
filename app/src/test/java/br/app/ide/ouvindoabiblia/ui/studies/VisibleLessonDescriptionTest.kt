package br.app.ide.ouvindoabiblia.ui.studies

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * ISSUE 9.A — [visibleLessonDescription] é a guarda que decide se o bloco de
 * descrição da aula renderiza no [LessonListItem]: null/blank não renderizam.
 */
class VisibleLessonDescriptionTest {

    @Test
    fun `null nao renderiza`() {
        assertNull(visibleLessonDescription(null))
    }

    @Test
    fun `vazia nao renderiza`() {
        assertNull(visibleLessonDescription(""))
    }

    @Test
    fun `blank (espacos e quebras) nao renderiza`() {
        assertNull(visibleLessonDescription("  \n\t "))
    }

    @Test
    fun `texto valido renderiza com trim`() {
        assertEquals(
            "Aula introdutória que estabelece os fundamentos.",
            visibleLessonDescription("  Aula introdutória que estabelece os fundamentos. \n")
        )
    }
}
