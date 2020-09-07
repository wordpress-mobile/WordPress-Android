package org.wordpress.android.ui.mediapicker

import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.FILE
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.PHOTO
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.Type.VIDEO
import org.wordpress.android.util.UriWrapper

sealed class MediaPickerUiItem(
    val type: Type,
    open val identifier: Identifier,
    open val isSelected: Boolean,
    open val selectedOrder: Int?,
    open val showOrderCounter: Boolean,
    open val toggleAction: ToggleAction,
    open val clickAction: ClickAction
) {
    data class PhotoItem(
        val url: String,
        override val identifier: Identifier,
        override val isSelected: Boolean = false,
        override val selectedOrder: Int? = null,
        override val showOrderCounter: Boolean = false,
        override val toggleAction: ToggleAction,
        override val clickAction: ClickAction
    ) : MediaPickerUiItem(PHOTO, identifier, isSelected, selectedOrder, showOrderCounter, toggleAction, clickAction)

    data class VideoItem(
        val url: String,
        override val identifier: Identifier,
        override val isSelected: Boolean = false,
        override val selectedOrder: Int? = null,
        override val showOrderCounter: Boolean = false,
        override val toggleAction: ToggleAction,
        override val clickAction: ClickAction
    ) : MediaPickerUiItem(VIDEO, identifier, isSelected, selectedOrder, showOrderCounter, toggleAction, clickAction)

    data class FileItem(
        val fileName: String,
        val fileExtension: String? = null,
        override val identifier: Identifier,
        override val isSelected: Boolean = false,
        override val selectedOrder: Int? = null,
        override val showOrderCounter: Boolean = false,
        override val toggleAction: ToggleAction,
        override val clickAction: ClickAction
    ) : MediaPickerUiItem(FILE, identifier, isSelected, selectedOrder, showOrderCounter, toggleAction, clickAction)

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
        PHOTO, VIDEO, FILE
    }
}
