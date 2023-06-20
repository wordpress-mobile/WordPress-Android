package org.wordpress.android.ui.mysite.items.listitem

import android.view.ViewGroup
import android.widget.ImageView
import org.wordpress.android.R
import org.wordpress.android.databinding.MySiteItemBlockBinding
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageType

class MySiteListItemViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers,
    private val accountStore: AccountStore,
    private val gravatarLoader: MeGravatarLoader
) : MySiteCardAndItemViewHolder<MySiteItemBlockBinding>(parent.viewBinding(MySiteItemBlockBinding::inflate)) {
    fun bind(cardAndItem: ListItem) = with(binding) {
        if (cardAndItem.disablePrimaryIconTint) mySiteItemPrimaryIcon.imageTintList = null

        if (cardAndItem.primaryIcon == R.drawable.ic_user_white_24dp) {
            loadGravatar(mySiteItemPrimaryIcon, accountStore.account.avatarUrl)
        } else {
            uiHelpers.setImageOrHide(mySiteItemPrimaryIcon, cardAndItem.primaryIcon)
        }

        uiHelpers.setImageOrHide(mySiteItemSecondaryIcon, cardAndItem.secondaryIcon)
        uiHelpers.setTextOrHide(mySiteItemPrimaryText, cardAndItem.primaryText)
        uiHelpers.setTextOrHide(mySiteItemSecondaryText, cardAndItem.secondaryText)
        itemView.setOnClickListener { cardAndItem.onClick.click() }
        mySiteItemQuickStartFocusPoint.setVisibleOrGone(cardAndItem.showFocusPoint)
    }

    private fun loadGravatar(imgIcon: ImageView, avatarUrl: String) {
        AppLog.d(AppLog.T.MAIN, gravatarLoader.constructGravatarUrl(avatarUrl))
        imgIcon.let {
            gravatarLoader.load(
                false,
                gravatarLoader.constructGravatarUrl(avatarUrl),
                null,
                it,
                ImageType.USER,
                null
            )
        }
    }
}
