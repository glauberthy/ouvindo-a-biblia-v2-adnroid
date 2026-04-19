package br.app.ide.ouvindoabiblia.ui.more

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

data class MoreMenuItemUi(
    val id: String,
    val title: String,
    val description: AnnotatedString,
    val icon: ImageVector
)

object MoreMenuIds {
    const val ABOUT = "about"
    const val MISSION = "mission"
    const val PRIVACY = "privacy"
    const val BIBLE_AUDIO_RIGHTS = "bible_audio_rights"
    const val STUDY_RIGHTS = "study_audio_rights"
    const val COVER_RIGHTS = "cover_rights"
    const val CURATION = "curation"
    const val LICENSES = "licenses"
}

private fun moreDescription(
    normalText: String,
    boldText: String? = null,
    suffix: String = ""
): AnnotatedString {
    return buildAnnotatedString {
        append(normalText)

        if (!boldText.isNullOrBlank()) {
            withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(boldText)
            }
        }

        append(suffix)
    }
}

val moreMenuItems = listOf(
    MoreMenuItemUi(
        id = MoreMenuIds.ABOUT,
        title = "Sobre o app",
        description = moreDescription(
            normalText = "Conheça a proposta e o propósito do ",
            boldText = "Ouvindo a Bíblia",
            suffix = "."
        ),
        icon = Icons.Filled.Info
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.MISSION,
        title = "Missão",
        description = moreDescription(
            normalText = "Entenda a missão editorial do aplicativo."
        ),
        icon = Icons.AutoMirrored.Filled.MenuBook
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.PRIVACY,
        title = "Política de privacidade",
        description = moreDescription(
            normalText = "Veja como tratamos informações e preferências do usuário."
        ),
        icon = Icons.Filled.PrivacyTip
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.BIBLE_AUDIO_RIGHTS,
        title = "Direitos dos áudios bíblicos",
        description = moreDescription(
            normalText = "Créditos e autorizações relacionados à narração bíblica."
        ),
        icon = Icons.Filled.GraphicEq
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.STUDY_RIGHTS,
        title = "Direitos dos estudos",
        description = moreDescription(
            normalText = "Créditos e autorizações dos estudos e conteúdos em áudio."
        ),
        icon = Icons.Filled.GraphicEq
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.COVER_RIGHTS,
        title = "Direitos das capas e imagens",
        description = moreDescription(
            normalText = "Referências visuais, autores e uso editorial das imagens."
        ),
        icon = Icons.Filled.Image
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.CURATION,
        title = "Curadoria de temas e estudos",
        description = moreDescription(
            normalText = "Pessoas que participaram da organização e revisão do conteúdo."
        ),
        icon = Icons.AutoMirrored.Filled.MenuBook
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.LICENSES,
        title = "Licenças open source",
        description = moreDescription(
            normalText = "Bibliotecas e licenças utilizadas no aplicativo."
        ),
        icon = Icons.Filled.Code
    )
)