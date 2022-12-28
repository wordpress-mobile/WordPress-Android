package org.wordpress.android.ui.mediapicker

import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.FILE
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.NEXT_PAGE_LOADER
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.PHOTO
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.VIDEO

sealed class MediaPickerUiItem(
    val type: Type,
    val fullWidthItem: Boolean = false
) {
    data class PhotoItem(
        val url: String,
        val identifier: Identifier,
        val isSelected: Boolean = false,
        val selectedOrder: Int? = null,
        val showOrderCounter: Boolean = false,
        val toggleAction: ToggleAction,
        val clickAction: ClickAction
    ) : MediaPickerUiItem(PHOTO)

    data class VideoItem(
        val url: String,
        val identifier: Identifier,
        val isSelected: Boolean = false,
        val selectedOrder: Int? = null,
        val showOrderCounter: Boolean = false,
        val toggleAction: ToggleAction,
        val clickAction: ClickAction
    ) : MediaPickerUiItem(VIDEO)

    data class FileItem(
        val identifier: Identifier,
        val fileName: String,
        val fileExtension: String? = null,
        val isSelected: Boolean = false,
        val selectedOrder: Int? = null,
        val showOrderCounter: Boolean = false,
        val toggleAction: ToggleAction,
        val clickAction: ClickAction
    ) : MediaPickerUiItem(FILE)

    data class NextPageLoader(val isLoading: Boolean, val loadAction: () -> Unit) :
        MediaPickerUiItem(NEXT_PAGE_LOADER, fullWidthItem = true)

    data class ToggleAction(
        val identifier: Identifier,
        val canMultiselect: Boolean,
        private val toggleSelected: (identifier: Identifier, canMultiselect: Boolean) -> Unit
    ) {
        fun toggle() = toggleSelected(identifier, canMultiselect)
    }

    data class ClickAction(
        val identifier: Identifier,
        val isVideo: Boolean,
        private val clickItem: (identifier: Identifier, isVideo: Boolean) -> Unit
    ) {
        fun click() = clickItem(identifier, isVideo)
    }

    enum class Type {
        PHOTO, VIDEO, FILE, NEXT_PAGE_LOADER
    }
}
