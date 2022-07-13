package org.wordpress.android.ui.mysite.jetpackbadge

import android.view.ViewGroup
import org.wordpress.android.databinding.JetpackBadgeBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.JetpackBadge
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.util.extensions.viewBinding

class MySiteJetpackBadgeViewHolder(
    parent: ViewGroup,
) : MySiteCardAndItemViewHolder<JetpackBadgeBinding>(parent.viewBinding(JetpackBadgeBinding::inflate)) {
    @Suppress("UNUSED_PARAMETER")
    fun bind(item: JetpackBadge) {
        // NOOP - To be updated when we're making the badge actionable
    }
}
