package com.music.bitchord.data.sources.addon

import com.music.bitchord.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject

/** Creates the per-install URL required by Eclipse's public SoundCloud addon. */
object EclipseCommunityAddon {
    const val SOUNDCLOUD_ROOT = "https://sc.eclipsemusic.app"
    const val SOUNDCLOUD_MANIFEST = "$SOUNDCLOUD_ROOT/manifest.json"

    fun isSoundCloudUrl(raw: String): Boolean =
        raw.toHttpUrlOrNull()?.host.equals("sc.eclipsemusic.app", ignoreCase = true)

    /** The plain host serves a manifest but is not a usable search URL. */
    fun needsSetup(raw: String): Boolean {
        val url = raw.toHttpUrlOrNull() ?: return false
        return url.host.equals("sc.eclipsemusic.app", ignoreCase = true) &&
            url.encodedPath.trimEnd('/').isEmpty()
    }

    suspend fun createSoundCloudUrl(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$SOUNDCLOUD_ROOT/api/setup")
                .post(ByteArray(0).toRequestBody(null))
                .header("Accept", "application/json")
                .build()
            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Eclipse setup failed (HTTP ${response.code})")
                val body = response.body?.string().orEmpty()
                val url = JSONObject(body).optString("url").trim()
                val parsed = url.toHttpUrlOrNull()
                    ?: error("Eclipse returned an invalid addon URL")
                if (!parsed.host.equals("sc.eclipsemusic.app", ignoreCase = true) ||
                    parsed.encodedPath.trimEnd('/').isEmpty()
                ) {
                    error("Eclipse returned an incomplete addon URL")
                }
                url
            }
        }
    }
}
