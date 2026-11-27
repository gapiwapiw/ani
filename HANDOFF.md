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
- Resolver URL decision (made 2026-11-27, owner had not answered): bundled resolver.js plus an OPTIONAL `BuildConfig.RESOLVER_URL` (gradle property `-PresolverUrl=` or env `RESOLVER_URL`, default empty). Empty means cached copy, then bundled. The owner can make the repo public later and set the URL, no code change needed.
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
- [x] Theme + static screens (fake data): Home, Search, Downloads, Settings, Show page, download sheet, Player UI (simulated timer), launcher icon. CI green.
- [x] resolver.js (search/episodes/sources/info/findSubs/toVtt + season grouping) and QuickJS runtime `data/Resolver.kt` compile in CI. NOT run on a device. quickjs-kt package is `com.dokar.quickjs` (group id is io.github.dokar3). Bundled copy: Gradle task copyResolver copies root resolver.js into assets.
- [x] Repository + persistence: `data/Repo.kt` (episode/info cache), `data/Store.kt` (state.json in filesDir: history, my list, watch later, recent searches/titles, prefs, download headers). Screens use real data (Fake.kt removed). Posters via Coil with resolver-declared `imageHeaders`.
- [x] Player: Media3 ExoPlayer HLS, Referer/UA headers, resume from history, auto-next, quality (max height), speed, own subtitle renderer (`data/Subs.kt`, VTT/SRT), extra subtitles via resolver findSubs. Compiles in CI. NOT run on a device.
- [x] Downloads: Media3 DownloadManager + foreground service (`AniDownloadService`, max 2 parallel), subtitle files saved next to the episode, offline playback through the shared cache (`Downloads.localLoaded`). Compiles in CI. NOT run on a device.
- [x] History, list, watch later, settings (subtitle language/size, speed, download quality, resolver status + refresh).
- [ ] README (build, push, update resolver)
- [ ] On-device verification of everything above (nothing has been run on a phone)

## Next steps
1. Install the CI APK on a phone and test in this order: search -> show page -> play -> download -> airplane-mode playback. Fix resolver.js first if search/sources fail (all site logic is there).
2. Known gaps: no notification-permission prompt (Android 13+: downloads run but the notification may be hidden), downloaded episodes are not marked in the episode list beyond the ring, no storage-location setting, no Cinemeta episode thumbnails yet (resolver returns them in `info().eps[s][e].th`).
3. Write README.md (build, push, how to change resolver.js and set RESOLVER_URL).
4. Optional: make the repo public or add a read-only token, then set RESOLVER_URL so resolver fixes ship without a new APK.

## UI map (app/src/main/java/app/ani/ui)
Theme.kt (colors C, shapes Shp, Motion, reduced-motion), Icons.kt (ImageVectors + AniLogo), Components.kt (AText, tap, Poster, ShowCard, Seg, Btn, IconBtn, Ring, AniSlider...), AniApp.kt (tabs, nav, back order sheet then player then show), Home/Search/Downloads/Settings/Show/Sheets/Player.kt.
Not Material: only foundation/ui/animation. Edge-to-edge is on (targetSdk 37), so insets are handled per screen.

## Known risks
- Resolver breaks when the site changes: edit resolver.js only.
- Blind-compile CI errors: fetch the log, fix minimally.
- Media3 API/version drift: check current docs before pinning versions.

## Session log
- 2026-11-27: repo scaffolding; versions looked up; Gradle skeleton + build.yml; CI green after the compileSdk 37.1 fix.
- 2026-11-27 (session 3): resolver.js + Resolver.kt + build wiring, CI green (fixes: assets srcDir must be a File on AGP 9, quickjs package name).
- 2026-11-27 (later): theme, icons, components, all static screens, launcher icon; CI green on second try (two import fixes). Not run on a device. Logo uses the prototype's calm colors, not logo.svg's old red.
- 2026-11-27 (session 3, cont.): repo/store, real screens, Media3 player, download manager and service, settings. CI green after fixes (Store.setPrefs JVM clash, DownloadHelper.Callback.onPrepared(helper, Boolean) in the Media3 version pinned here).
