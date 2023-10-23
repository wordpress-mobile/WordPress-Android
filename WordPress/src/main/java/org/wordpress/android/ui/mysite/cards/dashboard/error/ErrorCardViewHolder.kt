package org.wordpress.android.ui.mysite.cards.dashboard.error

import android.view.ViewGroup
import org.wordpress.android.databinding.MySiteErrorCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.util.extensions.viewBinding

class ErrorCardViewHolder(
    parent: ViewGroup
) : MySiteCardAndItemViewHolder<MySiteErrorCardBinding>(
    parent.viewBinding(MySiteErrorCardBinding::inflate)
) {
    fun bind(card: ErrorCard) = with(binding) {
        retry.setOnClickListener { card.onRetryClick.click() }
    }
}
