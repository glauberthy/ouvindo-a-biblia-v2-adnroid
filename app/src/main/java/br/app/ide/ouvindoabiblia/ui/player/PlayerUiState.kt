package br.app.ide.ouvindoabiblia.ui.player

data class PlayerUiState(
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val isBuffering: Boolean = false,
    val currentChapterIndex: Int = 0, // Para destacar qual está tocando
    val chapters: List<br.app.ide.ouvindoabiblia.data.local.model.ChapterWithBookInfo> = emptyList() // A lista para exibir
) {
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
}