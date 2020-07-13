package org.wordpress.android.ui.reader.discover.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import org.wordpress.android.ui.reader.discover.ReaderCardUiState

abstract class ReaderViewHolder(
    internal val parent: ViewGroup,
    @LayoutRes private val layout: Int,
    override val containerView: View = LayoutInflater.from(parent.context).inflate(layout, parent, false)
) : RecyclerView.ViewHolder(containerView),
        LayoutContainer {
    abstract fun onBind(uiState: ReaderCardUiState)
}
