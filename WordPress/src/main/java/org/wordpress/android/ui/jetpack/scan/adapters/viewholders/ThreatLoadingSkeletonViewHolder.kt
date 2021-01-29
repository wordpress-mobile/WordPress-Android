package org.wordpress.android.ui.jetpack.scan.adapters.viewholders

import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder

class ThreatLoadingSkeletonViewHolder(parent: ViewGroup) : JetpackViewHolder(
        R.layout.scan_list_threat_item_loading_skeleton,
        parent
) {
    override fun onBind(itemUiState: JetpackListItemState) {}
}
