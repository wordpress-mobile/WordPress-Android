package org.wordpress.android.ui.mediapicker

interface MediaSource {
    suspend fun load(
        forced: Boolean = false,
        loadMore: Boolean = false,
        filter: String? = null
    ): MediaLoadingResult

    sealed class MediaLoadingResult {
        data class Success(val data: List<MediaItem>, val hasMore: Boolean = false) : MediaLoadingResult()
        data class Failure(val message: String) : MediaLoadingResult()
        object NoChange : MediaLoadingResult()
    }
}
