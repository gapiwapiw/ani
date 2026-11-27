# ani – handoff (read first, update last)

## Goal
Native Kotlin/Compose Android app replacing a Termux web app. Personal use only. Built by GitHub Actions (debug APK). Design spec: design/ani-redesign.html. Full briefing: pleasereadthisifyouareclaude.md.

## Rules for every session
- The sandbox resets. COMMIT AND PUSH after every meaningful step (at least every few files), never leave work only in the sandbox.
- Start: git clone, read this file, continue from "Next steps".
- End (or if running low on budget): update "Status" and "Next steps" below, push, and tell the owner.
- Keep replies short. No permission-asking between steps.
- When CI fails: read the failed run's log via the GitHub API (Actions: read), fix only what the error implicates, push, repeat.
- Never write the token anywhere (files, commits, logs).

## Architecture (decided, don't reopen)
- Compose UI, Media3 (ExoPlayer + HLS + DownloadService), OkHttp, Coil, DataStore/JSON (no Room/KSP to keep the blind build simple).
- All site-specific scraping lives ONLY in resolver.js (repo root): search(query, mode), episodes(showId, mode), sources(showId, ep, mode), version. App injects http.get + base64/xor helpers.
- Resolver loads: remote RESOLVER_URL, then cached copy, then bundled copy. Settings shows source/version/refresh.
- Port only the resolving logic from the original lib.sh/server.js, not ani-cli's menus/history/mpv/update.
- Private repo means raw URL needs a read-only token: [DECISION PENDING: public repo / read-only token via Actions secret into BuildConfig].
- Downloads: foreground service (dataSync), max 2 concurrent, subtitles saved alongside, offline playback with the same headers/referer.

## Design tokens
bg #151413 | surfaces #1c1b19 #242220 #2e2c29 | hairline rgba(255,245,235,.08) | text #e6e1da | muted #9a948b | accent #b34a43 (on-accent #fff3ee) | accent text #d67a70
Radii 6/12/20dp | spacing 4dp grid | type 12/14/16/18/22/28sp, weights 400/500/600 | motion 120/200/300ms, no bounce/stagger | no glow/blur.

## Original sources (reference, in /reference)
reference/ani-lite.zip: app.js = behavior spec, dl.js = downloader spec, lib.sh + server.js getSources = resolver spec.

## Environment notes
- Sandbox cannot reach Maven/Gradle hosts. Look up versions with the `versions` workflow (.github/workflows/versions.yml), read its log via the API.
- AGP 9.x has built-in Kotlin: do not apply org.jetbrains.kotlin.android.

## Pinned versions (looked up 2026-11-27 via versions workflow)
AGP 9.5.3, Gradle 9.9.2 (set in build.yml), JDK 21 (temurin), Kotlin 2.4.21, Compose BOM 2026.11.01, Media3 1.11.1, OkHttp 5.5.1, Coil 3.6.3, DataStore 1.3.0, activity-compose 1.13.0, lifecycle 2.11.1.
Gotchas learned: Compose 1.13 needs compileSdk 37.1, so build.gradle.kts uses `compileSdk { version = release(37) { minorApiLevel = 1 } }`. AGP 9 has built-in Kotlin, so no kotlin.android plugin. Theme is the platform Material NoActionBar (no appcompat/material3).

## CI helper (not in repo)
Dispatch/poll with the token via the Actions API (`POST /actions/workflows/<file>/dispatches` needs `Content-Type: application/json`). Job logs: `GET /actions/jobs/<id>/logs` (follow redirects).

## Status
- [x] Repo + workflow + Gradle project skeleton compiles in CI (run green, APK artifact `app-debug`)
- [ ] Theme + static screens (fake data)
- [ ] resolver.js + runtime + search/episodes
- [ ] Player
- [ ] Downloads + offline
- [ ] History, list, watch later, settings
- [ ] README (build, push, update resolver)

## Next steps
1. Theme (`ui/theme`: colors, type, shapes, motion constants) + 1.6dp line-icon ImageVectors ported from the prototype's SVG symbols.
2. Static screens with fake data: Home, Search, Downloads, Settings, Show page, Player UI, download sheet; bottom nav; back-button order (player, show page, exit).
3. Launcher icon from reference/ani-lite.zip `public/icon.svg`, logo from `logo.svg` (vector drawables).
4. Then resolver.js + runtime. Candidate QuickJS binding: `io.github.dokar3:quickjs-kt` (verify it is maintained and its version via the versions workflow; `app.cash.quickjs` 0.9.x is the old unmaintained one). Otherwise hidden WebView + OkHttp bridge.
5. Add libs (media3, okhttp, coil, datastore, serialization) to app/build.gradle.kts only when first used. They are already in gradle/libs.versions.toml.

## Known risks
- Resolver breaks when the site changes: edit resolver.js only.
- Blind-compile CI errors: fetch the log, fix minimally.
- Media3 API/version drift: check current docs before pinning versions.

## Session log
- 2026-11-27: repo scaffolding; versions looked up; Gradle skeleton + build.yml; CI green after the compileSdk 37.1 fix. Nothing else built yet.
