package org.wordpress.android.ui.photopicker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.R.id

/*
 * ViewHolder containing a device thumbnail
 */
open class ThumbnailViewHolder(parent: ViewGroup, layout: Int) : ViewHolder(
        LayoutInflater.from(parent.context)
                .inflate(layout, parent, false)
)
