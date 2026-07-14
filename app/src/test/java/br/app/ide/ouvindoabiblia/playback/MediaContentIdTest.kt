package br.app.ide.ouvindoabiblia.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trava as convenções de `mediaId` centralizadas na ISSUE 2.A: cada forma faz
 * round-trip (format → parse → mesmo valor) e ids malformados viram `null`.
 */
class MediaContentIdTest {

    // --- Round-trip: raw(x) parseado devolve x ---

    @Test
    fun `biblia round-trip`() {
        val id = MediaContentId.Bible(chapterId = 1234L)
        assertEquals("1234", id.raw)
        assertEquals(id, MediaContentId.parse(id.raw))
    }

    @Test
    fun `estudo round-trip`() {
        val id = MediaContentId.Study(studyId = 12, lessonId = 7)
        assertEquals("study_12_7", id.raw)
        assertEquals(id, MediaContentId.parse(id.raw))
    }

    @Test
    fun `tema round-trip`() {
        val id = MediaContentId.ThemeMoment(momentId = "99")
        assertEquals("moment_99", id.raw)
        assertEquals(id, MediaContentId.parse(id.raw))
    }

    // --- Discriminação de tipo ---

    @Test
    fun `numerico puro e biblia`() {
        assertTrue(MediaContentId.parse("42") is MediaContentId.Bible)
        assertEquals(42L, (MediaContentId.parse("42") as MediaContentId.Bible).chapterId)
    }

    // --- Malformados: null ---

    @Test
    fun `estudo sem lesson e nulo`() {
        assertNull(MediaContentId.parse("study_12"))
        assertNull(MediaContentId.parse("study_"))
        assertNull(MediaContentId.parse("study_abc_x"))
    }

    @Test
    fun `tema vazio e nulo`() {
        assertNull(MediaContentId.parse("moment_"))
    }

    @Test
    fun `id com pipe nao e mais reconhecido (pasta de livro removida na 3-D)`() {
        // "|" deixou de ser um separador especial; ids com pipe não são numéricos → null.
        assertNull(MediaContentId.parse("6|3"))
        assertNull(MediaContentId.parse("abc|2"))
    }

    @Test
    fun `texto nao numerico e nulo`() {
        assertNull(MediaContentId.parse("qualquer_coisa"))
        assertNull(MediaContentId.parse(""))
    }
}
