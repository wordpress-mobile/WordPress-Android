package org.wordpress.android.ui.mediapicker

import kotlinx.coroutines.flow.Flow

interface MediaSource {
    suspend fun load(
        mediaTypes: Set<MediaType>,
        offset: Int = 0,
        pageSize: Int? = null
    ): Flow<MediaLoadingResult>

    suspend fun get(mediaTypes: Set<MediaType>, filter: String? = null): List<MediaItem>

    sealed class MediaLoadingResult {
        object Loading: MediaLoadingResult()
        data class Success(val hasMore: Boolean = false) : MediaLoadingResult()
        data class Failure(val message: String) : MediaLoadingResult()
    }
}
