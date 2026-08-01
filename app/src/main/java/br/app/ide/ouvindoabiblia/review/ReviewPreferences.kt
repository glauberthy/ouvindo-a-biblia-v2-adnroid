package br.app.ide.ouvindoabiblia.review

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistência do estado que alimenta [shouldAskForReview].
 *
 * **Por que este DataStore mora no `:app` e não no `:data:local`, contrariando a regra geral de
 * "toda persistência vive no `:data:local`":** o `:app` NÃO pode depender do `:data:local` (a
 * FASE 3.A cortou essa dependência de propósito; o caminho é `app` → `repository` → `local`).
 * Levar um contador de "quantas vezes abriu" até lá significaria expor métodos de avaliação no
 * `BibleRepository`, que é o ponto de orquestração do CONTEÚDO bíblico — estado de engajamento de
 * UI não tem o que fazer no domínio. Como são quatro valores escalares sem relação nenhuma com
 * Bíblia, temas ou estudos, ficam aqui.
 *
 * **Armadilha do Hilt:** o `:data:local` já publica um `DataStore<Preferences>` SEM qualificador
 * (`bible_settings`, em `DataStoreModule`), e o `:app` o enxerga pelo classpath transitivo do
 * repository. Um segundo binding sem qualificador quebraria a compilação com binding duplicado —
 * daí o [ReviewDataStore].
 */
@Singleton
class ReviewPreferences @Inject constructor(
    @ReviewDataStore private val dataStore: DataStore<Preferences>
) {

    suspend fun read(): ReviewPromptState {
        val prefs = dataStore.data.first()
        return ReviewPromptState(
            appOpenCount = prefs[KEY_APP_OPEN_COUNT] ?: 0,
            hasPlayedAudio = prefs[KEY_HAS_PLAYED_AUDIO] ?: false,
            lastPromptEpochDay = prefs[KEY_LAST_PROMPT_EPOCH_DAY]
                ?: ReviewPromptState.NEVER_PROMPTED,
            promptCount = prefs[KEY_PROMPT_COUNT] ?: 0
        )
    }

    suspend fun registerAppOpen() {
        dataStore.edit { prefs ->
            // Teto para o contador não crescer sem limite em quem usa o app todo dia — o valor só
            // é comparado com MIN_APP_OPENS, então acima disso a diferença não significa nada.
            val current = prefs[KEY_APP_OPEN_COUNT] ?: 0
            if (current < APP_OPEN_COUNT_CEILING) {
                prefs[KEY_APP_OPEN_COUNT] = current + 1
            }
        }
    }

    suspend fun registerAudioPlayed() {
        // Escrita idempotente: só grava na primeira vez, para não bater no disco a cada play.
        val alreadyMarked = dataStore.data.first()[KEY_HAS_PLAYED_AUDIO] ?: false
        if (alreadyMarked) return
        dataStore.edit { prefs -> prefs[KEY_HAS_PLAYED_AUDIO] = true }
    }

    /**
     * Registra que a TENTATIVA foi feita — não que o cartão apareceu, o que a API não informa.
     * Contar a tentativa é o que impede o app de insistir contra a cota invisível do Google.
     */
    suspend fun registerPromptAttempt(todayEpochDay: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_PROMPT_EPOCH_DAY] = todayEpochDay
            prefs[KEY_PROMPT_COUNT] = (prefs[KEY_PROMPT_COUNT] ?: 0) + 1
        }
    }

    private companion object {
        val KEY_APP_OPEN_COUNT = intPreferencesKey("review_app_open_count")
        val KEY_HAS_PLAYED_AUDIO = booleanPreferencesKey("review_has_played_audio")
        val KEY_LAST_PROMPT_EPOCH_DAY = longPreferencesKey("review_last_prompt_epoch_day")
        val KEY_PROMPT_COUNT = intPreferencesKey("review_prompt_count")

        const val APP_OPEN_COUNT_CEILING = 1_000
    }
}
