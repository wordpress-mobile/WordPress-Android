package org.wordpress.android.ui.mysite.cards.siteinfo

import android.view.View
import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteInfoHeaderCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR

class SiteInfoHeaderCardViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager
) : MySiteCardAndItemViewHolder<MySiteInfoHeaderCardBinding>(parent.viewBinding(MySiteInfoHeaderCardBinding::inflate)) {
    fun bind(item: SiteInfoHeaderCard) = with(binding) {
        if (item.iconState is SiteInfoHeaderCard.IconState.Visible) {
            mySiteBlavatar.visibility = View.VISIBLE
            imageManager.load(mySiteBlavatar, BLAVATAR, item.iconState.url ?: "")
            mySiteIconProgress.visibility = View.GONE
            mySiteBlavatar.setOnClickListener { item.onIconClick.click() }
        } else if (item.iconState is SiteInfoHeaderCard.IconState.Progress) {
            mySiteBlavatar.setOnClickListener(null)
            mySiteIconProgress.visibility = View.VISIBLE
            mySiteBlavatar.visibility = View.GONE
        }
        quickStartIconFocusPoint.setVisibleOrGone(item.showIconFocusPoint)
        if (item.onTitleClick != null) {
            siteInfoContainer.title.setOnClickListener { item.onTitleClick.click() }
        } else {
            siteInfoContainer.title.setOnClickListener(null)
        }
        quickStartTitleFocusPoint.setVisibleOrGone(item.showTitleFocusPoint)
        quickStartSubTitleFocusPoint.setVisibleOrGone(item.showSubtitleFocusPoint)
        siteInfoContainer.subtitle.text = item.url
        siteInfoContainer.title.text = item.title
        quickStartTitleFocusPoint.setVisibleOrGone(item.showTitleFocusPoint)
        siteInfoContainer.subtitle.text = item.url
        siteInfoContainer.subtitle.setOnClickListener { item.onUrlClick.click() }
        switchSite.setOnClickListener { item.onSwitchSiteClick.click() }
    }
}
