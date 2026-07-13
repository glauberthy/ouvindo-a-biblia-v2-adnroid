package br.app.ide.ouvindoabiblia.playback

/**
 * Identidade de conteúdo codificada no `mediaId` do Media3 (ISSUE 2.A).
 *
 * Antes, o `mediaId` era formatado e parseado em ~10 lugares (PlaybackService,
 * PlayerViewModel, ChaptersViewModel) com convenções string soltas (`study_`,
 * `moment_`, `{bookId}|{idx}`, numérico). Este é o ÚNICO ponto de format/parse:
 * construa via os `data class` (e use [raw] no `setMediaId`), interprete via
 * [parse]. `mediaId` malformado → [parse] devolve `null` (o call-site loga/ignora).
 *
 * Convenções:
 *  - Bíblia  → id numérico do capítulo (ex.: `"1234"`).
 *  - Estudo  → `"study_{studyId}_{lessonId}"`.
 *  - Tema    → `"moment_{momentId}"` (deliberadamente NÃO persistido como retomada).
 *  - Pasta de livro (pedido de play "browsable") → `"{bookId}|{chapterIndex}"`,
 *    expandido no `onSetMediaItems` para a playlist completa do livro.
 */
sealed interface MediaContentId {

    /** String a colocar no `MediaItem.setMediaId(...)`. */
    val raw: String

    data class Bible(val chapterId: Long) : MediaContentId {
        override val raw: String get() = chapterId.toString()
    }

    data class Study(val studyId: Int, val lessonId: Int) : MediaContentId {
        override val raw: String get() = "$PREFIX_STUDY${studyId}_$lessonId"
    }

    /** O id do momento não é re-parseado em lugar nenhum (tema não retoma). */
    data class ThemeMoment(val momentId: String) : MediaContentId {
        override val raw: String get() = "$PREFIX_MOMENT$momentId"
    }

    data class BookFolder(val bookId: Int, val chapterIndex: Int) : MediaContentId {
        override val raw: String get() = "$bookId$SEP_BOOK_FOLDER$chapterIndex"
    }

    companion object {
        private const val PREFIX_STUDY = "study_"
        private const val PREFIX_MOMENT = "moment_"
        private const val SEP_BOOK_FOLDER = "|"

        /**
         * Interpreta um `mediaId` cru. Retorna `null` quando o id é malformado
         * (prefixo/partes inválidas), cabendo ao chamador logar e seguir seguro.
         *
         * Observação: ids de nós "navegáveis" da árvore de browse (o `numericId`
         * puro de um livro em `onGetChildren`) NÃO passam por aqui — só ids de
         * conteúdo reproduzível/salvo. Um numérico puro é sempre [Bible].
         */
        fun parse(raw: String): MediaContentId? = when {
            raw.startsWith(PREFIX_STUDY) -> {
                val parts = raw.removePrefix(PREFIX_STUDY).split("_")
                val studyId = parts.getOrNull(0)?.toIntOrNull()
                val lessonId = parts.getOrNull(1)?.toIntOrNull()
                if (studyId != null && lessonId != null) Study(studyId, lessonId) else null
            }

            raw.startsWith(PREFIX_MOMENT) -> {
                val momentId = raw.removePrefix(PREFIX_MOMENT)
                if (momentId.isNotEmpty()) ThemeMoment(momentId) else null
            }

            raw.contains(SEP_BOOK_FOLDER) -> {
                val parts = raw.split(SEP_BOOK_FOLDER)
                val bookId = parts.getOrNull(0)?.toIntOrNull()
                // Índice ausente/ inválido cai em 0 (comportamento herdado do onSetMediaItems).
                val index = parts.getOrNull(1)?.toIntOrNull() ?: 0
                if (bookId != null) BookFolder(bookId, index) else null
            }

            else -> raw.toLongOrNull()?.let { Bible(it) }
        }
    }
}
