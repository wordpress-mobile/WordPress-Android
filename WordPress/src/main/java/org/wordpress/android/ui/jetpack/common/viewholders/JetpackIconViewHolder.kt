package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.jetpack_list_icon_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.IconState
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.image.ImageManager

class JetpackIconViewHolder(
    private val imageManager: ImageManager,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_list_icon_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val iconState = itemUiState as IconState
        val context = itemView.context

        with(icon.layoutParams) {
            val size = context.resources.getDimensionPixelSize(iconState.sizeResId)
            width = size
            height = size
        }

        if (iconState.colorResId == null) {
            imageManager.load(icon, iconState.icon)
        } else {
            ColorUtils.setImageResourceWithTint(
                icon,
                iconState.icon,
                iconState.colorResId
            )
        }
    }
}
