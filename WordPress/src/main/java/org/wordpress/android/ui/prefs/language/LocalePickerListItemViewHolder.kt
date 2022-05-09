package org.wordpress.android.ui.prefs.language

import android.view.ViewGroup
import org.wordpress.android.databinding.LocalePickerListItemBinding
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.LocaleRow
import org.wordpress.android.util.extensions.viewBinding

class LocalePickerListItemViewHolder(
    parent: ViewGroup
) : LocalePickerListViewHolder<LocalePickerListItemBinding>(parent.viewBinding(LocalePickerListItemBinding::inflate)) {
    fun bind(item: LocaleRow) = with(binding) {
        label.text = item.label
        localizedLabel.text = item.localizedLabel

        itemView.setOnClickListener {
            item.clickAction.onClick()
        }
    }
}
