package org.wordpress.android.ui.prefs.timezone

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

class TimezoneAdapter(
    private val onClick: (timezone: Timezone) -> Unit
) : ListAdapter<Timezone, TimezoneViewHolder>(DIFF_CALLBACK)
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimezoneViewHolder {
        return TimezoneViewHolder.from(parent)
    }

    // TODO: add continent sub headers
    override fun onBindViewHolder(holder: TimezoneViewHolder, position: Int) {
        holder.bind(getItem(position) as Timezone, onClick)
    }


    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Timezone>() {
            override fun areItemsTheSame(oldItem: Timezone, newItem: Timezone) = oldItem.label == newItem.label
            override fun areContentsTheSame(oldItem: Timezone, newItem: Timezone) = oldItem == newItem
        }
    }
}
