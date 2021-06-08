package org.wordpress.android.ui.prefs.language

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.LocalePickerListViewType.LOCALE
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.LocalePickerListViewType.SUB_HEADER
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.LocaleRow
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.SubHeader

class LocalePickerAdapter : ListAdapter<LocalePickerListItem, LocalePickerListViewHolder<*>>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalePickerListViewHolder<*> {
        return when (viewType) {
            SUB_HEADER.ordinal -> LocalePickerListSubHeaderViewHolder(parent)
            LOCALE.ordinal -> LocalePickerListItemViewHolder(parent)
            else -> throw IllegalArgumentException("Unexpected view holder in LocalePickerAdapter")
        }
    }

    override fun onBindViewHolder(holder: LocalePickerListViewHolder<*>, position: Int) {
        val item = getItem(position)
        when (holder) {
            is LocalePickerListSubHeaderViewHolder -> holder.bind(item as SubHeader)
            is LocalePickerListItemViewHolder -> holder.bind(item as LocaleRow)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)!!.type.ordinal
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LocalePickerListItem>() {
            override fun areItemsTheSame(oldItem: LocalePickerListItem, newItem: LocalePickerListItem): Boolean {
                return when {
                    oldItem is LocaleRow && newItem is LocaleRow -> oldItem.label == newItem.label
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: LocalePickerListItem, newItem: LocalePickerListItem) =
                    oldItem == newItem
        }
    }
}
