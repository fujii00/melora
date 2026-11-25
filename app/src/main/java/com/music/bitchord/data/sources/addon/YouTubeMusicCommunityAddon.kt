package com.music.bitchord.data.sources.addon

/** User-generated YouTube Music for Eclipse endpoint added at the user's request. */
object YouTubeMusicCommunityAddon {
    /** Eclipse's generated songs-only addon URL, including its per-user route. */
    const val SONGS_URL =
        "https://youtube.cyrusna29.workers.dev/u/71efd8e789559deaefa926f56377/songs"

    /** The generated `/songs` path selects a distinct manifest with songs-only defaults. */
    const val BASE_URL = SONGS_URL

    const val LABEL = "YouTube Music"
}
