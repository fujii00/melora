# Melora project handoff and change log

Last updated: 2026-11-25  
Project directory: `C:\Users\ndzan\Desktop\fujii\dev stuff\BitChord-main`  
Upstream source project: BitChord (`kushagrasinghx/BitChord`); this working copy is being rebranded as **Melora**.

This file is the project handoff for a future developer or AI agent. Keep the **Change log** current after each meaningful project change and update **Current priorities** when work is completed or reprioritized.

## Product direction

- Local music on the device is the primary experience; web streaming integrations are deferred and optional.
- Audience is broad: people who want a polished local player, a strong library experience, and useful extras.
- The Android dev APK is the current delivery target. Desktop portability is desirable if the existing shared UI architecture supports it.
- Preserve the premium visual language already in the project, including its optional liquid-glass treatment and reduced-blur accessibility setting.
- Local files should support metadata-backed artwork and lyrics, with existing lyrics used when present and downloads/fetching available where supported.
- Home should feel like a full music service, but all discovery and recommendations should come from the local collection and on-device listening behavior.

## Architecture notes

- Android entry point and tab composition: `app/src/main/java/com/music/bitchord/MainActivity.kt`.
- Shared Compose feed/card UI: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/HomeScreen.kt`.
- Local Explore UI: `app/src/main/java/com/music/bitchord/ui/screens/LocalExploreScreen.kt`.
- Device library scan and local release grouping: `app/src/main/java/com/music/bitchord/data/LocalMediaRepository.kt` and `LocalGroups.kt`.
- Device playlists and liked songs: `app/src/main/java/com/music/bitchord/data/OfflinePlaylists.kt`.
- Local listening statistics / Replay: `app/src/main/java/com/music/bitchord/data/stats/ListeningRecorder.kt` and `ListeningStats.kt`.
- Player artist credits already have an `onOpenArtist(browseId, name)` callback through `PlayerControls.kt` and `NowPlayingScreen.kt`; verify all visible artist-name surfaces use it before adding new navigation plumbing.
- Android Gradle namespace and Kotlin package remain `com.music.bitchord` for compatibility. Installed app IDs are Melora-branded (`com.melora.music` / `com.melora.music.dev`).
- Build the current phone dev APK from PowerShell with the installed Gradle 8.13 binary:

  ```powershell
  & 'C:\Users\ndzan\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat' :app:assembleDevDebug --no-daemon --console=plain
  ```

- ARM64 dev APK output: `app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk`.
- The project compiles against Android API 37 with AGP 8.10.1; Gradle currently prints an SDK compatibility warning (AGP was tested through API 36). Avoid toolchain upgrades unless needed for a concrete failure.

## Change log

### Project start and Melora direction

- Reviewed the BitChord project and agreed to rebrand it as **Melora**.
- Product direction set to local music first. Web/online integration is deferred; desktop portability is a future possibility.
- Planned local lyrics support, local metadata/artwork, quality/format-aware downloads, and a premium player experience.

### Rebrand and local-first Android foundation

- Added Melora app identity and dev package IDs while keeping source namespaces stable.
- Reordered onboarding so optional streaming sign-in comes last and added the local audio permission step before Home.
- Moved Google sign-in away from primary pages.
- Made Search start in Local Music; changed Home, Explore, and Library toward device collection content.
- Added a Local Explore page for songs, artists, albums, and folders; hid online library shelves from the phone Library.
- Rebranded visible strings and local download/share locations to Melora.
- Built and installed the dev APK on the attached Android device in earlier work.

### Local-first Home feed (2026-11-25)

- Replaced the single “On Device” shelf and redundant “open local music” message with Home quick actions and local sections: shuffle all, favorites, recently added, artist spotlight, continue listening, daily mix, weekly mix, and favorites shelf.
- Added `app/src/main/java/com/music/bitchord/data/LocalHomeHistory.kt`: private on-device play events, retained for 45 days with a 500-event cap and pause/resume duplicate suppression.
- Hooked local playback into that history from `ListeningRecorder`.
- Built daily and rolling-week rankings from listening events, artist-balanced; where history is sparse, mixes are filled from a stable daily/weekly rotation of local tracks.
- Reused existing Home/track-row styling and kept the app’s glass mini-player and navigation behavior.
- Built `:app:assembleDevDebug`, installed and launched the dev APK; visually reviewed Home on-device. The sample screen exposed early-mix repetition, which was corrected by blending ranked plays with artist-balanced library picks.

## Current priorities

Current task: clarify local playlist behavior and wire AutoMix into library and playback.

1. Daily and weekly mixes are deterministic random recommendations from the local library, stable for one day and one week respectively, with no listening-history weighting. Continue Listening remains the history feature.
2. Keep AutoMix stable for at least 24 hours; place it next to Liked Songs in a local playlists shelf. Saving must create a playlist visible on the local Library page.
3. Enable AutoMix artwork transitions and mini-player artwork transitions when playing the AutoMix playlist.
4. Diagnose the user's report that refreshed pages do not work, fix the refresh path(s), rebuild, install, and update this log.

### Active work update (2026-11-25)

- The handoff/change-log file was created before starting the requested UI work.
- Added a persistent Home card-size preference (`LARGE` default, `COMPACT` option) and a Content Preferences row to switch it.
- Reworked local Home recommendation sections into large artwork-led cards; Continue Listening, Picked for Today, Your Week in Music, and Artist Mix now open a new route for a full playlist-style song list. Recently Added stays as tappable individual song cards.
- Reworked Local Explore from full-width icon rows into a two-column artwork-color tile layout that shares the app’s Explore heading, gutters, and typography.
- Player artist taps already had a general artist-page callback. Added device-local routing so local song credits open their local artist group without relying on a streaming artist lookup.
- Preserved the existing navigation bar and bottom player; only page content and routes were touched.
- Built the dev APK successfully, installed/launched `com.melora.music.dev`, and reviewed Home, Explore, and the Continue Listening mix page on the attached phone. The mix page shows prominent local artwork, title/count, Play and Shuffle actions, and the full local song list. Explore displays the local collection as four consistent large tiles.
- Latest dev APK: `app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk`. Install ID remains `com.melora.music.dev`.

### Home shelf refinement and daily AutoMix (2026-11-25)

- Started from the user's on-device feedback: existing 232dp cards and one-mix-per-section presentation made Home feel oversized and repetitive.
- Target is one swipeable playlist row for Continue Listening, Picked Today, Your Week, Artist Mix, and daily AutoMix; playlist covers should be centered with names/details stacked beneath, and the large preference should use moderate sizing while compact remains available.
- Favorites should appear as a Spotify/Apple-style Liked Songs playlist card inside a purposeful playlist section. Keep other content grouped by context and do not touch bottom navigation.
- AutoMix should make a daily local playlist ordered using Melora's existing transition analysis/planning when analysis is available, and the user should be able to save the generated mix locally.
- Combined Continue Listening, Picked Today, AutoMix, Your Week, and Artist Mix into one swipeable Home shelf, avoiding repeated full-width sections.
- Reduced default mix artwork width from 232dp to 184dp; compact mix cards use the standard smaller shelf width. Recently Added stays compact at 148dp in either preference, so card size follows content context.
- Added a square playlist-cover card for Liked Songs in its own “Your playlists” section, with a centered heart treatment over artwork. Removed the duplicate Favorites quick-action chip.
- Added a daily deterministic AutoMix from local tracks. The existing transition planner selects its opening handoff candidate, while playback can refine the actual transition using its normal analysis. The AutoMix detail page has a “Save mix” action that creates a device-local playlist named with the date.
- Preserved bottom navigation and the mini-player.
- `:app:assembleDevDebug` completed successfully. Installed and launched `com.melora.music.dev` on the connected Android device. APK: `app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk`.

### Recommendation, AutoMix and local playlist corrections (2026-11-25)

- User clarified that Continue Listening is history, while Daily Mix is a random local recommendation that should remain fixed for a day and Weekly Mix is a random local recommendation fixed for a week. Neither recommendation should rank recent listening events.
- AutoMix should remain unchanged for at least 24 hours, appear beside Liked Songs as a local playlist, save into persistent offline playlists, and enable both AutoMix artwork transitions and mini-player artwork transitions during playback.
- User also reports that refreshed/reworked pages do not work; the affected refresh surface is being confirmed while refresh paths are inspected.
- Daily Mix now draws and persists a deterministic, artist-balanced random selection from the library for the current local date. Weekly Mix persists an independent random selection through the Monday-to-Sunday week. Playback events, rescans and refreshes do not reshuffle an active period; removed files are replaced from the remaining library. Play events only feed Continue Listening.
- AutoMix is derived from the stable daily list and its own day seed, so play events and Home refreshes no longer reorder it during the day. It now sits next to Liked Songs in Home's Your playlists shelf.
- Saving AutoMix creates a persistent OfflinePlaylists entry. Offline device playlists are now included in the main Library page's On device shelf, making saved AutoMix easy to find.
- Starting either the Home AutoMix or a saved AutoMix playlist enables the main AutoMix artwork transition and mini-player artwork transition preferences.
- Added pull-to-refresh to playlist/detail pages; local pages re-read their current device playlist or folder songs on refresh.
- `:app:installDevDebug` succeeded and installed the latest build on the connected Samsung SM-G996U; launched `com.melora.music.dev`. APK: `app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk`.
- The first install attempt after adding persistent recommendation snapshots hit Kotlin's method-size limit in the existing `BitChordApp` screen method. Moved the saved-playlist mapping and save/artwork settings helpers to top-level functions; the subsequent full install build succeeded.
- User clarified that the refresh issue affects playlist/detail pages. Those pages now have a visible native pull-to-refresh indicator and refresh callback.

## Working notes for the next agent

- Keep online streaming out of the local Home/Explore experience for now.
- Do not describe local mixes as trained/personalized AI; they are deterministic local heuristics driven by play events, tags, favorites, and added dates.
- A fresh install has no local play events. Recommendation screens must still be useful using library-based fallback picks.
- Keep large/compact selection persistent across app restarts and honor system font scaling and accessibility settings.
- Prefer extending existing local detail/release navigation and player artist callbacks instead of creating a parallel navigation style.

### Onboarding greeting and lazy local metadata (2026-11-25)

- Home now uses the onboarding nickname (falling back to the account display name) to show one of 16 welcome/greeting variants. The choice stays stable for the day and changes on a later day.
- Local artist group pages are now recognized as artist pages by their local page ID, so they use the existing Apple Music-style artist hero lookup. The first track cover remains a fallback when no artist image is found.
- Added a Settings switch for Apple-style full-bleed artist portraits (default) versus square artist artwork.
- Added a lazy iTunes metadata match for local track rows: exact title and artist matches can supply missing album, genre, year, and cover art. It runs only when a visible local row lacks album metadata or its embedded artwork fails. Results and misses are cached locally; fetched artwork is persisted through `MediaTagger`'s artwork cache. Original audio files and embedded tags are not changed.
- Verification: final `:app:assembleDevDebug` succeeded after the artwork-cache update. APK output: `app/build/outputs/apk/dev/debug/app-dev-universal-debug.apk`.

### Artist favorites, artist shelves, and navigation scroll (2026-11-25)

- Reconnected nested scrolling for both glass and standard navigation layouts. The standard bottom bar now slides away after scrolling down and returns when scrolling upward; the glass fold continues to use the same scroll connection.
- Added a persistent artist-favorites set independent of Liked Songs. The artist page header has a heart toggle; the local Library Artists tab sorts followed artists first and marks them with a heart.
- Artist avatars in the Library Artists list now lazily try the real Apple artist hero image, falling back to local track artwork. Existing Apple artist matching requires a title shared by the local catalog.
- Local artist detail pages now show an Albums carousel directly below Top Songs, built from the local tracks' album tags.
- Verification: `:app:assembleDevDebug` and `:app:installDevDebug` succeeded. Installed and launched `com.melora.music.dev` on the connected Samsung SM-G996U. APK: `app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk`.

### Artist favorites and Library artist list polish (2026-11-25)

- Replaced the artist-follow heart with a star on artist headers and the followed-artist marker in the local Artists list; song likes remain hearts.
- The artist page Play control now reflects the active artist track, toggles play/pause when that artist is already queued, and uses the same animated morph and haptics as the mini player. Starting from another artist track still starts the artist list in order.
- Added every local artist to the main Library directly after the On Device cards (including Liked Songs). Followed artists sort first with a star; rows lazily load the real Apple artist portrait and fall back to local artwork.
- Verification: `:app:assembleDevDebug` and `:app:installDevDebug` succeeded. Installed on the connected Samsung SM-G996U (Android 15), launched `com.melora.music.dev`, and confirmed the process remained running. APK: `app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk`.

### Artist star placement and play control correction (2026-11-25)

- Moved the local artist favorite star from beside the artist name to the right-side action button beside Play; it now toggles the independent favorite-artist state. Removed the duplicate star from the hero.
- The artist Play control uses a restrained 0.84-to-1 fade/scale icon transition instead of the shared player's deep 0.12 pulse, which looked like the glyph had collapsed. It toggles pause/resume only when the current track belongs to that artist; otherwise it starts that artist's songs.
- Set the Library artist title color explicitly to the theme's high-contrast foreground so names remain visible in dark mode. The heading uses the localized Artists string.
- Verification: `:app:assembleDevDebug` and `:app:installDevDebug` succeeded after the screenshot fixes. Relaunched `com.melora.music.dev` on the SM-G996U (Android 15); the process remained running. APK: `app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk`.

### Melora logo identity exploration (2026-11-25)

- User asked to keep the existing BitChord logo proportions: the source `Logo.png` is 1768 × 512 (3.453125:1) with a 1:1 app icon.
- Figma file: `https://www.figma.com/design/ZrYUP5nSgYBoO7sPRNWQtk/Untitled`; current identity board node `8:2` (`Melora — Brand identity direction 02`).
- The first concentric-ring vinyl studies were rejected as generic. The replacement direction uses a custom M-shaped sound groove inside an open vinyl channel, an editorial Cormorant Garamond wordmark, and an ember/velvet/bone/champagne palette.
- An earlier screenshot showed rough tagline spacing and contrast in alternate tiles; the user later manually refined the logo, approved it, and exported the final SVGs. The exported assets and current Android integration below supersede those interim observations.

### User-approved Melora identity export (2026-11-25)

- User manually refined the Figma logo and said the result is now the preferred design.
- Current Figma file: `https://www.figma.com/design/ZrYUP5nSgYBoO7sPRNWQtk/Untitled`; browser URL points to node `8:18`.
- The user supplied the approved exports locally, so Figma access is no longer needed to integrate this identity.
- The user exported the SVGs to `design/brand-exploration/melora logos`; see the completion entry below. Web logo/favicon remain outside the current scope.


### Melora logo export integrated (2026-11-25)

- Used the user's manually edited exports in `design/brand-exploration/melora logos` as the approved source artwork: Ember primary icon, Velvet and Bone alternatives, M-groove vinyl mark, Melora wordmark, and “Your library. Your listening room.” tagline.
- Updated the Android app's adaptive launcher icon, monochrome mark, legacy launcher fallbacks at each density, in-app mark resource, and notification glyph. Added Velvet and Bone to the existing Settings app-icon picker; the original default launcher alias remains the Ember choice.
- Updated the Wear OS in-app/notification branding and launcher icon to Ember.
- Replaced `app/src/main/assets/Logo.svg` and `LogoTransparent.svg`; added `MeloraLockup.svg`; rebuilt the root `Logo.png` composition with the exported wordmark and tagline, preserving the exported 1326:384 (3.453125:1) lockup ratio and square icon treatment. Updated README logo alt text.
- Left web/PWA assets unchanged as requested earlier.
- Verification: `:app:assembleDevDebug` succeeded. Build output: `app/build/outputs/apk/dev/debug/app-dev-universal-debug.apk` (also generated ABI-specific dev APKs). Gradle reported existing SDK XML v4 / API 37 compatibility warnings and source deprecation warnings; no build errors.

### Startup crash and launcher mark correction (2026-11-25)

- User reported that the Melora launcher mark was cut off and the app immediately closed on launch.
- Retrieved the connected SM-G996U crash log. The startup crash was `IllegalArgumentException: Invalid color value @android:color/transparent` thrown by Compose Multiplatform while loading `Res.drawable.ic_logo` on the onboarding screen. Replaced that unsupported resource reference with the literal transparent color `#00000000` in the shared vector; Android drawable vectors use the same literal for consistency.
- Root cause of the clipped launcher art: the app foreground used the 160×158 compact export as if it filled a 295×297 viewBox, then applied an off-center scale/translation. Rebuilt Ember, Velvet and Bone launcher foregrounds by fitting each actual SVG viewBox into the adaptive icon safe zone and centering it. Rebuilt legacy mipmap fallbacks and the Wear OS launcher mark with Ember. The in-app mark now uses the full-size 295×297 export.
- Verification: `:app:assembleDevDebug` succeeded. Installed `app-dev-arm64-v8a-debug.apk` to the connected SM-G996U and launched it. `com.melora.music.dev` remained running and reached the onboarding permission screen without a crash.

### Primary logo source clarified (2026-11-25)

- User clarified that the primary logo must come from `design/brand-exploration/melora logos/main`, not the similarly named mark under `main horizontal`. Applied the 160×158 mark from `main` to the app launcher, in-app logo, Android assets, notification/monochrome mark, Wear OS launcher, and primary horizontal lockup icon. The wordmark and tagline remain sourced from `main horizontal` because those letterform exports are located there. Velvet and Bone remain sourced from their respective variant folders.
- Centered the primary source mark according to its actual 160×158 viewBox; updated logo aspect ratios and raster launcher fallbacks.
- Rebuilt successfully with `:app:assembleDevDebug`, reinstalled the arm64 dev APK on SM-G996U, and launched it. `com.melora.music.dev` stayed running; Android reported the activity displayed, with no new startup exception.

### Launcher icon inset refined to match BitChord (2026-11-25)

- User reported that the primary mark still appeared too large against the app tile and asked for BitChord-like positioning. Reduced the centered mark from the previous 680px adaptive safe-zone target to 490px on the 1024px vector viewport, preserving the `main` SVG's original shape and proportions. Applied the same inset to Ember legacy mipmap rasters and kept Velvet/Bone scaled consistently.
- Rebuilt `:app:assembleDevDebug`, installed the arm64 APK on the connected SM-G996U, and relaunched the app.

### Icon variant proportions aligned (2026-11-25)

- User pointed out that Velvet and Bone still looked smaller/higher than Ember in the Settings icon picker. The exported alternate SVGs contain more viewBox whitespace, so applying the same nominal viewBox target left their visible marks undersized and top-shifted.
- Matched the alternate vector scale to Ember and centered each by the visible groove bounds. Confirmed the Settings icon row on the connected SM-G996U now shows the variants at matching visual scale and vertical placement.
- `:app:assembleDevDebug` succeeded and the updated arm64 dev APK was installed on the phone.

### Advanced addon downloads and album gap suggestions (2026-11-25)

- Replaced the Radio tab's main-page content with an Advanced Downloads browser while preserving the existing bottom navigation labels, order, and appearance.
- Added addon catalogue search for songs, singles, and album results, provider filtering, direct track download actions, and a Standard / High / Lossless quality picker that uses the existing download-quality preference and queue.
- Added addon album detail loading via `/album/{id}`. Album pages compare returned tracks with the on-device music library by normalized title, artist, and (when present locally) album name; they offer a missing-only view and a batch download action for absent tracks. If local audio permission is not granted, the page asks for it before claiming to know which tracks are missing.
- Reused the existing source-aware download path, so addon download policy, Wi-Fi restrictions, permissions, queue progress, file tagging, and album grouping remain in force.
- Extended addon response models and the cached client with album search and album detail support.
- Verification: `:app:assembleDevDebug` succeeded. Gradle emitted the existing API 37 / Android Gradle Plugin compatibility warnings and existing deprecations; no compile or packaging errors. Installed `app-dev-arm64-v8a-debug.apk` on the connected SM-G996U and launched `com.melora.music.dev`; its process remained running. APK: `app/build/outputs/apk/dev/debug/app-dev-universal-debug.apk` (arm64 variant: `app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk`).

### Downloads tab branding and one-tap Eclipse source (2026-11-25)

- Kept the existing Radio tab position/index and navigation layout, but changed its label to Downloads and its icon to the bold SF Symbols arrow-down-circle used by the app's iOS-style icon set.
- Added one-tap Eclipse SoundCloud setup in Advanced Downloads. It calls Eclipse's `/api/setup` endpoint to create the required per-install source URL and repairs older entries that only stored the public manifest host. Other addon URLs remain available in Sources.
- The generic Eclipse personal addon URL is not a public fixed endpoint: it is tied to an Eclipse account and uses Eclipse app access credentials. The one-tap preset is explicitly the public SoundCloud addon, not the personal account addon.
- Verification: `:app:assembleDevDebug` succeeded; installed and launched the dev build on the connected SM-G996U after the update.

### YouTube Music Eclipse addon (2026-11-25)

- Added the listener's generated Eclipse YouTube Music songs-only URL as an addon source when the Downloads page is first opened. It is added only once, is stored through the existing encrypted source registry, and appears with the other source filter chips.
- The supplied `/songs` suffix selects a distinct songs-only manifest, so it must be retained as the addon's base. Melora then appends `/search`, `/stream`, and `/album/{id}` after that suffix. Older dev installs that saved only the parent route are migrated in place while preserving source ID and selected settings.
- The generated source is account/user-specific. Keep the URL private and replace it with a fresh generated URL if it is regenerated; do not publish the current hard-coded dev preset in a public release.
- The addon's manifest was reachable and identified itself as YouTube Music. Its live search route returned a temporary `403 Unavailable` during implementation, so search/download availability still depends on the addon service being up.
- Verification: `:app:assembleDevDebug` succeeded. Installed `app-dev-arm64-v8a-debug.apk` on the connected SM-G996U and launched `com.melora.music.dev`; Android reports its launcher activity resumed and the process running. Build output: `app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk`.

### Addon HTTP 403 diagnostics (2026-11-25)

- The listener's Downloads screen showed both SoundCloud and YouTube Music failing with HTTP 403. Updated addon error reporting to include a short JSON `error` reason returned by the provider, so “Unavailable” or an access-denied response is visible instead of only the status code.
- Eclipse's public addon guide says providers may optionally require a short-lived `X-Eclipse-Access` token on search/stream requests. Melora does not currently obtain Eclipse account tokens; a browser sign-in is not shared with the phone app. The YouTube Music manifest is reachable, but its search endpoint returned HTTP 403 with `{"error":"Unavailable"}` in the live check, which points to that provider response rather than a malformed manifest URL.
- Verification: `:app:assembleDevDebug` succeeded; installed the arm64 dev APK on the connected SM-G996U and relaunched `com.melora.music.dev`. APK: `app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk`.

### Internet Archive download fallback (2026-11-25)

- Added the Eclipse community Internet Archive addon to the Downloads page automatically, once, without changing or removing the existing SoundCloud and YouTube Music sources.
- Confirmed the addon manifest and search endpoint respond. A Grateful Dead search returned recordings and formats, and the first track resolved to a direct archive.org MP3 URL.
- Added a host-specific stream compatibility rule: this addon embeds the selected file/format in each track ID and returns HTTP 400 when the app appends stream query parameters. Search settings remain sent; `/stream/{id}` is queried without parameters for this addon.
- The Archive is a fallback for recordings available there, not a complete catalog of new commercial releases. Individual Archive items may have different rights/terms; users should respect the item page's license and access notes.
- The app's previous `BitChord/…` User-Agent triggered the addon host's Cloudflare challenge (403 HTML). The same search request returned HTTP 200 when identified as `Melora/…`; updated the client User-Agent to the current app brand rather than impersonating Eclipse.
- Verification: `:app:assembleDevDebug` succeeded, and the arm64 dev APK was installed/launched on SM-G996U (`com.melora.music.dev` process and launcher activity confirmed). Direct Archive search with the exact Melora search settings returns 16 tracks, and its parameter-free stream request returns an archive.org MP3. The YouTube Music `/songs/search` route still responds `403 {"error":"Unavailable"}` with BitChord, Melora, Eclipse, and browser User-Agent strings, indicating a separate provider-side outage. APK: `app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk`.

### Retest YouTube Music and SoundCloud service availability (2026-11-25)

- Updated the addon HTTP User-Agent from the old `BitChord/{version}` brand to `Melora/{version}`. Direct Archive search was blocked by its Cloudflare rule with the old name, but succeeds with `Melora/…`; this is a truthful app identity, not an Eclipse impersonation.
- Rechecked the generated YouTube Music `/songs/search` route using Melora, Eclipse, and browser User-Agent values. Every request returned `403 {"error":"Unavailable"}`.
- Generated a temporary SoundCloud test URL through its official setup endpoint and checked `/search` with the same User-Agent variants. Every request returned `403 {"error":"Unavailable"}`. The registry's other public SoundCloud URL (`eclipse3.cyrusna29.workers.dev`) returns Cloudflare 1042 because its Worker is not deployed.
- These responses originate from the provider services, so Melora cannot restore these endpoints. The currently working fallback is Internet Archive; owner action or an authorized replacement source is required for YouTube Music/SoundCloud.
- Verification: built `:app:assembleDevDebug`, installed the arm64 dev APK on SM-G996U, and confirmed `com.melora.music.dev` is running in the foreground. Direct Archive search with Melora User-Agent returns results and its stream endpoint resolves an archive.org MP3.
- Follow-up: verified the provided `/songs/manifest.json` returns the songs-only manifest (`searchMode=songs`) while the parent manifest defaults to both songs and videos. Corrected the stored base URL to include `/songs` and added an in-place migration from the earlier parent route. Direct search checks against both `/search` and `/songs/search` still returned `403 {"error":"Unavailable"}`. Rebuilt and installed the updated arm64 dev APK on the connected SM-G996U; the launcher activity is resumed and process is running.

### Myspoti / FLAC Bridge integration review (2026-11-25)

- Reviewed `fksptfy/Myspoti` and its separate `solidsnakeater/flac-bridge-myspoti` extension. Myspoti is a GPL-3.0 BitChord-derived app; the bridge extension is also GPL-3.0 and specifically requires Myspoti's extension API (`apiVersion: 1`, min app 2.1.0).
- The bridge is not a Melora Eclipse addon and cannot be installed directly. Its JavaScript expects Myspoti hooks, storage/settings, custom HTTP/auth and online-work APIs. Melora's current addon contract is HTTP manifest/search/stream; its QuickJS module API is a different contract.
- The extension contacts `api.zarz.moe` and Tidal/Qobuz/Deezer/Amazon-related hosts, uses an in-app web verification flow, and stores verification sessions locally. Its README says song metadata/ISRC, track ID and install ID may be sent to those services. It accepts only direct, unencrypted FLAC streams and explicitly says Deezer/Amazon coverage is partial; playback and downloads are not guaranteed.
- Porting requires an explicit Melora source integration with equivalent user-visible host/auth permissions, private session storage and source controls; a direct script copy would fail and bypass the app's current addon consent model. GPL-3.0 is compatible with Melora's GPL-3.0 project license, with attribution and third-party notices retained.
- No bridge code has been copied into Melora yet. The private GitHub backup is being initialized separately before integration work.
