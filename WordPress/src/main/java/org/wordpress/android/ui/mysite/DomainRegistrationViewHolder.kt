package org.wordpress.android.ui.mysite

import android.view.ViewGroup
import org.wordpress.android.databinding.DomainRegistrationBlockBinding
import org.wordpress.android.ui.mysite.MySiteItem.DomainRegistrationBlock
import org.wordpress.android.util.viewBinding

class DomainRegistrationViewHolder(
    parent: ViewGroup
) : MySiteItemViewHolder<DomainRegistrationBlockBinding>(parent.viewBinding(DomainRegistrationBlockBinding::inflate)) {
    fun bind(item: DomainRegistrationBlock) = with(binding) {
        mySiteRegisterDomainCta.setOnClickListener { item.onClick.click() }
    }
}
