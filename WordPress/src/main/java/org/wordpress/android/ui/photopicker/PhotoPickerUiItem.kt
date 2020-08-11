package org.wordpress.android.ui.photopicker

import android.net.Uri

data class PhotoPickerUiItem(
    val id: Long,
    val uri: Uri? = null,
    val isVideo: Boolean = false,
    val isSelected: Boolean = false,
    val selectedOrder: Int? = null,
    val showOrderCounter: Boolean = false,
    val toggleAction: ToggleAction
) {
    data class ToggleAction(
        val id: Long,
        val canMultiselect: Boolean,
        val toggleSelected: (id: Long, canMultiselect: Boolean) -> Unit
    ) {
        fun toggle() = toggleSelected(id, canMultiselect)
    }
}
