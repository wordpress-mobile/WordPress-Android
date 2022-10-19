package org.wordpress.android.ui.photopicker

import org.wordpress.android.util.UriWrapper

@Deprecated("This class is being refactored, if you implement any change, please also update " +
        "{@link org.wordpress.android.ui.mediapicker.MedaPickerUiItem}")
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
    @Suppress("DEPRECATION")
    data class PhotoItem(
        override val id: Long,
        override val uri: UriWrapper? = null,
        override val isSelected: Boolean = false,
        override val selectedOrder: Int? = null,
        override val showOrderCounter: Boolean = false,
        override val toggleAction: ToggleAction,
        override val clickAction: ClickAction
    ) : PhotoPickerUiItem(Type.PHOTO, id, uri, isSelected, selectedOrder, showOrderCounter, toggleAction, clickAction)

    @Suppress("DEPRECATION")
    data class VideoItem(
        override val id: Long,
        override val uri: UriWrapper? = null,
        override val isSelected: Boolean = false,
        override val selectedOrder: Int? = null,
        override val showOrderCounter: Boolean = false,
        override val toggleAction: ToggleAction,
        override val clickAction: ClickAction
    ) : PhotoPickerUiItem(Type.VIDEO, id, uri, isSelected, selectedOrder, showOrderCounter, toggleAction, clickAction)

    data class ToggleAction(
        val id: Long,
        val canMultiselect: Boolean,
        private val toggleSelected: (id: Long, canMultiselect: Boolean) -> Unit
    ) {
        fun toggle() = toggleSelected(id, canMultiselect)
    }

    data class ClickAction(
        val uri: UriWrapper?,
        val isVideo: Boolean,
        private val clickItem: (uri: UriWrapper?, isVideo: Boolean) -> Unit
    ) {
        fun click() = clickItem(uri, isVideo)
    }

    enum class Type {
        PHOTO, VIDEO
    }
}
