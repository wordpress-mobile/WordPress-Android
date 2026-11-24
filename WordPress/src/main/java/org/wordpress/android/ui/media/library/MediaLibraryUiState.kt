package org.wordpress.android.ui.media.library

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState

data class MediaLibraryUiState(
    val filters: List<MediaFilter> = DEFAULT_FILTERS,
    val filter: MediaFilter = MediaFilter.ALL,
    val items: List<MediaGridItemUiState> = emptyList(),
    /** The filter and search query [items] were loaded for, which trail [filter] and [searchQuery] briefly */
    val itemsFilter: MediaFilter = filter,
    val itemsQuery: String = "",
    /** Local ids of the selected items, in the order they were selected */
    val selectedIds: List<Int> = emptyList(),
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val emptyState: MediaEmptyState = MediaEmptyState.LOADING,
    val quota: MediaQuotaUiState? = null,
    val canUpload: Boolean = false,
    val addMediaOptions: List<AddMediaOption> = emptyList(),
    val thumbnailSource: MediaThumbnailSource = MediaThumbnailSource(),
    val showDeleteConfirmation: Boolean = false
) {
    val isSelectionActive: Boolean get() = selectedIds.isNotEmpty()

    companion object {
        val DEFAULT_FILTERS = MediaFilter.entries - MediaFilter.AUDIO
    }
}

data class MediaGridItemUiState(
    val localId: Int,
    val type: MediaGridItemType,
    val fileName: String,
    val mimeType: String?,
    /** Title for document tiles: the media title, falling back to the file name */
    val displayName: String,
    /** Path of the file on the device, set only for media that hasn't been uploaded yet */
    val localFilePath: String?,
    val url: String?,
    val mediumLargeUrl: String?,
    val mediumUrl: String?,
    val largeUrl: String?,
    val thumbnailUrl: String?,
    val uploadState: MediaUploadState
) {
    val canSelect: Boolean get() = uploadState != MediaUploadState.DELETING && uploadState != MediaUploadState.DELETED
}

enum class MediaGridItemType { IMAGE, VIDEO, OTHER }

/** What the grid shows when there are no items; only read while [MediaLibraryUiState.items] is empty */
enum class MediaEmptyState { LOADING, NO_CONTENT, NETWORK_ERROR, PERMISSION_ERROR, GENERIC_ERROR }

data class MediaQuotaUiState(val text: String, val isNearLimit: Boolean)

/** Site details the grid needs to pick the best image URL for a tile */
data class MediaThumbnailSource(val usePhoton: Boolean = false, val isPrivateAtomic: Boolean = false)

enum class AddMediaOption(@StringRes val labelResId: Int) {
    CAPTURE_PHOTO(R.string.photo_picker_capture_photo),
    CAPTURE_VIDEO(R.string.photo_picker_capture_video),
    CHOOSE_FILE(R.string.photo_picker_choose_file),
    STOCK_MEDIA(R.string.photo_picker_stock_media)
}

sealed interface MediaLibraryEvent {
    data class OpenMediaSettings(val media: MediaModel, val mediaIds: ArrayList<String>) : MediaLibraryEvent
    data class ShowToast(val message: String) : MediaLibraryEvent
}
