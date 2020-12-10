package org.wordpress.android.ui.mysite

import android.view.ViewGroup
import kotlinx.android.synthetic.main.domain_registration_block.view.*
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteItem.DomainRegistrationBlock

class DomainRegistrationViewHolder(
    parent: ViewGroup
) : MySiteItemViewHolder(parent, R.layout.domain_registration_block) {
    fun bind(item: DomainRegistrationBlock) = itemView.apply {
        my_site_register_domain_cta.setOnClickListener { item.onClick.click() }
    }
}
