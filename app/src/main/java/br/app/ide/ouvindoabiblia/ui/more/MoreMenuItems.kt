package br.app.ide.ouvindoabiblia.ui.more

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.ui.graphics.vector.ImageVector

data class MoreMenuItemUi(
    val id: String,
    val title: String,
    val description: String,
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

val moreMenuItems = listOf(
    MoreMenuItemUi(
        id = MoreMenuIds.ABOUT,
        title = "Sobre o app",
        description = "Conheça a proposta e o propósito do Ouvindo a Bíblia.",
        icon = Icons.Filled.Info
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.MISSION,
        title = "Missão",
        description = "Entenda a missão espiritual e editorial do aplicativo.",
        icon = Icons.AutoMirrored.Filled.MenuBook
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.PRIVACY,
        title = "Política de privacidade",
        description = "Veja como tratamos informações e preferências do usuário.",
        icon = Icons.Filled.PrivacyTip
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.BIBLE_AUDIO_RIGHTS,
        title = "Direitos dos áudios bíblicos",
        description = "Créditos e autorizações relacionados à narração bíblica.",
        icon = Icons.Filled.GraphicEq
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.STUDY_RIGHTS,
        title = "Direitos dos estudos",
        description = "Créditos e autorizações dos estudos e conteúdos em áudio.",
        icon = Icons.Filled.GraphicEq
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.COVER_RIGHTS,
        title = "Direitos das capas e imagens",
        description = "Referências visuais, autores e uso editorial das imagens.",
        icon = Icons.Filled.Image
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.CURATION,
        title = "Curadoria de temas e estudos",
        description = "Pessoas que participaram da organização e revisão do conteúdo.",
        icon = Icons.AutoMirrored.Filled.MenuBook
    ),
    MoreMenuItemUi(
        id = MoreMenuIds.LICENSES,
        title = "Licenças open source",
        description = "Bibliotecas e licenças utilizadas no aplicativo.",
        icon = Icons.Filled.Code
    )
)