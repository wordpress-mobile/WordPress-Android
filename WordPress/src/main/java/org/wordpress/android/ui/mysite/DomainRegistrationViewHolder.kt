package org.wordpress.android.ui.mysite

import android.view.ViewGroup
import org.wordpress.android.databinding.DomainRegistrationCardBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.util.viewBinding

class DomainRegistrationViewHolder(
    parent: ViewGroup
) : MySiteItemViewHolder<DomainRegistrationCardBinding>(parent.viewBinding(DomainRegistrationCardBinding::inflate)) {
    fun bind(card: DomainRegistrationCard) = with(binding) {
        mySiteRegisterDomainCta.setOnClickListener { card.onClick.click() }
    }
}
