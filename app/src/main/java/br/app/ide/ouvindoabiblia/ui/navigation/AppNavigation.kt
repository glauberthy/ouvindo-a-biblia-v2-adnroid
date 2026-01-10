package br.app.ide.ouvindoabiblia.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Home : Screen

    // A rota Chapters ainda existe se quiser usar no futuro,
    // mas a Home vai pular ela agora.
    @Serializable
    data class Chapters(
        val bookId: String,
        val bookName: String
    ) : Screen

    @Serializable
    data class Player(
        val bookId: String,
        val bookTitle: String,
        val coverUrl: String
        // Removemos o chapterNumber
    ) : Screen
}