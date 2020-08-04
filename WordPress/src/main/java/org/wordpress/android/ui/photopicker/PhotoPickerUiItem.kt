package org.wordpress.android.ui.photopicker

import org.wordpress.android.util.UriWrapper

data class PhotoPickerUiItem(
    val id: Long,
    val uri: UriWrapper? = null,
    val isVideo: Boolean = false,
    val isSelected: Boolean = false,
    val selectedOrder: Int? = null,
    val showOrderCounter: Boolean = false,
    val toggleAction: ToggleAction,
    val clickAction: ClickAction
) {
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
}
