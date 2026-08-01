package br.app.ide.ouvindoabiblia.review

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Distingue o DataStore de avaliação do `bible_settings` publicado pelo `:data:local`, que chega
 * ao `:app` pelo classpath transitivo do repository. Sem este qualificador são dois
 * `DataStore<Preferences>` sem nome e o Hilt falha com binding duplicado.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReviewDataStore

private val Context.reviewDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "review_prefs"
)

@Module
@InstallIn(SingletonComponent::class)
object ReviewModule {

    @Provides
    @Singleton
    @ReviewDataStore
    fun provideReviewDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.reviewDataStore

    /**
     * O `ReviewManagerFactory.create` devolve o manager REAL mesmo em build de debug — não existe
     * variante que renderize o cartão fora de uma instalação vinda da Play. É por isso que este
     * caminho não tem como ser validado no emulador; ver [InAppReviewManager].
     */
    @Provides
    @Singleton
    fun provideReviewManager(@ApplicationContext context: Context): ReviewManager =
        ReviewManagerFactory.create(context)
}
