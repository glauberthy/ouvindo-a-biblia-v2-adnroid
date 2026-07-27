package br.app.ide.ouvindoabiblia.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Trava o texto da 2ª linha da notificação (ISSUE 10.A).
 *
 * O que estes testes protegem: a linha só existe porque o `subtitle` do Media3 NÃO chega à
 * notificação. Se alguém voltar a jogar o nome do app aqui, ou deixar a linha vazia quando o
 * conteúdo remoto vem incompleto, o usuário perde a única pista de O QUE está tocando fora
 * do app (qual aula da série, qual momento do tema).
 */
class MediaNotificationTextTest {

    // --- Bíblia -------------------------------------------------------------------------

    @Test
    fun `biblia mostra capitulo e total`() {
        assertEquals("Capítulo 3 de 4", bibleNotificationLine(chapterNumber = 3, totalChapters = 4))
    }

    @Test
    fun `biblia sem total conhecido omite o de N em vez de inventar`() {
        assertEquals("Capítulo 3", bibleNotificationLine(chapterNumber = 3, totalChapters = 0))
        assertEquals("Capítulo 3", bibleNotificationLine(chapterNumber = 3, totalChapters = -1))
    }

    // --- Estudo -------------------------------------------------------------------------

    @Test
    fun `estudo mostra posicao da aula e o titulo dela`() {
        assertEquals(
            "Aula 2 de 8 · Mortificar pecados",
            studyNotificationLine(lessonNumber = 2, totalLessons = 8, lessonTitle = "Mortificar pecados")
        )
    }

    @Test
    fun `estudo sem titulo de aula ainda diz a posicao`() {
        assertEquals("Aula 2 de 8", studyNotificationLine(2, 8, null))
        assertEquals("Aula 2 de 8", studyNotificationLine(2, 8, "   "))
    }

    @Test
    fun `estudo sem total conhecido mantem a aula`() {
        assertEquals("Aula 2 · Romanos 6", studyNotificationLine(2, 0, "Romanos 6"))
    }

    // --- Momento de tema ----------------------------------------------------------------

    @Test
    fun `momento mostra titulo e referencia`() {
        assertEquals(
            "Mortificar pecados · Cl 3:1-17",
            themeMomentNotificationLine("Mortificar pecados", "Cl 3:1-17")
        )
    }

    @Test
    fun `momento tolera campos vazios do conteudo remoto`() {
        assertEquals("Mortificar pecados", themeMomentNotificationLine("Mortificar pecados", ""))
        assertEquals("Cl 3:1-17", themeMomentNotificationLine(null, "Cl 3:1-17"))
    }

    @Test
    fun `momento sem nada cai no fallback em vez de linha vazia`() {
        assertEquals(NOTIFICATION_FALLBACK_LINE, themeMomentNotificationLine(null, null))
        assertEquals(NOTIFICATION_FALLBACK_LINE, themeMomentNotificationLine("  ", "  "))
    }

    // --- Regressão que motivou a issue --------------------------------------------------

    /**
     * O bug era literalmente "a linha é o nome do app". Nenhuma linha COM conteúdo pode
     * voltar a ser isso — o ícone do app já identifica o app na notificação.
     */
    @Test
    fun `nenhuma linha com conteudo repete o nome do app`() {
        val comConteudo = listOf(
            bibleNotificationLine(1, 50),
            studyNotificationLine(1, 8, "Romanos 6"),
            themeMomentNotificationLine("Mortificar pecados", "Cl 3:1-17")
        )
        comConteudo.forEach { linha ->
            assertFalse("linha ainda traz o nome do app: $linha", linha.contains("Ouvindo a Bíblia"))
        }
    }
}
