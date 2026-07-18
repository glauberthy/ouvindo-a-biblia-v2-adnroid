package br.app.ide.ouvindoabiblia.ui.player

import br.app.ide.ouvindoabiblia.data.repository.domain.model.Chapter
import br.app.ide.ouvindoabiblia.playback.MediaContentId

/**
 * Item da timeline do player para exibição na folha de capítulos (ISSUE 2.B).
 *
 * Projeção de exibição derivada do player, separada de [PlayerUiState.chapters]
 * (que agora usa o modelo de domínio `Chapter`, usado por Cast e favoritos).
 * Antes, a folha forçava tudo em número de capítulo — em Estudos gerava
 * numeração sintética ("1,2,3") no lugar dos títulos das aulas.
 *
 * @param label texto exibido: número do capítulo (Bíblia) ou título da aula (Estudo).
 * @param numbered `true` para Bíblia (grid de números); `false` para Estudo (lista de títulos).
 */
data class PlayerTimelineItem(
    val label: String,
    val numbered: Boolean,
)

/**
 * Deriva o [PlayerTimelineItem] de um item da timeline (ISSUE 2.B). Função pura
 * (testável) usada por `PlayerViewModel.extractTimelineFromPlayer`.
 *
 * - Bíblia → número do capítulo, extraído do fim do [title] ("Livro N");
 *   fallback = `index + 1`. `numbered = true` (grid).
 * - Estudo/demais → título da aula, que fica no [subtitle]; fallback em [title]
 *   ou "Faixa N". `numbered = false` (lista de títulos).
 */
internal fun timelineItemFor(
    mediaId: String,
    title: String?,
    subtitle: String?,
    index: Int,
): PlayerTimelineItem = when (MediaContentId.parse(mediaId)) {
    is MediaContentId.Bible -> {
        val number = title?.trim()?.substringAfterLast(' ')?.toIntOrNull() ?: (index + 1)
        PlayerTimelineItem(label = number.toString(), numbered = true)
    }

    else -> {
        val label = subtitle?.takeIf { it.isNotBlank() }
            ?: title?.takeIf { it.isNotBlank() }
            ?: "Faixa ${index + 1}"
        PlayerTimelineItem(label = label, numbered = false)
    }
}

/**
 * ISSUE 9.B — o item atual é uma aula de Estudo? Deriva do [MediaContentId] (mesma
 * fonte do isThemeMode). Usado pela capa do player: Estudos têm arte 1:1; o resto, 7:10.
 */
fun isStudyMediaId(mediaId: String?): Boolean =
    MediaContentId.parse(mediaId.orEmpty()) is MediaContentId.Study

data class PlayerUiState(
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val isBuffering: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val activeSleepTimerMinutes: Int = 0, // 0 significa Desativado
    // Lista e índice
    val currentChapterIndex: Int = 0,
    val chapters: List<Chapter> = emptyList(),
    // Projeção de exibição para a folha de capítulos (Bíblia: números; Estudo: títulos).
    val timeline: List<PlayerTimelineItem> = emptyList(),
    val isThemeMode: Boolean = false,
    // ISSUE 9.B: capa do player em 1:1 quando o conteúdo é Estudo (arte quadrada).
    val isStudyMode: Boolean = false,
    val currentIsFavorite: Boolean = false,
    val isSwitchingSource: Boolean = false,
    // ISSUE PUB-02: erro de reprodução (URL 404/rede fora após os retries do ExoPlayer).
    // Consumido uma vez pela UI (Toast) e limpo via consumePlaybackError(); null = sem erro.
    val playbackError: String? = null
)