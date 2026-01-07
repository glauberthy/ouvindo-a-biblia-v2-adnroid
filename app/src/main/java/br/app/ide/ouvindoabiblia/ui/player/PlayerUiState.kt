package br.app.ide.ouvindoabiblia.ui.player

data class PlayerUiState(
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val isBuffering: Boolean = false
) {
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
}