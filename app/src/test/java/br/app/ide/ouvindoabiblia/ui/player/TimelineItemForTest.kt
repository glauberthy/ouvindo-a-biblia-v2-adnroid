package br.app.ide.ouvindoabiblia.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ISSUE 2.B: a folha de capítulos deve mostrar o número do capítulo (Bíblia) e o
 * TÍTULO da aula (Estudo), não uma numeração sintética "1,2,3" para Estudos.
 */
class TimelineItemForTest {

    @Test
    fun `biblia usa o numero do capitulo do titulo`() {
        val item = timelineItemFor(
            mediaId = "1234",
            title = "Gênesis 3",
            subtitle = "Capítulo 3",
            index = 0,
        )
        assertTrue(item.numbered)
        assertEquals("3", item.label)
    }

    @Test
    fun `biblia sem numero no titulo cai no indice`() {
        val item = timelineItemFor(mediaId = "1234", title = "Salmos", subtitle = null, index = 4)
        assertTrue(item.numbered)
        assertEquals("5", item.label) // index + 1
    }

    @Test
    fun `estudo usa o titulo da aula do subtitle`() {
        val item = timelineItemFor(
            mediaId = "study_12_7",
            title = "Estudo sobre a Fé",   // título do estudo (não deve virar o rótulo)
            subtitle = "Aula 7 - A perseverança",
            index = 6,
        )
        assertFalse(item.numbered)
        assertEquals("Aula 7 - A perseverança", item.label)
    }

    @Test
    fun `estudo sem subtitle cai no titulo`() {
        val item = timelineItemFor(
            mediaId = "study_12_7",
            title = "Estudo sobre a Fé",
            subtitle = "",
            index = 0,
        )
        assertFalse(item.numbered)
        assertEquals("Estudo sobre a Fé", item.label)
    }

    @Test
    fun `sem titulo nem subtitle usa faixa e indice`() {
        val item = timelineItemFor(mediaId = "study_1_1", title = null, subtitle = null, index = 2)
        assertFalse(item.numbered)
        assertEquals("Faixa 3", item.label)
    }
}
