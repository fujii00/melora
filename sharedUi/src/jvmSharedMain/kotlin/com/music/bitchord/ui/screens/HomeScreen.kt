package com.music.bitchord.ui.screens

import com.music.bitchord.ui.components.trackArtSize
import com.music.bitchord.ui.components.contextClick
import com.music.bitchord.ui.AppUi
import com.music.bitchord.sharedui.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.music.bitchord.ui.components.longPressMenuClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import com.music.bitchord.ui.components.ShelfRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.GraphicEq
import com.music.bitchord.ui.icons.BitChordIcons
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickMood
import com.music.bitchord.data.model.UiState
import java.util.Locale
import java.time.LocalTime
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.RowMoreButton
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.components.SHELF_CARD_WIDTH
import com.music.bitchord.ui.components.VideoThumbnails
import com.music.bitchord.ui.components.SearchPlayingBars
import com.music.bitchord.ui.components.SignInBanner
import com.music.bitchord.ui.components.feedMoreSkeleton
import com.music.bitchord.ui.components.feedSkeleton
import com.music.bitchord.ui.components.recentlyPlayedSkeleton
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.components.trackColumnWidth
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.MeshPalette

/** How far the Listen now heading and feed sit above the shared top inset. */
private val HOME_TITLE_LIFT = 28.dp

private const val RECENTS_TITLE = "Recents"
private const val RECENT_TRACKS_PER_COLUMN = 4

private fun List<ShelfItem>.playingItemMatcher(song: Song?): (ShelfItem) -> Boolean {
    song ?: return { false }
    // The card that was played, when the shelf holds it, and only that one —
    // see [com.music.bitchord.data.model.playingRowMatcher], which rows use.
    if (any { it.videoId == song.videoId }) return { it.videoId == song.videoId }
    val byName = firstOrNull { it.videoId != null && it.title == song.title && it.subtitle == song.artist }
        ?: return { false }
    return { it == byName }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    /** The tapped item and the recommendation shelf it came from. */
    onItemClick: (ShelfItem, String) -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    /** The page's heading, or null for none — the desktop's pages carry no heading. */
    title: String?,
    signedIn: Boolean = true,
    onSignIn: (() -> Unit)? = null,
    /** Songs scanned from this device, shown before any optional online shelves. */
    localSongs: List<Song> = emptyList(),
    localRecentlyPlayed: List<Song> = emptyList(),
    localFavorites: List<Song> = emptyList(),
    localDailyMix: List<Song> = emptyList(),
    localWeeklyMix: List<Song> = emptyList(),
    localAutoMix: List<Song> = emptyList(),
    localArtistSpotlight: List<Song> = emptyList(),
    localArtistSpotlightName: String = "",
    localAlbumSpotlight: List<Song> = emptyList(),
    localAlbumSpotlightName: String = "",
    localArtistPicks: List<List<Song>> = emptyList(),
    localArtistPickArtwork: Map<String, String> = emptyMap(),
    localAlbumPicks: List<List<Song>> = emptyList(),
    localSongPicks: List<Song> = emptyList(),
    onOpenLocalArtist: (String) -> Unit = {},
    onOpenLocalAlbum: (Song) -> Unit = {},
    onPlayLocalCollection: (List<Song>, String, Boolean) -> Unit = { _, _, _ -> },
    onOpenLocalMix: (String, String, List<Song>) -> Unit = { _, _, _ -> },
    localMixCardsLarge: Boolean = true,
    onOpenLocalFavorites: () -> Unit = {},
    onLocalSongClick: (Song) -> Unit = {},
    onOpenLocalMusic: () -> Unit = {},
    /** Makes the phone's Home a view of its own collection, with no online feed. */
    localFirst: Boolean = false,
    onRefreshLocalMusic: () -> Unit = {},
    /**
     * Holding a card rather than tapping it. Every card on the feed answers,
     * whichever kind it is: the caller reads the item the same way it does for
     * a tap, so a track card opens the track menu and a card that points at a
     * collection opens the album / playlist one.
     */
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    // Explore doesn't page — only Home has a continuation worth following.
    onLoadMore: (() -> Unit)? = null,
    loadingMore: Boolean = false,
    recentlyPlayedLoading: Boolean = false,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    /**
     * The "Top Picks for You" row above Recents. Null while it has not been
     * built yet, empty when there is nothing to pick from. The row is only
     * drawn by a caller that can act on it, i.e. passes [onTopPickClick].
     */
    topPicks: List<TopPick>? = null,
    topPicksLoading: Boolean = false,
    /** The signed-in account's name, for the personal station's "Name's Station". */
    listenerName: String? = null,
    listenerAvatarUri: String? = null,
    listenerAvatarShape: String? = null,
    onTopPickClick: ((TopPick, String) -> Unit)? = null,
    onTopPickLongPress: ((TopPick) -> Unit)? = null,
    /** A Find Your Mood tile, and its name. The shelf is only drawn when this is set. */
    onMoodClick: ((TopPickMood, String) -> Unit)? = null,
    /** Find Your Mood's Show all: every mood and activity. */
    onMoodsShowAll: () -> Unit = {},
    /** Whether the Top Picks and mood artwork may animate — see [LocalArtworkMotion]. */
    artworkMotion: Boolean = true,
    /** The Replay year story's button beside the wordmark, or null for none. See [ReplayYearButton]. */
    replayYear: ReplayYearBadge? = null,
) {
    // Top Picks' pictures and motion wait for the feed — see LocalArtworkImages.
    val feedReady = localFirst || state !is UiState.Loading
    val shuffleAllLabel = stringResource(Res.string.home_shuffle_all)
    val recentlyAddedLabel = stringResource(Res.string.shelf_recents)
    val songsOfTheDayLabel = stringResource(Res.string.home_songs_of_the_day)
    val weekInMusicLabel = stringResource(Res.string.your_replay)
    CompositionLocalProvider(
        LocalArtworkMotion provides (artworkMotion && feedReady),
        LocalArtworkImages provides feedReady,
    ) {
    // The loops also hold still while the feed is being scrolled — see
    // LocalArtworkScrolling for why that is asked rather than provided.
    PausedWhileScrolling(listState) {

    PullToRefresh(
        refreshing = if (localFirst) false else refreshing,
        onRefresh = if (localFirst) onRefreshLocalMusic else onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // The heading sits higher than the other tabs' first row: pull the
            // whole feed up by [HOME_TITLE_LIFT] with it.
            contentPadding = if (title != null) {
                PaddingValues(
                    top = (contentPadding.calculateTopPadding() - HOME_TITLE_LIFT).coerceAtLeast(0.dp),
                    bottom = contentPadding.calculateBottomPadding(),
                )
            } else {
                contentPadding
            },
        ) {
            if (title != null) {
                item { HomeTitle(title, replayYear, listenerName = listenerName, listenerAvatarUri = listenerAvatarUri, listenerAvatarShape = listenerAvatarShape) }
            }
            if (localSongs.isNotEmpty()) {
                item(key = "local-quick-actions") {
                    LocalHomeQuickActions(
                        spotlight = localArtistSpotlight,
                        spotlightName = localArtistSpotlightName,
                        onShuffleAll = { onPlayLocalCollection(localSongs, shuffleAllLabel, true) },
                        onRecentlyAdded = { onPlayLocalCollection(localSongs.sortedByDescending { it.localDateAddedSeconds ?: 0L }.take(30), recentlyAddedLabel, false) },
                        onSpotlight = { onPlayLocalCollection(localArtistSpotlight, localArtistSpotlightName, true) },
                    )
                }
                if (localFavorites.isNotEmpty() || localAutoMix.isNotEmpty()) {
                    item(key = "local-favorites-playlist") {
                        LocalFavoritesShelf(
                            songs = localFavorites,
                            autoMix = localAutoMix,
                            onClick = onOpenLocalFavorites,
                            onAutoMixClick = { onOpenLocalMix("automix", "AutoMix", localAutoMix) },
                        )
                    }
                }
                if (localRecentlyPlayed.isNotEmpty() || localDailyMix.isNotEmpty() || localAutoMix.isNotEmpty() || localWeeklyMix.isNotEmpty() || localArtistSpotlight.isNotEmpty()) {
                    // One streaming-style shelf keeps the daily playlists together and
                    // avoids spending a full screen section on each individual mix.
                    item(key = "local-made-for-you") {
                        val recentsLabel = stringResource(Res.string.shelf_recents)
                        val dailyTitle = stringResource(Res.string.home_songs_of_the_day)
                        val localReady = stringResource(Res.string.local_music_home_ready)
                        val weekTitle = stringResource(Res.string.your_replay)
                        val artistsLabel = stringResource(Res.string.artists)
                        LocalMixShelf(
                            sectionTitle = stringResource(Res.string.top_pick_made_for_you),
                            sectionSubtitle = stringResource(Res.string.local_music_home_ready),
                            mixes = buildList {
                                if (localRecentlyPlayed.isNotEmpty()) add(LocalMixCardData(
                                    recentsLabel, recentsLabel, localRecentlyPlayed,
                                ) { onOpenLocalMix("continue", recentsLabel, localRecentlyPlayed) })
                                if (localDailyMix.isNotEmpty()) add(LocalMixCardData(
                                    songsOfTheDayLabel, localReady, localDailyMix,
                                ) { onOpenLocalMix("daily", songsOfTheDayLabel, localDailyMix) })
                                if (localWeeklyMix.isNotEmpty()) add(LocalMixCardData(
                                    weekInMusicLabel, localReady, localWeeklyMix,
                                ) { onOpenLocalMix("weekly", weekInMusicLabel, localWeeklyMix) })
                                if (localArtistSpotlight.isNotEmpty()) add(LocalMixCardData(
                                    artistsLabel, localArtistSpotlightName, localArtistSpotlight,
                                ) { onOpenLocalMix("artist-${localArtistSpotlightName.hashCode()}", "$artistsLabel · $localArtistSpotlightName", localArtistSpotlight) })
                            },
                            largeCards = localMixCardsLarge,
                        )
                    }
                }
                if (localArtistPicks.isNotEmpty()) {
                    item(key = "made-for-you-artists") {
                        LocalArtistDayShelf(
                            artists = localArtistPicks,
                            artworkByArtist = localArtistPickArtwork,
                            onArtistClick = onOpenLocalArtist,
                        )
                    }
                }
                if (localAlbumPicks.isNotEmpty()) {
                    item(key = "made-for-you-albums") {
                        val unknownAlbumLabel = stringResource(Res.string.albums)
                        LocalMixShelf(
                            sectionTitle = stringResource(Res.string.home_albums_of_the_day),
                            sectionSubtitle = stringResource(Res.string.on_device),
                            mixes = localAlbumPicks.map { pick ->
                                val artist = pick.firstOrNull()?.artist.orEmpty()
                                val name = pick.firstOrNull()?.albumName?.trim().orEmpty()
                                    .takeIf { it.isNotBlank() && !it.equals(artist.trim(), ignoreCase = true) }
                                    ?: unknownAlbumLabel
                                LocalMixCardData(name, artist, pick) {
                                    pick.firstOrNull()?.copy(albumName = name)?.let(onOpenLocalAlbum)
                                }
                            },
                            largeCards = localMixCardsLarge,
                        )
                    }
                }
                if (localSongPicks.isNotEmpty()) {
                    item(key = "songs-of-the-day") {
                        LocalSongCardShelf(
                            title = stringResource(Res.string.home_songs_of_the_day),
                            songs = localSongPicks,
                            onSongClick = onLocalSongClick,
                        )
                    }
                }
                item(key = "local-recent-additions") {
                    LocalSongCardShelf(
                        title = stringResource(Res.string.shelf_recents),
                        songs = localSongs.sortedByDescending { it.localDateAddedSeconds ?: 0L }.take(12),
                        onSongClick = onLocalSongClick,
                    )
                }
            } else {
                item(key = "local-music-empty") {
                    MessageState(
                        message = stringResource(Res.string.local_music_home_empty),
                        actionLabel = stringResource(Res.string.open_local_music),
                        onAction = onOpenLocalMusic,
                    )
                }
            }
            if (!localFirst) {
                if (!signedIn && onSignIn != null) {
                    item {
                        // Read in this item, so a scroll starting or ending
                        // recomposes the banner alone.
                        SignInBanner(
                            onSignIn = onSignIn,
                            modifier = Modifier.padding(bottom = 8.dp),
                            motion = LocalArtworkMotion.current && !listState.isScrollInProgress,
                        )
                    }
                }
                if (onTopPickClick != null) {
                    when {
                        !topPicks.isNullOrEmpty() -> item(key = "top-picks") {
                            TopPicksShelf(
                                picks = topPicks,
                                listenerName = listenerName,
                                onClick = onTopPickClick,
                                onLongPress = onTopPickLongPress,
                            )
                        }
                        topPicks == null && topPicksLoading -> topPicksSkeleton()
                    }
                }
                when (state) {
                    is UiState.Loading -> {
                        if (recentlyPlayedLoading) {
                            recentlyPlayedSkeleton()
                        }
                        feedSkeleton()
                    }
                    is UiState.Error -> item {
                        MessageState(state.message, actionLabel = stringResource(Res.string.retry), onAction = onRetry)
                    }
                    is UiState.Success -> {
                        if (recentlyPlayedLoading) {
                            recentlyPlayedSkeleton()
                        }
                        itemsIndexedShelves(
                            shelves = state.data,
                            onMoodClick = onMoodClick,
                            onMoodsShowAll = onMoodsShowAll,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                        )
                        if (loadingMore) feedMoreSkeleton()
                    }
                }
            }
        }
    }

    }
    }

    if (!localFirst && onLoadMore != null && state is UiState.Success) {
        val loadMore by rememberUpdatedState(onLoadMore)
        // Re-checked on every layout change, rather than on the rising edge of
        // "the tail is in view". A page that appends only a shelf or two leaves
        // the list still near its end, so an edge-triggered effect would never
        // fire a second time: the feed dead-ended at the bottom with no
        // skeleton and no request in flight to explain it. Restarting on
        // [loadingMore] re-checks the moment a page settles, so the next one is
        // asked for while the tail is still on screen to show it loading.
        LaunchedEffect(listState, loadingMore) {
            snapshotFlow {
                val layout = listState.layoutInfo
                (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) to layout.totalItemsCount
            }.collect { (lastVisible, total) ->
                if (!loadingMore && total > 0 && lastVisible >= total - 3) loadMore()
            }
        }
    }
}

/** Recents mirrors the artist page's top-tracks pager; other shelves use square cards. */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedShelves(
    shelves: List<HomeShelf>,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    currentSong: Song?,
    isPlaying: Boolean,
    onMoodClick: ((TopPickMood, String) -> Unit)? = null,
    onMoodsShowAll: () -> Unit = {},
) {
    shelves.forEachIndexed { index, shelf ->
        // Find Your Mood follows the lead shelf, as on Apple Music's Home.
        if (index == 1 && onMoodClick != null) {
            item(key = "find-your-mood") { FindYourMoodShelf(onMoodClick, onMoodsShowAll) }
        }
        item(key = shelf.title + index) {
            val openItem: (ShelfItem) -> Unit = { item -> onItemClick(item, shelf.title) }
            // Listen again and Quick picks are lists on the web player, as
            // Recents is here: one pager for all three.
            if (shelf.layout == ShelfLayout.LIST ||
                (index == 0 && shelf.title.equals(RECENTS_TITLE, ignoreCase = true))
            ) {
                RecentShelf(
                    shelf = shelf,
                    onItemClick = openItem,
                    onItemLongPress = onItemLongPress,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                )
            } else {
                Shelf(
                    shelf = shelf,
                    onItemClick = openItem,
                    onItemLongPress = onItemLongPress,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                )
            }
        }
    }
}

/** The same four-rows-per-page treatment used by an artist's Top songs. */
@Composable
private fun RecentShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlayAll: (() -> Unit)? = null,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        RecentSectionHeader(
            title = localizeShelfTitle(shelf.title),
            subtitle = localizeShelfSubtitle(shelf.subtitle),
            onPlayAll = onPlayAll,
        )
        val isCurrentItem = shelf.items.playingItemMatcher(currentSong)
        BoxWithConstraints {
            val columnWidth = trackColumnWidth(maxWidth)
            ShelfRow(
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shelf.items.chunked(RECENT_TRACKS_PER_COLUMN)) { column ->
                    Column(Modifier.width(columnWidth)) {
                        column.forEach { item ->
                            RecentTrackRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onLongPress = onItemLongPress?.let { { it(item) } },
                                isCurrent = isCurrentItem(item),
                                isPlaying = isCurrentItem(item) && isPlaying,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSectionHeader(title: String, subtitle: String, onPlayAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onPlayAll != null) {
            Text(
                stringResource(Res.string.play),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onPlayAll).padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun LocalHomeQuickActions(
    spotlight: List<Song>,
    spotlightName: String,
    onShuffleAll: () -> Unit,
    onRecentlyAdded: () -> Unit,
    onSpotlight: () -> Unit,
) {
    Column(Modifier.padding(bottom = 12.dp)) {
        SectionHeader(stringResource(Res.string.local_music_home_ready), stringResource(Res.string.on_device))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AssistChip(
                onClick = onShuffleAll,
                label = { Text(stringResource(Res.string.home_shuffle_all)) },
                leadingIcon = { Icon(Icons.Rounded.Shuffle, contentDescription = null) },
            )
            AssistChip(
                onClick = onRecentlyAdded,
                label = { Text(stringResource(Res.string.shelf_recents)) },
                leadingIcon = { Icon(Icons.Rounded.History, contentDescription = null) },
            )
            if (spotlight.isNotEmpty()) {
                AssistChip(
                    onClick = onSpotlight,
                    label = { Text("${stringResource(Res.string.artists)} · $spotlightName", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun LocalMixShelf(
    sectionTitle: String,
    sectionSubtitle: String = "",
    mixes: List<LocalMixCardData>,
    largeCards: Boolean,
) {
    if (mixes.isEmpty()) return
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(sectionTitle, sectionSubtitle)
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(mixes, key = { mix ->
                "${mix.title}-${mix.subtitle}-${mix.songs.firstOrNull()?.localUri ?: mix.songs.firstOrNull()?.videoId.orEmpty()}"
            }) { mix ->
                LocalPlaylistCard(
                    title = mix.title,
                    subtitle = mix.subtitle,
                    artwork = mix.songs.firstNotNullOfOrNull(Song::thumbnailUrl),
                    width = if (largeCards) LOCAL_HOME_CARD_LARGE else SHELF_CARD_WIDTH,
                    onClick = mix.onClick,
                )
            }
        }
    }
}

@Composable
private fun LocalArtistDayShelf(
    artists: List<List<Song>>,
    artworkByArtist: Map<String, String>,
    onArtistClick: (String) -> Unit,
) {
    if (artists.isEmpty()) return
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(stringResource(Res.string.home_artists_of_the_day), stringResource(Res.string.local_music_home_ready))
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(artists, key = { pick -> "artist-${pick.firstOrNull()?.artist.orEmpty()}" }) { pick ->
                val firstSong = pick.firstOrNull()
                val name = firstSong?.artist.orEmpty()
                Column(
                    modifier = Modifier.width(108.dp).clickable {
                        if (name.isNotBlank()) onArtistClick(name)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Prefer a real artist profile photo. If no provider
                        // match exists, use that artist's own most recent
                        // track sleeve rather than leaving the card blank.
                        val artwork = artworkByArtist[name]
                            ?: pick.firstNotNullOfOrNull { it.thumbnailUrl }
                        if (artwork != null) {
                            AsyncImage(
                                model = artwork.artworkAt(CARD_ART_PX),
                                contentDescription = name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private data class LocalMixCardData(
    val title: String,
    val subtitle: String,
    val songs: List<Song>,
    val onClick: () -> Unit,
)

@Composable
private fun LocalFavoritesShelf(
    songs: List<Song>,
    autoMix: List<Song>,
    onClick: () -> Unit,
    onAutoMixClick: () -> Unit,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(stringResource(Res.string.playlists))
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "liked-songs") {
                LocalPlaylistCard(
                    title = stringResource(Res.string.home_liked_songs),
                    subtitle = stringResource(Res.string.card_songs_format, songs.size.toString()),
                    artwork = songs.firstNotNullOfOrNull(Song::thumbnailUrl),
                    width = LOCAL_HOME_CARD_COMPACT,
                    onClick = onClick,
                    badgeIcon = Icons.Rounded.Favorite,
                )
            }
            if (autoMix.isNotEmpty()) {
                item(key = "automix-daily") {
                    LocalPlaylistCard(
                        title = "AutoMix",
                        subtitle = stringResource(Res.string.local_music_home_ready),
                        artwork = autoMix.firstNotNullOfOrNull(Song::thumbnailUrl),
                        width = LOCAL_HOME_CARD_COMPACT,
                        onClick = onAutoMixClick,
                        badgeIcon = Icons.Rounded.GraphicEq,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalPlaylistCard(
    title: String,
    subtitle: String,
    artwork: String?,
    width: Dp,
    onClick: () -> Unit,
    badgeIcon: ImageVector? = null,
) {
    Column(
        modifier = Modifier.width(width).clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (artwork != null) {
                AsyncImage(
                    model = artwork.artworkAt(CARD_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (badgeIcon != null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0x55000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(badgeIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LocalSongCardShelf(
    title: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
) {
    if (songs.isEmpty()) return
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(title)
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(songs, key = { it.localUri ?: it.videoId }) { song ->
                ShelfCard(
                    item = ShelfItem(song.title, song.artist, song.thumbnailUrl, song.videoId, null),
                    onClick = { onSongClick(song) },
                    modifier = Modifier.width(LOCAL_HOME_CARD_COMPACT),
                )
            }
        }
    }
}

private val LOCAL_HOME_CARD_LARGE = 184.dp
private val LOCAL_HOME_CARD_COMPACT = 148.dp

@Composable
private fun RecentTrackRow(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    isCurrent: Boolean,
    isPlaying: Boolean,
) {
    CompactTrackRow(
        title = item.title,
        subtitle = item.subtitle,
        thumbnailUrl = item.thumbnailUrl,
        onClick = onClick,
        onLongPress = onLongPress,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        isVideo = item.isVideo,
    )
}

/**
 * One row of Recents' four-to-a-column pager. Public so the desktop artist page
 * can list its top songs the same way.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactTrackRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    /** A music video: its thumbnail at 16:9 rather than squared off. */
    isVideo: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.trackArtSize(48.dp, isVideo)) {
            AsyncImage(
                model = thumbnailUrl.artworkAt(ROW_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(7.dp))
                    .thumbnailBorder(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onLongPress != null) {
            RowMoreButton(onClick = onLongPress)
        }
    }
}

/**
 * Listen now's large heading, with the wordmark stacked above it.
 *
 * Only the home tab carries it — the top bar no longer shows the logo, so this
 * is where the app's mark lives. Its height is taken from the heading's own
 * font size rather than a fixed dp, so the two keep their proportion under the
 * system font scale.
 */
@Composable
private fun HomeTitle(
    title: String,
    replayYear: ReplayYearBadge? = null,
    listenerName: String? = null,
    listenerAvatarUri: String? = null,
    listenerAvatarShape: String? = null,
    modifier: Modifier = Modifier,
) {
    val style = MaterialTheme.typography.displayLarge
    val logoHeight = with(LocalDensity.current) { (style.fontSize * LOGO_TO_FONT).toDp() }
    Column(modifier = modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 24.dp, bottom = 8.dp)) {
        // The wordmark, and the year story's button right after it when there
        // is a year to tell. Sized off the wordmark so the two keep their
        // proportion under the system font scale.
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_logo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .height(logoHeight)
                    .aspectRatio(LOGO_ASPECT),
            )
            if (replayYear != null) {
                Spacer(Modifier.width(14.dp))
                ReplayYearButton(replayYear, height = logoHeight * 0.68f)
            }
            if (listenerAvatarUri != null) {
                Spacer(Modifier.weight(1f))
                AsyncImage(
                    model = listenerAvatarUri,
                    contentDescription = stringResource(Res.string.home_profile_picture),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(if (listenerAvatarShape == "square") RoundedCornerShape(20.dp) else CircleShape),
                )
            }
        }
        // "Listen now" is hidden for now; the wordmark above stays.
        // Text(
        //     text = title,
        //     style = style,
        //     color = MaterialTheme.colorScheme.onBackground,
        // )
        val name = listenerName?.trim()?.takeIf { it.isNotEmpty() }
        if (name != null) {
            val greeting = when (LocalTime.now().hour) {
                in 5..11 -> Res.string.home_greeting_morning
                in 12..16 -> Res.string.home_greeting_afternoon
                in 17..21 -> Res.string.home_greeting_evening
                else -> Res.string.home_greeting_hello
            }
            Text(stringResource(greeting, name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

/** The wordmark's height against the heading's font size. */
private const val LOGO_TO_FONT = 0.9f

/** ic_logo's viewport, 160 x 158. */
private const val LOGO_ASPECT = 160f / 158f

/**
 * Shared by the home feed, Explore and Library so headings line up across tabs.
 *
 * [onShowAll] is only ever set on Library, whose rows stop at five cards
 * rather than running the shelf's whole length — see [LibraryGridShelf].
 * Home and Explore never pass it, so their heading is unchanged.
 */
@Composable
internal fun SectionHeader(title: String, subtitle: String = "", onShowAll: (() -> Unit)? = null) {
    val displayTitle = localizeShelfTitle(title)
    val displaySubtitle = localizeShelfSubtitle(subtitle)
    Row(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (displaySubtitle.isNotBlank()) {
                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onShowAll != null) {
            Text(
                text = stringResource(Res.string.show_all),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onShowAll)
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}
@Composable
internal fun localizeShelfTitle(title: String): String {
    val trimmed = title.trim()
    return when {
        trimmed.equals("Recents", ignoreCase = true) ||
            trimmed.equals("Recently played", ignoreCase = true) ||
            trimmed.equals("Gần đây", ignoreCase = true) ||
            trimmed.equals("最近", ignoreCase = true) ->
            stringResource(Res.string.shelf_recents)
        trimmed.equals("Playlists", ignoreCase = true) ||
            trimmed.equals("Danh sách phát", ignoreCase = true) ||
            trimmed.equals("再生リスト", ignoreCase = true) ->
            stringResource(Res.string.playlists)
        trimmed.equals("Albums", ignoreCase = true) ||
            trimmed.equals("Album", ignoreCase = true) ||
            trimmed.equals("アルバム", ignoreCase = true) ->
            stringResource(Res.string.albums)
        trimmed.equals("Artists", ignoreCase = true) ||
            trimmed.equals("Nghệ sĩ", ignoreCase = true) ||
            trimmed.equals("アーティスト", ignoreCase = true) ->
            stringResource(Res.string.artists)
        trimmed.equals("Subscriptions", ignoreCase = true) ||
            trimmed.equals("Đã đăng ký", ignoreCase = true) ||
            trimmed.equals("登録チャンネル", ignoreCase = true) ->
            stringResource(Res.string.subscriptions)
        trimmed.equals("Trending community playlists", ignoreCase = true) ||
            trimmed.equals("Danh sách phát cộng đồng thịnh hành", ignoreCase = true) ||
            trimmed.equals("Danh sách phát thịnh hành trong cộng đồng người dùng", ignoreCase = true) ||
            trimmed.equals("急上昇のコミュニティ再生リスト", ignoreCase = true) ->
            stringResource(Res.string.shelf_trending_community_playlists)
        trimmed.equals("Featured playlists for you", ignoreCase = true) ||
            trimmed.equals("Danh sách phát đề xuất cho bạn", ignoreCase = true) ||
            trimmed.equals("Danh sách phát nổi bật dành cho bạn", ignoreCase = true) ||
            trimmed.equals("おすすめの再生リスト", ignoreCase = true) ->
            stringResource(Res.string.shelf_featured_playlists_for_you)
        trimmed.equals("Quick picks", ignoreCase = true) ||
            trimmed.equals("Lựa chọn nhanh", ignoreCase = true) ||
            trimmed.equals("Chọn nhanh đài phát", ignoreCase = true) ||
            trimmed.equals("クイック ミックス", ignoreCase = true) ->
            stringResource(Res.string.shelf_quick_picks)
        trimmed.equals("Listen again", ignoreCase = true) ||
            trimmed.equals("Nghe lại", ignoreCase = true) ||
            trimmed.equals("もう一度聴く", ignoreCase = true) ->
            stringResource(Res.string.shelf_listen_again)
        trimmed.equals("Mixed for you", ignoreCase = true) ||
            trimmed.equals("Dành riêng cho bạn", ignoreCase = true) ||
            trimmed.equals("ミックス", ignoreCase = true) ->
            stringResource(Res.string.shelf_mixed_for_you)
        trimmed.startsWith("Similar to", ignoreCase = true) -> {
            val rest = trimmed.substring(10).trim()
            stringResource(Res.string.shelf_similar_to, rest)
        }
        trimmed.startsWith("Tương tự như", ignoreCase = true) -> {
            val rest = trimmed.substring(12).trim()
            stringResource(Res.string.shelf_similar_to, rest)
        }
        trimmed.equals("Forgotten favorites", ignoreCase = true) ||
            trimmed.equals("Giai điệu quen thuộc", ignoreCase = true) ||
            trimmed.equals("よく聴いたお気に入りの曲", ignoreCase = true) ->
            stringResource(Res.string.shelf_forgotten_favorites)
        trimmed.equals("Recommended music videos", ignoreCase = true) ||
            trimmed.equals("Video âm nhạc đề xuất", ignoreCase = true) ||
            trimmed.equals("おすすめのミュージック ビデオ", ignoreCase = true) ->
            stringResource(Res.string.shelf_recommended_music_videos)
        trimmed.equals("From your library", ignoreCase = true) ||
            trimmed.equals("Từ thư viện của bạn", ignoreCase = true) ||
            trimmed.equals("ライブラリから", ignoreCase = true) ->
            stringResource(Res.string.shelf_from_your_library)
        trimmed.equals("Charts", ignoreCase = true) ||
            trimmed.equals("Bảng xếp hạng", ignoreCase = true) ||
            trimmed.equals("チャート", ignoreCase = true) ->
            stringResource(Res.string.shelf_charts)
        trimmed.equals("New releases", ignoreCase = true) ||
            trimmed.equals("Bản phát hành mới", ignoreCase = true) ||
            trimmed.equals("最新リリース", ignoreCase = true) ->
            stringResource(Res.string.shelf_new_releases)
        trimmed.equals("Top music videos", ignoreCase = true) ||
            trimmed.equals("Video âm nhạc hàng đầu", ignoreCase = true) ||
            trimmed.equals("人気のミュージック ビデオ", ignoreCase = true) ->
            stringResource(Res.string.shelf_top_music_videos)
        trimmed.equals("For you", ignoreCase = true) ||
            trimmed.equals("Dành cho bạn", ignoreCase = true) ||
            trimmed.equals("あなたへのおすすめ", ignoreCase = true) ->
            stringResource(Res.string.shelf_for_you)
        trimmed.equals("Hits today", ignoreCase = true) ||
            trimmed.equals("Today's Hits", ignoreCase = true) ||
            trimmed.equals("Bản hit hôm nay", ignoreCase = true) ||
            trimmed.equals("今日のヒット曲", ignoreCase = true) ->
            stringResource(Res.string.shelf_hits_today)
        trimmed.equals("Artists on the rise", ignoreCase = true) ||
            trimmed.equals("Nghệ sĩ đang lên", ignoreCase = true) ->
            stringResource(Res.string.shelf_artists_on_the_rise)
        trimmed.equals("Concerts", ignoreCase = true) ||
            trimmed.equals("Buổi hòa nhạc", ignoreCase = true) ||
            trimmed.equals("コンサート", ignoreCase = true) ->
            stringResource(Res.string.shelf_concerts)
        trimmed.startsWith("Shorts", ignoreCase = true) ||
            trimmed.equals("Shorts nổi bật", ignoreCase = true) ||
            trimmed.equals("ショート", ignoreCase = true) ->
            stringResource(Res.string.shelf_shorts)
        trimmed.equals("Trending", ignoreCase = true) ||
            trimmed.equals("Thịnh hành", ignoreCase = true) ||
            trimmed.equals("急上昇", ignoreCase = true) ->
            stringResource(Res.string.shelf_trending)
        else -> title
    }
}
@Composable
internal fun localizeShelfSubtitle(subtitle: String): String {
    val trimmed = subtitle.trim()
    return when {
        trimmed.equals("TOP TUNES RIGHT NOW", ignoreCase = true) ||
            trimmed.equals("Top tunes right now", ignoreCase = true) ||
            trimmed.equals("Giai điệu hàng đầu hiện nay", ignoreCase = true) ||
            trimmed.equals("注目の曲", ignoreCase = true) ->
            stringResource(Res.string.shelf_top_tunes_right_now)
        trimmed.equals("From the community", ignoreCase = true) ||
            trimmed.equals("Từ cộng đồng", ignoreCase = true) ||
            trimmed.equals("コミュニティより", ignoreCase = true) ->
            stringResource(Res.string.shelf_from_the_community)
        trimmed.equals("YouTube Charts", ignoreCase = true) ||
            trimmed.equals("Bảng xếp hạng YouTube", ignoreCase = true) ||
            trimmed.equals("YouTube チャート", ignoreCase = true) ->
            stringResource(Res.string.shelf_youtube_charts)
        else -> subtitle
    }
}

@Composable
internal fun localizeCardSubtitle(subtitle: String): String {
    if (subtitle.isBlank()) return subtitle
    val delimiter = " • "
    val parts = subtitle.split(delimiter)
    val songLabel = stringResource(Res.string.shelf_song_item)
    val singleLabel = stringResource(Res.string.shelf_single)
    val chartLabel = stringResource(Res.string.shelf_chart)
    val playlistLabel = stringResource(Res.string.playlist)
    val localizedParts = parts.map { part ->
        val trimmed = part.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        when {
            trimmed.equals("Song", ignoreCase = true) || trimmed.equals("Titre", ignoreCase = true) ||
                trimmed.equals("Bài hát", ignoreCase = true) || trimmed.equals("曲", ignoreCase = true) -> songLabel
            trimmed.equals("Single", ignoreCase = true) || trimmed.equals("Đĩa đơn", ignoreCase = true) ||
                trimmed.equals("シングル", ignoreCase = true) -> singleLabel
            trimmed.equals("Chart", ignoreCase = true) || trimmed.equals("Bảng xếp hạng", ignoreCase = true) ||
                trimmed.equals("チャート", ignoreCase = true) -> chartLabel
            trimmed.equals("Playlist", ignoreCase = true) || trimmed.equals("Danh sách phát", ignoreCase = true) ||
                trimmed.equals("再生リスト", ignoreCase = true) -> playlistLabel
            lower.endsWith(" views") || lower.endsWith(" view") || lower.endsWith(" lượt xem") ||
                lower.endsWith(" 回視聴") || lower.endsWith("回視聴") -> {
                val count = trimmed.substringBeforeLast(' ', "").trim()
                if (count.any { it.isDigit() }) stringResource(Res.string.card_views_format, count) else part
            }
            lower.endsWith(" plays") || lower.endsWith(" play") || lower.endsWith(" lượt phát") ||
                lower.endsWith(" 回再生") || lower.endsWith("回再生") -> {
                val count = trimmed.substringBeforeLast(' ', "").trim()
                if (count.any { it.isDigit() }) stringResource(Res.string.card_plays_format, count) else part
            }
            lower.endsWith(" songs") || lower.endsWith(" song") || lower.endsWith(" bài hát") ||
                lower.endsWith(" 曲") || lower.endsWith("曲") -> {
                val count = trimmed.substringBeforeLast(' ', "").trim()
                if (count.any { it.isDigit() }) stringResource(Res.string.card_songs_format, count) else part
            }
            else -> part
        }
    }
    return localizedParts.joinToString(delimiter)
}

/**
 * [leadingCard] rides at the head of the row, ahead of the content — the
 * Library tab's "New playlist" tile, which belongs among the playlists rather
 * than in a bar somewhere above them. [onItemLongPress] opens the album /
 * playlist menu, and is null only where a card points at something with no
 * track list behind it to act on.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Shelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    leadingCard: (@Composable () -> Unit)? = null,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(shelf.title, shelf.subtitle)
        val isCurrentItem = shelf.items.playingItemMatcher(currentSong)
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leadingCard?.let { card -> item(key = "leading") { card() } }
            items(shelf.items) { item ->
                if (shelf.layout == ShelfLayout.VIDEO) {
                    VideoShelfCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onLongPress = onItemLongPress?.let { { it(item) } },
                        isCurrent = isCurrentItem(item),
                        isPlaying = isCurrentItem(item) && isPlaying,
                    )
                } else {
                    ShelfCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onLongPress = onItemLongPress?.let { { it(item) } },
                        isCurrent = isCurrentItem(item),
                        isPlaying = isCurrentItem(item) && isPlaying,
                    )
                }
            }
        }
    }
}

/**
 * A music video, as the web player lays one out: the thumbnail at its own
 * 16:9 rather than cropped into a square, the title and credit under it. About
 * as tall as the square cards, so a video shelf sits in the feed's rhythm.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoShelfCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier.width(VIDEO_CARD_WIDTH),
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Column(
        modifier = modifier
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(VIDEO_ASPECT)
                .clip(RoundedCornerShape(12.dp))
                .thumbnailBorder(RoundedCornerShape(12.dp))
                .background(placeholderColor),
        ) {
            AsyncImage(
                // Video stills are not resizable googleusercontent art; the
                // largest the row listed is already about card size.
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = localizeCardSubtitle(item.subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = subtitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A music video's frame, or a square one when the listener asked for that — see [VideoThumbnails]. */
val VIDEO_ASPECT: Float get() = if (VideoThumbnails.square) 1f else 16f / 9f

/** Wide enough that a 16:9 card stands about as tall as a square [SHELF_CARD_WIDTH] one. */
val VIDEO_CARD_WIDTH: Dp get() = if (VideoThumbnails.square) SHELF_CARD_WIDTH else 248.dp

/**
 * A card that isn't a thing yet — the dashed "New playlist" tile at the head
 * of the Library's playlist row, sized to sit in line with the covers beside
 * it rather than as a button bolted above them.
 */
@Composable
internal fun NewShelfCard(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
    /** A full-colour mark drawn in place of [icon], for a card that stands for a brand. */
    logo: DrawableResource? = null,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (logo != null) {
                // The mark is a disc with the bars cut out, so tinting it
                // leaves the bars clear: white on dark, black on light.
                Icon(
                    painter = painterResource(logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(44.dp),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A device-folder card: mesh gradient in the folder's colours with its glyph
 * on top. One composable for local music, WebDAV and SMB rather than three
 * copies of the same box.
 */
@Composable
private fun ServiceCard(colors: List<Color>, trackKey: String, icon: ImageVector) {
    val palette = remember { MeshPalette(colors) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        MeshGradientBackground(
            palette = palette,
            trackKey = trackKey,
            continuous = true,
            blurRadius = 24.dp,
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShelfCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
    /** Set on a Library playlist card that's in [AppSettings.pinnedPlaylists][com.music.bitchord.data.settings.AppSettings.pinnedPlaylists]. */
    isPinned: Boolean = false,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
) {
    Column(
        modifier = modifier
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress),
    ) {
        when (item.browseId) {
            "local:downloads" -> {
                val palette = remember { MeshPalette(listOf(Color(0xFF1E3C72), Color(0xFF2A5298))) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    MeshGradientBackground(
                        palette = palette,
                        trackKey = "local:downloads",
                        continuous = true,
                        blurRadius = 24.dp,
                    )
                    Icon(
                        imageVector = BitChordIcons.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            "local:all" -> ServiceCard(
                colors = listOf(Color(0xFF134E5E), Color(0xFF71B280)),
                trackKey = "local:all",
                icon = Icons.Rounded.LibraryMusic,
            )
            "local:cache" -> ServiceCard(
                colors = listOf(Color(0xFF42275A), Color(0xFF734B6D)),
                trackKey = "local:cache",
                icon = Icons.Rounded.Cached,
            )
            "local:webdav" -> ServiceCard(
                colors = listOf(Color(0xFF3A1C71), Color(0xFFD76D77)),
                trackKey = "local:webdav",
                icon = Icons.Rounded.Folder,
            )
            "local:smb" -> ServiceCard(
                colors = listOf(Color(0xFF0F2027), Color(0xFF2C5364)),
                trackKey = "local:smb",
                icon = Icons.Rounded.Storage,
            )
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .thumbnailBorder(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPinned) {
                Icon(
                    imageVector = BitChordIcons.Pin,
                    contentDescription = stringResource(Res.string.pinned),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        val displaySubtitle = localizeCardSubtitle(item.subtitle)
        Text(
            text = displaySubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
