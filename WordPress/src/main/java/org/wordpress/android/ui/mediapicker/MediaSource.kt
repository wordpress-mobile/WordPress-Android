package org.wordpress.android.ui.mediapicker

interface MediaSource {
    suspend fun load(
        mediaTypes: Set<MediaType>,
        forced: Boolean = false,
        loadMore: Boolean = false
    ): MediaLoadingResult

    suspend fun get(mediaTypes: Set<MediaType>, filter: String? = null): List<MediaItem>

    sealed class MediaLoadingResult {
        data class Success(val hasMore: Boolean = false) : MediaLoadingResult()
        data class Failure(val message: String) : MediaLoadingResult()
    }
}
