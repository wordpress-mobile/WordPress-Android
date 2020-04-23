package org.wordpress.android.ui.posts.prepublishing

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState
import org.wordpress.android.ui.posts.PrepublishingActionItemUiState.PrepublishingHomeHeaderUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DisplayUtils.dpToPx
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR

private const val ROUNDING_RADIUS_DP = 10

class PrepublishingHeaderListItemViewHolder(
    internal val parent: ViewGroup,
    val uiHelpers: UiHelpers,
    val imageManager: ImageManager
) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                R.layout.prepublishing_home_header_list_item, parent, false
        )
) {
    private val siteName: TextView = itemView.findViewById(R.id.site_name)
    private val siteIcon: ImageView = itemView.findViewById(R.id.site_icon)

    fun bind(uiState: PrepublishingActionItemUiState) {
        uiState as PrepublishingHomeHeaderUiState

        siteName.text = uiHelpers.getTextOfUiString(itemView.context, uiState.siteName)
        imageManager.loadWithRoundedCorners(
                siteIcon,
                BLAVATAR,
                uiState.siteIconUrl,
                dpToPx(itemView.context, ROUNDING_RADIUS_DP)
        )
    }
}
