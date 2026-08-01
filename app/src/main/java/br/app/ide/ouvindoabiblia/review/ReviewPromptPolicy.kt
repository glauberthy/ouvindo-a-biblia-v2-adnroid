package br.app.ide.ouvindoabiblia.review

/**
 * Decisão pura de "é hora de pedir avaliação?".
 *
 * Está separada do resto porque é a ÚNICA parte da In-App Review que dá para provar. A API do
 * Play é cega de propósito (anti-abuso: se o app soubesse que o cartão não apareceu, tentaria de
 * novo até aparecer), então ela não diz se mostrou nada — e em build que não veio da Play ela
 * nunca mostra. Ou seja: o cartão em si só se confere com o olho, numa trilha de teste. Esta
 * função é o que sobra de testável, e por isso vive sozinha, no padrão de
 * [br.app.ide.ouvindoabiblia.playback.MediaContentId] e `TaskRemovalPolicy`.
 *
 * As regras existem para não queimar a cota invisível do Google com pedidos ruins:
 *  - **só pede a quem usou o app.** Pedir nota a quem abriu e não ouviu nada é pedir nota do nada.
 *  - **não pede na estreia.** [MIN_APP_OPENS] aberturas antes da primeira tentativa.
 *  - **espaça no tempo.** [MIN_DAYS_BETWEEN_PROMPTS] dias entre tentativas.
 *  - **desiste.** No máximo [MAX_PROMPTS] tentativas na vida da instalação: quem não avaliou em
 *    três oportunidades não vai avaliar na quarta.
 */
data class ReviewPromptState(
    /** Quantas vezes o app foi aberto (contado no processo, não por sessão de UI). */
    val appOpenCount: Int,
    /** Se o usuário já tocou algum áudio desde que instalou. */
    val hasPlayedAudio: Boolean,
    /** Dia epoch da última tentativa; [NEVER_PROMPTED] se nunca pediu. */
    val lastPromptEpochDay: Long,
    /** Quantas tentativas já foram feitas. */
    val promptCount: Int
) {
    companion object {
        const val NEVER_PROMPTED = -1L
    }
}

const val MIN_APP_OPENS = 4
const val MIN_DAYS_BETWEEN_PROMPTS = 60L
const val MAX_PROMPTS = 3

/**
 * @param todayEpochDay dia de hoje em epoch day. Entra por parâmetro — e não é lido aqui dentro —
 *   justamente para a função continuar pura e testável sem relógio.
 */
fun shouldAskForReview(state: ReviewPromptState, todayEpochDay: Long): Boolean {
    // Quem nunca ouviu nada não tem o que avaliar.
    if (!state.hasPlayedAudio) return false

    if (state.appOpenCount < MIN_APP_OPENS) return false

    if (state.promptCount >= MAX_PROMPTS) return false

    if (state.lastPromptEpochDay == ReviewPromptState.NEVER_PROMPTED) return true

    val elapsed = todayEpochDay - state.lastPromptEpochDay

    // Relógio andando para trás (fuso, viagem, usuário mexendo na data) daria `elapsed` negativo,
    // que passaria em qualquer comparação de "faz tempo suficiente?" mal escrita. Aqui o negativo
    // reprova junto com o "cedo demais": no pior caso o usuário deixa de ver um cartão, o que é
    // muito melhor do que vê-lo toda vez que o relógio recuar.
    return elapsed >= MIN_DAYS_BETWEEN_PROMPTS
}
