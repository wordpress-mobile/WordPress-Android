package org.wordpress.android.ui.prefs.language

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class LocalePickerListViewHolder<T : ViewBinding>(protected val binding: T) :
    RecyclerView.ViewHolder(binding.root)
