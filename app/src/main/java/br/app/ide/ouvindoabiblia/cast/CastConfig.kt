package br.app.ide.ouvindoabiblia.cast

/**
 * Ponto ÚNICO de liga/desliga do Google Cast (ISSUE 4.C).
 *
 * O Cast está FORA desta versão. O código de Cast (SessionManagerListener,
 * RemoteMediaClient, loadMediaOnCast, CastButton, CastOptionsProvider) fica
 * dormente no repo para reativar no futuro.
 *
 * Para reativar:
 *  1. [ENABLED] = true (religa o initializeCast() no PlayerViewModel);
 *  2. descomentar o CastButton em SharedPlayerScreen.
 *
 * Com [ENABLED] = false, o CastContext.getSharedInstance nunca é chamado — some a
 * violação de StrictMode (I/O de disco na main no startup/rotação) e o usuário não
 * vê o botão de uma função que não funciona.
 */
object CastConfig {
    const val ENABLED = false
}
