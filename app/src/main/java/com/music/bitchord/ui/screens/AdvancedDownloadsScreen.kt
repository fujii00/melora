package com.music.bitchord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.DownloadQuality
import com.music.bitchord.data.sources.SourceConfig
import com.music.bitchord.data.sources.SourceKind
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.data.sources.addon.AddonAlbum
import com.music.bitchord.data.sources.addon.AddonClient
import com.music.bitchord.data.sources.addon.EclipseCommunityAddon
import com.music.bitchord.data.sources.addon.AddonTrack
import com.music.bitchord.data.sources.addon.YouTubeMusicCommunityAddon
import com.music.bitchord.data.sources.addon.InternetArchiveAddon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class AddonTrackHit(val source: SourceConfig, val track: AddonTrack)
private data class AddonAlbumHit(val source: SourceConfig, val album: AddonAlbum)

/** Search enabled download addons and download tracks at the app's chosen quality. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedDownloadsScreen(
    localSongs: List<Song>,
    hasLocalAccess: Boolean,
    onRequestLocalAccess: () -> Unit,
    onDownload: (List<Song>, String, String, String?) -> Unit,
    onManageSources: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val configured by SourceRegistry.configs.collectAsState()
    val quality by AppSettings.downloadQuality.collectAsState()
    val addons = remember(configured) {
        configured.filter { it.kind == SourceKind.ADDON && it.enabled && it.isComplete && it.allowDownloads }
    }
    val eclipseSoundCloud = configured.firstOrNull { EclipseCommunityAddon.isSoundCloudUrl(it.baseUrl) }
    val hasEclipseSoundCloud = eclipseSoundCloud != null
    val clients = remember(addons) { addons.associate { it.id to AddonClient(it.baseUrl, it.pickedQuality) } }
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedSource by rememberSaveable { mutableStateOf("all") }
    var qualityMenu by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tracks by remember { mutableStateOf<List<AddonTrackHit>>(emptyList()) }
    var albums by remember { mutableStateOf<List<AddonAlbumHit>>(emptyList()) }
    var openedAlbum by remember { mutableStateOf<AddonAlbumHit?>(null) }
    var albumTracks by remember { mutableStateOf<List<AddonTrackHit>>(emptyList()) }
    var albumLoading by remember { mutableStateOf(false) }
    var showMissingOnly by remember { mutableStateOf(false) }
    var settingUpEclipseSource by remember { mutableStateOf(false) }
    var eclipseSetupError by remember { mutableStateOf<String?>(null) }

    // The listener supplied a generated Eclipse URL. Store its addon root once;
    // the client appends `/search`, `/stream`, etc. as required by the manifest.
    LaunchedEffect(Unit) {
        val base = AddonClient.normalizeBase(YouTubeMusicCommunityAddon.BASE_URL)
        val parentBase = base.removeSuffix("/songs")
        val existing = SourceRegistry.configs.value.firstOrNull {
            it.kind == SourceKind.ADDON &&
                AddonClient.normalizeBase(it.baseUrl) in setOf(base, parentBase)
        }
        if (existing == null) {
            SourceRegistry.add(
                SourceConfig(
                    kind = SourceKind.ADDON,
                    label = YouTubeMusicCommunityAddon.LABEL,
                    baseUrl = YouTubeMusicCommunityAddon.BASE_URL,
                ),
            )
        } else if (AddonClient.normalizeBase(existing.baseUrl) != base) {
            // Earlier dev builds kept the parent route. Preserve its source ID
            // and user-selected settings while switching to the generated
            // songs-only variant the listener actually provided.
            SourceRegistry.update(
                existing.copy(
                    label = YouTubeMusicCommunityAddon.LABEL,
                    baseUrl = YouTubeMusicCommunityAddon.BASE_URL,
                ),
            )
        }

        val archiveBase = AddonClient.normalizeBase(InternetArchiveAddon.BASE_URL)
        val archiveAlreadyAdded = SourceRegistry.configs.value.any {
            it.kind == SourceKind.ADDON && AddonClient.normalizeBase(it.baseUrl) == archiveBase
        }
        if (!archiveAlreadyAdded) {
            SourceRegistry.add(
                SourceConfig(
                    kind = SourceKind.ADDON,
                    label = InternetArchiveAddon.LABEL,
                    baseUrl = InternetArchiveAddon.BASE_URL,
                ),
            )
        }
    }

    fun setUpEclipseSoundCloud() {
        if (settingUpEclipseSource) return
        settingUpEclipseSource = true
        eclipseSetupError = null
        scope.launch {
            val result = EclipseCommunityAddon.createSoundCloudUrl()
            result.fold(
                onSuccess = { generatedUrl ->
                    val current = SourceRegistry.configs.value.firstOrNull {
                        EclipseCommunityAddon.isSoundCloudUrl(it.baseUrl)
                    }
                    if (current != null) {
                        SourceRegistry.update(current.copy(baseUrl = generatedUrl))
                    } else {
                        SourceRegistry.add(
                            SourceConfig(
                                kind = SourceKind.ADDON,
                                label = ECLIPSE_SOUNDCLOUD_LABEL,
                                baseUrl = generatedUrl,
                            ),
                        )
                    }
                },
                onFailure = { failure ->
                    eclipseSetupError = failure.message ?: "Could not connect to Eclipse. Try again."
                },
            )
            settingUpEclipseSource = false
        }
    }

    // Older Melora builds added the public host URL directly. Eclipse serves
    // its manifest there, but requires a generated per-install path for
    // search/stream. Repair that entry in place instead of making the listener
    // remove it and start over.
    LaunchedEffect(eclipseSoundCloud?.id, eclipseSoundCloud?.baseUrl) {
        val existing = eclipseSoundCloud ?: return@LaunchedEffect
        if (!EclipseCommunityAddon.needsSetup(existing.baseUrl)) return@LaunchedEffect
        settingUpEclipseSource = true
        eclipseSetupError = null
        val result = EclipseCommunityAddon.createSoundCloudUrl()
        result.fold(
            onSuccess = { generatedUrl ->
                SourceRegistry.config(existing.id)?.let { current ->
                    if (EclipseCommunityAddon.needsSetup(current.baseUrl)) {
                        SourceRegistry.update(current.copy(baseUrl = generatedUrl))
                    }
                }
            },
            onFailure = { failure ->
                eclipseSetupError = failure.message ?: "Could not connect to Eclipse. Try again."
            },
        )
        settingUpEclipseSource = false
    }

    fun tierFor(value: DownloadQuality): String = when (value) {
        DownloadQuality.STANDARD -> AddonClient.TIER_LOW
        DownloadQuality.HIGH -> AddonClient.TIER_HIGH
        DownloadQuality.LOSSLESS -> AddonClient.TIER_LOSSLESS
    }

    fun toSong(hit: AddonTrackHit, album: AddonAlbum? = null): Song = Song(
        videoId = SourceRegistry.trackKey(hit.source.id, hit.track.id),
        title = hit.track.title,
        artist = hit.track.artist.ifBlank { album?.artist.orEmpty() },
        thumbnailUrl = hit.track.artwork ?: album?.artwork,
        durationText = hit.track.durationSec?.let { "${it / 60}:${"%02d".format(it % 60)}" },
        albumName = hit.track.album.ifBlank { album?.title },
    )

    fun search() {
        val submitted = query.trim()
        if (submitted.isEmpty()) return
        loading = true
        error = null
        openedAlbum = null
        scope.launch {
            val sources = addons.filter { selectedSource == "all" || it.id == selectedSource }
            val sourceFailures = mutableListOf<String>()
            val answers = withContext(Dispatchers.IO) {
                sources.mapNotNull { source ->
                    val client = clients[source.id] ?: return@mapNotNull null
                    val result = client.searchCatalog(submitted, tierFor(quality))
                    val response = result.getOrElse { failure ->
                        sourceFailures += "${source.displayName}: ${failure.message ?: "search failed"}"
                        return@mapNotNull null
                    }
                    Triple(source, response.tracks, response.albums)
                }
            }
            tracks = answers.flatMap { (source, rows, _) ->
                rows.filter { it.id.isNotBlank() && it.title.isNotBlank() }.map { AddonTrackHit(source, it) }
            }
            albums = answers.flatMap { (source, _, rows) ->
                rows.filter { it.id.isNotBlank() && it.title.isNotBlank() }.map { AddonAlbumHit(source, it) }
            }
            if (answers.isEmpty()) error = sourceFailures.joinToString(" · ")
                .ifBlank { "No enabled addon returned results. Check its connection in Sources." }
            else if (tracks.isEmpty() && albums.isEmpty()) error = "No songs or albums found for “$submitted”."
            loading = false
        }
    }

    fun openAlbum(hit: AddonAlbumHit) {
        openedAlbum = hit
        showMissingOnly = false
        albumTracks = hit.album.tracks.map { AddonTrackHit(hit.source, it) }
        if (albumTracks.isNotEmpty()) return
        albumLoading = true
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                clients[hit.source.id]?.album(hit.album.id, tierFor(quality))?.getOrNull()
            }
            albumTracks = loaded?.tracks.orEmpty().map { AddonTrackHit(hit.source, it) }
            if (loaded != null) openedAlbum = hit.copy(album = loaded)
            albumLoading = false
        }
    }

    val pageAlbum = openedAlbum
    val displayedTracks = if (pageAlbum == null) tracks else albumTracks
    val missingTracks = remember(displayedTracks, localSongs, pageAlbum, hasLocalAccess) {
        if (pageAlbum == null || !hasLocalAccess) emptyList() else displayedTracks.filter { hit ->
            localSongs.none { local -> sameRecording(local, hit.track, pageAlbum.album) }
        }
    }
    val visibleTracks = if (pageAlbum != null && showMissingOnly) missingTracks else displayedTracks

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (pageAlbum != null) IconButton(onClick = { openedAlbum = null }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to results")
                }
                Column(Modifier.weight(1f)) {
                    Text(if (pageAlbum == null) "Music downloads" else pageAlbum.album.title,
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (pageAlbum == null) "Find tracks and albums from your installed sources"
                        else pageAlbum.album.artist.ifBlank { pageAlbum.source.displayName },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (pageAlbum == null) IconButton(onClick = onManageSources) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Manage sources")
                }
            }
        }

        if (pageAlbum == null) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search songs, singles, albums") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = { TextButton(onClick = ::search, enabled = !loading) { Text("Search") } },
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Download quality", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Box {
                        OutlinedButton(onClick = { qualityMenu = true }) { Text(quality.label) }
                        DropdownMenu(expanded = qualityMenu, onDismissRequest = { qualityMenu = false }) {
                            DownloadQuality.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Column {
                                        Text(option.label, fontWeight = FontWeight.Medium)
                                        Text(option.detail, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } },
                                    onClick = {
                                        AppSettings.setDownloadQuality(option)
                                        qualityMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
            if (!hasEclipseSoundCloud ||
                (EclipseCommunityAddon.needsSetup(eclipseSoundCloud?.baseUrl.orEmpty()) &&
                    !settingUpEclipseSource && eclipseSetupError != null)
            ) item {
                OutlinedButton(
                    onClick = ::setUpEclipseSoundCloud,
                    enabled = !settingUpEclipseSource,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(when {
                        settingUpEclipseSource -> "Setting up Eclipse SoundCloud…"
                        hasEclipseSoundCloud -> "Retry Eclipse SoundCloud setup"
                        else -> "Add Eclipse SoundCloud source"
                    })
                }
            }
            if (!eclipseSetupError.isNullOrBlank()) item {
                Text(eclipseSetupError.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            if (addons.isNotEmpty()) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceChip("All sources", selectedSource == "all") { selectedSource = "all" }
                    addons.forEach { source ->
                        SourceChip(source.displayName, selectedSource == source.id) { selectedSource = source.id }
                    }
                }
            }
            if (addons.isEmpty()) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Add a music source", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Install an addon to search its catalogue and download tracks. Add the public Eclipse SoundCloud source above, or paste any compatible addon URL in Sources.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onManageSources) { Text("More sources") }
                    }
                }
            }
            if (albums.isNotEmpty()) item { SectionTitle("Albums") }
            items(albums, key = { "album:${it.source.id}:${it.album.id}" }) { hit ->
                AlbumResultCard(hit.album, hit.source.displayName, onClick = { openAlbum(hit) })
            }
        } else {
            item { AlbumHeaderCard(pageAlbum.album, pageAlbum.source.displayName) }
            if (!albumLoading && albumTracks.isNotEmpty()) item {
                val count = missingTracks.size
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(when {
                            !hasLocalAccess -> "Check your local collection"
                            count == 0 -> "Your collection has every track"
                            else -> "$count tracks missing from your library"
                        },
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(if (hasLocalAccess) {
                            "Compare this album with music already on this device, then add only what is missing."
                        } else {
                            "Allow Melora to scan local audio to find tracks you already have before downloading."
                        },
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!hasLocalAccess) Button(onClick = onRequestLocalAccess) {
                                Text("Allow local music access")
                            } else OutlinedButton(onClick = { showMissingOnly = !showMissingOnly }) {
                                Text(if (showMissingOnly) "Show full album" else "Show missing only")
                            }
                            if (hasLocalAccess && count > 0) Button(onClick = {
                                onDownload(missingTracks.map { toSong(it, pageAlbum.album) },
                                    "addon:${pageAlbum.source.id}:${pageAlbum.album.id}",
                                    pageAlbum.album.title, pageAlbum.album.artwork)
                            }) {
                                Icon(Icons.Rounded.Download, contentDescription = null)
                                Text("  Download missing")
                            }
                        }
                    }
                }
            }
        }

        if (loading || albumLoading) item {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        if (!error.isNullOrBlank() && pageAlbum == null) item {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
        }
        if (displayedTracks.isNotEmpty()) item { SectionTitle(if (pageAlbum == null) "Tracks and singles" else "Album tracks") }
        items(visibleTracks, key = { "track:${it.source.id}:${it.track.id}" }) { hit ->
            TrackResultRow(hit.track, pageAlbum?.album?.artwork, hit.source.displayName) {
                val song = toSong(hit, pageAlbum?.album)
                onDownload(listOf(song), "", song.title, song.thumbnailUrl)
            }
        }
        if (pageAlbum != null && !albumLoading && albumTracks.isEmpty()) item {
            Text("This source did not return the album track list.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SourceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
}

@Composable
private fun AlbumResultCard(album: AddonAlbum, sourceName: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(album.artwork, 72)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(album.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(album.artist.ifBlank { "Album" }, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sourceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.Album, contentDescription = "Open album", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AlbumHeaderCard(album: AddonAlbum, sourceName: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(album.artwork, 104)
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(album.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(album.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(sourceName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TrackResultRow(track: AddonTrack, albumArtwork: String?, sourceName: String, onDownload: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(track.artwork ?: albumArtwork, 52)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(track.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist.ifBlank { sourceName }, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val quality = listOf(track.audioQuality, track.format).filter(String::isNotBlank).distinct().joinToString(" · ")
            if (quality.isNotBlank()) Text(quality, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDownload) { Icon(Icons.Rounded.Download, contentDescription = "Download ${track.title}") }
    }
}

@Composable
private fun Cover(url: String?, size: Int) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)) {
        if (!url.isNullOrBlank()) AsyncImage(url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Rounded.Album, contentDescription = null, modifier = Modifier.align(Alignment.Center),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun sameRecording(local: Song, remote: AddonTrack, album: AddonAlbum): Boolean {
    fun key(value: String) = value.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
    val sameTitle = key(local.title) == key(remote.title)
    val remoteArtist = remote.artist.ifBlank { album.artist }
    val sameArtist = key(local.artist).isNotBlank() && key(remoteArtist).isNotBlank() &&
        (key(local.artist).contains(key(remoteArtist)) || key(remoteArtist).contains(key(local.artist)))
    val remoteAlbum = remote.album.ifBlank { album.title }
    val sameAlbum = local.albumName.isNullOrBlank() || key(local.albumName.orEmpty()) == key(remoteAlbum)
    return sameTitle && sameArtist && sameAlbum
}

private const val ECLIPSE_SOUNDCLOUD_LABEL = "Eclipse SoundCloud"
