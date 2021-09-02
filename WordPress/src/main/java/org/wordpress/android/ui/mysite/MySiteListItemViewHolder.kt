package org.wordpress.android.ui.mysite

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteItemBlockBinding
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.viewBinding

class MySiteListItemViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteItemViewHolder<MySiteItemBlockBinding>(parent.viewBinding(MySiteItemBlockBinding::inflate)) {
    fun bind(cardAndItem: MySiteCardAndItem.Item.ListItem) = with(binding) {
        uiHelpers.setImageOrHide(mySiteItemPrimaryIcon, cardAndItem.primaryIcon)
        uiHelpers.setImageOrHide(mySiteItemSecondaryIcon, cardAndItem.secondaryIcon)
        uiHelpers.setTextOrHide(mySiteItemPrimaryText, cardAndItem.primaryText)
        uiHelpers.setTextOrHide(mySiteItemSecondaryText, cardAndItem.secondaryText)
        itemView.setOnClickListener { cardAndItem.onClick.click() }
        mySiteItemQuickStartFocusPoint.setVisibleOrGone(cardAndItem.showFocusPoint)
    }
}
