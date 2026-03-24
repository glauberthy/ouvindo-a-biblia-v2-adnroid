package br.app.ide.ouvindoabiblia.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_lessons",
    foreignKeys = [
        ForeignKey(
            entity = StudyEntity::class,
            parentColumns = ["id"],
            childColumns = ["studyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studyId"])]
)
data class StudyLessonEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "studyId") val studyId: Int,
    val title: String,
    val url: String,
    val duration: Long
)