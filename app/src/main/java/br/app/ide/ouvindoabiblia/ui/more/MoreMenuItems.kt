package br.app.ide.ouvindoabiblia.ui.more

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
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

    /**
     * Entrada que existe APENAS na UI, para agrupar os itens jurídicos. Não é uma seção
     * do `mais.json`, então nunca deve ser procurada em `content.sections` — ela navega
     * para a sublista em vez de abrir conteúdo.
     */
    const val RIGHTS_GROUP = "rights_group"
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

// Cada item tem ícone PRÓPRIO: ícone repetido anula a função dele, que é permitir achar
// a linha batendo o olho.
private val aboutItem = MoreMenuItemUi(
    id = MoreMenuIds.ABOUT,
    title = "Sobre o app",
    description = moreDescription(
        normalText = "Conheça a proposta e o propósito do ",
        boldText = "Ouvindo a Bíblia",
        suffix = "."
    ),
    icon = Icons.Filled.Info
)

private val missionItem = MoreMenuItemUi(
    id = MoreMenuIds.MISSION,
    title = "Missão",
    description = moreDescription(
        normalText = "Entenda a missão editorial do aplicativo."
    ),
    icon = Icons.AutoMirrored.Filled.MenuBook
)

private val curationItem = MoreMenuItemUi(
    id = MoreMenuIds.CURATION,
    title = "Curadoria de temas e estudos",
    description = moreDescription(
        normalText = "Pessoas que participaram da organização e revisão do conteúdo."
    ),
    icon = Icons.Filled.Groups
)

private val privacyItem = MoreMenuItemUi(
    id = MoreMenuIds.PRIVACY,
    title = "Política de privacidade",
    description = moreDescription(
        normalText = "Veja como tratamos informações e preferências do usuário."
    ),
    icon = Icons.Filled.PrivacyTip
)

private val bibleAudioRightsItem = MoreMenuItemUi(
    id = MoreMenuIds.BIBLE_AUDIO_RIGHTS,
    title = "Direitos dos áudios bíblicos",
    description = moreDescription(
        normalText = "Créditos e autorizações relacionados à narração bíblica."
    ),
    icon = Icons.Filled.GraphicEq
)

private val studyRightsItem = MoreMenuItemUi(
    id = MoreMenuIds.STUDY_RIGHTS,
    title = "Direitos dos estudos",
    description = moreDescription(
        normalText = "Créditos e autorizações dos estudos e conteúdos em áudio."
    ),
    icon = Icons.Filled.Headphones
)

private val coverRightsItem = MoreMenuItemUi(
    id = MoreMenuIds.COVER_RIGHTS,
    title = "Direitos das capas e imagens",
    description = moreDescription(
        normalText = "Referências visuais, autores e uso editorial das imagens."
    ),
    icon = Icons.Filled.Image
)

private val licensesItem = MoreMenuItemUi(
    id = MoreMenuIds.LICENSES,
    title = "Licenças open source",
    description = moreDescription(
        normalText = "Bibliotecas e licenças utilizadas no aplicativo."
    ),
    icon = Icons.Filled.Code
)

/** Entrada da raiz que abre a sublista dos itens jurídicos. */
val rightsGroupMenuItem = MoreMenuItemUi(
    id = MoreMenuIds.RIGHTS_GROUP,
    title = "Direitos e licenças",
    description = moreDescription(
        normalText = "Privacidade, créditos de áudio e imagens, e licenças open source."
    ),
    icon = Icons.Filled.Gavel
)

/**
 * Todos os itens que abrem conteúdo (folhas). O `id` casa com o `id` da seção no
 * `mais.json` — é por aqui que a tela resolve o conteúdo de cada item.
 */
val moreMenuItems = listOf(
    aboutItem,
    missionItem,
    privacyItem,
    bibleAudioRightsItem,
    studyRightsItem,
    coverRightsItem,
    curationItem,
    licensesItem
)

/**
 * Raiz da tela Mais: o que é institucional fica visível, e os 5 itens jurídicos
 * (raramente lidos, mas obrigatórios) ficam atrás de uma única entrada. Sem isso os 8
 * cards competem com o mesmo peso visual e nada é prioritário.
 */
val moreRootMenuItems = listOf(
    aboutItem,
    missionItem,
    curationItem,
    rightsGroupMenuItem
)

/** Itens dentro de "Direitos e licenças", na ordem em que aparecem na sublista. */
val moreRightsMenuItems = listOf(
    privacyItem,
    bibleAudioRightsItem,
    studyRightsItem,
    coverRightsItem,
    licensesItem
)
