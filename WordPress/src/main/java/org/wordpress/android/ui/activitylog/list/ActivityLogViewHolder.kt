package org.wordpress.android.ui.activitylog.list

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

abstract class ActivityLogViewHolder(parent: ViewGroup, @LayoutRes layout: Int)
    : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    open fun updateChanges(bundle: Bundle) { }
}
