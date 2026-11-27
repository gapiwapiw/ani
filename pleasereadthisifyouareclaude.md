# Please read this if you are Claude

Hi. You are one of several Claude sessions working on the same personal project, one after another. Earlier sessions ran out of budget partway through, so the whole setup exists to let you pick up where they stopped and let the next session pick up where you stop. Read this file fully, then `HANDOFF.md`, then start working.

## What the project is

**ani** is a personal anime watching app for one user (the owner). It currently exists as a web app that runs inside Termux: a Node server plus an HTML/CSS/JS UI. The goal is to replace it with a **standalone native Android app** (Kotlin, Jetpack Compose) that needs no Termux, no Node and no shell.

- It is a personal project. There is no Play Store release, no other users and no need for signing beyond the default debug key.
- The owner **cannot run Android Studio**. The app is compiled by **GitHub Actions** into a debug APK, which the owner downloads and installs. So your code has to compile in CI without local testing. Boring and correct beats clever.
- The owner's Claude budget is limited. Keep replies short, do not ask for permission between steps, and do not repeat explanations.

## What the owner will give you in the chat

The owner will attach these files to the chat themselves, so **do not fetch them from the repo**:

- `ani-lite.zip`: the existing Termux app. `public/app.js` is the behavior spec for every screen and the player. `dl.js` is the downloader spec. `lib.sh` (the `hianime_*` functions) and `server.js` (`getSources`), `subs.js` and `group.js` are the specs for finding streams, subtitles and grouping seasons.
- `ani-redesign.html`: a clickable prototype of the new look, with every screen. It is the visual spec; read its CSS for exact values.

If a file is missing from the chat, say so in one line and ask the owner to attach it. Do not guess its contents.

## The repository

- Repo: `https://github.com/gapiwapiw/ani` (private)
- Branch: `main`
- `HANDOFF.md` at the repo root holds the current status and next steps. It is the source of truth for progress. This file (`pleasereadthisifyouareclaude.md`) is the static briefing and should rarely change.

## The token (read carefully)

The owner will paste a GitHub **fine-grained personal access token** in the first message of your chat. It is scoped to this one repo with Contents (read/write), Workflows (read/write) and Actions (read) permissions, and it expires within days.

Rules for the token:

1. **Never write it into any file, commit, commit message, log, README or `HANDOFF.md`.** GitHub may auto-revoke tokens that appear in pushed code, and it would leak.
2. **Never repeat it in your replies.** If you must refer to it, call it "the token".
3. Use it only for `github.com` and `api.github.com` over HTTPS. For example:
   `git clone https://x-access-token:<TOKEN>@github.com/gapiwapiw/ani.git`
   Do not echo the remote URL back to the owner, and do not commit `.git/config`.
4. With it you can also read CI results through the GitHub API (Actions: read). When the build fails, fetch the failed run's log yourself and fix the error instead of asking the owner to paste it.
5. If a push or API call is rejected for permissions, tell the owner exactly which permission is missing. Do not try workarounds.
6. At the end of your work, remind the owner to revoke the token.

If no token was provided, say so in one line. Work in the sandbox, deliver a zip, and update `HANDOFF.md` content in your final reply so the owner can commit it by hand.

## How to work

- **Start:** clone the repo, read `HANDOFF.md`, and continue from "Next steps".
- **During:** commit and push after every meaningful step, at least every few files. The sandbox resets, so anything not pushed is lost. A half-finished but pushed state is better than a perfect unpushed one.
- **If your budget is running low:** stop starting new work, update "Status", "Next steps" and "Session log" in `HANDOFF.md`, push, and tell the owner. The next session depends on this.
- **When CI fails:** read the log, fix only what the error implicates, push, and repeat. Do not rewrite unrelated files.
- **Be honest about what is unverified.** If you cannot compile locally, say so in one line, do a deliberate review (imports, nullability, `@OptIn`, deprecated APIs, manifest merge), and let CI confirm.
- Look up the current stable versions of Compose BOM, Media3, OkHttp, Coil, Room, AGP and Kotlin with web search before pinning them. Do not rely on memory.

## Architecture (decided, do not reopen)

- **UI:** Jetpack Compose with a custom theme. Do not use the default Material 3 look.
- **Player and downloads:** Media3 (ExoPlayer, HLS, `DownloadService` / `DownloadManager`).
- **Networking and storage:** OkHttp, Coil, and Room or DataStore. The existing data shape for history, the list and settings is in `server.js` and `app.js`.
- **Resolver:** all site-specific scraping (search, episode list, stream and subtitle extraction, decoding) lives **only** in one JavaScript file, `resolver.js`, at the repo root. This lets the owner fix breakage by editing one file, without rebuilding the APK.
  - Contract: `search(query, mode)`, `episodes(showId, mode)`, `sources(showId, ep, mode)` returning `{m3u8, referer, headers, subtitles:[{lang,url}], audio?, qualities?}`, and `version` (integer).
  - The app injects `http.get(url, headers)` plus base64, utf8 and xor helpers.
  - Runtime: either a maintained QuickJS binding for Android, or a hidden WebView with an OkHttp bridge. Verify the library exists and is maintained before choosing.
  - Loading order: remote `RESOLVER_URL` (a `BuildConfig` constant), then the cached last good copy in `filesDir`, then the copy bundled in assets. Settings shows source, version, last update time and a Refresh button.
  - No site-specific string, regex, key or URL may exist outside `resolver.js`.
  - Because the repo is private, its raw URL needs a read-only token. Prefer storing a separate read-only token as a GitHub Actions secret and injecting it into `BuildConfig` at build time, unless the owner has made the repo public. See `HANDOFF.md` for the current decision.
- **Do not port** the ani-cli menus, history, mpv or self-update code. Port only the source-finding logic.
- **Downloads:** foreground service of type `dataSync`, HLS at the chosen quality, subtitles saved alongside, maximum 2 concurrent, pause/resume/remove, survives process death, offline playback with the same headers and referer. Manifest needs `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS` (runtime request on Android 13+) and `WAKE_LOCK`.

## Features (confirm each against `app.js`)

Home (continue watching, my list, recently viewed with Clear), Search (recent searches, grid, Sub/Dub), Show page (hero, meta, description with More, watch later, my list, seasons, episodes with watched/progress, Watch or Continue), Downloads (queue, per-item pause/resume/remove, storage summary), Settings (subtitle language, subtitle size, default speed, resolver status, about), offline banner, and the Player (resume position, seek, double-tap skip, quality menu, subtitle menu, speed, fit/fill, immersive fullscreen, keep screen on, auto-hide controls, intro skip if the web app has it). The back button closes the player, then the show page, then exits.

## Design

Dark only. Follow `ani-redesign.html` exactly.

- **Colors:** background `#151413`; surfaces `#1c1b19`, `#242220`, `#2e2c29`; hairline `rgba(255,245,235,.08)`; text `#e6e1da`; muted `#9a948b`; accent `#b34a43` with on-accent `#fff3ee`; accent for text and progress `#d67a70`. Use the accent only for the Watch button, progress and active states.
- **Shape and spacing:** radii 6 / 12 / 20 dp. Pills only for the Sub/Dub toggle and chips. A 4 dp spacing grid.
- **Type:** 12, 14, 16, 18, 22, 28 sp; weights 400, 500 and 600 only; system font; tabular numerals (`fontFeatureSettings = "tnum"`) for times and counts.
- **No** glows, blurs or decorative gradients. The only gradients are the hero fade and the player's top and bottom scrims.
- **Icons:** 1.6 dp stroke line icons. Port the path data from the prototype's SVG symbols into `ImageVector`s.
- **Motion:** 120 / 200 / 300 ms, one easing curve, no stagger, no bounce, no scale-on-press (change opacity or background instead). Respect reduced motion.
- **Player:** play/pause 56 dp, skip 36 dp, 3 dp seek track, 12 dp thumb, soft subtitle shadow, 16 sp title, controls hide on tap.
- **Brand:** reuse `logo.svg` and `icon.svg` from the zip (convert to vector drawables) and make an adaptive launcher icon from `icon.svg`.
- **Copy:** sentence case, plain verbs, no all-caps labels.

## Build setup (must compile blind)

- Single module `app`, package `app.ani`, application id `app.ani`, app name "ani", `minSdk` 26, current stable `compileSdk` and `targetSdk`. Kotlin 2.x with the `org.jetbrains.kotlin.plugin.compose` plugin and a version catalog.
- Do not rely on a Gradle wrapper. The workflow installs Gradle with `gradle/actions/setup-gradle`, using the Gradle version the AGP version requires, then runs `gradle assembleDebug --stacktrace`. Use the JDK that AGP requires.
- `gradle.properties`: `android.useAndroidX=true`, `org.gradle.jvmargs=-Xmx3g`, `org.gradle.daemon=false`.
- Workflow at `.github/workflows/build.yml`: triggers `workflow_dispatch` and push to `main`; `ubuntu-latest`; checkout, setup-java, setup-gradle, `assembleDebug`; upload the APK as artifact `app-debug` with `if: always()`; upload `app/build/reports` as artifact `reports` when the build fails.
- Avoid exotic libraries. Add `@OptIn(UnstableApi::class)` where Media3 requires it. Debug builds do not use ProGuard.

## When you finish a session

Reply with: the file tree, what could not be verified, and the known risks (resolver breakage, download headers and referer, Media3 API drift). Confirm that everything is pushed and that `HANDOFF.md` is up to date. Remind the owner to revoke the token.

Thank you, and good luck.
