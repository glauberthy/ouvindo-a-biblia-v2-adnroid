package br.app.ide.ouvindoabiblia.review

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Espera antes de pedir. Sem isto o cartão competiria com a abertura do app e com o diálogo de
 * `POST_NOTIFICATIONS` (ISSUE 5.A), que aparece na primeira execução.
 */
private const val PROMPT_DELAY_MS = 15_000L

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val manager: InAppReviewManager
) : ViewModel() {

    /**
     * Uma tentativa por processo. O ViewModel sobrevive a mudança de configuração, então isto
     * também protege de rotacionar a tela e disparar de novo.
     */
    private var attemptedThisProcess = false

    fun onAppOpened() {
        viewModelScope.launch { manager.registerAppOpen() }
    }

    fun onAudioPlayed() {
        viewModelScope.launch { manager.registerAudioPlayed() }
    }

    fun maybeAsk(activity: Activity) {
        if (attemptedThisProcess) return
        attemptedThisProcess = true
        viewModelScope.launch { manager.maybeAsk(activity) }
    }
}

/**
 * Liga a avaliação in-app à tela principal.
 *
 * Fica num efeito à parte, e não dentro do `MainScreen`, para o assunto não se misturar com a
 * navegação e o player — o `MainScreen` já é grande.
 *
 * @param hasPlayedAudio se o usuário já tocou algo NESTA sessão. Alimenta o gate, que só pede nota
 *   a quem de fato usou o app.
 * @param isPlayerExpanded o cartão do Play cobre a tela; abri-lo com o player aberto atropela o
 *   que o usuário está fazendo. A política da Play pede explicitamente para não interromper tarefa.
 */
@Composable
fun ReviewPromptEffect(
    hasPlayedAudio: Boolean,
    isPlayerExpanded: Boolean,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.onAppOpened()
    }

    LaunchedEffect(hasPlayedAudio) {
        if (hasPlayedAudio) viewModel.onAudioPlayed()
    }

    LaunchedEffect(hasPlayedAudio, isPlayerExpanded) {
        if (!hasPlayedAudio || isPlayerExpanded) return@LaunchedEffect

        delay(PROMPT_DELAY_MS)

        // Reconferido DEPOIS da espera: em 15s o usuário pode ter aberto o player. O
        // LaunchedEffect é recriado se `isPlayerExpanded` mudar, mas a releitura aqui deixa a
        // intenção explícita para quem mexer nisto depois.
        if (isPlayerExpanded) return@LaunchedEffect

        context.findActivity()?.let { viewModel.maybeAsk(it) }
    }
}

/**
 * Desembrulha a Activity do Context do Compose. Não uso `LocalActivity` porque ele só existe a
 * partir do activity-compose 1.10 e o projeto está no 1.9.3 (`libs.versions.toml`).
 */
private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
