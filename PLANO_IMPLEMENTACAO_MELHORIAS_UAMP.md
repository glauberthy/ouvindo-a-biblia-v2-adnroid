# 🚀 Plano de Implementação: Melhorias UAMP para Ouvindo a Bíblia

**Data de Criação:** 08/02/2026  
**Versão Base:** v1.0.0-rc1  
**Objetivo:** Implementar boas práticas de infraestrutura do UAMP mantendo 100% da navegação atual

---

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Pré-requisitos](#pré-requisitos)
3. [Fase 1: Ferramentas de Debug](#fase-1-ferramentas-de-debug)
4. [Fase 2: Refatoração do Service](#fase-2-refatoração-do-service)
5. [Fase 3: Lifecycle-Aware MediaController](#fase-3-lifecycle-aware-mediacontroller)
6. [Fase 4: Melhorias de UX](#fase-4-melhorias-de-ux)
7. [Fase 5: Android Auto](#fase-5-android-auto)
8. [Checklist de Validação](#checklist-de-validação)
9. [Rollback Plan](#rollback-plan)

---

## 🎯 Visão Geral

### **O que vai mudar:**

- ✅ Arquitetura interna do `PlaybackService` (mais limpa e extensível)
- ✅ Gerenciamento de recursos do `PlayerViewModel` (zero memory leaks)
- ✅ Ferramentas de detecção de problemas (LeakCanary + StrictMode)
- ✅ Tela de splash profissional (opcional)
- ✅ Suporte a Android Auto

### **O que NÃO vai mudar:**

- ✅ Navegação do usuário (Bottom Nav + Player Flutuante)
- ✅ UI/UX (todas as telas permanecem iguais)
- ✅ Funcionalidades (Cast, Sleep Timer, Speed Control, etc.)
- ✅ Arquitetura de dados (Repository, Room, DataStore)

### **Impacto Estimado:**

- ⏱️ **Tempo total:** 20-25 horas
- 🐛 **Risco:** Baixo (mudanças isoladas e testáveis)
- 📈 **Benefício:** Alto (código mais robusto, zero leaks, Android Auto)

---

## 🔧 Pré-requisitos

### **1. Backup e Versionamento**

```bash
# Criar branch para desenvolvimento
git checkout -b feature/uamp-improvements

# Tag do estado atual (fallback)
git tag -a v1.0.0-rc1-pre-uamp -m "Estado antes das melhorias UAMP"
git push origin v1.0.0-rc1-pre-uamp
```

### **2. Dependências Necessárias**

Verificar que já temos todas as dependências necessárias:

```toml
# gradle/libs.versions.toml (já temos tudo, apenas validar)

[versions]
media3 = "1.9.2"
hilt = "2.51.1"
lifecycle = "2.10.0"
leakcanary = "2.14"  # ← Será adicionado
```

### **3. Ambiente de Teste**

- ✅ Dispositivo físico ou emulador (API 26+)
- ✅ Android Studio atualizado (Hedgehog ou superior)
- ✅ Conexão com internet (para testes de streaming)

---

## 📱 Fase 1: Ferramentas de Debug

**⏱️ Tempo estimado:** 1 hora  
**🎯 Objetivo:** Adicionar ferramentas para detectar leaks e problemas de performance  
**📍 Impacto na navegação:** ZERO  
**🎨 Impacto visual:** ZERO (apenas em debug builds)

---

### **Passo 1.1: Adicionar LeakCanary**

**Arquivo:** `app/build.gradle.kts`

```kotlin
dependencies {
    // ...existing dependencies...

    // LeakCanary - Detecção automática de memory leaks (só debug)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}
```

**Ação:**

```bash
# Sincronizar projeto
./gradlew clean build
```

**Validação:**

- Executar app em modo debug
- Navegar entre telas
- Verificar se LeakCanary aparece na notificação
- Testar: abrir app → tocar livro → fechar app → verificar leaks

---

### **Passo 1.2: Adicionar StrictMode**

**Arquivo:** `app/src/main/java/br/app/ide/ouvindoabiblia/OuvindoBibliaApp.kt`

```kotlin
package br.app.ide.ouvindoabiblia

import android.app.Application
import android.os.StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OuvindoBibliaApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()

        // Habilitar StrictMode apenas em builds de debug
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }

    /**
     * Habilita políticas do StrictMode para detectar problemas durante desenvolvimento:
     * - Thread Policy: Detecta I/O na main thread, disk reads, network access
     * - VM Policy: Detecta memory leaks, closeable leaks, SQLite leaks
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll() // Detecta todos os tipos de violações
                .penaltyLog() // Loga no Logcat
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll() // Detecta todos os tipos de leaks
                .penaltyLog() // Loga no Logcat
                .build()
        )
    }

    override fun newImageLoader(): ImageLoader {
        return imageLoader
    }
}
```

**Validação:**

```bash
# Executar app e verificar Logcat
adb logcat | grep StrictMode

# Não deve haver violações. Se houver:
# - StrictMode: policy XXXX violated = corrigir o problema
```

**✅ Checkpoint 1:** LeakCanary e StrictMode funcionando sem erros críticos.

---

## 🏗️ Fase 2: Refatoração do Service

**⏱️ Tempo estimado:** 6 horas  
**🎯 Objetivo:** Separar lógica de MediaSession (base abstrata) de implementação específica  
**📍 Impacto na navegação:** ZERO  
**🎨 Impacto visual:** ZERO

---

### **Passo 2.1: Criar BaseMediaPlaybackService**

**Arquivo:** `app/src/main/java/br/app/ide/ouvindoabiblia/service/BaseMediaPlaybackService.kt`

```kotlin
package br.app.ide.ouvindoabiblia.service

import android.app.PendingIntent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Classe base abstrata para serviços de reprodução de mídia.
 *
 * Implementa toda a lógica comum de MediaLibraryService, deixando apenas
 * a criação de PendingIntents para subclasses.
 *
 * Arquitetura:
 * ```

* BaseMediaPlaybackService (lógica genérica)
* └── PlaybackService (implementação específica)
* ```
*
* Benefícios:
*
    - Código reutilizável (fácil criar novos services)
*
    - Separação de responsabilidades (MediaSession vs Navegação)
*
    - Testabilidade (mockar PendingIntents é simples)
*
    - Manutenibilidade (mudanças na MediaSession não afetam subclasses)
*
* @see PlaybackService Implementação concreta para Ouvindo a Bíblia
  */
  abstract class BaseMediaPlaybackService : MediaLibraryService() {

  /** MediaLibrarySession gerenciando esta sessão */
  private var mediaLibrarySession: MediaLibrarySession? = null

  /** ExoPlayer para reprodução de áudio */
  protected lateinit var player: ExoPlayer
  private set

  companion object {
  /** ID do nó raiz da biblioteca de mídia */
  protected const val ROOT_ID = "[rootID]"
  }

  override fun onCreate() {
  super.onCreate()
  initializeSessionAndPlayer()
  }

  override fun onDestroy() {
  mediaLibrarySession?.run {
  player.release()
  release()
  mediaLibrarySession = null
  }
  super.onDestroy()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
  mediaLibrarySession

  /**
    * Corrige crash no Android 12+ quando notificação é atualizada
    * enquanto o service está em background.
      */
      @OptIn(UnstableApi::class)
      override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
      super.onUpdateNotification(session, startInForegroundRequired)
      }

  /**
    * Inicializa ExoPlayer e MediaLibrarySession.
    * Chamado automaticamente no onCreate().
      */
      private fun initializeSessionAndPlayer() {
      // Criar ExoPlayer com Audio Focus automático
      player = ExoPlayer.Builder(this)
      .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
      .setWakeMode(C.WAKE_MODE_NETWORK) // Mantém CPU acordado durante streaming
      .build()

      // Criar MediaLibrarySession
      mediaLibrarySession = MediaLibrarySession.Builder(
      this,
      player,
      LibrarySessionCallback()
      )
      .setSessionActivity(getSingleTopActivity())
      .build()
      }

  /**
    * Retorna os filhos de um nó pai na hierarquia da biblioteca.
    *
    * Subclasses devem sobrescrever para fornecer sua própria estrutura.
    *
    * Exemplo de hierarquia:
    * ```
    * Root
    * ├── Antigo Testamento
    * │ ├── Gênesis
    * │ └── Êxodo
    * └── Novo Testamento
    *     └── Mateus
    * ```
    *
    * @param parentId ID do nó pai
    * @return Lista de itens filhos
      */
      protected open fun getChildren(parentId: String): ImmutableList<MediaItem> {
      // Implementação padrão: sem filhos
      return ImmutableList.of()
      }

  /**
    * Retorna um item específico por seu ID.
    *
    * Subclasses devem sobrescrever para resolver seus próprios IDs.
    *
    * @param mediaId ID do item a ser recuperado
    * @return MediaItem correspondente
      */
      protected open fun getItemFromId(mediaId: String): MediaItem? {
      // Implementação padrão: item não encontrado
      return null
      }

  /**
    * Callback da MediaLibrarySession.
    * Delega para métodos extensíveis da classe base.
      */
      private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

      override fun onGetLibraryRoot(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      params: LibraryParams?
      ): ListenableFuture<LibraryResult<MediaItem>> {
      // Retornar nó raiz (sempre navegável, nunca reproduzível)
      val rootItem = MediaItem.Builder()
      .setMediaId(ROOT_ID)
      .setMediaMetadata(
      androidx.media3.common.MediaMetadata.Builder()
      .setIsPlayable(false)
      .setIsBrowsable(true)
      .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
      .build()
      )
      .build()

           return Futures.immediateFuture(
               LibraryResult.ofItem(rootItem, params)
           )
      }

      override fun onGetChildren(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      parentId: String,
      page: Int,
      pageSize: Int,
      params: LibraryParams?
      ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
      // Delegar para método extensível
      val children = getChildren(parentId)
      return Futures.immediateFuture(
      LibraryResult.ofItemList(children, params)
      )
      }

      override fun onGetItem(
      session: MediaLibrarySession,
      browser: MediaSession.ControllerInfo,
      mediaId: String
      ): ListenableFuture<LibraryResult<MediaItem>> {
      // Delegar para método extensível
      val item = getItemFromId(mediaId)
      return if (item != null) {
      Futures.immediateFuture(LibraryResult.ofItem(item, null))
      } else {
      Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
      }
      }
      }

  /**
    * Retorna PendingIntent para abrir a activity principal em modo single-top.
    * Usado para abrir o app ao clicar na notificação.
    *
    * @return PendingIntent para single-top activity
      */
      abstract fun getSingleTopActivity(): PendingIntent?

  /**
    * Retorna PendingIntent com back stack completo.
    * Usado quando necessário garantir navegação completa.
    *
    * @return PendingIntent com back stack
      */
      abstract fun getBackStackedActivity(): PendingIntent?
      }

```

---

### **Passo 2.2: Refatorar PlaybackService**

**Arquivo:** `app/src/main/java/br/app/ide/ouvindoabiblia/service/PlaybackService.kt`

**⚠️ IMPORTANTE:** Não vamos reescrever tudo. Vamos fazer uma transição gradual:

1. **Primeira iteração:** Herdar de `BaseMediaPlaybackService`
2. **Segunda iteração:** Mover lógica comum para a base
3. **Terceira iteração:** Manter apenas lógica específica do app

**Mudança Inicial (Mínima):**

```kotlin
package br.app.ide.ouvindoabiblia.service

import android.app.PendingIntent
import android.content.Intent
import br.app.ide.ouvindoabiblia.MainActivity
// ...existing imports...

/**
 * Serviço de reprodução de áudio da Bíblia.
 * 
 * Extende BaseMediaPlaybackService para herdar toda a lógica de MediaSession,
 * implementando apenas os PendingIntents específicos do app.
 * 
 * Funcionalidades:
 * - Reprodução de áudio em background
 * - Notificação de mídia com artwork e controles
 * - Lock screen controls
 * - Google Cast integration
 * - Estado persistente (via Repository)
 * 
 * @see BaseMediaPlaybackService Classe base com lógica genérica
 */
class PlaybackService : BaseMediaPlaybackService() {

    // ...existing fields (repository, mediaSession, cast, etc.)...

    /**
     * PendingIntent para abrir MainActivity em modo single-top.
     * Usado quando usuário clica na notificação.
     */
    override fun getSingleTopActivity(): PendingIntent? {
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_SINGLE_TOP,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * PendingIntent com back stack.
     * Atualmente não usado, mas mantido para compatibilidade.
     */
    override fun getBackStackedActivity(): PendingIntent? {
        // Poderia usar TaskStackBuilder se precisássemos de back stack complexo
        return getSingleTopActivity()
    }

    // ...existing code (onCreate, onTaskRemoved, etc.)...
    // MANTER TUDO COMO ESTÁ POR ENQUANTO

    companion object {
        private const val REQUEST_CODE_SINGLE_TOP = 100
        // ...existing constants...
    }
}
```

**🔴 ATENÇÃO:** Nesta fase, NÃO remover código do `PlaybackService` ainda. Apenas:

1. Mudar extends de `MediaLibraryService` para `BaseMediaPlaybackService`
2. Implementar métodos abstratos (`getSingleTopActivity`, `getBackStackedActivity`)
3. Manter todo o resto funcionando

---

### **Passo 2.3: Testar Refatoração**

**Checklist de Testes:**

```bash
# 1. Build sem erros
./gradlew clean assembleDebug

# 2. Instalar e executar
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Testar fluxo completo:
```

- [ ] App abre normalmente
- [ ] Click em livro → Mini player aparece
- [ ] Play → Áudio toca
- [ ] Notificação aparece
- [ ] Controles da notificação funcionam (play/pause/next)
- [ ] Click na notificação → App abre
- [ ] Lock screen controls funcionam
- [ ] Cast continua funcionando
- [ ] Fechar app → Estado salvo
- [ ] Reabrir app → Estado restaurado

**✅ Checkpoint 2:** PlaybackService refatorado e 100% funcional.

---

## 🔄 Fase 3: Lifecycle-Aware MediaController

**⏱️ Tempo estimado:** 4 horas  
**🎯 Objetivo:** Gerenciamento automático de recursos do MediaController  
**📍 Impacto na navegação:** ZERO  
**🎨 Impacto visual:** ZERO

---

### **Passo 3.1: Adicionar Dependência Coroutines-Guava**

**Arquivo:** `gradle/libs.versions.toml`

```toml
[versions]
# ...existing versions...
kotlinxCoroutines = "1.9.0"

[libraries]
# ...existing libraries...
kotlinx-coroutines-guava = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-guava", version.ref = "kotlinxCoroutines" }
```

**Arquivo:** `app/build.gradle.kts`

```kotlin
dependencies {
    // ...existing dependencies...
    implementation(libs.kotlinx.coroutines.guava)
}
```

---

### **Passo 3.2: Refatorar PlayerViewModel**

**Arquivo:** `app/src/main/java/br/app/ide/ouvindoabiblia/ui/player/PlayerViewModel.kt`

**Mudanças:**

```kotlin
package br.app.ide.ouvindoabiblia.ui.player

import android.content.ComponentName
import android.content.Context
// ...existing imports...
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.guava.await // ← NOVO
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
// ...existing imports...

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BibleRepository
) : ViewModel() {

    // ...existing fields...

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // Job para controlar o ciclo de vida da conexão
    private var connectionJob: Job? = null

    init {
        // Cast initialization...
        // ...existing cast code...

        // Conectar ao service com lifecycle management
        connectToServiceWithLifecycle()
    }

    /**
     * Conecta ao PlaybackService com gerenciamento automático de lifecycle.
     *
     * Usa coroutines estruturadas para garantir que:
     * - Conexão só acontece quando ViewModel está ativo
     * - Desconexão automática quando ViewModel é destruído
     * - Zero memory leaks
     */
    private fun connectToServiceWithLifecycle() {
        connectionJob?.cancel() // Cancelar conexão anterior se existir

        connectionJob = viewModelScope.launch {
            try {
                // Conectar ao service
                val sessionToken = SessionToken(
                    context,
                    ComponentName(context, PlaybackService::class.java)
                )

                controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

                // Aguardar conexão usando coroutines (ao invés de callback)
                mediaController = controllerFuture!!.await()

                // Setup listeners
                setupPlayerListener()

                // Restaurar último estado
                restoreLastPlaybackState()

                // Iniciar loop de progresso
                startProgressLoop()

                Log.d("PlayerViewModel", "MediaController conectado com sucesso")

            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Erro ao conectar MediaController", e)
                mediaController = null
                controllerFuture = null
            }
        }
    }

    /**
     * Restaura o último estado de reprodução salvo.
     */
    private suspend fun restoreLastPlaybackState() {
        val controller = mediaController ?: return

        // Se controller já tem mídia, não restaurar
        if (controller.mediaItemCount > 0) {
            updateStateFromPlayer()
            return
        }

        // Buscar último estado salvo
        val lastState = repository.getLatestPlaybackState().first()
        if (lastState != null) {
            _uiState.update { it.copy(title = lastState.title, isPlaying = false) }

            // Criar MediaItem do estado salvo
            val mediaItem = createMediaItemFromState(lastState)

            // Configurar player
            controller.setMediaItem(mediaItem, lastState.positionMs)
            controller.prepare()

            // Sincronizar playlist
            loadBookPlaylist(lastState.chapterId, lastState.title, "")

            Log.d("PlayerViewModel", "Estado restaurado: ${lastState.title}")
        }
    }

    // ...existing methods (togglePlayPause, seekTo, etc.)...

    override fun onCleared() {
        super.onCleared()

        // Cancelar job de conexão (desconecta automaticamente)
        connectionJob?.cancel()

        // Liberar MediaController
        controllerFuture?.let {
            MediaController.releaseFuture(it)
            controllerFuture = null
        }
        mediaController = null

        // Cast cleanup
        castSession?.remoteMediaClient?.unregisterCallback(castCallback)
        castContext?.sessionManager?.removeSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )

        Log.d("PlayerViewModel", "Resources released")
    }

    // ...rest of existing code...
}
```

**📝 Principais mudanças:**

1. **Adicionado `connectionJob`** - Para controlar lifecycle da conexão
2. **`connectToServiceWithLifecycle()`** - Novo método com coroutines estruturadas
3. **`await()` ao invés de callbacks** - Código mais limpo e linear
4. **`onCleared()` melhorado** - Cancela job antes de liberar recursos
5. **`restoreLastPlaybackState()`** - Separado para clareza

---

### **Passo 3.3: Testar Lifecycle Management**

**Teste de Memory Leaks:**

```bash
# 1. Executar app com LeakCanary
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 2. Fazer o seguinte fluxo 10 vezes:
```

1. Abrir app
2. Tocar um livro
3. Pausar
4. Sair do app (Home button)
5. Reabrir app
6. Fechar app (Back button)

**Verificar:**

- [ ] LeakCanary não reporta leaks de `PlayerViewModel`
- [ ] LeakCanary não reporta leaks de `MediaController`
- [ ] StrictMode não reporta violações

**Teste de Restauração:**

1. Tocar Gênesis Capítulo 1
2. Pausar no meio (ex: 1:30)
3. Fechar app completamente (swipe no multitasking)
4. Reabrir app
5. **Verificar:** Mini player aparece com "Gênesis" e posição 1:30

**✅ Checkpoint 3:** Lifecycle management funcionando sem leaks.

---

## 🎨 Fase 4: Melhorias de UX

**⏱️ Tempo estimado:** 5 horas  
**🎯 Objetivo:** Tela de splash + Feedback de permissões  
**📍 Impacto na navegação:** Mínimo (apenas splash de 2s)  
**🎨 Impacto visual:** Adiciona splash screen profissional

---

### **Passo 4.1: Criar SplashActivity**

**Arquivo:** `app/src/main/res/layout/activity_splash.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto" android:layout_width="match_parent"
    android:layout_height="match_parent" android:background="@color/deep_blue_dark">

    <ImageView android:id="@+id/logo" android:layout_width="120dp" android:layout_height="120dp"
        android:src="@mipmap/ic_launcher" app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent" app:layout_constraintEnd_toEndOf="parent"
        android:contentDescription="@string/app_name" />

    <TextView android:id="@+id/app_name" android:layout_width="wrap_content"
        android:layout_height="wrap_content" android:text="@string/app_name" android:textSize="24sp"
        android:textColor="@color/cream_background" android:fontFamily="sans-serif-medium"
        android:layout_marginTop="24dp" app:layout_constraintTop_toBottomOf="@id/logo"
        app:layout_constraintStart_toStartOf="parent" app:layout_constraintEnd_toEndOf="parent" />

    <ProgressBar android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:indeterminateTint="@color/cream_background" android:layout_marginTop="32dp"
        app:layout_constraintTop_toBottomOf="@id/app_name"
        app:layout_constraintStart_toStartOf="parent" app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

**Arquivo:** `app/src/main/java/br/app/ide/ouvindoabiblia/SplashActivity.kt`

```kotlin
package br.app.ide.ouvindoabiblia

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import br.app.ide.ouvindoabiblia.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity de splash/inicialização da aplicação.
 *
 * Responsabilidades:
 * - Exibir logo e loading enquanto app inicializa
 * - Solicitar permissão de notificações (Android 13+)
 * - Conectar ao PlaybackService para garantir que está pronto
 * - Navegar para MainActivity quando tudo estiver pronto
 *
 * Fluxo:
 * 1. Mostrar splash (logo + progress)
 * 2. Pedir permissão POST_NOTIFICATIONS (se necessário)
 * 3. Conectar ao PlaybackService (MediaBrowser)
 * 4. Aguardar 2 segundos (UX)
 * 5. Navegar para MainActivity
 * 6. finish() (SplashActivity é destruída)
 */
class SplashActivity : ComponentActivity() {

    private lateinit var browserFuture: ListenableFuture<MediaBrowser>
    private var permissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Solicitar permissão de notificações no Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionIfNeeded()
        } else {
            permissionGranted = true
            startInitialization()
        }
    }

    /**
     * Solicita permissão POST_NOTIFICATIONS se ainda não concedida.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermissionIfNeeded() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionGranted = true
            startInitialization()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_POST_NOTIFICATIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            permissionGranted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                // Permissão negada - avisar usuário mas continuar
                Toast.makeText(
                    this,
                    "Permissão de notificação negada. Você não verá controles na tela de bloqueio.",
                    Toast.LENGTH_LONG
                ).show()
            }

            startInitialization()
        }
    }

    /**
     * Inicia processo de inicialização da aplicação.
     */
    private fun startInitialization() {
        lifecycleScope.launch {
            try {
                // Conectar ao MediaBrowser para garantir que service está pronto
                initializeBrowser()

                // Aguardar mínimo de 2 segundos (UX - splash visível)
                delay(2000)

                // Navegar para MainActivity
                navigateToMain()

            } catch (e: Exception) {
                // Se falhar, continuar mesmo assim
                Toast.makeText(
                    this@SplashActivity,
                    "Erro ao inicializar serviço de áudio",
                    Toast.LENGTH_SHORT
                ).show()

                navigateToMain()
            }
        }
    }

    /**
     * Conecta ao PlaybackService via MediaBrowser.
     * Garante que o service está rodando antes de ir para MainActivity.
     */
    private fun initializeBrowser() {
        browserFuture = MediaBrowser.Builder(
            this,
            SessionToken(this, ComponentName(this, PlaybackService::class.java))
        ).buildAsync()
    }

    /**
     * Navega para MainActivity e finaliza SplashActivity.
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Destruir splash para não voltar a ela
    }

    override fun onDestroy() {
        if (::browserFuture.isInitialized) {
            MediaBrowser.releaseFuture(browserFuture)
        }
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 100
    }
}
```

---

### **Passo 4.2: Atualizar AndroidManifest.xml**

**Arquivo:** `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- ...existing permissions... -->

    <application android:name=".OuvindoBibliaApp" android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules" android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name" android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true" android:theme="@style/Theme.OuvindoABiblia" tools:targetApi="31">

        <!-- ============================================ -->
        <!-- NOVA: SplashActivity como LAUNCHER           -->
        <!-- ============================================ -->
        <activity android:name=".SplashActivity" android:exported="true"
            android:theme="@style/Theme.OuvindoABiblia.Splash">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- ============================================ -->
        <!-- MainActivity NÃO É MAIS LAUNCHER             -->
        <!-- ============================================ -->
        <activity android:name=".MainActivity" android:exported="false"
            android:theme="@style/Theme.OuvindoABiblia">
            <!-- Removido intent-filter MAIN/LAUNCHER -->
        </activity>

        <!-- ...existing service, receivers, etc... -->

    </application>

</manifest>
```

---

### **Passo 4.3: Criar Tema para Splash**

**Arquivo:** `app/src/main/res/values/themes.xml`

```xml

<resources>
    <!-- ...existing themes... -->

    <!-- Tema para SplashActivity (sem ActionBar) -->
    <style name="Theme.OuvindoABiblia.Splash" parent="Theme.OuvindoABiblia">
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowActionBar">false</item>
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowContentOverlay">@null</item>
    </style>

</resources>
```

---

### **Passo 4.4: Testar Splash Screen**

**Fluxo de Teste:**

```bash
# Desinstalar app anterior (para testar primeira instalação)
adb uninstall br.app.ide.ouvindoabiblia

# Instalar versão nova
./gradlew installDebug

# Abrir app
adb shell am start -n br.app.ide.ouvindoabiblia/.SplashActivity
```

**Verificar:**

- [ ] Splash aparece primeiro (logo + progress)
- [ ] Dialog de permissão aparece (Android 13+)
- [ ] Se aceitar: Vai para MainActivity
- [ ] Se negar: Toast de aviso + Vai para MainActivity
- [ ] Splash dura ~2 segundos
- [ ] MainActivity abre normalmente
- [ ] Back button em MainActivity fecha app (não volta para splash)

**✅ Checkpoint 4:** Splash screen funcionando com permissões.

---

## 📱 Fase 5: Android Auto

**⏱️ Tempo estimado:** 6 horas  
**🎯 Objetivo:** Expor biblioteca de mídia para Android Auto  
**📍 Impacto na navegação do app:** ZERO  
**🎨 Impacto visual no app:** ZERO (apenas no carro)

---

### **Passo 5.1: Criar Estrutura de MediaLibrary**

**Arquivo:** `app/src/main/java/br/app/ide/ouvindoabiblia/service/MediaLibraryStructure.kt`

```kotlin
package br.app.ide.ouvindoabiblia.service

/**
 * Constantes para estrutura hierárquica da biblioteca de mídia.
 *
 * Hierarquia:
 * ```

* Root
* ├── Antigo Testamento (browsable)
* │ ├── Gênesis (browsable)
* │ │ ├── Capítulo 1 (playable)
* │ │ ├── Capítulo 2 (playable)
* │ │ └── ...
* │ ├── Êxodo (browsable)
* │ └── ...
* └── Novo Testamento (browsable)
*     ├── Mateus (browsable)
*     └── ...
* ```

*/
object MediaLibraryStructure {
/** ID do nó raiz */
const val ROOT_ID = "[rootID]"

    /** ID da categoria Antigo Testamento */
    const val AT_ID = "[at]"
    
    /** ID da categoria Novo Testamento */
    const val NT_ID = "[nt]"
    
    /**
     * Cria ID de livro: "book_genesis"
     */
    fun bookId(bookSlug: String) = "book_$bookSlug"
    
    /**
     * Cria ID de capítulo: "chapter_genesis_1"
     */
    fun chapterId(bookSlug: String, chapterNumber: Int) = 
        "chapter_${bookSlug}_$chapterNumber"
    
    /**
     * Extrai slug do livro do ID: "book_genesis" → "genesis"
     */
    fun extractBookSlug(mediaId: String): String? {
        return if (mediaId.startsWith("book_")) {
            mediaId.removePrefix("book_")
        } else if (mediaId.startsWith("chapter_")) {
            // "chapter_genesis_1" → "genesis"
            mediaId.removePrefix("chapter_").split("_").firstOrNull()
        } else {
            null
        }
    }
    
    /**
     * Verifica se ID é de um testamento
     */
    fun isTestamentId(mediaId: String) = mediaId == AT_ID || mediaId == NT_ID
    
    /**
     * Verifica se ID é de um livro
     */
    fun isBookId(mediaId: String) = mediaId.startsWith("book_")
    
    /**
     * Verifica se ID é de um capítulo
     */
    fun isChapterId(mediaId: String) = mediaId.startsWith("chapter_")

}

```

---

### **Passo 5.2: Implementar getChildren() no PlaybackService**

**Arquivo:** `app/src/main/java/br/app/ide/ouvindoabiblia/service/PlaybackService.kt`

```kotlin
package br.app.ide.ouvindoabiblia.service

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.runBlocking
// ...existing imports...

class PlaybackService : BaseMediaPlaybackService() {

    // ...existing fields...
    
    /**
     * Sobrescreve getChildren() para expor hierarquia ao Android Auto.
     * 
     * Estrutura:
     * Root → Testamentos → Livros → Capítulos
     */
    override fun getChildren(parentId: String): ImmutableList<MediaItem> {
        return runBlocking {
            when (parentId) {
                MediaLibraryStructure.ROOT_ID -> getRootChildren()
                MediaLibraryStructure.AT_ID -> getTestamentChildren("at")
                MediaLibraryStructure.NT_ID -> getTestamentChildren("nt")
                else -> {
                    when {
                        MediaLibraryStructure.isBookId(parentId) -> getBookChildren(parentId)
                        else -> ImmutableList.of()
                    }
                }
            }
        }
    }

    /**
     * Retorna categorias do nó raiz (Antigo/Novo Testamento).
     */
    private fun getRootChildren(): ImmutableList<MediaItem> {
        return ImmutableList.of(
            createBrowsableMediaItem(
                mediaId = MediaLibraryStructure.AT_ID,
                title = "Antigo Testamento",
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
            ),
            createBrowsableMediaItem(
                mediaId = MediaLibraryStructure.NT_ID,
                title = "Novo Testamento",
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
            )
        )
    }

    /**
     * Retorna livros de um testamento.
     */
    private suspend fun getTestamentChildren(testament: String): ImmutableList<MediaItem> {
        val books = repository.getBooks().first().filter { it.testament == testament }
        
        return ImmutableList.builder<MediaItem>().apply {
            books.forEach { book ->
                add(createBrowsableMediaItem(
                    mediaId = MediaLibraryStructure.bookId(book.bookId),
                    title = book.name,
                    mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                    artworkUri = book.imageUrl
                ))
            }
        }.build()
    }

    /**
     * Retorna capítulos de um livro.
     */
    private suspend fun getBookChildren(bookMediaId: String): ImmutableList<MediaItem> {
        val bookSlug = MediaLibraryStructure.extractBookSlug(bookMediaId) ?: return ImmutableList.of()
        val chapters = repository.getChapters(bookSlug).first()
        
        return ImmutableList.builder<MediaItem>().apply {
            chapters.forEach { chapterInfo ->
                add(createPlayableMediaItem(
                    mediaId = MediaLibraryStructure.chapterId(bookSlug, chapterInfo.chapter.number),
                    title = chapterInfo.bookName,
                    subtitle = "Capítulo ${chapterInfo.chapter.number}",
                    audioUri = chapterInfo.chapter.audioUrl,
                    artworkUri = chapterInfo.coverUrl ?: chapterInfo.bookName
                ))
            }
        }.build()
    }

    /**
     * Cria MediaItem navegável (pasta).
     */
    private fun createBrowsableMediaItem(
        mediaId: String,
        title: String,
        mediaType: Int,
        artworkUri: String? = null
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .setMediaType(mediaType)
                    .apply {
                        artworkUri?.let { setArtworkUri(it.toUri()) }
                    }
                    .build()
            )
            .build()
    }

    /**
     * Cria MediaItem reproduzível (áudio).
     */
    private fun createPlayableMediaItem(
        mediaId: String,
        title: String,
        subtitle: String,
        audioUri: String,
        artworkUri: String?
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(audioUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .apply {
                        artworkUri?.let { setArtworkUri(it.toUri()) }
                    }
                    .build()
            )
            .build()
    }

    /**
     * Sobrescreve getItemFromId() para resolver IDs individuais.
     */
    override fun getItemFromId(mediaId: String): MediaItem? {
        return runBlocking {
            when {
                mediaId == MediaLibraryStructure.ROOT_ID -> {
                    createBrowsableMediaItem(
                        mediaId,
                        "Ouvindo a Bíblia",
                        MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
                    )
                }
                mediaId == MediaLibraryStructure.AT_ID -> {
                    createBrowsableMediaItem(
                        mediaId,
                        "Antigo Testamento",
                        MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
                    )
                }
                mediaId == MediaLibraryStructure.NT_ID -> {
                    createBrowsableMediaItem(
                        mediaId,
                        "Novo Testamento",
                        MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
                    )
                }
                MediaLibraryStructure.isBookId(mediaId) -> {
                    val bookSlug = MediaLibraryStructure.extractBookSlug(mediaId)
                    val book = bookSlug?.let { repository.getBook(it) }
                    book?.let {
                        createBrowsableMediaItem(
                            mediaId,
                            it.name,
                            MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            it.imageUrl
                        )
                    }
                }
                MediaLibraryStructure.isChapterId(mediaId) -> {
                    // Buscar capítulo específico do banco
                    null // Implementar se necessário
                }
                else -> null
            }
        }
    }

    // ...existing methods...
}
```

---

### **Passo 5.3: Adicionar Meta-Data para Android Auto**

**Arquivo:** `app/src/main/res/xml/auto_app_desc.xml` (CRIAR)

```xml
<?xml version="1.0" encoding="utf-8"?>
<automotiveApp>
    <uses name="media" />
</automotiveApp>
```

**Arquivo:** `app/src/main/AndroidManifest.xml`

```xml

<application ...>

    <!-- ...existing activities... -->

    <!-- ============================================ -->
    <!-- META-DATA PARA ANDROID AUTO                  -->
    <!-- ============================================ -->
<meta-data android:name="com.google.android.gms.car.application"
android:resource="@xml/auto_app_desc" />

    <!-- ============================================ -->
    <!-- SERVICE COM INTENT FILTERS COMPLETOS         -->
    <!-- ============================================ -->
<service android:name=".service.PlaybackService" android:exported="true"
android:foregroundServiceType="mediaPlayback">
<intent-filter>
    <!-- Media3 padrão -->
    <action android:name="androidx.media3.session.MediaLibraryService" />

    <!-- Compatibilidade com apps antigos -->
    <action android:name="android.media.browse.MediaBrowserService" />

    <!-- Google Assistant / Comandos de voz -->
    <action android:name="android.media.action.MEDIA_PLAY_FROM_SEARCH" />
</intent-filter>
</service>

    <!-- ...existing receivers, etc... -->

    </application>
```

---

### **Passo 5.4: Testar Android Auto**

**Opção A: Teste com Android Auto App (Recomendado)**

```bash
# 1. Instalar Android Auto no celular
# Google Play Store: "Android Auto"

# 2. Habilitar modo desenvolvedor no Android Auto:
# - Abrir Android Auto
# - Tocar 10 vezes na versão (About → Version)
# - Ativar "Unknown sources"

# 3. Executar app de teste
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Abrir Android Auto
# 5. Procurar "Ouvindo a Bíblia" na lista de apps de mídia
```

**Opção B: Desktop Head Unit (DHU)**

```bash
# 1. Baixar DHU (Android Auto Desktop)
# https://developer.android.com/training/cars/testing

# 2. Executar DHU
./desktop-head-unit

# 3. Testar navegação:
# Root → AT → Gênesis → Capítulo 1 → Tocar
```

**Verificar:**

- [ ] App aparece na lista de mídia do Android Auto
- [ ] Hierarquia navegável: Root → Testamentos → Livros → Capítulos
- [ ] Capas dos livros aparecem
- [ ] Tocar capítulo inicia reprodução
- [ ] Controles (play/pause/next) funcionam
- [ ] Notificação no celular também funciona

**✅ Checkpoint 5:** Android Auto funcionando com hierarquia completa.

---

## ✅ Checklist de Validação Final

### **Testes de Regressão (App Principal)**

Após TODAS as implementações, testar fluxo completo:

- [ ] **Splash Screen:**
    - [ ] Aparece ao abrir app
    - [ ] Pede permissão de notificação (Android 13+)
    - [ ] Navega para MainActivity após 2s

- [ ] **Navegação Principal:**
    - [ ] Bottom Nav funciona (5 tabs)
    - [ ] HomeScreen carrega livros
    - [ ] Filtro AT/NT funciona
    - [ ] Click em livro → Mini player aparece

- [ ] **Player:**
    - [ ] Mini player funciona (play/pause)
    - [ ] Drag up → Expande para tela cheia
    - [ ] Drag down → Minimiza
    - [ ] Controles funcionam (rewind/forward)
    - [ ] Sleep timer funciona
    - [ ] Speed control funciona
    - [ ] Lista de capítulos funciona
    - [ ] Favoritar funciona

- [ ] **Notificação:**
    - [ ] Aparece quando tocando
    - [ ] Controles funcionam
    - [ ] Click abre app
    - [ ] Lock screen controls funcionam

- [ ] **Cast:**
    - [ ] Detecta Chromecast
    - [ ] Envia áudio
    - [ ] Controles sincronizados
    - [ ] Troca de capítulo funciona

- [ ] **Persistência:**
    - [ ] Fechar app → Estado salvo
    - [ ] Reabrir app → Estado restaurado
    - [ ] Posição correta restaurada

- [ ] **Memory Leaks:**
    - [ ] LeakCanary não reporta leaks após 10 ciclos
    - [ ] StrictMode sem violações críticas

- [ ] **Android Auto:**
    - [ ] App aparece no Android Auto
    - [ ] Navegação hierárquica funciona
    - [ ] Reprodução funciona

---

## 🔙 Rollback Plan

Se algo der errado, reverter para estado anterior:

### **Rollback Completo:**

```bash
# Voltar para tag pré-UAMP
git reset --hard v1.0.0-rc1-pre-uamp
git push origin feature/uamp-improvements --force
```

### **Rollback Parcial (por fase):**

**Remover Android Auto:**

```bash
git revert <commit-hash-fase-5>
```

**Remover Splash:**

```bash
git revert <commit-hash-fase-4>
```

**Remover Lifecycle:**

```bash
git revert <commit-hash-fase-3>
```

**Remover Service Refactor:**

```bash
git revert <commit-hash-fase-2>
```

---

## 📊 Métricas de Sucesso

### **Antes (v1.0.0-rc1-pre-uamp):**

- ⚠️ Memory leaks ocasionais (PlayerViewModel)
- ⚠️ Sem detecção automática de problemas
- ⚠️ Service monolítico (600+ linhas)
- ⚠️ Sem Android Auto

### **Depois (v1.0.0-rc2-uamp):**

- ✅ Zero memory leaks (confirmado por LeakCanary)
- ✅ StrictMode habilitado (detecta violações)
- ✅ Service modular (BaseMediaPlaybackService)
- ✅ Lifecycle-aware MediaController
- ✅ Splash screen profissional
- ✅ Android Auto funcional
- ✅ Navegação 100% preservada

---

## 🎯 Próximos Passos (Pós-Implementação)

1. **Criar PR para branch main:**
   ```bash
   git push origin feature/uamp-improvements
   # Criar Pull Request no GitHub
   ```

2. **Code Review:**
    - Revisar todas as mudanças
    - Testar em dispositivos diferentes
    - Validar com equipe

3. **Beta Testing:**
    - Distribuir para testadores via Firebase App Distribution
    - Coletar feedback

4. **Release:**
   ```bash
   git checkout main
   git merge feature/uamp-improvements
   git tag -a v1.0.0-rc2 -m "Release Candidate 2: Melhorias UAMP"
   git push origin main --tags
   ```

5. **Monitoramento:**
    - Acompanhar crashes no Firebase Crashlytics
    - Verificar ANRs no Play Console
    - Monitorar reviews negativos

---

## 📚 Referências

- [Media3 Official Documentation](https://developer.android.com/guide/topics/media/media3)
- [UAMP (Universal Android Music Player)](https://github.com/android/uamp)
- [Android Auto Developer Guide](https://developer.android.com/training/cars)
- [LeakCanary Documentation](https://square.github.io/leakcanary/)
- [StrictMode Guide](https://developer.android.com/reference/android/os/StrictMode)

---

## 👥 Equipe

- **Desenvolvedor:** [Seu Nome]
- **Revisor:** [Nome do Revisor]
- **Testador:** [Nome do Testador]

---

## 📝 Log de Mudanças

| Data       | Fase         | Status     | Observações             |
|------------|--------------|------------|-------------------------|
| 08/02/2026 | Planejamento | ✅ Completo | Documento criado        |
| ___        | Fase 1       | ⏳ Pendente | LeakCanary + StrictMode |
| ___        | Fase 2       | ⏳ Pendente | Service Refactor        |
| ___        | Fase 3       | ⏳ Pendente | Lifecycle Management    |
| ___        | Fase 4       | ⏳ Pendente | Splash + UX             |
| ___        | Fase 5       | ⏳ Pendente | Android Auto            |
| ___        | Validação    | ⏳ Pendente | Testes completos        |
| ___        | Release      | ⏳ Pendente | v1.0.0-rc2              |

---

**🎉 Boa sorte com a implementação!**

*Lembre-se: Faça commits frequentes, teste cada fase isoladamente e mantenha a navegação intacta!*

