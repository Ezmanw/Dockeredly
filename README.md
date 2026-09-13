# Dockeredly

Dockeredly is a native Android app that turns any website into a standalone,
app-like "web app" — similar in spirit to installing a PWA, but as a
dedicated launcher and container you fully control. Each web app gets its own
name, icon, and choice of rendering engine, and launches into a fullscreen
experience with no browser chrome: no address bar, no tabs, no visible URL.

Built entirely with Kotlin, Jetpack Compose, and Material 3 / Material 3
Expressive. It is not a browser, not a WebView demo, and not built on
Flutter/Electron/any cross-platform toolkit.

## What it does

- **Web App Library** — a Material 3 home screen listing every web app you've
  created, with search, drag-free reordering, and an empty state that
  explains the app on first launch.
- **Create / Edit a web app** — enter a URL, optionally name it yourself (or
  let the name be derived from the domain), pick an icon (the site's own
  favicon/manifest icon, a photo from your gallery, or a generic fallback),
  and choose its rendering engine.
- **Two independent rendering engines, per web app**
  - **Chromium** — Android System WebView.
  - **Firefox** — Mozilla GeckoView, embedded directly in the app (not a
    hand-off to the installed Firefox browser).

  Changing the app-wide default engine in Settings only affects *new* web
  apps; existing ones keep whatever engine they were created with, and you
  can change a single web app's engine from its edit screen.
- **Fullscreen runtime** — launching a web app opens it edge-to-edge with no
  browser UI. Android back gestures navigate the site's own history first,
  and only exit the web app once there's nowhere left to go back to.
- **Persistent, isolated browser data** — logins, cookies, and local storage
  survive closing the web app, backgrounding the app, and full restarts.
  Each web app's data is kept separate from every other web app's (see
  [Engine isolation](#engine-isolation) below for the real constraints this
  runs into on Android).
- **Material 3 theming** — light/dark/system, Android dynamic color, or a
  custom accent color with a generated `ColorScheme`, all persisted.
- **Settings** — default engine for new web apps, external-link handling,
  per-app or global "clear cache" / "reset browser data", and about info.

## Architecture

```
app/src/main/java/com/dockeredly/app/
├── browser/            # Engine abstraction — UI/domain never import WebView or GeckoView directly
│   ├── core/           #   BrowserEngine interface, factory, availability checks, process-slot logic
│   ├── chromium/        #   Android System WebView implementation
│   └── gecko/           #   Mozilla GeckoView implementation
├── data/
│   ├── database/        # Room entities/DAO for web app records
│   ├── preferences/      # DataStore-backed global settings
│   └── repository/       # Repository implementation
├── domain/
│   ├── model/            # Immutable domain models (WebApp, RenderEngine, AppSettings, ...)
│   └── repository/       # Repository interface
├── di/                   # Minimal hand-rolled composition root (no DI framework needed at this size)
├── navigation/           # Navigation Compose graph and routes
├── runtime/              # The fullscreen web app runtime activity + launcher
├── ui/
│   ├── components/       # Shared composables (icon, cards, engine selector, empty state)
│   ├── screens/          # Library, editor, details, settings, theme customizer
│   ├── theme/            # Material 3 theme, typography, seed-color generation
│   └── viewmodel/        # ViewModels (StateFlow-based, one per screen)
└── util/                 # URL validation, favicon fetching, icon processing, download handling
```

The `browser` package is the architectural core: the rest of the app talks
only to the `BrowserEngine` interface, so adding a third engine later
wouldn't require touching UI or domain code.

### Engine isolation

Real per-web-app isolation is engine-dependent, and this is documented rather
than glossed over:

- **GeckoView** supports `contextId`-based storage partitioning (the same
  mechanism behind Firefox's multi-account containers), so each Gecko web
  app gets genuinely separate cookies/storage within one shared
  `GeckoRuntime`.
- **Android System WebView** has no equivalent per-instance API — a
  process's WebView data directory (`setDataDirectorySuffix`) can only be set
  once, before any WebView is created, for the lifetime of that process.
  Dockeredly works around this by launching each Chromium web app into one
  of a fixed pool of dedicated processes (`WebAppRuntimeActivitySlot0..7`),
  chosen by hashing the web app's id. This gives real, OS-enforced isolation
  up to the pool size; two web apps that hash into the same slot share that
  slot's WebView storage if both are ever opened. See
  `browser/core/BrowserProcessSlots.kt` for the full reasoning.

## Requirements

- Android Studio (a recent stable release) or the command-line tools below.
- JDK 17.
- Android SDK Platform 35, Build-Tools 35.0.0 (installed automatically by
  Android Studio, or via `sdkmanager`).

## Building

```bash
./gradlew :app:assembleDebug   # debug APK -> app/build/outputs/apk/debug/
./gradlew :app:lintDebug       # static analysis
```

`local.properties` (not committed) should point `sdk.dir` at your Android
SDK, e.g.:

```
sdk.dir=/path/to/Android/sdk
```

Continuous integration (`.github/workflows/android-build.yml`) runs lint and
an assemble on every push/PR and on `v*` tags, and uploads the resulting
debug APK as a build artifact.

## Notable dependency choices

- **GeckoView** is pinned to a late-2024 release rather than the newest
  build available at time of writing, because newer GeckoView releases pull
  in `androidx.core`/`androidx.lifecycle` versions that require a
  `compileSdk`/Android Gradle Plugin combination beyond current stable
  tooling. See `gradle/libs.versions.toml` for the exact version and the
  commit history for the tooling investigation.
- The Google Sans Flex font is bundled from Google Fonts (OFL-licensed; see
  `OFL-GoogleSansFlex.txt`) rather than fetched at runtime, so typography
  doesn't depend on Google Play Services being present.

## License

No license file has been added yet; treat this as all-rights-reserved until
one is added.
