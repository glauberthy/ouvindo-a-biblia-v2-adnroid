package br.app.ide.ouvindoabiblia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "themes")
data class ThemeEntity(
    @PrimaryKey val id: Int, // ID vindo do JSON
    val title: String,       // "Ansiedade e Confiança"
    val description: String, // Breve descrição do tema
    val imageUrl: String?    // Capa do card temático
)