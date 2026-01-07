package br.app.ide.ouvindoabiblia.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data class Chapters(
        val bookId: String,
        val bookName: String
    ) : Screen

    @Serializable
    data class Player(
        val bookId: String,
        val bookTitle: String,
        val chapterNumber: Int,
        val coverUrl: String
    ) : Screen
}