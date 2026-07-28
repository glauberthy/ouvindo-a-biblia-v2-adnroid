# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

"Ouvindo a Bíblia" — an Android audio-streaming app (audio Bible, themed moments, studies) built with Jetpack Compose, Media3/ExoPlayer, Hilt, and Room. The codebase and comments are in Portuguese (pt-BR); match that language in user-facing strings and inline comments.

## Where the project docs live

Code comments reference issues by number (`ISSUE 2.A`, `ISSUE 9.G`, `DIAGNOSTICO_02 §5.1`, `PUB-01`). Those numbers resolve to:

- **`PLANO_FASES_E_ISSUES.md`** — the live backlog/issue log by phase (FASES 0→9 done, FASE 8 = publicação Play Store still open). Each issue records problem → files → acceptance criteria → validation, and is marked ✅ with date/commit when closed. **When you close something non-trivial, update the matching issue here.**
- **`DIAGNOSTICO_01_ARQUITETURA.md`** / **`DIAGNOSTICO_02_PLAYER.md`** — the audits that most of the current design decisions cite.
- **`ROADMAP.md`** — prioritized remaining work; **`README.md`** — short public overview + player lifecycle summary.
- **`docs/`** — privacy policy, Play Store listing text. **`docs/archive/`** — superseded plans (`ANALISE_MODULARIZACAO_PLAYER.md`, `REFACTOR_MEDIA3_TODO.md`, auditorias, smoke tests). Historical record, not current state.

Release status: the app ships as a **new** app (not a reactivation of the old listing — see the `applicationId` gotcha), `versionCode = 1` / `versionName = "1.0"`. FASE 8 (publicação) is still open — check `PLANO_FASES_E_ISSUES.md` for what's left instead of assuming it's live.

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
./gradlew :app:testDebugUnitTest --tests "*.TaskRemovalPolicyTest"
./gradlew :app:testDebugUnitTest --tests "*.AudioRetryPolicyTest.someMethod"
```

`--tests` only exists on the **variant** task (`testDebugUnitTest`), not on the aggregate `test` — `./gradlew :app:test --tests …` fails with `Unknown command-line option '--tests'`.

The meaningful JVM tests all lock **pure decision functions** extracted from bigger classes precisely because that logic used to hide bugs — treat them as the spec:

- `:app` — `service/TaskRemovalPolicyTest` (swipe nos recentes), `playback/{AudioRetryPolicyTest, MediaContentIdTest, PlaybackErrorMessageTest}`, `ui/player/*` (progresso do mini player, `isStudyMediaId`, timeline), `ui/studies/VisibleLessonDescriptionTest`.
- `:data:remote` — `error/NetworkErrorKindTest`, `StudyDtoParseTest`.
- `:data:repository` — `SyncErrorMessageTest`.
- Instrumented: `:app` `PlaybackServiceLifecycleTest` / `AndroidAutoBrowseTest`, `:data:local` `MigrationTest` / `StudyLessonRefreshTest`.

The `ExampleUnitTest`/`ExampleInstrumentedTest` files in every module are the template stubs — ignore them.

Note: all modules use `compileSdk = 36` and the app `targetSdk = 36` (unified). `minSdk` is 26 (app) / 24 (libraries). KSP (not kapt) drives Room and Hilt code generation — a clean build after touching entities/DAOs/`@Module`s may need `./gradlew clean`. Toolchain: AGP 8.13.2, Kotlin 2.0.21, JVM target 11, Media3 1.9.2, Room 2.6.1, Hilt 2.51.1 (all pinned in `gradle/libs.versions.toml`).

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

- **`:app`** — UI (Compose), `PlaybackService`, dormant Cast code, and the media/image DI wiring (`MediaModule` provides the ExoPlayer — *unscoped*, see below; `CoilModule` provides the app-wide `ImageLoader`).
- **`:data:repository`** — `BibleRepository` interface + `BibleRepositoryImpl`. The single orchestration point: decides when to hit the network vs. serve from Room, exposes everything to the UI as `Flow`. Defines the UI-facing domain models (`Book`, `Chapter`, `Theme`, `Study`, `PlaybackState`, …) and the `Resource<T>` load state.
- **`:data:local`** — Room (`BibleDatabase`, entities, `BibleDao`) + DataStore (sync version keys). All persistence lives here.
- **`:data:remote`** — Retrofit `BibleApi` + kotlinx-serialization DTOs.

DI: Hilt with `@InstallIn(SingletonComponent::class)`. Each module owns its modules — `NetworkModule` (remote), `DatabaseModule`/`DataStoreModule` (local), `RepositoryModule` (repository binds the impl), `MediaModule`/`CoilModule` (app).

## Data sync model

Content is static JSON fetched from `https://ouvindo-a-biblia.ide.app.br/` (`biblia_index.json`, `themes.json`, `estudos.json`, `mais.json`). Sync is **version-gated**: each payload carries a `meta.version`; `BibleRepositoryImpl` compares it against a value stored in DataStore (`bible_settings` → `bible_data_version`, `themes_data_version`, `studies_data_version`, …) and only rewrites Room when the version changed. There is no background worker.

**The repository owns the load state, not the ViewModels (ISSUE 3.B).** `getBooksResource()` / `getThemesResource()` / `getStudiesResource()` / `getMoreContentResource()` are built by the private `syncedListResource(cacheFlow, ::sync)` helper and emit `Resource.Loading` while there's no cache, `Resource.Success` as soon as there's data (even if the sync failed silently), and `Resource.Error` only when there's no cache *and* the sync failed. ViewModels just map `Resource` → their own `UiState`; don't reimplement "tem cache? falha silenciosa?" in a new screen — add a `*Resource` flow instead. Note that a synced-but-empty list is treated as **terminal `Error` with Retry**, not as eternal `Loading` (ISSUE 6.D).

**Room migrations are mandatory now — `fallbackToDestructiveMigration()` is gone.** `BibleDatabase` is at `version = 10` with `exportSchema = true` and explicit `MIGRATION_8_9` (no-op, just to stop the destructive fallback) and `MIGRATION_9_10` (`study_lessons.description`), wired in `DatabaseModule`; only `fallbackToDestructiveMigrationOnDowngrade()` remains. Reason: favorites (chapters *and* study lessons) and the "continuar ouvindo" position are **local-only data with no server copy** — a destructive bump silently deletes user data. So any entity change needs its own `Migration` + an entry in `MigrationTest`; without one Room now *throws* instead of wiping.

## Playback architecture (the core of the app)

This is the most intricate part — read `PlaybackService.kt` and `PlayerViewModel.kt` together before changing playback. Background notes live in `README.md`, `DIAGNOSTICO_02_PLAYER.md` and (superseded) `docs/archive/ANALISE_MODULARIZACAO_PLAYER.md` / `docs/archive/REFACTOR_MEDIA3_TODO.md`.

- **`PlaybackService`** extends `MediaLibraryService` and owns the `MediaLibrarySession` + a `MediaLibrarySession.Callback`.
- **The `ExoPlayer` belongs to the service — it is deliberately NOT `@Singleton`.** `MediaModule.provideExoPlayer` is an *unscoped* binding, and `PlaybackService` is its only injection point, so each `onCreate` gets a fresh player released once in the real `onDestroy`. A process-scoped singleton caused the critical bug where a new session was built on top of an already-released player (`DIAGNOSTICO_02 §5.1`). Don't re-add `@Singleton` and don't inject `ExoPlayer` anywhere else.
- **`PlayerViewModel`** connects to the service via a `MediaController` (Media3 session). It does **not** touch ExoPlayer directly; it sends transport commands through the controller.
- **Google Cast is dormant, not active.** All the Cast code (`SessionManagerListener`, `RemoteMediaClient`, `loadMediaOnCast`, `CastButton`, `CastOptionsProvider`) is still in the repo but gated by a single kill-switch, `CastConfig.ENABLED = false` (ISSUE 4.C) — with it off, `CastContext.getSharedInstance` is never called (that was a StrictMode disk-I/O-on-main violation at startup/rotation) and the button is commented out in `SharedPlayerScreen`. Reactivating means flipping the flag *and* un-commenting the button. Cast branches (`castSession?.isConnected == true`) still guard every transport method — keep them consistent when editing transport.
- **`mediaId` has exactly one format/parse point: `playback/MediaContentId.kt`** (ISSUE 2.A). Build ids via the `data class`es and use `.raw` in `setMediaId`; read them via `MediaContentId.parse(raw)`, which returns `null` for malformed ids (call site logs and moves on). Do **not** hand-roll the string prefixes — they used to be duplicated in ~10 places.
  - Bible chapter → numeric chapter id as string (e.g. `"1234"`) → `MediaContentId.Bible`.
  - Study lesson → `"study_{studyId}_{lessonId}"` → `MediaContentId.Study`.
  - Theme moment → `"moment_{momentId}"` → `MediaContentId.ThemeMoment` (deliberately **not** persisted as resume state, and never re-parsed).
  - Browsable-tree node ids (a book's plain `numericId` in `onGetChildren`) don't go through `parse` — a bare numeric always means `Bible`.
  - The old "book folder" id `"{bookId}|{chapterIndex}"` **was removed in ISSUE 3.D** — don't reintroduce it. Phone playlists arrive complete from `PlayerViewModel`; the expansion path that remains is for **Android Auto**, where a tapped browsed item arrives as a single item with `localConfiguration == null`, and `onSetMediaItems` → `buildPlaylistFromMediaId` expands it into the whole book/study with the right start index (so auto-advance and next/prev work). `onAddMediaItems` → `resolvePlayableItem` does the same resolution for id-only items.
- **Resume / persistence:** playback position is auto-saved to Room (`PlaybackStateEntity`) on media transitions and pause (`setupAutoSaveListener` → `saveCurrentState`) **and periodically every 15s while playing** (`startPeriodicSave`/`PERIODIC_SAVE_INTERVAL_MS`, ISSUE 1.A) so a process kill loses at most 15s instead of the whole chapter. On service create, `restoreLastSession` rebuilds the playlist from the saved state but does **not** auto-play. `buildPlaylistFromState` reconstructs `MediaItem`s for Bible/Study from the repository. Theme moments are excluded from saving.
- **`shouldBlockDatabaseResumption` (5s window):** guards against a saved session "resurrecting" over a fresh explicit play command during the transition window. `markExplicitPlaybackRequest` is called on every explicit `onSetMediaItems`. Be careful not to break this when editing playback start logic.
- **Lifecycle / swipe nos recentes:** the service survives the `MainActivity` being destroyed and the app going to background (it's started + foreground while playing), but **not** task removal. `onTaskRemoved` decides via the pure function `decideOnTaskRemoval` (`service/TaskRemovalPolicy.kt`, locked by `TaskRemovalPolicyTest`): **playing → stop** (save position synchronously, pause, release player + session, `stopForeground(STOP_FOREGROUND_REMOVE)`, `stopSelf()`); **paused after having played → keep** the dismissible notification (ISSUE 4.A); **restored-but-never-played → stop** (BUG B, no orphan notification). It deliberately does **not** call `super.onTaskRemoved()` — Media3's default only stops when *not* playing, the opposite of what's needed.
- **`onPlaybackResumption`** is implemented: it honors `shouldBlockDatabaseResumption()`, then serves the in-memory playlist if the player still has items, else rebuilds from Room via `buildPlaylistFromState`; failures are logged and answered with an empty list instead of crashing. It returns the same `MediaItemsWithStartPosition` regardless of the (deprecated) `isForPlayback` flag — the framework decides whether to actually play.
- The shutdown path must save the position with `saveCurrentStateBlocking()`, not `saveCurrentState()`: the latter writes on `serviceScope`, which `onDestroy` cancels (`serviceJob.cancel()`), so a `stopSelf()` right after would drop the write and resume would jump back to the start of the chapter. For the same reason the swipe path captures a synchronous `PlaybackSnapshot` before releasing the player — reading `player.currentPosition` inside the IO coroutine would read a released player.
- **Android Auto** is wired through the browse tree (`onGetLibraryRoot`/`onGetChildren`/`onGetItem`) and declared via `@xml/automotive_app_desc`; `AndroidAutoBrowseTest` covers it. The service is declared with `foregroundServiceType="mediaPlayback"` and needs `POST_NOTIFICATIONS` (without it the audio plays but the user gets no controls).

## UI conventions

- Single-Activity (`MainActivity`, `AppCompatActivity`, `@AndroidEntryPoint`) hosting Compose. `MainScreen` holds the bottom nav + the shared/expandable player (`SharedPlayerScreen`) and a single shared `PlayerViewModel`. The notification deep-link (`OPEN_PLAYER_FROM_NOTIF` intent extra) drives opening the player.
- Navigation uses **type-safe Navigation-Compose** with `@Serializable` destinations defined in `Screen` (`AppNavigation.kt`); routes are built in `NavigationGraph.kt`. Add a destination by adding a `Screen` subtype and a `composable<Screen.X>` block. Five bottom-nav tabs: `Home` ("Livros"), `Favorites`, `Themes`, `Estudos`, `More` ("Mais") — note the destination is still `Screen.Home`, only the label changed; sub-destinations are `ThemeDetails`, `StudyDetails`, `MoreRights`, `MoreSection`.
- Screens follow an MVI-ish **LCE pattern**: a `*Contract.kt` defines a sealed `UiState` (`Loading`/`Error`/`Success`) and a sealed `Intent`; the `*ViewModel` exposes a `StateFlow` and a single `handle(intent)`. Mirror this for new screens — including the two idioms `HomeViewModel` shows: map `Resource` → `UiState` in a private extension, and `stateIn(started = SharingStarted.WhileSubscribed(5_000))` (revisit under 5s reuses the stream; over 5s intentionally re-checks `meta.version`, which is cheap because sync is version-gated). Local UI state that must not re-trigger a sync (e.g. the testament filter) is `combine`d *on top of* the resource flow.
- **Favorites** are local-only: `toggleFavorite(chapterId, …)` for Bible chapters and `toggleStudyFavorite(studyId, lessonId, …)` for study lessons, surfaced by `getFavorites()` / `getFavoriteStudyLessons()`. There is no server copy — see the migration rule above.
- **Dark theme is ON** (`DARK_THEME_ENABLED = true` in `ui/theme/Theme.kt`, following `isSystemInDarkTheme()`; both schemes were checked screen by screen on the emulator). The flag exists as a regression brake: if a new screen hard-codes light colors, turning it off beats shipping a half-cream/half-dark app.
  - **Color rule:** use the `@Composable` semantic tokens in `AppColors` (`background`, `card`, `textPrimary`, `textSecondary`, `outline`, `badge`/`onBadge`) for anything that is *page*, and the brand tokens (`BrandNavy`/`OnBrandNavy`) for what stays dark in **both** themes (bottom bar, player). Swapping one for the other inverts the screen. Never reference the top-level `val` colors (`CreamBackground`, `DeepBlueDark`, …) from a screen — they don't react to the theme, which is exactly why dark mode was broken before.
- `WindowSizeClass` is threaded down from `MainActivity` (which also calls `installSplashScreen()`) for tablet/responsive layouts.
- **`StatusBarScrim` is the app's single status-bar veil** (ISSUE 9.G), rendered once in `MainScreen` over every screen, replacing gradients that used to be baked into server images. **Never add a second one** — there used to be two stacked (this component on Theme/Study headers *plus* an inline `Brush.verticalGradient` starting at alpha 1.0 in `MainScreen`), which measured 14.5:1 behind the status bar icons over the darkest cover art (3× the 4.5:1 needed) and read as a bright haze band over dark photos. Peaks are calibrated at the legibility limit (0.52 light / 0.62 dark — asymmetric on purpose) and `StatusBarScrimContrastTest` locks both the floor and the ceiling, so tune only inside the component.
- Images: Coil with the app-wide `ImageLoader` (provided via `OuvindoBibliaApp : ImageLoaderFactory`). The service loads notification artwork through the same `ImageLoader` via a custom `CoilBitmapLoader`.

## Rate limiting & user-facing error messages

The server sits behind a WAF that **rate-limits (429)**, and the app hits it in bursts (Home fires ~66 covers at once; fast chapter switching bursts audio requests). Each transport has its own retry policy, and the two that surface text have their own classifier + message (both locked by unit tests). Keep them aligned when you touch any of them:

| Transport | Retry/backoff | Classify | Message |
|---|---|---|---|
| JSON sync (OkHttp/Retrofit) | 429 interceptor in `NetworkModule`, up to 3 tries, `Retry-After` capped at 5s | `networkErrorKind()` (`:data:remote`) | `syncErrorMessage()` (`:data:repository`) → `Resource.Error` |
| Audio (ExoPlayer) | `audioRetryBaseDelayMs()` via a `DefaultLoadErrorHandlingPolicy` in `MediaModule` (+ jitter there, not in the pure fn) | `classifyPlaybackError()` | `playbackErrorText()` |
| Cover images (Coil) | 429 interceptor in `CoilModule`, 3 tries, backoff + jitter | — | — |

Rules baked into those policies, with the bug each one fixes:

- **Definitive HTTP codes are not retried** (`UNRECOVERABLE_HTTP_CODES` = 400/401/403/404/410/416/501) — an audio pulled from the server used to cost ~7s of pointless waiting before failing anyway.
- **429 gets a longer ladder than generic errors** (2s/5s/12s vs 1s/2s/4s), because the WAF window is wider than the old 1+2+4 backoff — 6 track changes in 18s killed playback. `Retry-After` is honored, capped at 15s (audio) / 5s (sync).
- **Never surface library text to the user.** HTTP knowledge stays in `:data:remote`, the wording lives one layer up; messages never contain a stacktrace or an HTTP code. A first install under rate limit literally read "HTTP 429 Too Many Requests" before this.
- **"Verifique sua conexão" is wrong for 429** — the user's network is fine and it sends them hunting the wrong defect. `SERVER_BUSY` says the server is busy, `CONTENT_UNAVAILABLE` says the audio isn't there.
- **Playback errors reach the UI across the Binder**, which loses the exception *type* but keeps the *message* — so `classifyPlaybackError` falls back to regex-reading `"Response code: NNN"` from the cause chain. That text path is the one that matters in production; don't "clean it up".

## Gotchas

- Many Media3 APIs are `@UnstableApi` and require `@OptIn(UnstableApi::class)`.
- The OkHttp client sends a required `User-Agent: BibliaFaladaApp` header (server WAF rejects without it); the ExoPlayer `DefaultHttpDataSource` sets the same UA. Don't drop it.
- `setMaxSeekToPreviousPositionMs(Long.MAX_VALUE)` in `MediaModule` is deliberate: with Media3's 3s default, "previous" from a Bluetooth/headset/notification/Auto button **restarts** the current chapter past 3s, while the in-app button (`skipToPreviousChapter`) always skips items — the same action behaved two different ways. Chapters are long; restarting is what `seekBack` (10s) and the progress bar are for.
- **`applicationId` == `namespace` == `br.app.ide.ouvindoabiblia`.** So the launch component is `br.app.ide.ouvindoabiblia/.MainActivity`. History (don't reintroduce): the app briefly shipped with applicationId `ag.uny.ouvindoabiblia` to reactivate the old store listing with its ~100k downloads, while the namespace stayed `br.app.ide.…`; commit `dcbee84` **reverted it** — the app is published as a NEW app. Anything mixing the two (`am start -n ag.uny.ouvindoabiblia/br.app.ide.ouvindoabiblia.MainActivity`) now fails with `Activity class {…} does not exist`. `ag.uny.…` in `docs/archive/` and in `PLANO_FASES_E_ISSUES.md` is historical record of that period, not current config.
  - If **Android Studio** reports `Activity class {ag.uny.ouvindoabiblia/br.app.ide.ouvindoabiblia.MainActivity} does not exist` right after "Install successfully finished", the build is fine — the IDE's cached Gradle model still holds the old applicationId (`GradleApkProvider` logs it in `~/.cache/Google/AndroidStudio*/log/idea.log`). Fix: *Sync Project with Gradle Files*, then *Invalidate Caches / Restart*. Not reproducible from the CLI.
  - Stale release outputs may also predate the revert: `app/build/outputs/apk/release/` held an `ag.uny.ouvindoabiblia` vc4/vn3.0 APK. Run `./gradlew clean` before release smoke tests so you don't validate the old identity.
- Type-safe Navigation route matching must use `NavDestination.hasRoute(KClass)`, **not** `route.contains(::class.simpleName)` — R8 obfuscates `simpleName` in release, breaking substring matches (caused a double-selected bottom bar). Related: when you add a **sub-destination** of a bottom-nav tab (e.g. `MoreRights` under `More`, like `ThemeDetails` under `Themes`), add it to the `isSelected` mapping in `MainScreen.kt` too, or that tab loses its selected state while the sub-screen is open.
- Naming traps — search by class name, not filename/path: entity file `MoreContenEntity.kt` holds class `MoreContentEntity`; the Themes UI package is `ui/themas/` (pt-BR typo) while the destinations are `Screen.Themes`/`Screen.ThemeDetails` and the domain model is `Theme`; `ui/theme/` (singular, no "s") is the *design system*, unrelated to content themes.
