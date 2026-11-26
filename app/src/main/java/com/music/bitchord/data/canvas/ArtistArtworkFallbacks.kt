package com.music.bitchord.data.canvas

import android.content.Context
import com.music.bitchord.data.AppleCatalogue
import com.music.bitchord.data.DebugLog as Log
import com.music.bitchord.data.Http
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Secondary catalog artwork lookups. These return pictures only; they never play or download audio. */
object ArtistArtworkFallbacks {
    private const val TAG = "ArtistArtworkFallbacks"
    private const val CACHE_SIZE = 80
    private val cache = object : LinkedHashMap<String, String>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>) = size > CACHE_SIZE
    }
    @Volatile private var preferences: android.content.SharedPreferences? = null

    /** Restores verified provider artwork URLs across ordinary process restarts. */
    fun init(context: Context) {
        if (preferences != null) return
        synchronized(cache) {
            if (preferences != null) return
            val prefs = context.applicationContext.getSharedPreferences("artist_art_cache", Context.MODE_PRIVATE)
            preferences = prefs
            prefs.getString("fallbacks", null)?.let { encoded ->
                runCatching {
                    val rows = JSONObject(encoded)
                    rows.keys().asSequence().forEach { key -> rows.optString(key).takeIf(String::isNotBlank)?.let { cache[key] = it } }
                }.onFailure { Log.d(TAG, "could not restore fallback artwork cache: $it") }
            }
        }
    }

    private fun remember(key: String, url: String) {
        synchronized(cache) { cache[key] = url }
        val rows = JSONObject()
        synchronized(cache) { cache.forEach { (cacheKey, value) -> rows.put(cacheKey, value) } }
        preferences?.edit()?.putString("fallbacks", rows.toString())?.apply()
    }

    suspend fun afterApple(artistName: String, localTrackTitles: Collection<String>): String? =
        spotify(artistName, localTrackTitles) ?: deezer(artistName, localTrackTitles)

    /** Spotify is queried only when an existing Spotify web session can mint a catalog token. */
    suspend fun spotify(artistName: String, localTrackTitles: Collection<String>): String? {
        val matchTitles = AppleCatalogue.keys(localTrackTitles)
        if (matchTitles.isEmpty()) return null
        val key = "spotify:${artistName.key()}"
        synchronized(cache) { cache[key] }?.let { return it }
        val token = SpotifyToken.accessToken() ?: return null
        val localTitles = localTrackTitles.mapNotNull { title ->
            AppleCatalogue.titleKey(title)?.let { key -> title.trim() to key }
        }.distinctBy { it.second }.take(3)
        if (localTitles.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://api.spotify.com/v1/search?q=${URLEncoder.encode("artist:\"$artistName\"", "UTF-8")}&type=artist&limit=5"
                val root = JSONObject(get(url, token) ?: return@runCatching null)
                val items = root.optJSONObject("artists")?.optJSONArray("items") ?: return@runCatching null
                for (i in 0 until items.length()) {
                    val artist = items.optJSONObject(i) ?: continue
                    if (artist.optString("name").key() != artistName.key()) continue
                    val id = artist.optString("id").takeIf { it.isNotBlank() } ?: continue
                    // Spotify removed the artist top-tracks endpoint in 2026;
                    // verify the match using its still-supported track search.
                    val verified = localTitles.any { (title, titleKey) ->
                        val searchQuery = "artist:\"$artistName\" track:\"$title\""
                        val tracksJson = get(
                            "https://api.spotify.com/v1/search?q=${URLEncoder.encode(searchQuery, "UTF-8")}&type=track&limit=10",
                            token,
                        )?.let(::JSONObject)?.optJSONObject("tracks")?.optJSONArray("items") ?: return@any false
                        (0 until tracksJson.length()).any { index ->
                            val track = tracksJson.optJSONObject(index)
                            if (track == null) return@any false
                            val exactTitle = AppleCatalogue.titleKey(track.optString("name")) == titleKey
                            val artists = track.optJSONArray("artists")
                            exactTitle && artists != null && (0 until artists.length()).any { artistIndex ->
                                artists.optJSONObject(artistIndex)?.optString("id") == id
                            }
                        }
                    }
                    if (!verified) continue
                    val images = artist.optJSONArray("images") ?: continue
                    val image = (0 until images.length())
                        .mapNotNull { images.optJSONObject(it) }
                        .maxByOrNull { it.optInt("width", 0) }
                        ?.optString("url")?.takeIf { it.startsWith("https://") }
                    if (image != null) return@runCatching image
                }
                null
            }.onFailure { Log.d(TAG, "Spotify artist lookup failed for $artistName: $it") }
                .getOrNull()?.also { remember(key, it) }
        }
    }

    /** Deezer's public catalog is checked against the local artist's own track titles before its photo is used. */
    suspend fun deezer(artistName: String, localTrackTitles: Collection<String>): String? {
        val matchTitles = AppleCatalogue.keys(localTrackTitles)
        if (matchTitles.isEmpty()) return null
        val key = "deezer:${artistName.key()}"
        synchronized(cache) { cache[key] }?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val query = URLEncoder.encode("artist:\"$artistName\"", "UTF-8")
                val root = JSONObject(get("https://api.deezer.com/search/artist?q=$query") ?: return@runCatching null)
                val items = root.optJSONArray("data") ?: return@runCatching null
                for (i in 0 until items.length()) {
                    val artist = items.optJSONObject(i) ?: continue
                    if (artist.optString("name").key() != artistName.key()) continue
                    val id = artist.optLong("id").takeIf { it > 0 } ?: continue
                    val top = JSONObject(get("https://api.deezer.com/artist/$id/top?limit=50") ?: continue)
                    val tracks = top.optJSONArray("data") ?: continue
                    val verified = (0 until tracks.length()).any { index ->
                        val title = tracks.optJSONObject(index)?.optString("title").orEmpty()
                        AppleCatalogue.titleKey(title)?.let(matchTitles::contains) == true
                    }
                    if (!verified) continue
                    val image = artist.optString("picture_xl").takeIf { it.startsWith("https://") }
                        ?: artist.optString("picture_big").takeIf { it.startsWith("https://") }
                    if (image != null) return@runCatching image
                }
                null
            }.onFailure { Log.d(TAG, "Deezer artist lookup failed for $artistName: $it") }
                .getOrNull()?.also { remember(key, it) }
        }
    }

    private fun get(url: String, token: String? = null): String? {
        val request = Request.Builder().url(url)
            .header("User-Agent", "Melora/1.0 (local music artwork lookup)")
            .apply {
                if (token != null) {
                    header("Authorization", "Bearer $token")
                    SpotifyToken.clientToken()?.let { header("Client-Token", it) }
                    header("App-platform", "WebPlayer")
                }
            }
            .build()
        Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    private fun String.key(): String = AppleCatalogue.matchKey(trim()).lowercase(Locale.ROOT)
}
