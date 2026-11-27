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

## Status
- [ ] Repo + workflow + Gradle project skeleton compiles in CI
- [ ] Theme + static screens (fake data)
- [ ] resolver.js + runtime + search/episodes
- [ ] Player
- [ ] Downloads + offline
- [ ] History, list, watch later, settings
- [ ] README (build, push, update resolver)

## Next steps
1. Get versions from the versions workflow, write the Gradle skeleton + build.yml, get CI green.

## Known risks
- Resolver breaks when the site changes: edit resolver.js only.
- Blind-compile CI errors: fetch the log, fix minimally.
- Media3 API/version drift: check current docs before pinning versions.

## Session log
- 2026-11-27: repo scaffolding (reference/, design/, HANDOFF.md, .gitignore).
