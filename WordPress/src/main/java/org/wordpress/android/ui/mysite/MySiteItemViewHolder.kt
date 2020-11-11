package org.wordpress.android.ui.mysite

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder

open class MySiteItemViewHolder(parent: ViewGroup, layout: Int) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(layout, parent, false)
)
