package org.wordpress.android.ui.jetpack.scan.adapters.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import org.wordpress.android.ui.jetpack.scan.ScanListItemState

abstract class ScanViewHolder(
    @LayoutRes layout: Int,
    parent: ViewGroup,
    override val containerView: View = LayoutInflater.from(parent.context).inflate(layout, parent, false)
) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    abstract fun onBind(itemUiState: ScanListItemState)
}
