package com.music.bitchord.data.profile

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MeloraProfile(
    val name: String = "",
    val pictureUri: String? = null,
    val pictureShape: String = "circle",
)

/** Small, local-only profile used by Home's greeting. */
object MeloraProfileStore {
    private const val PREFS = "melora_profile"
    private const val KEY_NAME = "name"
    private const val KEY_PICTURE = "picture_uri"
    private const val KEY_PICTURE_SHAPE = "picture_shape"
    private val mutableProfile = MutableStateFlow(MeloraProfile())
    val profile: StateFlow<MeloraProfile> = mutableProfile.asStateFlow()
    @Volatile private var initialized = false
    private var preferences: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            mutableProfile.value = MeloraProfile(
                name = preferences?.getString(KEY_NAME, "").orEmpty(),
                pictureUri = preferences?.getString(KEY_PICTURE, null),
                pictureShape = preferences?.getString(KEY_PICTURE_SHAPE, "circle") ?: "circle",
            )
            initialized = true
        }
    }

    fun save(name: String, pictureUri: String?, pictureShape: String = "circle") {
        val cleanName = name.trim().take(48)
        preferences?.edit()
            ?.putString(KEY_NAME, cleanName)
            ?.putString(KEY_PICTURE, pictureUri)
            ?.putString(KEY_PICTURE_SHAPE, pictureShape.takeIf { it == "square" } ?: "circle")
            ?.apply()
        mutableProfile.value = MeloraProfile(cleanName, pictureUri, pictureShape.takeIf { it == "square" } ?: "circle")
    }
}
