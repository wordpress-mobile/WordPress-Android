package org.wordpress.android.ui.comments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

@Deprecated("Comments are being refactored as part of Comments Unification project. If you are adding any" +
        " features or modifying this class, please ping develric or klymyam")
abstract class CommentListViewHolder(parent: ViewGroup, @LayoutRes layout: Int) : RecyclerView.ViewHolder(
        LayoutInflater.from(
                parent.context
        ).inflate(layout, parent, false)
)
