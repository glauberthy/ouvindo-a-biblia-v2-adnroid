package br.app.ide.ouvindoabiblia.ui.player

import br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo

data class PlayerUiState(
    val title: String = "",
    val subtitle: String = "",
    val artist: String = "Ouvindo a Bíblia",
    val imageUrl: String = "",
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val isBuffering: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = 0, // 0 = OFF, 1 = ONE, 2 = ALL
    val activeSleepTimerMinutes: Int = 0, // 0 significa Desativado
    // Lista e índice
    val currentChapterIndex: Int = 0,
    val chapters: List<ChapterWithBookInfo> = emptyList()
) {
    val progress: Float
        get() = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(
            0f,
            1f
        ) else 0f
}