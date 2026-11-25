package com.music.bitchord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.SongRow
import com.music.bitchord.ui.theme.rememberArtworkPalette

@Composable
fun LocalMixPage(
    title: String,
    subtitle: String,
    songs: List<Song>,
    contentPadding: PaddingValues,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlay: (List<Song>, Int, Boolean) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSaveMix: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val artwork = songs.firstNotNullOfOrNull(Song::thumbnailUrl)
    val palette = rememberArtworkPalette(artwork)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = PAGE_GUTTER,
            top = contentPadding.calculateTopPadding() + 12.dp,
            end = PAGE_GUTTER,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item(key = "mix-hero") {
            Column(horizontalAlignment = Alignment.Start) {
                AsyncImage(
                    model = artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(232.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "YOUR LOCAL MIX",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accent,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$subtitle · ${songs.size} songs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { if (songs.isNotEmpty()) onPlay(songs, 0, false) }) {
                        Text("Play")
                    }
                    OutlinedButton(onClick = { if (songs.isNotEmpty()) onPlay(songs, 0, true) }) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Shuffle")
                    }
                    if (onSaveMix != null) {
                        OutlinedButton(onClick = onSaveMix) { Text("Save mix") }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Songs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
            }
        }
        itemsIndexed(songs, key = { index, song -> "${song.localUri ?: song.videoId}-$index" }) { index, song ->
            SongRow(
                song = song,
                isCurrent = currentSong?.localUri == song.localUri && song.localUri != null ||
                    (currentSong?.localUri == null && currentSong?.videoId == song.videoId),
                isPlaying = isPlaying,
                activeTint = palette.accent,
                searchPlayingStyle = true,
                downloadedTint = null,
                rowBackground = MaterialTheme.colorScheme.background,
                onClick = { onPlay(songs, index, false) },
                onLongPress = { onSongLongPress(song) },
            )
        }
    }
}
