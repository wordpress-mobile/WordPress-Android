package org.wordpress.android.ui.prefs.timezone

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.databinding.SiteSettingsTimezoneBottomSheetListItemBinding

class TimezoneViewHolder(
    private val binding: SiteSettingsTimezoneBottomSheetListItemBinding
) : ViewHolder(binding.root) {
    fun bind(timezone: Timezone, onClick: (timezone: Timezone) -> Unit) {
        binding.apply {
            itemTimeZone.text = timezone.label
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
