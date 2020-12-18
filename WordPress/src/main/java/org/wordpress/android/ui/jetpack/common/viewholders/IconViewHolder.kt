package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.backup_download_list_image_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.IconState
import org.wordpress.android.util.image.ImageManager

class IconViewHolder(
    private val imageManager: ImageManager,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.backup_download_list_image_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val iconState = itemUiState as IconState
        imageManager.load(backup_download_icon, iconState.icon)
    }
}
