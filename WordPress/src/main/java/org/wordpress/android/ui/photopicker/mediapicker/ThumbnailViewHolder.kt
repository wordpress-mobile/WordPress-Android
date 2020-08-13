package org.wordpress.android.ui.photopicker.mediapicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/*
 * ViewHolder containing a device thumbnail
 */
open class ThumbnailViewHolder(parent: ViewGroup, layout: Int) : ViewHolder(
        LayoutInflater.from(parent.context)
                .inflate(layout, parent, false)
)
