package org.wordpress.android.ui.jetpack.scan.adapters.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.ScanListThreatItemLoadingSkeletonBinding
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder

class ThreatLoadingSkeletonViewHolder(
    parent: ViewGroup
) : JetpackViewHolder<ScanListThreatItemLoadingSkeletonBinding>(
    parent,
    ScanListThreatItemLoadingSkeletonBinding::inflate
) {
    override fun onBind(itemUiState: JetpackListItemState) {}
}
