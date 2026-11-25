package com.music.bitchord.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/** Small, private listening event log used to shape the device-first Home feed. */
data class LocalPlayEvent(val uri: String, val playedAt: Long)

object LocalHomeHistory {
    private const val PREFS = "melora_local_home"
    private const val KEY_EVENTS = "plays"
    private const val KEY_DAILY_RECOMMENDATIONS = "daily_recommendations"
    private const val KEY_WEEKLY_RECOMMENDATIONS = "weekly_recommendations"
    private const val MAX_EVENTS = 500
    private const val RETAIN_MS = 45L * 24 * 60 * 60 * 1000

    private lateinit var prefs: SharedPreferences
    private val lock = Any()
    private val _events = MutableStateFlow<List<LocalPlayEvent>>(emptyList())
    val events: StateFlow<List<LocalPlayEvent>> = _events.asStateFlow()

    fun init(context: Context) {
        if (this::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _events.value = readEvents()
    }

    /** Called once when playback first reaches a track, not on every progress tick. */
    fun record(uri: String?) {
        if (!this::prefs.isInitialized || uri.isNullOrBlank()) return
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val previous = _events.value
            // Pausing and resuming the same track should not make it look like
            // two separate listens. Replays after a little while still count.
            if (previous.firstOrNull()?.let { it.uri == uri && now - it.playedAt < 2 * 60_000 } == true) return
            val next = (listOf(LocalPlayEvent(uri, now)) + previous)
                .filter { now - it.playedAt <= RETAIN_MS }
                .take(MAX_EVENTS)
            _events.value = next
            val json = JSONArray().apply {
                next.forEach { put(JSONObject().put("uri", it.uri).put("at", it.playedAt)) }
            }
            prefs.edit().putString(KEY_EVENTS, json.toString()).apply()
        }
    }

    /**
     * Returns and persists a random library selection for one named day/week.
     * Playback events and library rescans cannot reshuffle an active period;
     * only tracks that disappeared from the device are replaced.
     */
    fun stableRecommendationIds(
        slot: String,
        period: String,
        availableIds: List<String>,
        limit: Int,
        seed: Int,
    ): List<String> {
        if (availableIds.isEmpty() || limit <= 0) return emptyList()
        val key = when (slot) {
            "daily" -> KEY_DAILY_RECOMMENDATIONS
            "weekly" -> KEY_WEEKLY_RECOMMENDATIONS
            else -> return availableIds.shuffled(Random(seed)).take(limit)
        }
        if (!this::prefs.isInitialized) return availableIds.shuffled(Random(seed)).take(limit)
        synchronized(lock) {
            val available = availableIds.distinct()
            val availableSet = available.toHashSet()
            val saved = runCatching {
                prefs.getString(key, null)?.let(::JSONObject)
            }.getOrNull()
            val storedArray = saved?.optJSONArray("ids") ?: JSONArray()
            val storedIds = (0 until storedArray.length()).mapNotNull { index ->
                storedArray.optString(index).takeIf(String::isNotBlank)
            }
            val existing = if (saved?.optString("period") == period) {
                storedIds.filter { it in availableSet }.distinct()
            } else emptyList()
            val missing = (limit - existing.size).coerceAtLeast(0)
            val additions = available.filterNot { it in existing }.shuffled(Random(seed)).take(missing)
            val result = (existing + additions).take(limit)
            if (saved?.optString("period") != period || result != storedIds) {
                val json = JSONObject().put("period", period).put("ids", JSONArray().apply {
                    result.forEach { id -> put(id) }
                })
                prefs.edit().putString(key, json.toString()).apply()
            }
            return result
        }
    }

    private fun readEvents(): List<LocalPlayEvent> {
        val raw = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
        val now = System.currentTimeMillis()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                val uri = item.optString("uri").takeIf(String::isNotBlank) ?: return@mapNotNull null
                val at = item.optLong("at", 0L).takeIf { it > 0L && now - it <= RETAIN_MS }
                    ?: return@mapNotNull null
                LocalPlayEvent(uri, at)
            }.sortedByDescending(LocalPlayEvent::playedAt).take(MAX_EVENTS)
        }.getOrDefault(emptyList())
    }
}
