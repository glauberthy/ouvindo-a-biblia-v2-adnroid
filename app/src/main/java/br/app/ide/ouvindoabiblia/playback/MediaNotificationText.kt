package br.app.ide.ouvindoabiblia.playback

/**
 * Texto da 2ª linha da notificação de mídia (ISSUE 10.A).
 *
 * Por quê existe: a notificação (e o controle do sistema, e a tela de bloqueio, e o Android
 * Auto) mostra `MediaMetadata.title` na linha 1 e **`artist`** na linha 2 — o `subtitle`
 * NÃO é exibido. O app enchia o `artist` com o nome do app em todos os itens, ou seja,
 * gastava a única linha de apoio repetindo o que o ícone já diz, e a informação que o
 * usuário escolheu ouvir ficava só dentro do app:
 *
 *  - Estudo: `title` = nome da SÉRIE e `subtitle` = nome da AULA → fora do app não havia
 *    como saber qual aula estava tocando.
 *  - Momento de tema: `title` = "Colossenses 3" e `subtitle` = "Mortificar pecados…
 *    (Cl 3:1-17)" → o momento escolhido desaparecia (reproduzido no emulador em 2026-07-26).
 *
 * O `artist` não tem nenhum consumidor dentro do app (só `title`/`albumTitle`/`subtitle`
 * são lidos pela UI e pela persistência), então ele é exatamente o campo livre para isto.
 *
 * Estas funções são puras para o teste poder travar o formato sem subir Media3.
 */

/** Separador entre as duas informações da linha. Ponto médio, não hífen, para não competir
 *  com hífens que já aparecem em títulos de estudo ("Ester - Vida Cristã…"). */
private const val SEP = " · "

/**
 * Último recurso quando não há nada melhor a dizer. Deixar a linha VAZIA é pior: o controle
 * do sistema fica com um buraco sob o título, o que parece defeito.
 */
internal const val NOTIFICATION_FALLBACK_LINE = "Ouvindo a Bíblia"

/**
 * Bíblia → "Capítulo 3 de 4". O número sozinho já está no título ("Colossenses 3"); o que
 * esta linha acrescenta é o TOTAL, isto é, onde o ouvinte está dentro do livro.
 *
 * [totalChapters] inválido (0 ou negativo, playlist desconhecida) → cai para "Capítulo 3"
 * em vez de inventar um total errado.
 */
internal fun bibleNotificationLine(chapterNumber: Int, totalChapters: Int): String =
    if (totalChapters > 0) {
        "Capítulo $chapterNumber de $totalChapters"
    } else {
        "Capítulo $chapterNumber"
    }

/**
 * Estudo → "Aula 2 de 8 · Mortificar pecados; revestir". A aula é o que muda dentro da
 * série, e a série já está no título — por isso ela vem aqui, com a posição na frente
 * (informação curta, sobrevive à elipse quando o título da aula é longo).
 */
internal fun studyNotificationLine(
    lessonNumber: Int,
    totalLessons: Int,
    lessonTitle: String?
): String {
    val position = if (totalLessons > 0) {
        "Aula $lessonNumber de $totalLessons"
    } else {
        "Aula $lessonNumber"
    }
    val title = lessonTitle?.trim().orEmpty()
    return if (title.isEmpty()) position else "$position$SEP$title"
}

/**
 * Momento de tema → "Mortificar pecados; revestir · Cl 3:1-17". Aqui a ORDEM se inverte:
 * o título do momento vem primeiro porque é o que o usuário escolheu na lista do tema; a
 * referência é o complemento e é curta, então sobrevive no fim.
 *
 * Campos vazios são tolerados (o `themes.json` é conteúdo remoto): sem referência, sobra o
 * título; sem título, sobra a referência; sem nenhum dos dois, [NOTIFICATION_FALLBACK_LINE].
 */
internal fun themeMomentNotificationLine(momentTitle: String?, reference: String?): String {
    val title = momentTitle?.trim().orEmpty()
    val ref = reference?.trim().orEmpty()
    return when {
        title.isNotEmpty() && ref.isNotEmpty() -> "$title$SEP$ref"
        title.isNotEmpty() -> title
        ref.isNotEmpty() -> ref
        else -> NOTIFICATION_FALLBACK_LINE
    }
}
