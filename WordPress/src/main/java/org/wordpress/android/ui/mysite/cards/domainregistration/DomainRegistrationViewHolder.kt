package org.wordpress.android.ui.mysite.cards.domainregistration

import android.view.ViewGroup
import org.wordpress.android.databinding.DomainRegistrationCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.util.extensions.viewBinding

class DomainRegistrationViewHolder(
    parent: ViewGroup
) : MySiteCardAndItemViewHolder<DomainRegistrationCardBinding>(
        parent.viewBinding(DomainRegistrationCardBinding::inflate)
) {
    fun bind(card: DomainRegistrationCard) = with(binding) {
        mySiteRegisterDomainCta.setOnClickListener { card.onClick.click() }
    }
}
