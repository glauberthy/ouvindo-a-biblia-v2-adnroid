import androidx.room.ColumnInfo
import androidx.room.Embedded
import br.app.ide.ouvindoabiblia.data.local.entity.MomentEntity

data class MomentWithAudio(
    @Embedded val moment: MomentEntity,
    @ColumnInfo(name = "audioUrl") val audioUrl: String,
    @ColumnInfo(name = "bookName") val bookName: String
)