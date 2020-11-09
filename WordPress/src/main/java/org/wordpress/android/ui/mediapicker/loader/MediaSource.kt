package org.wordpress.android.ui.mediapicker.loader

import org.wordpress.android.ui.mediapicker.MediaItem
import org.wordpress.android.ui.utils.UiString

interface MediaSource {
    suspend fun load(
        forced: Boolean = false,
        loadMore: Boolean = false,
        filter: String? = null
    ): MediaLoadingResult

    sealed class MediaLoadingResult(open val data: List<MediaItem>) {
        data class Success(override val data: List<MediaItem>, val hasMore: Boolean = false) : MediaLoadingResult(data)
        data class Empty(
            val title: UiString,
            val htmlSubtitle: UiString? = null,
            val image: Int? = null,
            val bottomImage: Int? = null,
            val bottomImageContentDescription: UiString? = null
        ) : MediaLoadingResult(listOf())

        data class Failure(
            val title: UiString,
            val htmlSubtitle: UiString? = null,
            val image: Int? = null,
            override val data: List<MediaItem> = listOf()
        ) : MediaLoadingResult(data)
    }
}
