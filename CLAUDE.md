# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

"Ouvindo a Bíblia" — an Android audio-streaming app (audio Bible, themed moments, studies) built with Jetpack Compose, Media3/ExoPlayer, Hilt, and Room. The codebase and comments are in Portuguese (pt-BR); match that language in user-facing strings and inline comments.

## Build & test commands

Use the Gradle wrapper (`./gradlew`). There is no separate lint/format step configured beyond the Android defaults.

```bash
./gradlew assembleDebug              # build debug APK
./gradlew installDebug               # build + install on a connected device/emulator
./gradlew :app:lint                  # Android lint
./gradlew test                       # JVM unit tests (all modules)
./gradlew :data:repository:test      # unit tests for a single module
./gradlew connectedAndroidTest       # instrumented tests (needs device/emulator)
```

Run a single unit test class/method:

```bash
./gradlew :data:repository:test --tests "br.app.ide.ouvindoabiblia.data.repository.ExampleUnitTest"
./gradlew :data:repository:test --tests "*.ExampleUnitTest.someMethod"
```

Note: all modules use `compileSdk = 36` and the app `targetSdk = 36` (unified). `minSdk` is 26 (app) / 24 (libraries). KSP (not kapt) drives Room and Hilt code generation — a clean build after touching entities/DAOs/`@Module`s may need `./gradlew clean`.

Release build: `assembleRelease`/`bundleRelease` are signed via `signingConfigs.release`, which reads `keystore.properties` (repo root, git-ignored; see `keystore.properties.example`). Without that file the release builds unsigned but doesn't break `assembleDebug`.

## Rodando no emulador

O SDK está em `~/Android/Sdk`, mas `ANDROID_HOME`/PATH **não** estão exportados no shell (daí `adb: command not found`). Exporte antes de qualquer coisa:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
```

O AVD de teste já existe — **`OuvindoBiblia_API36`** (Pixel 8, API 36.1 `google_apis_playstore` x86_64, RAM 4 GB / heap 512 MB / data 8 GB, `gpu=host`). KVM está habilitado e o usuário está no grupo `kvm`; boot completo em ~30 s.

```bash
emulator -avd OuvindoBiblia_API36 -gpu host -no-boot-anim &   # subir
adb wait-for-device                                            # esperar
./gradlew installDebug                                         # compilar + instalar
adb -s emulator-5554 shell am start -n br.app.ide.ouvindoabiblia/br.app.ide.ouvindoabiblia.MainActivity
adb -s emulator-5554 exec-out screencap -p > /tmp/tela.png     # screenshot
```

Se o AVD for perdido, recriar com (a imagem já está baixada, não precisa de rede):

```bash
avdmanager create avd -n OuvindoBiblia_API36 -d pixel_8 \
  -k "system-images;android-36.1;google_apis_playstore;x86_64"
# depois ajustar ~/.android/avd/OuvindoBiblia_API36.avd/config.ini:
# hw.ramSize=4096  vm.heapSize=512  disk.dataPartition.size=8G  hw.gpu.enabled=yes  hw.gpu.mode=host  hw.keyboard=yes
```

Duas armadilhas:

- **Sempre passe `-s <serial>` para o `adb`.** Costuma haver um celular físico (moto g53 5G, API 34) pareado por ADB/WiFi ao mesmo tempo; sem `-s` o adb responde `error: more than one device/emulator`. Ter os dois é útil: API 34 real + API 36 emulada em paralelo.
- **Não abra o app com `monkey -c android.intent.category.LAUNCHER`.** O debug build inclui LeakCanary, que registra a própria `LeakLauncherActivity` como launcher, e o monkey abre ela em vez da `MainActivity`. Use o `am start` com componente explícito acima.

A imagem é `google_apis_playstore`, ou seja **sem `adb root`** — para inspecionar o Room use `adb -s emulator-5554 shell run-as br.app.ide.ouvindoabiblia ...` (funciona porque o debug build é `debuggable`).

## Module architecture

Gradle multi-module, dependencies flow one direction only (`app` → `repository` → {`local`, `remote`}):

- **`:app`** — UI (Compose), `PlaybackService`, Cast, DI wiring for app-level singletons (ExoPlayer, Coil).
- **`:data:repository`** — `BibleRepository` interface + `BibleRepositoryImpl`. The single orchestration point: decides when to hit the network vs. serve from Room, exposes everything to the UI as `Flow`. Defines the UI-facing `PlaybackState` domain model.
- **`:data:local`** — Room (`BibleDatabase`, entities, `BibleDao`) + DataStore (sync version keys). All persistence lives here.
- **`:data:remote`** — Retrofit `BibleApi` + kotlinx-serialization DTOs.

DI: Hilt with `@InstallIn(SingletonComponent::class)`. Each module owns its modules — `NetworkModule` (remote), `DatabaseModule`/`DataStoreModule` (local), `RepositoryModule` (repository binds the impl), `MediaModule`/`CoilModule` (app).

## Data sync model

Content is static JSON fetched from `https://ouvindo-a-biblia.ide.app.br/` (`biblia_index.json`, `themes.json`, `estudos.json`, `mais.json`). Sync is **version-gated**: each payload carries a `meta.version`; `BibleRepositoryImpl` compares it against a value stored in DataStore (`bible_data_version`, etc.) and only rewrites Room when the version changed. Sync is triggered lazily from the relevant ViewModels (`HomeViewModel`, `ThemesViewModel`, `StudiesViewModel`, `MoreViewModel`) on load — there is no background worker. The Room DB uses `fallbackToDestructiveMigration()`, so bumping `BibleDatabase` `version` wipes local data (acceptable since it's re-synced from the server).

## Playback architecture (the core of the app)

This is the most intricate part — read `PlaybackService.kt` and `PlayerViewModel.kt` together before changing playback. Background notes also live in `README.md` and `ANALISE_MODULARIZACAO_PLAYER.md` / `REFACTOR_MEDIA3_TODO.md`.

- **`PlaybackService`** extends `MediaLibraryService`. The `ExoPlayer` is a Hilt singleton from `MediaModule` and injected into both the service and the player UI path. The service owns the `MediaLibrarySession` and a `MediaLibrarySession.Callback`.
- **`PlayerViewModel`** connects to the service via a `MediaController` (Media3 session). It does **not** touch ExoPlayer directly; it sends transport commands through the controller. It also integrates Google Cast (`CastContext`/`RemoteMediaClient`) as an alternate playback target.
- **`mediaId` encodes the content type** and is parsed in several places — keep these conventions consistent:
  - Bible chapter → numeric chapter id as string (e.g. `"1234"`).
  - Study lesson → `"study_{studyId}_{lessonId}"`.
  - Theme moment → `"moment_..."` (these are deliberately **not** persisted as resume state).
  - A "book folder" play request arrives with a browsable item whose id is `"{bookId}|{chapterIndex}"`; `onSetMediaItems` expands it into the full chapter playlist server-side.
- **Resume / persistence:** playback position is auto-saved to Room (`PlaybackStateEntity`) on media transitions and pause (`setupAutoSaveListener` → `saveCurrentState`). On service create, `restoreLastSession` rebuilds the playlist from the saved state but does **not** auto-play. `buildPlaylistFromState` reconstructs `MediaItem`s for Bible/Study from the repository. Theme moments are excluded from saving.
- **`shouldBlockDatabaseResumption` (5s window):** guards against a saved session "resurrecting" over a fresh explicit play command during the transition window. `markExplicitPlaybackRequest` is called on every explicit `onSetMediaItems`. Be careful not to break this when editing playback start logic.
- **Lifecycle / swipe nos recentes:** the service survives the `MainActivity` being destroyed and the app going to background (it's started + foreground while playing), but **not** task removal. `onTaskRemoved` decides via the pure function `decideOnTaskRemoval` (`service/TaskRemovalPolicy.kt`, locked by `TaskRemovalPolicyTest`): **playing → stop** (save position synchronously, pause, release player + session, `stopForeground(STOP_FOREGROUND_REMOVE)`, `stopSelf()`); **paused after having played → keep** the dismissible notification (ISSUE 4.A); **restored-but-never-played → stop** (BUG B, no orphan notification). It deliberately does **not** call `super.onTaskRemoved()` — Media3's default only stops when *not* playing, the opposite of what's needed. `onPlaybackResumption` is partially implemented.
- The shutdown path must save the position with `saveCurrentStateBlocking()`, not `saveCurrentState()`: the latter writes on `serviceScope`, which `onDestroy` cancels (`serviceJob.cancel()`), so a `stopSelf()` right after would drop the write and resume would jump back to the start of the chapter.

## UI conventions

- Single-Activity (`MainActivity`, `AppCompatActivity`, `@AndroidEntryPoint`) hosting Compose. `MainScreen` holds the bottom nav + the shared/expandable player (`SharedPlayerScreen`) and a single shared `PlayerViewModel`. The notification deep-link (`OPEN_PLAYER_FROM_NOTIF` intent extra) drives opening the player.
- Navigation uses **type-safe Navigation-Compose** with `@Serializable` destinations defined in `Screen` (`AppNavigation.kt`); routes are built in `NavigationGraph.kt`. Add a destination by adding a `Screen` subtype and a `composable<Screen.X>` block.
- Screens follow an MVI-ish **LCE pattern**: a `*Contract.kt` (or contract section) defines a sealed `UiState` (`Loading`/`Error`/`Success`) and a sealed `Intent`; the `*ViewModel` exposes a `StateFlow` and a single intent handler. Mirror this for new screens.
- `WindowSizeClass` is threaded down from `MainActivity` for tablet/responsive layouts.
- Images: Coil with the app-wide `ImageLoader` (provided via `OuvindoBibliaApp : ImageLoaderFactory`). The service loads notification artwork through the same `ImageLoader` via a custom `CoilBitmapLoader`.

## Gotchas

- Many Media3 APIs are `@UnstableApi` and require `@OptIn(UnstableApi::class)`.
- The OkHttp client sends a required `User-Agent: BibliaFaladaApp` header (server WAF rejects without it); the ExoPlayer `DefaultHttpDataSource` sets the same UA. Don't drop it. The WAF also **rate-limits** — the Coil `ImageLoader` (`CoilModule`) has a 429 retry/backoff interceptor because the Home fires ~66 cover requests at once.
- **`applicationId` ≠ `namespace`**: applicationId is `ag.uny.ouvindoabiblia` (store identity — reactivating the old app with its ~100k downloads); the code namespace and all packages stay `br.app.ide.ouvindoabiblia`. So `context.packageName` = `ag.uny.…`, but class names (e.g., the `CastOptionsProvider` meta-data, `MainActivity` component) are `br.app.ide.…`. Don't "fix" one to match the other.
- Type-safe Navigation route matching must use `NavDestination.hasRoute(KClass)`, **not** `route.contains(::class.simpleName)` — R8 obfuscates `simpleName` in release, breaking substring matches (caused a double-selected bottom bar).
- Entity file is named `MoreContenEntity.kt` (typo in filename) but the class is `MoreContentEntity` — search by class name.
