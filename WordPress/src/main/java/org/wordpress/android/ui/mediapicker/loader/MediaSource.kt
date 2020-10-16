package org.wordpress.android.ui.mediapicker.loader

import org.wordpress.android.ui.mediapicker.MediaItem
import org.wordpress.android.ui.utils.UiString

interface MediaSource {
    suspend fun load(
        forced: Boolean = false,
        loadMore: Boolean = false,
        filter: String? = null
    ): MediaLoadingResult

    sealed class MediaLoadingResult {
        data class Success(val data: List<MediaItem>, val hasMore: Boolean = false) : MediaLoadingResult()
        data class Empty(
            val title: UiString,
            val htmlSubtitle: UiString? = null,
            val image: Int? = null,
            val bottomImage: Int? = null,
            val bottomImageContentDescription: UiString? = null
        ) : MediaLoadingResult()

        data class Failure(val message: String) : MediaLoadingResult()
    }
}
