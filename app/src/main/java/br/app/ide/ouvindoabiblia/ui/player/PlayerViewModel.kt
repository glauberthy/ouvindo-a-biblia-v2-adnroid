package br.app.ide.ouvindoabiblia.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import br.app.ide.ouvindoabiblia.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
                updateStateFromPlayer() // Carga inicial
                startProgressLoop() // Começa a atualizar a barra de progresso
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Ouve eventos do Media3 (Pause, Play, Troca de Música)
    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                updateStateFromPlayer()
            }
        })
    }

    private fun updateStateFromPlayer() {
        mediaController?.let { player ->
            val mediaItem = player.currentMediaItem

            _uiState.update { state ->
                state.copy(
                    isPlaying = player.isPlaying,
                    isBuffering = player.playbackState == Player.STATE_BUFFERING,
                    duration = if (player.duration > 0) player.duration else 0L,
                    title = mediaItem?.mediaMetadata?.title?.toString() ?: "Sem título",
                    // Media3 infelizmente não passa metadados customizados facilmente via Controller
                    // sem configuração extra, então por enquanto o título pode vir vazio se não configurado no Service.
                )
            }
        }
    }

    // Loop infinito que atualiza a barra de progresso enquanto a tela estiver ativa
    private fun startProgressLoop() {
        viewModelScope.launch {
            while (isActive) {
                mediaController?.let { player ->
                    if (player.isPlaying) {
                        _uiState.update { it.copy(currentPosition = player.currentPosition) }
                    }
                }
                delay(1000) // Atualiza a cada 1 segundo
            }
        }
    }

    // --- Ações do Usuário ---

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _uiState.update { it.copy(currentPosition = positionMs) }
    }

    fun skipForward() {
        mediaController?.let { it.seekTo(it.currentPosition + 10_000) } // +10s
    }

    fun skipBack() {
        mediaController?.let { it.seekTo(it.currentPosition - 10_000) } // -10s
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}