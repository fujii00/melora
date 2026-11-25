package com.music.bitchord.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.bitchord.R

@Composable
fun LocalExploreScreen(
    hasPermission: Boolean,
    songCount: Int,
    onRequestPermission: () -> Unit,
    onBrowse: (LocalFolderTab) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val sections = listOf(
        ExploreSection(R.string.local_explore_songs, R.string.local_explore_songs_detail, LocalFolderTab.SONGS, Icons.Rounded.LibraryMusic, Color(0xFF6345B8), Color(0xFF33205C)),
        ExploreSection(R.string.local_explore_artists, R.string.local_explore_artists_detail, LocalFolderTab.ARTISTS, Icons.Rounded.Person, Color(0xFFB54D6E), Color(0xFF57283F)),
        ExploreSection(R.string.local_explore_albums, R.string.local_explore_albums_detail, LocalFolderTab.ALBUMS, Icons.Rounded.Album, Color(0xFF398C7B), Color(0xFF1E4F48)),
        ExploreSection(R.string.local_explore_folders, R.string.local_explore_folders_detail, LocalFolderTab.FOLDERS, Icons.Rounded.FolderOpen, Color(0xFFB07938), Color(0xFF5D4328)),
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 720.dp) 3 else 2
        val rows = sections.chunked(columns)
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "local-explore-heading") {
                Column(Modifier.padding(bottom = 2.dp)) {
                    Text(
                        text = stringResource(R.string.explore),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (hasPermission) {
                            stringResource(R.string.local_explore_count, songCount)
                        } else {
                            stringResource(R.string.local_explore_subtitle)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!hasPermission) {
                item(key = "local-explore-access") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(Icons.Rounded.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.local_access_title), style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(3.dp))
                                Text(stringResource(R.string.local_access_detail), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = onRequestPermission) { Text(stringResource(R.string.local_access_action)) }
                        }
                    }
                }
            }
            items(rows, key = { row -> row.first().tab.name }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { section ->
                        LocalExploreCard(section, Modifier.weight(1f), onClick = { onBrowse(section.tab) })
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun LocalExploreCard(section: ExploreSection, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .aspectRatio(1.35f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(section.light, section.dark)))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.88f),
            modifier = Modifier.size(42.dp).align(Alignment.TopEnd),
        )
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(
                text = stringResource(section.title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(section.description),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 2,
            )
        }
    }
}

private data class ExploreSection(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val tab: LocalFolderTab,
    val icon: ImageVector,
    val light: Color,
    val dark: Color,
)
