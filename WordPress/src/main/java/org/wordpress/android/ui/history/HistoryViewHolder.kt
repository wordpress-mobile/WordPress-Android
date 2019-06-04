package org.wordpress.android.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes

abstract class HistoryViewHolder(parent: ViewGroup, @LayoutRes layout: Int)
    : androidx.recyclerview.widget.RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    open fun updateChanges(bundle: Bundle) { }
}
