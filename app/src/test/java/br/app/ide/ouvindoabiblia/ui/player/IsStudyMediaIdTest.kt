package br.app.ide.ouvindoabiblia.ui.player

import br.app.ide.ouvindoabiblia.playback.MediaContentId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ISSUE 9.B — [isStudyMediaId] decide a proporção da capa do player
 * (Estudo → 1:1; Bíblia/Tema/desconhecido → 7:10).
 */
class IsStudyMediaIdTest {

    @Test
    fun `mediaId de estudo retorna true`() {
        assertTrue(isStudyMediaId(MediaContentId.Study(1, 2).raw))
    }

    @Test
    fun `biblia tema nulo e malformado retornam false`() {
        assertFalse(isStudyMediaId(MediaContentId.Bible(1234L).raw))
        assertFalse(isStudyMediaId(MediaContentId.ThemeMoment("77").raw))
        assertFalse(isStudyMediaId(null))
        assertFalse(isStudyMediaId(""))
        assertFalse(isStudyMediaId("lixo_qualquer"))
    }
}
