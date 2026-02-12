# 🎯 Análise: Modularização do Player, Service e Infraestrutura de Áudio

**Data:** 09/02/2026  
**Projeto:** Ouvindo a Bíblia  
**Versão Atual:** v1.0.0-rc1  
**Autor:** Android Principal Engineer

---

## 📋 Índice

1. [Contexto Atual](#contexto-atual)
2. [Análise da Estrutura Existente](#análise-da-estrutura-existente)
3. [Proposta de Modularização](#proposta-de-modularização)
4. [Benefícios vs. Custos](#benefícios-vs-custos)
5. [Recomendação Final](#recomendação-final)
6. [Plano de Implementação](#plano-de-implementação)

---

## 🔍 Contexto Atual

### Estrutura de Módulos Existente

```
OuvindoABiblia/
├── :app                    # UI, ViewModels, Service, DI
├── :data:local             # Room, DAOs, Entities
├── :data:remote            # Retrofit, APIs
└── :data:repository        # Repository Pattern
```

### Componentes de Áudio Atuais (no `:app`)

```
:app/
├── service/
│   └── PlaybackService.kt         # MediaLibraryService (200+ linhas)
├── di/
│   └── MediaModule.kt              # Provê ExoPlayer singleton
├── ui/player/
│   ├── PlayerViewModel.kt          # 700+ linhas (Cast + Media3)
│   ├── PlayerScreen.kt             # Tela expandida
│   ├── MiniPlayer.kt               # Player flutuante
│   ├── PlayerUiState.kt            # Estado da UI
│   └── components/
│       ├── ChaptersSheet.kt
│       ├── PlaylistSheet.kt
│       └── SpeedControlSheet.kt
```

---

## 🏗️ Análise da Estrutura Existente

### ✅ **Pontos Positivos Atuais**

1. **Separação de Dados Bem Definida:**
    - `:data:local` → Room, DAO
    - `:data:remote` → Retrofit, APIs
    - `:data:repository` → Lógica de negócio

2. **Service Limpo e Focado:**
    - `PlaybackService` implementa Media3 corretamente
    - Injeção via Hilt funcional
    - CoilBitmapLoader para artwork

3. **ViewModel Gerencia Estado Complexo:**
    - Google Cast integrado
    - Sleep Timer
    - Controle de velocidade
    - Shuffle/Repeat

### ⚠️ **Problemas Identificados**

1. **PlayerViewModel Inchado (700+ linhas):**
   ```kotlin
   // Tudo misturado:
   - Lógica de Cast
   - Controle de Media3 (MediaController)
   - Sleep Timer
   - Gerenciamento de playlist
   - Atualização de UI
   ```

2. **Acoplamento Forte:**
    - Service depende de `BibleRepository` (data layer)
    - ViewModel depende de `PlaybackService` (ComponentName hardcoded)
    - Módulo `:app` conhece detalhes de implementação do player

3. **Reusabilidade Zero:**
    - Se criar outro app de áudio → precisa copiar tudo
    - PlaybackService específico para "Bíblia"
    - Não há abstração do player

4. **Testabilidade Comprometida:**
    - Difícil mockar MediaController
    - Service não testável isoladamente
    - Acoplamento com `Context` do Android

---

## 🎨 Proposta de Modularização

### **Estrutura Ideal: Feature + Core Modules**

```
OuvindoABiblia/
├── :app                              # Application, MainActivity, DI raiz
│
├── :core:player                      # ⭐ NOVO: Infraestrutura de Áudio
│   ├── service/
│   │   ├── BasePlaybackService       # Service genérico Media3
│   │   └── PlayerNotificationConfig  # Configuração de notificação
│   ├── controller/
│   │   └── MediaControllerManager    # Wrapper do MediaController
│   ├── session/
│   │   └── PlaybackSessionManager    # MediaSession + Lifecycle
│   ├── model/
│   │   ├── PlaybackState             # Estado de reprodução
│   │   └── MediaTrack                # Item genérico (não só bíblia)
│   └── di/
│       └── PlayerModule              # Provê ExoPlayer, etc.
│
├── :core:cast                        # ⭐ NOVO: Google Cast isolado
│   ├── CastManager                   # Gerencia CastSession
│   ├── CastMediaMapper               # Converte MediaItem → CastMedia
│   └── di/
│       └── CastModule
│
├── :feature:player                   # ⭐ NOVO: UI do Player
│   ├── PlayerViewModel               # SÓ UI + navegação
│   ├── PlayerScreen                  # Tela expandida
│   ├── MiniPlayer                    # Player compacto
│   ├── components/
│   │   ├── ChaptersSheet
│   │   ├── SpeedControl
│   │   └── SleepTimer
│   └── navigation/
│       └── PlayerNavigation          # Deep links
│
├── :feature:library                  # (já existe como "home")
│   └── BooksListScreen
│
├── :data:local                       # (já existe)
├── :data:remote                      # (já existe)
└── :data:repository                  # (já existe)
```

---

## 📊 Benefícios vs. Custos

### ✅ **Benefícios da Modularização**

#### 1. **Separação de Responsabilidades**

```kotlin
// ANTES (tudo no :app)
:app → PlayerViewModel conhece PlaybackService, Cast, Repository

// DEPOIS (módulos independentes)
:core:player → Lógica de Media3+Service
:core:cast → Lógica de Google Cast
:feature:player → UI+ViewModel (só consome core:player)
:app → Apenas conecta tudo via DI
```

#### 2. **Reusabilidade**

```kotlin
// Outro app de podcasts pode usar:
implementation(project(":core:player"))
implementation(project(":core:cast"))

// E criar seu próprio :feature:player com UI diferente
```

#### 3. **Testabilidade**

```kotlin
// Testar core:player sem dependências do Android
@Test
fun `when play pressed, should start playback`() {
    val mediaController = FakeMediaController()
    val manager = MediaControllerManager(mediaController)
    manager.play()
    assert(mediaController.isPlaying)
}
```

#### 4. **Build Time (Paralelização)**

```kotlin
// Gradle compila módulos em paralelo:
:core:player--\
:core:cast-----\
:data:local-----> :app (assembly)
:feature:player-/
```

#### 5. **Limites Claros de API**

```kotlin
// :core:player expõe apenas interfaces públicas
interface AudioPlayer {
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    val state: StateFlow<PlaybackState>
}

// PlaybackService fica internal (não exposto)
internal class PlaybackService : MediaLibraryService() { ... }
```

### ⚠️ **Custos da Modularização**

#### 1. **Complexidade Inicial (+20-30 horas)**

- Criar estrutura de módulos
- Mover código existente
- Refatorar dependências
- Testar integração

#### 2. **Overhead de Gradle**

```gradle
// Mais arquivos build.gradle.kts para gerenciar
:core:player/build.gradle.kts
:core:cast/build.gradle.kts
:feature:player/build.gradle.kts
```

#### 3. **Curva de Aprendizado**

- Desenvolvedores precisam entender a arquitetura modular
- Onde adicionar novas features?
- Quais módulos dependem de quais?

#### 4. **Risco de Over-Engineering**

```kotlin
// Modularizar demais pode criar abstrações desnecessárias
// Ex: Um módulo :core:player:notification, :core:player:session, etc.
// (Evitar sub-módulos excessivos)
```

---

## 🎯 Recomendação Final

### **SIM, modularizar faz sentido, MAS de forma incremental!**

### **Estratégia Recomendada: Modularização Faseada**

#### **Fase 1: `:core:player` (Prioridade ALTA) 🚀**

**Tempo:** 8-10 horas  
**Impacto:** ALTO (base para tudo)

**O que mover:**

```
:core:player/
├── service/
│   └── PlaybackService.kt            # (do :app)
├── controller/
│   └── MediaControllerManager.kt     # ⭐ NOVO: Wrapper
├── model/
│   └── PlaybackState.kt              # (do :data:repository)
└── di/
    └── PlayerModule.kt               # (do :app/di/MediaModule.kt)
```

**Por que começar aqui?**

- Isola a infraestrutura de áudio (Media3, ExoPlayer)
- Reduz acoplamento do `:app`
- Facilita testes e UAMP improvements

**Dependências:**

```kotlin
// :core:player/build.gradle.kts
dependencies {
    // SEM dependências de :data ou :feature
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.hilt.android)
}
```

---

#### **Fase 2: `:core:cast` (Prioridade MÉDIA) 🎬**

**Tempo:** 5-6 horas  
**Impacto:** MÉDIO (desacopla Cast do ViewModel)

**O que mover:**

```
:core:cast/
├── CastManager.kt                    # ⭐ NOVO: Gerencia CastSession
├── CastMediaMapper.kt                # ⭐ NOVO: MediaItem → CastMedia
└── di/
    └── CastModule.kt
```

**Por que?**

- PlayerViewModel tem 200+ linhas de código de Cast
- Cast é uma feature opcional (nem todo app precisa)
- Facilita testes sem dispositivos Cast

**Dependências:**

```kotlin
// :core:cast/build.gradle.kts
dependencies {
    implementation(project(":core:player"))  # Para PlaybackState
    implementation(libs.play.services.cast.framework)
    implementation(libs.hilt.android)
}
```

---

#### **Fase 3: `:feature:player` (Prioridade BAIXA) 🎨**

**Tempo:** 10-12 horas  
**Impacto:** BAIXO (organização, não funcionalidade)

**O que mover:**

```
:feature:player/
├── PlayerViewModel.kt                # Refatorado (só UI + navegação)
├── PlayerScreen.kt                   # (do :app)
├── MiniPlayer.kt                     # (do :app)
├── PlayerUiState.kt                  # (do :app)
└── components/
    ├── ChaptersSheet.kt
    ├── PlaylistSheet.kt
    └── SpeedControlSheet.kt
```

**Por que deixar para depois?**

- Impacto visual zero (usuário não vê diferença)
- Precisa de `:core:player` e `:core:cast` prontos
- Complexo (mexe em navegação)

**Dependências:**

```kotlin
// :feature:player/build.gradle.kts
dependencies {
    implementation(project(":core:player"))
    implementation(project(":core:cast"))
    implementation(project(":data:repository"))
    implementation(libs.compose.runtime)
    implementation(libs.hilt.android)
}
```

---

## 📅 Plano de Implementação

### **Timeline Recomendada**

| Fase      | Módulo            | Tempo      | Quando            | Bloqueador?                                  |
|-----------|-------------------|------------|-------------------|----------------------------------------------|
| 1         | `:core:player`    | 8-10h      | **AGORA**         | ❌ Não                                        |
| 2         | `:core:cast`      | 5-6h       | Depois do Phase 1 | ✅ Sim (depende de :core:player)              |
| 3         | `:feature:player` | 10-12h     | Depois do Phase 2 | ✅ Sim (depende de :core:player + :core:cast) |
| **TOTAL** |                   | **23-28h** |                   |                                              |

---

### **Passo a Passo: Fase 1 (`:core:player`)**

#### **1.1 Criar Módulo**

```bash
# Criar estrutura de pastas
mkdir -p core/player/src/main/java/br/app/ide/core/player/{service,controller,model,di}

# Criar build.gradle.kts
touch core/player/build.gradle.kts
```

#### **1.2 Configurar Gradle**

```kotlin
// core/player/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "br.app.ide.core.player"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Media3
    api(libs.androidx.media3.exoplayer)
    api(libs.androidx.media3.session)
    api(libs.androidx.media3.common)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
```

```kotlin
// settings.gradle.kts
include(":app")
include(":data:local")
include(":data:remote")
include(":data:repository")
include(":core:player")  // ⭐ NOVO
```

#### **1.3 Criar Interface Pública**

```kotlin
// core/player/src/main/java/br/app/ide/core/player/AudioPlayer.kt
package br.app.ide.core.player

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface pública do Player de Áudio.
 * Abstrações sobre Media3/ExoPlayer.
 */
interface AudioPlayer {
    val playbackState: StateFlow<PlaybackState>

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun skipToNext()
    fun skipToPrevious()
    fun setPlaybackSpeed(speed: Float)
    fun release()
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentTrackId: String = "",
    val currentTrackTitle: String = "",
    val currentTrackArtwork: String = "",
    val playbackSpeed: Float = 1.0f,
    val isBuffering: Boolean = false
)
```

#### **1.4 Mover PlaybackService**

```kotlin
// core/player/src/main/java/br/app/ide/core/player/service/PlaybackService.kt
package br.app.ide.core.player.service

import androidx.media3.session.MediaLibraryService
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var sessionManager: PlaybackSessionManager  // ⭐ NOVO

    override fun onCreate() {
        super.onCreate()
        sessionManager.initialize(this, player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return sessionManager.getSession()
    }

    override fun onDestroy() {
        sessionManager.release()
        super.onDestroy()
    }
}
```

#### **1.5 Criar MediaControllerManager**

```kotlin
// core/player/src/main/java/br/app/ide/core/player/controller/MediaControllerManager.kt
package br.app.ide.core.player.controller

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import br.app.ide.core.player.AudioPlayer
import br.app.ide.core.player.PlaybackState
import br.app.ide.core.player.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaControllerManager @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioPlayer {

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun connect() {
        if (controller != null) return

        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future

        future.addListener({
            try {
                controller = future.get()
                setupListener()
            } catch (e: Exception) {
                // Handle error
            }
        }, context.mainExecutor)
    }

    override fun play() {
        controller?.play()
    }

    override fun pause() {
        controller?.pause()
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    override fun skipToNext() {
        controller?.seekToNextMediaItem()
    }

    override fun skipToPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    override fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
    }

    override fun release() {
        MediaController.releaseFuture(controllerFuture ?: return)
        controller = null
        controllerFuture = null
    }

    private fun setupListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
            }

            override fun onEvents(player: Player, events: Player.Events) {
                updateState()
            }
        })
    }

    private fun updateState() {
        val player = controller ?: return
        _playbackState.value = PlaybackState(
            isPlaying = player.isPlaying,
            currentPosition = player.currentPosition,
            duration = player.duration,
            currentTrackId = player.currentMediaItem?.mediaId ?: "",
            currentTrackTitle = player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "",
            currentTrackArtwork = player.currentMediaItem?.mediaMetadata?.artworkUri?.toString()
                ?: "",
            playbackSpeed = player.playbackParameters.speed,
            isBuffering = player.playbackState == Player.STATE_BUFFERING
        )
    }
}
```

#### **1.6 Atualizar `:app` para usar `:core:player`**

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":core:player"))  // ⭐ NOVO
    // ... resto das dependências
}
```

```kotlin
// app/src/main/java/br/app/ide/ouvindoabiblia/ui/player/PlayerViewModel.kt
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BibleRepository,
    private val audioPlayer: AudioPlayer  // ⭐ INJETADO do :core:player
) : ViewModel() {

    init {
        audioPlayer.connect()
    }

    fun togglePlayPause() {
        if (audioPlayer.playbackState.value.isPlaying) {
            audioPlayer.pause()
        } else {
            audioPlayer.play()
        }
    }

    // ... resto do código
}
```

---

## 🎯 Critérios de Sucesso

### **Fase 1 (`:core:player`) está pronta quando:**

✅ `PlaybackService` roda no `:core:player`  
✅ `MediaControllerManager` gerencia conexão com Service  
✅ `PlayerViewModel` injeta `AudioPlayer` (interface)  
✅ Testes do `:core:player` passam (unit tests sem Android)  
✅ App compila e roda sem erros  
✅ Playback funciona igual ao anterior

---

## 📚 Referências de Arquitetura

### **Projetos Open Source com Modularização**

1. **[Now In Android](https://github.com/android/nowinandroid)**
   ```
   :core:data
   :core:domain
   :core:ui
   :feature:foryou
   :feature:bookmarks
   ```

2. **[UAMP (Universal Audio Media Player)](https://github.com/android/uamp)**
   ```
   :common (lógica compartilhada)
   :mobile (app mobile)
   :automotive (Android Auto)
   ```

3. **[Iosched (Google I/O App)](https://github.com/google/iosched)**
   ```
   :mobile
   :shared
   :model
   ```

---

## 🚨 Quando NÃO Modularizar

### **Evite modularização SE:**

❌ Time com menos de 3 desenvolvedores  
❌ Projeto com menos de 6 meses de vida  
❌ MVP/Protótipo (velocidade > arquitetura)  
❌ Build time < 30 segundos (não há ganho)  
❌ Código < 10.000 linhas (overhead desnecessário)

### **Status do Ouvindo a Bíblia:**

✅ Time: 1-2 devs (OK, mas atenção)  
✅ Projeto: 3+ meses (maduro)  
✅ Código: 5.000+ linhas (justifica)  
✅ Funcionalidades: Cast, Auto, Sleep Timer (complexo)  
✅ Roadmap: UAMP improvements (facilita)

**Veredito:** ✅ **VALE A PENA modularizar `:core:player`**

---

## 📖 Conclusão

### **Resposta Direta à Pergunta:**

> **"Faz sentido pensar em Modularização para player service etc?"**

✅ **SIM**, mas de forma **incremental**:

1. **AGORA (Fase 1):** Criar `:core:player` (Service + MediaController)
    - **Tempo:** 8-10 horas
    - **Benefício:** Base sólida para UAMP improvements
    - **Risco:** Baixo (mudanças isoladas)

2. **DEPOIS (Fase 2):** Criar `:core:cast` (Google Cast isolado)
    - **Tempo:** 5-6 horas
    - **Benefício:** PlayerViewModel mais limpo
    - **Risco:** Baixo

3. **FUTURO (Fase 3):** Criar `:feature:player` (UI modularizada)
    - **Tempo:** 10-12 horas
    - **Benefício:** Organização, reutilização
    - **Risco:** Médio (mexe em navegação)

### **Recomendação Final:**

🚀 **COMECE COM FASE 1 (`:core:player`) ANTES das melhorias UAMP**

**Por quê?**

- As melhorias UAMP vão mexer justamente no Service e MediaController
- Melhor refatorar ANTES de adicionar features novas
- Evita duplicação de esforço

**Próximos Passos:**

1. ✅ Leia este documento
2. ✅ Crie branch `feature/core-player-module`
3. ✅ Implemente Fase 1 (`:core:player`)
4. ✅ Teste completamente
5. ✅ Merge e TAG
6. ✅ DEPOIS implemente UAMP improvements

---

**Tag Sugerida para este momento:**

```bash
git tag -a v1.0.0-pre-modularization -m "Estado antes da modularização :core:player"
```

**Tag Sugerida após Fase 1:**

```bash
git tag -a v1.1.0-modular-player -m "Módulo :core:player implementado"
```

---

**Dúvidas? Consulte:**

- `PLANO_IMPLEMENTACAO_MELHORIAS_UAMP.md` (melhorias após modularização)
- [Android Developers - Modularization](https://developer.android.com/topic/modularization)
- [Now In Android - Architecture](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md)

