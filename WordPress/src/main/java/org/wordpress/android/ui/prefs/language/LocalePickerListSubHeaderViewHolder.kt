package org.wordpress.android.ui.prefs.language

import android.view.ViewGroup
import org.wordpress.android.databinding.LocalePickerListSubheaderBinding
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.LocaleRow
import org.wordpress.android.ui.prefs.language.LocalePickerListItem.SubHeader
import org.wordpress.android.util.viewBinding

class LocalePickerListSubHeaderViewHolder(
    parent: ViewGroup
) : LocalePickerListViewHolder<LocalePickerListSubheaderBinding>(parent.viewBinding(LocalePickerListSubheaderBinding::inflate)) {
    fun bind(item: SubHeader) = with(binding) {
        label.text = item.label
    }
}
