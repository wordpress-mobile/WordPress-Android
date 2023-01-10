package org.wordpress.android.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

abstract class HistoryViewHolder(parent: ViewGroup, @LayoutRes layout: Int) : RecyclerView.ViewHolder(
    LayoutInflater.from(
        parent.context
    ).inflate(layout, parent, false)
) {
    open fun updateChanges(bundle: Bundle) {}
}
