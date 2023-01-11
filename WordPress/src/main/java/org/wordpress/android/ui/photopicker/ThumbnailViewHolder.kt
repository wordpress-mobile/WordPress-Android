package org.wordpress.android.ui.photopicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/*
 * ViewHolder containing a device thumbnail
 */

@Deprecated(
    "This class is being refactored, if you implement any change, please also update " +
            "{@link org.wordpress.android.ui.mediapicker.ThumbnailViewHolder}"
)
open class ThumbnailViewHolder(parent: ViewGroup, layout: Int) : ViewHolder(
    LayoutInflater.from(parent.context)
        .inflate(layout, parent, false)
)
