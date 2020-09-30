package org.wordpress.android.ui.mediapicker

import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.FILE
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.NEXT_PAGE_LOADER
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.PHOTO
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.VIDEO
import org.wordpress.android.util.UriWrapper

sealed class MediaPickerUiItem(
    val type: Type,
    val fullWidthItem: Boolean = false
) {
    data class PhotoItem(
        val uri: UriWrapper? = null,
        val mimeTypeNotSupported: Boolean = false,
        val isSelected: Boolean = false,
        val selectedOrder: Int? = null,
        val showOrderCounter: Boolean = false,
        val toggleAction: ToggleAction,
        val clickAction: ClickAction
    ) : MediaPickerUiItem(PHOTO)

    data class VideoItem(
        val uri: UriWrapper? = null,
        val mimeTypeNotSupported: Boolean = false,
        val isSelected: Boolean = false,
        val selectedOrder: Int? = null,
        val showOrderCounter: Boolean = false,
        val toggleAction: ToggleAction,
        val clickAction: ClickAction
    ) : MediaPickerUiItem(VIDEO)

    data class FileItem(
        val uri: UriWrapper? = null,
        val mimeTypeNotSupported: Boolean = false,
        val fileName: String,
        val fileExtension: String? = null,
        val isSelected: Boolean = false,
        val selectedOrder: Int? = null,
        val showOrderCounter: Boolean = false,
        val toggleAction: ToggleAction,
        val clickAction: ClickAction
    ) : MediaPickerUiItem(FILE)

    data class NextPageLoader(val isLoading: Boolean, val error: String? = null, val loadAction: () -> Unit) :
            MediaPickerUiItem(NEXT_PAGE_LOADER, fullWidthItem = true)

    data class ToggleAction(
        val uri: UriWrapper,
        val canMultiselect: Boolean,
        private val toggleSelected: (uri: UriWrapper, canMultiselect: Boolean) -> Unit
    ) {
        fun toggle() = toggleSelected(uri, canMultiselect)
    }

    data class ClickAction(
        val uri: UriWrapper?,
        val isVideo: Boolean,
        private val clickItem: (uri: UriWrapper?, isVideo: Boolean) -> Unit
    ) {
        fun click() = clickItem(uri, isVideo)
    }

    enum class Type {
        PHOTO, VIDEO, FILE, NEXT_PAGE_LOADER
    }
}
