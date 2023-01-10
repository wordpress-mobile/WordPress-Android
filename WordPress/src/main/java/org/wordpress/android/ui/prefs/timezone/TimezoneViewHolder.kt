package org.wordpress.android.ui.prefs.timezone

import android.view.LayoutInflater
import android.view.View
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

            if (timezone.offset.isNotBlank()) {
                zoneOffset.text = timezone.offset
                zoneOffset.visibility = View.VISIBLE
            } else {
                zoneOffset.visibility = View.GONE
            }

            if (timezone.time.isNotBlank()) {
                zoneTime.text = timezone.time
                zoneTime.visibility = View.VISIBLE
            } else {
                zoneTime.visibility = View.GONE
            }

            itemTimeZone.setOnClickListener {
                onClick(timezone)
            }
        }
    }

    companion object {
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
