package br.app.ide.ouvindoabiblia.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "studies")
data class StudyEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val author: String,
    val description: String,
    @ColumnInfo(name = "image_url") val imageUrl: String
)