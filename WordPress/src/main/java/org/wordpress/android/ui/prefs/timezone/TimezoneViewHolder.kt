package org.wordpress.android.ui.prefs.timezone

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.databinding.SiteSettingsTimezoneBottomSheetListItemBinding
import org.wordpress.android.ui.prefs.timezone.TimezonesList.TimezoneItem

class TimezoneViewHolder(
    private val binding: SiteSettingsTimezoneBottomSheetListItemBinding
) : ViewHolder(binding.root) {
    fun bind(timezone: TimezoneItem, onClick: (timezone: TimezoneItem) -> Unit) {
        binding.apply {
            timeZone.text = timezone.label
            zoneOffset.text = timezone.offset
            itemTimeZone.setOnClickListener {
                onClick(timezone)
            }
        }
    }

    companion object{
        fun from(parent: ViewGroup): TimezoneViewHolder {
            val binding = SiteSettingsTimezoneBottomSheetListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
            )
            return TimezoneViewHolder(binding)
        }
    }
}
