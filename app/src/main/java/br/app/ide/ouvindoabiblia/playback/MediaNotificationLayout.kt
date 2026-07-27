package br.app.ide.ouvindoabiblia.playback

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import com.google.common.collect.ImmutableList

/**
 * Botões que o app oferece às superfícies de mídia do sistema (ISSUE 10.B).
 *
 * Vale para TODAS elas de uma vez: sombra de notificação, controle do sistema, tela de
 * bloqueio e Android Auto. Antes não havia nenhum botão declarado — a notificação só tinha
 * o padrão do Media3 (anterior / play / próximo).
 *
 * **Por que ±segundos na frente de pular capítulo:** o conteúdo aqui é palavra falada, com
 * itens longos (um capítulo, uma aula de estudo). A necessidade real de quem ouve é "me
 * distraí, volta um pouco", não "pula a aula inteira". Os incrementos já existiam no player
 * (`MediaModule`: `seekBack` 10s / `seekForward` 30s) e simplesmente não tinham como ser
 * acionados fora do app.
 *
 * **Orçamento de slots:** o controle do sistema mostra ~5 ações. Os slots primários
 * ([CommandButton.SLOT_BACK]/[CommandButton.SLOT_FORWARD]) ficam com ±segundos, e
 * anterior/próximo vão para os slots secundários, exibidos pelas superfícies que têm espaço
 * (notificação expandida, Auto). Cada botão declara [CommandButton.SLOT_OVERFLOW] como
 * segunda opção: se a superfície não tiver o slot preferido, o botão vai para o menu em vez
 * de desaparecer.
 *
 * Os nomes são NEUTROS de propósito ("Anterior", não "Capítulo anterior"): a mesma fila
 * pode ser capítulo da Bíblia, aula de estudo ou momento de tema. O `displayName` é o que
 * leitores de tela anunciam e o que o Auto mostra.
 */
@OptIn(UnstableApi::class)
internal fun mediaButtonPreferences(): ImmutableList<CommandButton> = ImmutableList.of(
    CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
        .setPlayerCommand(Player.COMMAND_SEEK_BACK)
        .setDisplayName("Voltar 10 segundos")
        .setSlots(CommandButton.SLOT_BACK, CommandButton.SLOT_OVERFLOW)
        .build(),
    CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30)
        .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
        .setDisplayName("Avançar 30 segundos")
        .setSlots(CommandButton.SLOT_FORWARD, CommandButton.SLOT_OVERFLOW)
        .build(),
    CommandButton.Builder(CommandButton.ICON_PREVIOUS)
        .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        .setDisplayName("Anterior")
        .setSlots(CommandButton.SLOT_BACK_SECONDARY, CommandButton.SLOT_OVERFLOW)
        .build(),
    CommandButton.Builder(CommandButton.ICON_NEXT)
        .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        .setDisplayName("Próximo")
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY, CommandButton.SLOT_OVERFLOW)
        .build()
)
