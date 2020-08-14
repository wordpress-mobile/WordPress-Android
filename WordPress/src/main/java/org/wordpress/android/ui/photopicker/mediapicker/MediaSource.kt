package org.wordpress.android.ui.photopicker.mediapicker

interface MediaSource {
    suspend fun load(
        mediaTypes: Set<MediaType>,
        offset: Int = 0,
        pageSize: Int? = null
    ): MediaLoadingResult

    sealed class MediaLoadingResult {
        data class Success(val mediaItems: List<MediaItem>, val hasMore: Boolean = false) : MediaLoadingResult()
        data class Failure(val message: String) : MediaLoadingResult()
    }
}
