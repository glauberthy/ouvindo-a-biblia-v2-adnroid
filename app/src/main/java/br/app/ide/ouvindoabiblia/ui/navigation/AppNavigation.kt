package br.app.ide.ouvindoabiblia.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    // Telas Principais da Bottom Bar
    @Serializable
    data object Home : Screen
    @Serializable
    data object Favorites : Screen
    @Serializable
    data object Search : Screen
    @Serializable
    data object History : Screen
    @Serializable
    data object More : Screen

    // Telas de Detalhe
    @Serializable
    data class Player(
        val bookId: String,
        val bookTitle: String,
        val coverUrl: String
    ) : Screen

    @Serializable
    data class Chapters(
        val bookId: String,
        val bookName: String
    ) : Screen

    // Conteúdo do Menu "Mais"
    @Serializable
    data object About : Screen
    @Serializable
    data object Copyright : Screen
}