package br.app.ide.ouvindoabiblia.service

/**
 * O que fazer quando o usuário remove o app dos recentes (swipe).
 */
internal enum class TaskRemovalDecision {
    /** Encerra playback + serviço + notificação. Swipe = intenção explícita de fechar. */
    STOP_SERVICE,

    /** Mantém a notificação (dismissível) para retomar por ela/headset — ISSUE 4.A. */
    KEEP_PAUSED_NOTIFICATION
}

/**
 * Decisão extraída como função pura porque foi exatamente aqui que morou o bug: o
 * caso "tocando" caía no ramo de MANTER e o áudio seguia tocando indefinidamente
 * depois do swipe, sem o usuário ter como pará-lo a não ser pela notificação.
 *
 * Tabela:
 *  - tocando                        -> STOP_SERVICE (o fix; antes era manter)
 *  - pausado após ter tocado        -> KEEP_PAUSED_NOTIFICATION (4.A)
 *  - sessão restaurada nunca tocada -> STOP_SERVICE (BUG B: não deixar notificação órfã)
 */
internal fun decideOnTaskRemoval(
    isPlaying: Boolean,
    hasStartedPlaybackThisProcess: Boolean
): TaskRemovalDecision =
    if (!isPlaying && hasStartedPlaybackThisProcess) {
        TaskRemovalDecision.KEEP_PAUSED_NOTIFICATION
    } else {
        TaskRemovalDecision.STOP_SERVICE
    }
