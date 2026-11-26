package com.music.bitchord.ui.components

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import com.music.bitchord.R
import com.music.bitchord.data.SongInfo
import com.music.bitchord.data.SongInfoRepository
import com.music.bitchord.data.compactCount
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown

// Body height adapts to the 5 standard rows (measured), so it fits any phone/font like before.

/**
 * Drawn as the same UIAlertController-style card as [AlertScaffold] — centred
 * title, hairline [AlertRule]s, full-width [AlertAction]s — rather than a
 * Material [AlertDialog], so it reads as one of the app's own alerts.
 *
 * Kept in a window [Dialog] because it opens from the song actions menu, which
 * can sit inside the player's own window; an in-tree overlay would be drawn
 * underneath it. A
 * separate window can't sample the app's haze, so the card takes the plain
 * surface the other alerts fall back to under "reduce dynamic blur".
 */
@Composable
internal fun SongInfoDialog(song: Song, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var retry by remember(song.videoId) { mutableIntStateOf(0) }
    val info by produceState<SongInfo?>(null, song, retry) {
        value = null
        value = SongInfoRepository.load(song)
    }
    val localTechnicalInfo by produceState<LocalTrackTechnicalInfo?>(null, song.localUri, retry) {
        value = song.localUri?.let { readLocalTrackTechnicalInfo(context, it) }
    }
    val creditsScroll = rememberScrollState()
    val onSurface = MaterialTheme.colorScheme.onSurface

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .width(ALERT_WIDTH)
                .clip(RoundedCornerShape(ALERT_CORNER))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 19.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.song_info),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.W600),
                    color = onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = song.title,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                    color = onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (song.artist.isNotBlank()) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                        color = onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            AlertRule()
            val surface = MaterialTheme.colorScheme.surface
            SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
                val rowsHeight = subcompose("rows") { SongInfoStandardRows(info, song, localTechnicalInfo) }
                .first().measure(constraints).height
                val body = subcompose("content") {
                    Box(Modifier.fillMaxWidth().height(rowsHeight.toDp())) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(creditsScroll)
                                .drawWithContent {
                                    drawContent()
                                    if (creditsScroll.canScrollForward) {
                                        drawRect(
                                            Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                1f to surface,
                                                startY = size.height - 28.dp.toPx(),
                                                endY = size.height,
                                            )
                                        )
                                    }
                                },
                        ) {
                            SongInfoStandardRows(info, song, localTechnicalInfo)
                            info?.credits?.takeIf { it.isNotEmpty() }?.forEach { credit ->
                                AlertRule(Modifier.padding(start = 16.dp))
                                SongInfoCreditRow(role = credit.role, names = credit.text)
                            }
                            if (info?.failed == true) {
                                Text(
                                    text = stringResource(R.string.song_info_error),
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 15.sp),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        if (creditsScroll.canScrollForward) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(16.dp),
                            )
                        }
                    }
                }.first().measure(Constraints.fixed(constraints.maxWidth, rowsHeight))
                layout(constraints.maxWidth, rowsHeight) { body.place(0, 0) }
            }

            AlertRule()
            if (info?.failed == true) {
                AlertAction(label = stringResource(R.string.song_info_retry), emphasised = false, onClick = { retry++ })
                AlertRule()
            }
            AlertAction(label = stringResource(R.string.done), emphasised = true, onClick = onDismiss)
        }
    }
}

@Composable
private fun SongInfoStandardRows(info: SongInfo?, song: Song, local: LocalTrackTechnicalInfo?) {
    val unavailable = stringResource(R.string.song_info_unavailable)
    val rows = listOf(
        R.string.song_info_album to (info?.album ?: song.albumName),
        R.string.song_info_release_date to (info?.releaseDate ?: info?.releaseYear),
        R.string.song_info_duration to song.durationText,
        R.string.song_info_views to info?.views?.let(::compactCount),
        R.string.song_info_likes to info?.likes,
        R.string.song_info_comments to info?.comments,
    ).filter { song.localUri == null || it.first in setOf(R.string.song_info_album, R.string.song_info_release_date, R.string.song_info_duration) } +
        if (song.localUri != null) listOf(
            R.string.song_info_format to local?.format,
            R.string.song_info_file_size to local?.fileSize,
            R.string.song_info_bitrate to local?.bitrate,
            R.string.song_info_sample_rate to local?.sampleRate,
            R.string.song_info_channels to local?.channels,
            R.string.song_info_track_number to song.trackNumber?.toString(),
            R.string.song_info_genre to local?.genre,
        ) else emptyList()
    Column {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) AlertRule(Modifier.padding(start = 16.dp))
            SongInfoRow(
                label = stringResource(label),
                value = value ?: unavailable.takeIf { info != null },
            )
        }
    }
}

private data class LocalTrackTechnicalInfo(
    val format: String? = null,
    val fileSize: String? = null,
    val bitrate: String? = null,
    val sampleRate: String? = null,
    val channels: String? = null,
    val genre: String? = null,
)

private suspend fun readLocalTrackTechnicalInfo(context: Context, rawUri: String): LocalTrackTechnicalInfo =
    withContext(Dispatchers.IO) {
        val uri = Uri.parse(rawUri)
        var displayName: String? = null
        var size: Long? = null
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameColumn >= 0) displayName = cursor.getString(nameColumn)
                    if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn)
                }
            }
        }
        var bitrate: Int? = null
        var sampleRate: Int? = null
        var channels: Int? = null
        var genre: String? = null
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
                genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.takeIf(String::isNotBlank)
            }
        }
        runCatching {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
                for (index in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(index)
                    if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        if (format.containsKey(MediaFormat.KEY_BIT_RATE)) bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE)
                        break
                    }
                }
            } finally {
                extractor.release()
            }
        }
        val name = displayName ?: uri.lastPathSegment.orEmpty()
        val format = name.substringAfterLast('.', "").takeIf(String::isNotBlank)?.uppercase(Locale.ROOT)
        LocalTrackTechnicalInfo(
            format = format,
            fileSize = size?.let { bytes ->
                when {
                    bytes >= 1_073_741_824 -> String.format(Locale.getDefault(), "%.1f GB", bytes / 1_073_741_824.0)
                    bytes >= 1_048_576 -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576.0)
                    bytes >= 1_024 -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1_024.0)
                    else -> "$bytes B"
                }
            },
            bitrate = bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps" },
            sampleRate = sampleRate?.takeIf { it > 0 }?.let { rate -> if (rate % 1000 == 0) "${rate / 1000} kHz" else "%.1f kHz".format(Locale.getDefault(), rate / 1000f) },
            channels = channels?.takeIf { it > 0 }?.toString(),
            genre = genre,
        )
    }

/** Label on the left, value on the right; a small spinner stands in for the value while it loads. */
@Composable
private fun SongInfoRow(label: String, value: String?) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ACTION_HEIGHT)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (value == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = onSurface.copy(alpha = 0.5f),
                    strokeWidth = 1.75.dp,
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = onSurface,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun SongInfoCreditRow(role: String, names: String) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = role,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
            color = onSurface.copy(alpha = 0.6f),
        )
        Text(
            text = names,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = onSurface,
        )
    }
}
