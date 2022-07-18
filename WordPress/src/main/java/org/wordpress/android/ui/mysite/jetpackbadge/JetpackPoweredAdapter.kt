package org.wordpress.android.ui.mysite.jetpackbadge

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Caption
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Illustration
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Title
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Type
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Type.CAPTION
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Type.ILLUSTRATION
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Type.TITLE
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredViewHolder.CaptionViewHolder
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredViewHolder.IllustrationViewHolder
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredViewHolder.TitleViewHolder

import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class JetpackPoweredAdapter @Inject constructor(
    private val uiHelpers: UiHelpers
) : ListAdapter<JetpackPoweredItem, JetpackPoweredViewHolder<*>>(JetpackPoweredDiffCallback) {
    override fun onBindViewHolder(holder: JetpackPoweredViewHolder<*>, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }

    override fun onBindViewHolder(holder: JetpackPoweredViewHolder<*>, position: Int, payloads: List<Any>) {
        val item = getItem(position)
        when (holder) {
            is IllustrationViewHolder -> holder.onBind(item as Illustration)
            is TitleViewHolder -> holder.onBind(item as Title)
            is CaptionViewHolder -> holder.onBind(item as Caption)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JetpackPoweredViewHolder<*> {
        return when (Type.values()[viewType]) {
            TITLE -> TitleViewHolder(parent, uiHelpers)
            ILLUSTRATION -> IllustrationViewHolder(parent)
            CAPTION -> CaptionViewHolder(parent, uiHelpers)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    object JetpackPoweredDiffCallback : DiffUtil.ItemCallback<JetpackPoweredItem>() {
        override fun areItemsTheSame(oldItem: JetpackPoweredItem, newItem: JetpackPoweredItem): Boolean {
            return oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: JetpackPoweredItem, newItem: JetpackPoweredItem): Boolean {
            return oldItem == newItem
        }
    }
}
