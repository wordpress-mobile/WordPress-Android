package org.wordpress.android.ui.mysite.items.infoitem

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteInfoItemBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.InfoItem
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class MySiteInfoItemViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : CardViewHolder<MySiteInfoItemBinding>(
    parent.viewBinding(MySiteInfoItemBinding::inflate)
) {
    fun bind(item: InfoItem) = with(binding) {
        uiHelpers.setTextOrHide(mySiteInfoMessage, item.title)
    }
}
