package org.wordpress.android.ui.mysite.jetpackbadge

import android.view.ViewGroup
import org.wordpress.android.databinding.JetpackBadgeBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.JetpackBadge
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class MySiteJetpackBadgeViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers,
) : MySiteCardAndItemViewHolder<JetpackBadgeBinding>(parent.viewBinding(JetpackBadgeBinding::inflate)) {
    fun bind(item: JetpackBadge) = with(binding) {
        jetpackPoweredBadge.text = uiHelpers.getTextOfUiString(binding.jetpackPoweredBadge.context, item.text)
        item.onClick?.run {
            jetpackPoweredBadge.setOnClickListener { click() }
        }
    }
}
