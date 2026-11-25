# Melora product roadmap

Melora is a design-led music player for everyone. The local collection is the default experience; online catalogs are optional additions. Keep the polished player and the strongest existing playback features while making it effortless to discover, organize, and enjoy music already available to the listener.

## Product principles

- A listener can play and organize local music without creating an account.
- Ask for file access when it is useful, explain the request, and offer a folder picker.
- Treat the user's audio files as read-only. Store fetched lyrics, artwork, and analysis in Melora's cache or in sidecar files unless the user explicitly chooses to edit tags.
- Prefer embedded metadata and lyrics; make online enrichment visible and optional.
- Make source, quality, and format clear whenever music is fetched or saved.
- Keep the visual experience inviting to casual listeners, with advanced audio controls available when wanted.

## Delivery sequence

### 1. Rebrand and local-first entry

- Use Melora as the product name and “Your music, beautifully played” as the working tagline.
- Center onboarding on the user's collection; keep account connections clearly optional.
- Make local library, recently played, favorites, and downloads easy to reach from the main navigation.
- Provide first-run guidance for permission, folder selection, scan progress, and an empty collection.
- Keep existing package identifiers during the initial rebrand so installed builds can be evaluated for upgrade compatibility before any identifier migration.

### 2. Library quality

- Review scanning and refresh for device storage, selected folders, USB, SMB, and WebDAV.
- Improve metadata editing, duplicate handling, compilations, multi-disc albums, missing artwork, and large-library search and filtering.
- Make lost permissions, removed drives, moved files, and broken server connections recoverable.
- Keep playlists, favorites, play counts, and queue behavior independent of streaming account state.
- Offer a clear format support page based on actual device and decoder capabilities.

### 3. Lyrics for local tracks

1. Read embedded lyrics and matching `.lrc` sidecars first.
2. If none exist, search enabled lyric providers using normalized title, artist, album, duration, and recording identifiers when available.
3. Show match confidence and provider attribution; let the listener choose a result when matches are ambiguous.
4. Cache the selected lyrics offline and keep them linked to the local track identity.
5. Offer an explicit “Save lyrics beside music” action for a sidecar file. Never silently rewrite an audio file's tags.
6. Support plain text, line-timed LRC, and word-timed formats where a provider supplies them; retain current synced lyric presentation.

### 4. Quality-aware downloads

- Add a download chooser showing available quality, codec/container, file size estimate, and source before saving.
- Allow a preferred format and quality ceiling, with an automatic mode that selects the best available option.
- Keep format conversion opt-in and explain when conversion changes the source audio quality.
- Preserve tags and artwork where the source permits; make downloaded files easy to find, rescan, move, and remove.
- Respect each configured source's capabilities and permissions. Do not imply that every track or quality is downloadable.

### 5. Listening experience and discovery

- Add local-first Home shelves: Continue Listening, Recently Added, Favorites, and albums or playlists from the collection.
- Search local tracks first, then let the listener expand to selected online sources.
- Extend recommendations and Automix to use local library metadata and analysis, with understandable controls.
- Keep lyrics, artwork, replay history, scrobbling, crossfade, equalizer, and high resolution output coherent across local and remote playback.

### 6. Platform strategy

- Android phones and tablets are the first reference experience.
- Android TV builds on existing USB library support and focuses on remote-friendly collection browsing.
- Wear OS remains a companion for phone playback unless a separate on-watch library is explicitly scoped.
- Defer the web integration while local Android work lands.
- Explore a desktop port after the shared UI and playback boundaries are mapped. Reuse Kotlin Multiplatform shared UI and core where they fit; treat filesystem permissions, audio output, library scanning, and platform integrations as desktop-specific work. Decide a target desktop platform before committing to delivery scope.

## First release checks

- Fresh install reaches useful local playback without signing into an online service.
- A listener can grant access or choose a folder, see scan progress, and recover from an empty or unavailable library.
- Local playlists and favorites still work offline and after online services are disconnected.
- Embedded lyrics win over network lookup; fetched lyrics remain available offline without modifying audio files.
- Download choices accurately reflect the configured source's available quality and formats.
- Existing installations retain their data and can upgrade safely through the rebrand.
