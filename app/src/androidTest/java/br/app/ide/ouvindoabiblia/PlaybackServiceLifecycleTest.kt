package br.app.ide.ouvindoabiblia

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.app.ide.ouvindoabiblia.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Trava a regressão do bug 5.1 (DIAGNOSTICO_02 §5.1 / ROADMAP-ISSUE 0.3).
 *
 * Propriedade garantida: quando a reprodução começa, o PlaybackService é
 * INICIADO (startForegroundService, em produção via PlayerViewModel.ensureServiceStarted).
 * Um serviço *started* sobrevive ao unbind de TODOS os clientes — então destruir a
 * Activity (que libera o MediaController) NÃO pode destruir o serviço nem liberar o player.
 *
 * Aqui iniciamos o serviço com startService (mesma garantia de sobrevivência ao unbind,
 * sem o contrato de 5s do startForegroundService nem necessidade de áudio real — o player
 * de produção usa data source só-HTTP, inviável de tocar de forma hermética).
 */
@RunWith(AndroidJUnit4::class)
class PlaybackServiceLifecycleTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val serviceIntent = Intent(context, PlaybackService::class.java)

    private val controllers = mutableListOf<ListenableFuture<MediaController>>()

    @Before
    fun setUp() {
        PlaybackService.createCount.set(0)
        PlaybackService.destroyCount.set(0)
    }

    @After
    fun tearDown() {
        runOnMain { controllers.forEach { MediaController.releaseFuture(it) } }
        context.stopService(serviceIntent)
    }

    @Test
    fun servicoSobreviveAoUnbindDoController_ePlayerNaoEhLiberado() {
        // 1) Inicia o serviço (estado STARTED — sobrevive ao unbind).
        context.startService(serviceIntent)

        // 2) Controller #1 conecta e define a playlist.
        val (future1, controller1) = buildConnectedController()
        val mediaId = "12345"
        runOnMain {
            controller1.setMediaItems(
                listOf(
                    MediaItem.Builder()
                        .setMediaId(mediaId)
                        .setUri("https://example.invalid/a.ogg")
                        .build()
                )
            )
        }
        assertEquals("playlist deveria ter 1 item antes do unbind", 1, readOnMain { controller1.mediaItemCount })

        // 3) Activity "morre" → MediaController é liberado (unbind do último cliente).
        runOnMain { MediaController.releaseFuture(future1) }
        controllers.remove(future1)

        // 4) Janela de observação: perder o cliente NÃO pode destruir o serviço.
        val deadline = SystemClock.uptimeMillis() + 3_000
        while (SystemClock.uptimeMillis() < deadline) {
            assertEquals(
                "serviço NÃO deveria ser destruído ao unbind (regressão 5.1)",
                0, PlaybackService.destroyCount.get()
            )
            Thread.sleep(100)
        }

        // 5) Reconecta: sessão/player continuam vivos COM o estado — não foram
        //    liberados nem recriados vazios.
        val (_, controller2) = buildConnectedController()
        assertEquals("serviço não pode ter sido destruído na janela", 0, PlaybackService.destroyCount.get())
        assertEquals("a playlist deveria persistir após o unbind", 1, readOnMain { controller2.mediaItemCount })
        assertEquals(
            "o item deveria ser o mesmo (player não foi liberado)",
            mediaId,
            readOnMain { controller2.currentMediaItem?.mediaId ?: controller2.getMediaItemAt(0).mediaId }
        )
    }

    // --- helpers (MediaController exige main thread) ---

    private fun buildConnectedController(): Pair<ListenableFuture<MediaController>, MediaController> {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        var future: ListenableFuture<MediaController>? = null
        runOnMain { future = MediaController.Builder(context, token).buildAsync() }
        val f = future!!
        controllers.add(f)
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (!f.isDone && SystemClock.uptimeMillis() < deadline) Thread.sleep(50)
        assertTrue("MediaController não conectou a tempo", f.isDone)
        var controller: MediaController? = null
        runOnMain { controller = f.get() }
        return f to controller!!
    }

    private fun runOnMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun <T> readOnMain(block: () -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}
