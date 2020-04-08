package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.EMPTY_SPACE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.NORMAL
import org.wordpress.android.util.getColorStateListFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND
import org.wordpress.android.util.image.ImageType.ICON

open class BlockListItemViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    protected fun TextView.setTextOrHide(@StringRes resource: Int?, value: String?) {
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
                this.imageTintList = this.context.getColorStateListFromAttribute(R.attr.wpColorOnSurfaceMedium)
                this.visibility = View.VISIBLE
                imageManager.load(this, item.icon)
            }
            item.iconUrl != null -> {
                this.visibility = View.VISIBLE
                imageManager.load(this, ICON, item.iconUrl)
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
                imageManager.loadIntoCircle(this, AVATAR_WITH_BACKGROUND, item.iconUrl)
            }
            else -> this.visibility = View.GONE
        }
    }

    protected fun LinearLayout.setIconOrAvatar(item: ListItemWithIcon, imageManager: ImageManager) {
        val avatar = findViewById<ImageView>(R.id.avatar)
        val icon = findViewById<ImageView>(R.id.icon)
        val hasIcon = item.icon != null || item.iconUrl != null
        when {
            hasIcon -> {
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
                    EMPTY_SPACE -> {
                        this.visibility = View.INVISIBLE
                    }
                }
            }
            item.iconStyle == EMPTY_SPACE -> this.visibility = View.INVISIBLE
            else -> this.visibility = View.GONE
        }
    }
}
