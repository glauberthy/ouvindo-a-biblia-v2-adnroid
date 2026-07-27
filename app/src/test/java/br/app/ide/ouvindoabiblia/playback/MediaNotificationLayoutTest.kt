package br.app.ide.ouvindoabiblia.playback

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trava os botões oferecidos às superfícies de mídia (ISSUE 10.B).
 *
 * Vale um teste porque o erro aqui é SILENCIOSO: um botão com o comando errado aparece
 * bonito na notificação e não faz nada, e um slot trocado só se descobre olhando o
 * aparelho. `CommandButton` constrói na JVM (verificado), então dá para conferir sem
 * subir sessão nem Robolectric.
 */
@OptIn(UnstableApi::class)
class MediaNotificationLayoutTest {

    private val buttons = mediaButtonPreferences()

    @Test
    fun `oferece exatamente os quatro botoes previstos`() {
        assertEquals(4, buttons.size)
    }

    /**
     * O ponto da issue: em palavra falada, ±segundos ocupa os slots PRINCIPAIS, e pular
     * item vai para os secundários. Se alguém invertesse, o app voltaria a só oferecer
     * "pula a aula inteira" onde o usuário quer "volta um pouco".
     */
    @Test
    fun `mais e menos segundos ficam nos slots principais`() {
        val back = buttons.single { it.playerCommand == Player.COMMAND_SEEK_BACK }
        val forward = buttons.single { it.playerCommand == Player.COMMAND_SEEK_FORWARD }

        assertEquals(CommandButton.SLOT_BACK, back.slots[0])
        assertEquals(CommandButton.SLOT_FORWARD, forward.slots[0])
    }

    @Test
    fun `anterior e proximo ficam nos slots secundarios`() {
        val previous = buttons.single {
            it.playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
        }
        val next = buttons.single {
            it.playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
        }

        assertEquals(CommandButton.SLOT_BACK_SECONDARY, previous.slots[0])
        assertEquals(CommandButton.SLOT_FORWARD_SECONDARY, next.slots[0])
    }

    /**
     * Ícones do próprio Media3 em vez de drawable nosso: eles já vêm com o número certo
     * desenhado (10 / 30) e seguem o estilo do sistema. Se o incremento do player mudar,
     * este teste lembra que o ícone tem de mudar junto — senão o botão MENTE o valor.
     */
    @Test
    fun `icones batem com os incrementos configurados no player`() {
        val back = buttons.single { it.playerCommand == Player.COMMAND_SEEK_BACK }
        val forward = buttons.single { it.playerCommand == Player.COMMAND_SEEK_FORWARD }

        assertEquals(CommandButton.ICON_SKIP_BACK_10, back.icon)
        assertEquals(CommandButton.ICON_SKIP_FORWARD_30, forward.icon)
    }

    /**
     * Sem uma segunda opção de slot, a superfície que não tem o slot preferido simplesmente
     * NÃO mostra o botão. O overflow garante que ele apareça no menu em vez de sumir.
     */
    @Test
    fun `todo botao tem overflow como alternativa`() {
        buttons.forEach { button ->
            assertTrue(
                "botão '${button.displayName}' não tem SLOT_OVERFLOW como alternativa",
                button.slots.contains(CommandButton.SLOT_OVERFLOW)
            )
        }
    }

    /**
     * A mesma fila pode ser capítulo da Bíblia, aula de estudo ou momento de tema — um
     * rótulo específico de um tipo estaria errado nos outros dois (é o que leitor de tela
     * anuncia e o que o Android Auto mostra).
     */
    @Test
    fun `rotulos nao presumem o tipo de conteudo`() {
        val proibidos = listOf("Capítulo", "Aula", "Momento")
        buttons.forEach { button ->
            val nome = button.displayName.toString()
            proibidos.forEach { palavra ->
                assertTrue(
                    "rótulo '$nome' presume tipo de conteúdo ('$palavra')",
                    !nome.contains(palavra)
                )
            }
        }
    }

    @Test
    fun `todo botao tem nome e comando de player`() {
        buttons.forEach { button ->
            assertTrue("botão sem displayName", button.displayName.isNotEmpty())
            assertTrue(
                "botão '${button.displayName}' sem comando de player",
                button.playerCommand != Player.COMMAND_INVALID
            )
        }
    }
}
