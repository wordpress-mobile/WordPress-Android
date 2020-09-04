package org.wordpress.android.ui.mediapicker

import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.FILE
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.PHOTO
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.VIDEO
import org.wordpress.android.util.UriWrapper

sealed class MediaPickerUiItem(
    val type: Type,
    open val uri: UriWrapper?,
    open val isSelected: Boolean,
    open val selectedOrder: Int?,
    open val showOrderCounter: Boolean,
    open val toggleAction: ToggleAction,
    open val clickAction: ClickAction
) {
    data class PhotoItem(
        override val uri: UriWrapper? = null,
        override val isSelected: Boolean = false,
        override val selectedOrder: Int? = null,
        override val showOrderCounter: Boolean = false,
        override val toggleAction: ToggleAction,
        override val clickAction: ClickAction
    ) : MediaPickerUiItem(PHOTO, uri, isSelected, selectedOrder, showOrderCounter, toggleAction, clickAction)

    data class VideoItem(
        override val uri: UriWrapper? = null,
        override val isSelected: Boolean = false,
        override val selectedOrder: Int? = null,
        override val showOrderCounter: Boolean = false,
        override val toggleAction: ToggleAction,
        override val clickAction: ClickAction
    ) : MediaPickerUiItem(VIDEO, uri, isSelected, selectedOrder, showOrderCounter, toggleAction, clickAction)

    data class FileItem(
        override val uri: UriWrapper? = null,
        val fileName: String,
        val fileExtension: String? = null,
        override val isSelected: Boolean = false,
        override val selectedOrder: Int? = null,
        override val showOrderCounter: Boolean = false,
        override val toggleAction: ToggleAction,
        override val clickAction: ClickAction
    ) : MediaPickerUiItem(FILE, uri, isSelected, selectedOrder, showOrderCounter, toggleAction, clickAction)

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
        PHOTO, VIDEO, FILE
    }
}
