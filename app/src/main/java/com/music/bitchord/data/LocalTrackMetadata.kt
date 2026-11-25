package com.music.bitchord.data

import android.content.Context
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TrackMetadataSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/** On-demand metadata lookup for a visible local track. */
object LocalTrackMetadata {
    private const val PREFS = "melora_local_metadata"
    private const val POSITIVE_TTL = 90L * 24 * 60 * 60 * 1000
    private const val NEGATIVE_TTL = 7L * 24 * 60 * 60 * 1000

    suspend fun metadataFor(context: Context, song: Song): TrackMetadataSuggestion? = withContext(Dispatchers.IO) {
        if (song.localUri == null || song.title.isBlank() || song.artist == "Unknown Artist") return@withContext null
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "track_${song.localUri.hashCode().toUInt().toString(16)}"
        val now = System.currentTimeMillis()
        prefs.getString(key, null)?.let { encoded ->
            runCatching {
                val cached = JSONObject(encoded)
                val ttl = if (cached.optBoolean("found")) POSITIVE_TTL else NEGATIVE_TTL
                if (now - cached.optLong("at") < ttl) {
                    if (!cached.optBoolean("found")) return@withContext null
                    return@withContext TrackMetadataSuggestion(
                        artworkUrl = cached.optString("art").takeIf(String::isNotBlank),
                        album = cached.optString("album").takeIf(String::isNotBlank),
                        genre = cached.optString("genre").takeIf(String::isNotBlank),
                        year = cached.optString("year").takeIf(String::isNotBlank),
                    )
                }
            }
        }

        val result = runCatching {
            val query = URLEncoder.encode("${song.title} ${song.artist}", "UTF-8")
            val request = Request.Builder()
                .url("https://itunes.apple.com/search?term=$query&entity=song&limit=12")
                .header("User-Agent", "Melora/1.0 (local music metadata lookup)")
                .build()
            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val rows = JSONObject(response.body?.string().orEmpty()).optJSONArray("results") ?: return@use null
                (0 until rows.length()).asSequence().mapNotNull { rows.optJSONObject(it) }
                    .firstOrNull { row ->
                        AppleCatalogue.matchKey(row.optString("trackName")) == AppleCatalogue.matchKey(song.title) &&
                            AppleCatalogue.matchKey(row.optString("artistName")) == AppleCatalogue.matchKey(song.artist)
                    }?.let { row ->
                        TrackMetadataSuggestion(
                            artworkUrl = row.optString("artworkUrl100").replace("100x100", "600x600").takeIf(String::isNotBlank),
                            album = row.optString("collectionName").takeIf(String::isNotBlank),
                            genre = row.optString("primaryGenreName").takeIf(String::isNotBlank),
                            year = row.optString("releaseDate").takeIf(String::isNotBlank)?.take(4),
                        )
                    }
            }
        }.getOrNull()
        val cachedArtwork = result?.artworkUrl?.let { url ->
            com.music.bitchord.download.MediaTagger.cacheArtwork(context.applicationContext, url) ?: url
        }
        val record = JSONObject()
            .put("at", now)
            .put("found", result != null)
            .put("art", cachedArtwork.orEmpty())
            .put("album", result?.album.orEmpty())
            .put("genre", result?.genre.orEmpty())
            .put("year", result?.year.orEmpty())
        prefs.edit().putString(key, record.toString()).apply()
        result?.copy(artworkUrl = cachedArtwork)
    }
}
