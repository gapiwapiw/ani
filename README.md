# ani

A calm Android anime player: search, watch, download for offline. Kotlin, Jetpack Compose, Media3.
Personal project. See `HANDOFF.md` for architecture, status and risks.

## Build
There is no local build setup. GitHub Actions builds the debug APK on every push to `main`
(`.github/workflows/build.yml`). Open the latest run and download the `app-debug` artifact, then install it.
Toolchain is pinned in `gradle/libs.versions.toml` (AGP 9.5.3, Gradle 9.9.2, Kotlin 2.4.21, compileSdk 37.1).
To look up current library versions, run the `versions` workflow and read its log.

## How the app finds videos
All site-specific logic (URLs, regexes, keys) lives in one file: `resolver.js`. The Kotlin code only runs it
inside QuickJS and calls `search`, `episodes`, `sources`, `info`, `findSubs`.
The app loads the resolver from, in order: `RESOLVER_URL` (if set), the cached copy, the bundled copy.

## When search or playback stops working
1. Edit `resolver.js` only (the site probably changed its markup, API path or embed format).
2. Bump `var version`.
3. Push. A new APK is built with the fixed bundled copy.
4. To fix without a new APK, host `resolver.js` at a URL the phone can read and build with
   `-PresolverUrl=https://.../resolver.js` (or the `RESOLVER_URL` env var). Then use Settings > Source > Refresh.

## Data
History, my list, watch later and settings: `state.json` in the app's files folder.
Downloads: the app's `dlcache` folder plus `dl/` for subtitles. Removing the app removes them.
