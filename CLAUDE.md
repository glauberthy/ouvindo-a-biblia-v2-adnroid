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

Note: the `app` module compiles against `compileSdk = 35`; the `data:*` library modules use `compileSdk = 36`. `minSdk` is 26 (app) / 24 (libraries). KSP (not kapt) drives Room and Hilt code generation — a clean build after touching entities/DAOs/`@Module`s may need `./gradlew clean`.

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
- **Lifecycle ("Clean Exit"):** `onTaskRemoved` currently saves state then stops/releases the player and `stopSelf()`s. The roadmap in `README.md` describes moving to a Spotify-like persistent/`onPlaybackResumption` model — `onPlaybackResumption` is already partially implemented.

## UI conventions

- Single-Activity (`MainActivity`, `AppCompatActivity`, `@AndroidEntryPoint`) hosting Compose. `MainScreen` holds the bottom nav + the shared/expandable player (`SharedPlayerScreen`) and a single shared `PlayerViewModel`. The notification deep-link (`OPEN_PLAYER_FROM_NOTIF` intent extra) drives opening the player.
- Navigation uses **type-safe Navigation-Compose** with `@Serializable` destinations defined in `Screen` (`AppNavigation.kt`); routes are built in `NavigationGraph.kt`. Add a destination by adding a `Screen` subtype and a `composable<Screen.X>` block.
- Screens follow an MVI-ish **LCE pattern**: a `*Contract.kt` (or contract section) defines a sealed `UiState` (`Loading`/`Error`/`Success`) and a sealed `Intent`; the `*ViewModel` exposes a `StateFlow` and a single intent handler. Mirror this for new screens.
- `WindowSizeClass` is threaded down from `MainActivity` for tablet/responsive layouts.
- Images: Coil with the app-wide `ImageLoader` (provided via `OuvindoBibliaApp : ImageLoaderFactory`). The service loads notification artwork through the same `ImageLoader` via a custom `CoilBitmapLoader`.

## Gotchas

- Many Media3 APIs are `@UnstableApi` and require `@OptIn(UnstableApi::class)`.
- The OkHttp client sends a required `User-Agent: BibliaFaladaApp` header (server WAF rejects without it); the ExoPlayer `DefaultHttpDataSource` sets the same UA. Don't drop it.
- Entity file is named `MoreContenEntity.kt` (typo in filename) but the class is `MoreContentEntity` — search by class name.
