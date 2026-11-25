package com.music.bitchord.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.music.bitchord.data.LocalGroup
import com.music.bitchord.data.LocalGroups
import com.music.bitchord.data.canvas.AppleArtistArtRepository
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song

@Composable
fun rememberLocalLibraryArtists(songs: List<Song>, favorites: Set<String>): List<LibraryArtistItem> =
    remember(songs, favorites) {
        LocalGroups.artistsOf("local:all", songs)
            .sortedWith(
                compareByDescending<LocalGroup> { it.title.trim().lowercase() in favorites }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
            )
            .map { group ->
                LibraryArtistItem(
                    item = ShelfItem(
                        title = group.title,
                        subtitle = "${group.songs.size} songs",
                        thumbnailUrl = group.thumbnailUrl,
                        videoId = null,
                        browseId = group.browseId,
                    ),
                    favorite = group.title.trim().lowercase() in favorites,
                )
            }
    }

suspend fun resolveLocalLibraryArtistArtwork(item: ShelfItem, songs: List<Song>): String? {
    val group = item.browseId?.let { LocalGroups.groupOf(it, songs) } ?: return null
    return AppleArtistArtRepository.artFor(group.browseId, group.title) {
        group.songs.map { it.title }
    }?.heroUrl
}
