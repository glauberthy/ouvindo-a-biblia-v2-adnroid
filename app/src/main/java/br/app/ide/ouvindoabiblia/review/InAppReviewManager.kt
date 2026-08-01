package br.app.ide.ouvindoabiblia.review

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManager
// As extensões suspend vivem em `core.ktx`, não em `core.review.ktx` como o nome do artefato
// (`review-ktx`) sugere — conferido com javap dentro do próprio .aar.
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pede a avaliação in-app do Google Play (o cartão de estrelas que o próprio Play desenha por
 * cima do app).
 *
 * **Três coisas que NÃO dá para fazer com esta API — e que explicam o desenho daqui:**
 *
 * 1. **Não dá para saber se o cartão apareceu.** É cegueira deliberada, anti-abuso: se o app
 *    soubesse que não apareceu, tentaria de novo até aparecer. O listener completa igual, tendo
 *    mostrado ou não. Por isso [ReviewPreferences.registerPromptAttempt] conta TENTATIVA, não
 *    exibição — é o único fato de que dispomos.
 * 2. **Em build que não veio da Play, o cartão nunca aparece.** Instalação por `adb`/Android
 *    Studio — que é como este projeto testa — não renderiza nada. Não é bug e não há log: o fluxo
 *    completa em silêncio. A única forma de VER isto funcionando é uma trilha de teste interno ou
 *    Internal App Sharing, onde quem entrega o app é a Play.
 * 3. **A cota é do Google e é invisível.** Ele limita quantas vezes por usuário sem documentar o
 *    número. Insistir não aumenta a chance, só gasta tentativa. O gate de [shouldAskForReview]
 *    existe para não desperdiçar as poucas que temos com pedidos ruins.
 *
 * Política da Play que o código precisa respeitar: **não** disparar isto a partir de um botão
 * "Avaliar" (um botão desses tem de abrir a ficha na loja por URL) e **não** perguntar nada antes
 * ("Está gostando?"). Ambos reprovam.
 */
@Singleton
class InAppReviewManager @Inject constructor(
    private val reviewManager: ReviewManager,
    private val preferences: ReviewPreferences
) {

    suspend fun registerAppOpen() = preferences.registerAppOpen()

    suspend fun registerAudioPlayed() = preferences.registerAudioPlayed()

    /**
     * Avalia o gate e, se passar, dispara o fluxo. Silencioso por definição: retorna sem sinal
     * tanto quando decide não pedir quanto quando pede e o Play não mostra nada.
     */
    suspend fun maybeAsk(activity: Activity) {
        val today = LocalDate.now().toEpochDay()
        val state = preferences.read()

        if (!shouldAskForReview(state, today)) {
            Log.d(TAG, "Avaliação não pedida: gate reprovou ($state)")
            return
        }

        try {
            val reviewInfo = reviewManager.requestReview()

            // Registra ANTES de lançar, de propósito. Se registrasse depois e o `launchReview`
            // estourasse, o contador não subiria e o app tentaria de novo na próxima abertura,
            // toda vez, para sempre. Falhar fechado custa no máximo uma oportunidade.
            preferences.registerPromptAttempt(today)

            reviewManager.launchReview(activity, reviewInfo)
            Log.i(TAG, "Fluxo de avaliação disparado (não há como saber se o cartão apareceu)")
        } catch (e: Exception) {
            // Caminho normal fora da Play: a requisição falha e não há nada a fazer nem a dizer ao
            // usuário — ele não pediu isto. Engolir é o comportamento correto.
            Log.i(TAG, "Fluxo de avaliação indisponível: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "InAppReview"
    }
}
