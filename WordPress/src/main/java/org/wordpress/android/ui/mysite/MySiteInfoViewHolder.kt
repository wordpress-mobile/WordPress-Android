package org.wordpress.android.ui.mysite

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock.IconState
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR
import org.wordpress.android.widgets.MySiteTitleAndSubtitleLabelView
import org.wordpress.android.widgets.QuickStartFocusPoint

class MySiteInfoViewHolder(parent: ViewGroup, private val imageManager: ImageManager) : MySiteItemViewHolder(
        parent,
        R.layout.my_site_info_block
) {
    private val mySiteBlavatar = itemView.findViewById<ImageView>(R.id.my_site_blavatar)
    private val mySiteIconProgress = itemView.findViewById<ProgressBar>(R.id.my_site_icon_progress)
    private val siteInfoContainer = itemView.findViewById<MySiteTitleAndSubtitleLabelView>(R.id.site_info_container)
    private val switchSite = itemView.findViewById<ImageButton>(R.id.switch_site)
    private val quickStartFocusPoint = itemView.findViewById<QuickStartFocusPoint>(R.id.quick_start_focus_point)
    fun bind(item: SiteInfoBlock) {
        if (item.iconState is IconState.Visible) {
            mySiteBlavatar.visibility = View.VISIBLE
            imageManager.load(mySiteBlavatar, BLAVATAR, item.iconState.url ?: "")
            mySiteIconProgress.visibility = View.GONE
            mySiteBlavatar.setOnClickListener { item.onIconClick.click() }
        } else if (item.iconState is IconState.Progress) {
            mySiteBlavatar.setOnClickListener(null)
            mySiteIconProgress.visibility = View.VISIBLE
            mySiteBlavatar.visibility = View.GONE
        }
        if (item.onTitleClick != null) {
            siteInfoContainer.title.setOnClickListener { item.onTitleClick.click() }
        } else {
            siteInfoContainer.title.setOnClickListener(null)
        }
        siteInfoContainer.title.text = item.title
        quickStartFocusPoint.setVisibleOrGone(item.showTitleFocusPoint)
        siteInfoContainer.subtitle.text = item.url
        siteInfoContainer.subtitle.setOnClickListener { item.onUrlClick.click() }
        switchSite.setOnClickListener { item.onSwitchSiteClick.click() }
    }
}
