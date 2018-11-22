package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup

abstract class BaseStatsViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
