package org.wordpress.android.ui.main

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

open class AddContentViewHolder<T : ViewBinding>(protected val binding: T) : RecyclerView.ViewHolder(binding.root)
