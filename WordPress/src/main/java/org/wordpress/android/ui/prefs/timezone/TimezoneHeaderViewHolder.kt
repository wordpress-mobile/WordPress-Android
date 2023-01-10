package org.wordpress.android.ui.prefs.timezone

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.databinding.SiteSettingsTimezoneBottomSheetListHeaderBinding
import org.wordpress.android.ui.prefs.timezone.TimezonesList.TimezoneHeader

class TimezoneHeaderViewHolder(
    private val binding: SiteSettingsTimezoneBottomSheetListHeaderBinding
) : ViewHolder(binding.root) {
    fun bind(header: TimezoneHeader) {
        binding.apply {
            headerTimeZone.text = header.label
        }
    }

    companion object {
        fun from(parent: ViewGroup): TimezoneHeaderViewHolder {
            val binding = SiteSettingsTimezoneBottomSheetListHeaderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return TimezoneHeaderViewHolder(binding)
        }
    }
}
