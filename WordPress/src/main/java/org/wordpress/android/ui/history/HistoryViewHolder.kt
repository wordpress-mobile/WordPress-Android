package org.wordpress.android.ui.history

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

abstract class HistoryViewHolder(parent: ViewGroup, @LayoutRes layout: Int)
    : androidx.recyclerview.widget.RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    open fun updateChanges(bundle: Bundle) { }
}
