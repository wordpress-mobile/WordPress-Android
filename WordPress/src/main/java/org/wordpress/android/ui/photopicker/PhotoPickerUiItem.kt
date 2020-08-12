package org.wordpress.android.ui.photopicker

import org.wordpress.android.ui.photopicker.PhotoPickerUiItem.Type.PHOTO
import org.wordpress.android.ui.photopicker.PhotoPickerUiItem.Type.VIDEO
import org.wordpress.android.util.UriWrapper

sealed class PhotoPickerUiItem(
    val type: Type,
    open val id: Long,
    open val uri: UriWrapper? = null,
    open val isSelected: Boolean = false,
    open val selectedOrder: Int? = null,
    open val showOrderCounter: Boolean = false,
    open val toggleAction: ToggleAction,
    open val clickAction: ClickAction
) {
    data class PhotoItem(
        override val id: Long,
        override val uri: UriWrapper? = null,
        override val isSelected: Boolean = false,
        override val selectedOrder: Int? = null,
        override val showOrderCounter: Boolean = false,
        override val toggleAction: ToggleAction,
        override val clickAction: ClickAction
    ) : PhotoPickerUiItem(PHOTO, id, uri, isSelected, selectedOrder, showOrderCounter, toggleAction, clickAction)

    data class VideoItem(
        override val id: Long,
        override val uri: UriWrapper? = null,
        override val isSelected: Boolean = false,
        override val selectedOrder: Int? = null,
        override val showOrderCounter: Boolean = false,
        override val toggleAction: ToggleAction,
        override val clickAction: ClickAction
    ) : PhotoPickerUiItem(VIDEO, id, uri, isSelected, selectedOrder, showOrderCounter, toggleAction, clickAction)

    data class ToggleAction(
        val id: Long,
        val canMultiselect: Boolean,
        private val toggleSelected: (id: Long, canMultiselect: Boolean) -> Unit
    ) {
        fun toggle() = toggleSelected(id, canMultiselect)
    }

    data class ClickAction(
        val id: Long,
        val uri: UriWrapper?,
        val isVideo: Boolean,
        private val clickItem: (id: Long, uri: UriWrapper?, isVideo: Boolean) -> Unit
    ) {
        fun click() = clickItem(id, uri, isVideo)
    }

    enum class Type {
        PHOTO, VIDEO
    }
}
