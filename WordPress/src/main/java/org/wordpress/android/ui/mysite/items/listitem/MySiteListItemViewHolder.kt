package org.wordpress.android.ui.mysite.items.listitem

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.MySiteItemBlockBinding
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

class MySiteListItemViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers,
    private val accountStore: AccountStore,
    private val gravatarLoader: MeGravatarLoader
) : MySiteCardAndItemViewHolder<MySiteItemBlockBinding>(parent.viewBinding(MySiteItemBlockBinding::inflate)) {
    fun bind(cardAndItem: ListItem) = with(binding) {
        if (cardAndItem.disablePrimaryIconTint) mySiteItemPrimaryIcon.imageTintList = null

        if (cardAndItem.primaryIcon == R.drawable.ic_user_primary_white_24) {
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
                object : ImageManager.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(e: Exception?, model: Any?) {
                        val appLogMessage = "onLoadFailed while loading Gravatar image!"
                        if (e == null) {
                            AppLog.e(
                                AppLog.T.MAIN,
                                "$appLogMessage e == null"
                            )
                        } else {
                            AppLog.e(
                                AppLog.T.MAIN,
                                appLogMessage,
                                e
                            )
                        }
                        ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(Color.GRAY))
                    }

                    override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any?) {
                        ImageViewCompat.setImageTintList(imgIcon, null)
                        if (resource is BitmapDrawable) {
                            var bitmap = resource.bitmap
                            // create a copy since the original bitmap may by automatically recycled
                            bitmap = bitmap.copy(bitmap.config, true)
                            WordPress.getBitmapCache().put(
                                avatarUrl,
                                bitmap
                            )
                        }
                    }
                }
            )
        }
    }
}
