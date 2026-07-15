package br.app.ide.ouvindoabiblia

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Valida a ISSUE 4.D (Android Auto): a árvore de navegação e o playback por `mediaId`
 * expostos pelo `MediaLibrarySession` do [br.app.ide.ouvindoabiblia.service.PlaybackService].
 *
 * Conecta um [MediaBrowser] real (o mesmo protocolo que o Android Auto/AAOS usa) e exercita
 * os callbacks `onGetLibraryRoot`/`onGetChildren`/`onGetItem` e a resolução de playback via
 * `onSetMediaItems` (item navegado, só com `mediaId`, expande na playlist completa com URI).
 *
 * É validação independente da UI do carro (que no emulador AAOS filtra apps user-installed).
 *
 * PRÉ-REQUISITO: o conteúdo precisa estar sincronizado no Room (abrir o app uma vez com rede).
 * Sem conteúdo, a árvore vem vazia — o teste falha com mensagem explícita pedindo o sync.
 */
@RunWith(AndroidJUnit4::class)
class AndroidAutoBrowseTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext

    private val browsers = mutableListOf<ListenableFuture<MediaBrowser>>()

    @After
    fun tearDown() {
        runOnMain { browsers.forEach { MediaBrowser.releaseFuture(it) } }
    }

    @Test
    fun arvoreDeNavegacao_raizLivrosCapitulos_eResolvePlaybackPorMediaId() {
        val browser = buildConnectedBrowser()

        // 1) Raiz navegável (onGetLibraryRoot).
        val root = awaitLibrary { browser.getLibraryRoot(null) }
        assertEquals("raiz deveria vir com sucesso", LibraryResult.RESULT_SUCCESS, root.resultCode)
        assertEquals("mediaId da raiz", "root_bible", root.value!!.mediaId)

        // 2) Filhos da raiz = LIVROS (navegáveis), via onGetChildren.
        val books = awaitChildren { browser.getChildren("root_bible", 0, 200, null) }
        assertTrue(
            "árvore vazia: sincronize o conteúdo abrindo o app com rede antes do teste",
            books.isNotEmpty()
        )
        val firstBook = books.first()
        assertTrue("livro deveria ser navegável", firstBook.mediaMetadata.isBrowsable == true)
        assertTrue("livro NÃO deveria ser tocável", firstBook.mediaMetadata.isPlayable != true)

        // 3) Filhos de um livro = CAPÍTULOS (tocáveis).
        val chapters = awaitChildren { browser.getChildren(firstBook.mediaId, 0, 500, null) }
        assertTrue("livro ${firstBook.mediaId} deveria ter capítulos", chapters.isNotEmpty())
        val firstChapter = chapters.first()
        assertTrue("capítulo deveria ser tocável", firstChapter.mediaMetadata.isPlayable == true)

        // 4) onGetItem resolve o capítulo (deixou de ser stub ERROR_NOT_SUPPORTED).
        val item = awaitLibrary { browser.getItem(firstChapter.mediaId) }
        assertEquals("onGetItem deveria resolver o capítulo", LibraryResult.RESULT_SUCCESS, item.resultCode)
        assertEquals(firstChapter.mediaId, item.value!!.mediaId)

        // 5) DEALBREAKER: playback por mediaId. O Auto reenvia só o id (sem URI). Enviamos um
        //    item cru (sem localConfiguration) e esperamos que onSetMediaItems expanda no livro
        //    inteiro, começando no capítulo pedido — provando a resolução com URI server-side.
        val chapterId = firstChapter.mediaId
        runOnMain {
            browser.setMediaItem(MediaItem.Builder().setMediaId(chapterId).build())
            browser.prepare()
        }
        // A expansão é assíncrona (serviceScope IO) e o controller primeiro reflete
        // OTIMISTICAMENTE o 1 item enviado, só depois recebe os N expandidos. Aguardamos
        // a playlist estabilizar no tamanho do livro (não sair no primeiro count>0).
        val deadline = SystemClock.uptimeMillis() + 10_000
        var count = 0
        while (SystemClock.uptimeMillis() < deadline) {
            count = readOnMain { browser.mediaItemCount }
            if (count >= chapters.size) break
            Thread.sleep(100)
        }
        assertTrue("setMediaItem por mediaId deveria produzir uma playlist tocável", count > 0)
        assertEquals(
            "deveria expandir no livro inteiro (mesma qtde de capítulos)",
            chapters.size, count
        )
        assertEquals(
            "o item corrente deveria ser o capítulo pedido (startIndex correto)",
            chapterId,
            readOnMain { browser.currentMediaItem?.mediaId }
        )
    }

    // --- helpers (MediaBrowser exige a thread que o construiu = main) ---

    private fun buildConnectedBrowser(): MediaBrowser {
        val token = SessionToken(context, ComponentName(context, "br.app.ide.ouvindoabiblia.service.PlaybackService"))
        var future: ListenableFuture<MediaBrowser>? = null
        runOnMain { future = MediaBrowser.Builder(context, token).buildAsync() }
        val f = future!!
        browsers.add(f)
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (!f.isDone && SystemClock.uptimeMillis() < deadline) Thread.sleep(50)
        assertTrue("MediaBrowser não conectou a tempo", f.isDone)
        var browser: MediaBrowser? = null
        runOnMain { browser = f.get() }
        assertNotNull(browser)
        return browser!!
    }

    private fun awaitLibrary(supplier: () -> ListenableFuture<LibraryResult<MediaItem>>): LibraryResult<MediaItem> {
        var f: ListenableFuture<LibraryResult<MediaItem>>? = null
        runOnMain { f = supplier() }
        return awaitFuture(f!!)
    }

    private fun awaitChildren(
        supplier: () -> ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>
    ): List<MediaItem> {
        var f: ListenableFuture<LibraryResult<ImmutableList<MediaItem>>>? = null
        runOnMain { f = supplier() }
        val result = awaitFuture(f!!)
        assertEquals("getChildren deveria vir com sucesso", LibraryResult.RESULT_SUCCESS, result.resultCode)
        return result.value ?: emptyList()
    }

    private fun <T> awaitFuture(f: ListenableFuture<T>): T {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (!f.isDone && SystemClock.uptimeMillis() < deadline) Thread.sleep(50)
        assertTrue("future não completou a tempo", f.isDone)
        var result: T? = null
        runOnMain { result = f.get() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun runOnMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun <T> readOnMain(block: () -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}
