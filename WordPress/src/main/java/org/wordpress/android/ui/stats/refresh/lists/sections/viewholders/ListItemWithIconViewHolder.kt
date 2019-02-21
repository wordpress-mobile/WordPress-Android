package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.content.res.ColorStateList
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.color
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.NORMAL
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITHOUT_BACKGROUND
import org.wordpress.android.util.image.ImageType.IMAGE

class ListItemWithIconViewHolder(parent: ViewGroup, val imageManager: ImageManager) : BlockListItemViewHolder(
        parent,
        layout.stats_block_list_item
) {
    private val iconContainer = itemView.findViewById<LinearLayout>(id.icon_container)
    private val text = itemView.findViewById<TextView>(id.text)
    private val subtext = itemView.findViewById<TextView>(id.subtext)
    private val value = itemView.findViewById<TextView>(id.value)
    private val divider = itemView.findViewById<View>(id.divider)

    fun bind(item: ListItemWithIcon) {
        iconContainer.setIconOrAvatar(item, imageManager)
        text.setTextOrHide(item.textResource, item.text)
        subtext.setTextOrHide(item.subTextResource, item.subText)
        value.setTextOrHide(item.valueResource, item.value)
        divider.visibility = if (item.showDivider) {
            View.VISIBLE
        } else {
            View.GONE
        }
        val clickAction = item.navigationAction
        if (clickAction != null) {
            itemView.isClickable = true
            itemView.setOnClickListener { clickAction.click() }
        } else {
            itemView.isClickable = false
            itemView.background = null
            itemView.setOnClickListener(null)
        }
    }

    internal fun TextView.setTextOrHide(@StringRes resource: Int?, value: String?) {
        this.visibility = View.VISIBLE
        when {
            resource != null -> {
                this.visibility = View.VISIBLE
                this.setText(resource)
            }
            value != null -> {
                this.visibility = View.VISIBLE
                this.text = value
            }
            else -> this.visibility = View.GONE
        }
    }

    private fun ImageView.setImageOrLoad(
        item: ListItemWithIcon,
        imageManager: ImageManager
    ) {
        when {
            item.icon != null -> {
                this.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                                this.context,
                                color.grey_dark
                        )
                )
                this.visibility = View.VISIBLE
                imageManager.load(this, item.icon)
            }
            item.iconUrl != null -> {
                this.visibility = View.VISIBLE
                imageManager.load(this, IMAGE, item.iconUrl)
            }
            else -> this.visibility = View.GONE
        }
    }

    private fun ImageView.setAvatarOrLoad(
        item: ListItemWithIcon,
        imageManager: ImageManager
    ) {
        when {
            item.icon != null -> {
                this.visibility = View.VISIBLE
                imageManager.load(this, item.icon)
            }
            item.iconUrl != null -> {
                this.visibility = View.VISIBLE
                imageManager.loadIntoCircle(this,
                        AVATAR_WITHOUT_BACKGROUND, item.iconUrl)
            }
            else -> this.visibility = View.GONE
        }
    }

    internal fun LinearLayout.setIconOrAvatar(item: ListItemWithIcon, imageManager: ImageManager) {
        val avatar = findViewById<ImageView>(R.id.avatar)
        val icon = findViewById<ImageView>(R.id.icon)
        val hasIcon = item.icon != null || item.iconUrl != null
        if (hasIcon) {
            this.visibility = View.VISIBLE
            when (item.iconStyle) {
                NORMAL -> {
                    findViewById<ImageView>(R.id.avatar).visibility = View.GONE
                    icon.setImageOrLoad(item, imageManager)
                }
                AVATAR -> {
                    icon.visibility = View.GONE
                    avatar.setAvatarOrLoad(item, imageManager)
                }
            }
        } else {
            this.visibility = View.GONE
        }
    }
}
