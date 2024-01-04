package org.wordpress.android.ui.domains

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.ui.domains.DomainsDashboardItem.AddDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PurchaseDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PurchasePlan
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomains
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomainsHeader
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.ADD_DOMAIN
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.PURCHASE_DOMAIN
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.PURCHASE_PLAN
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.SITE_DOMAINS
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.SITE_DOMAINS_HEADER
import org.wordpress.android.ui.domains.DomainsDashboardViewHolder.AddDomainViewHolder
import org.wordpress.android.ui.domains.DomainsDashboardViewHolder.PurchaseDomainViewHolder
import org.wordpress.android.ui.domains.DomainsDashboardViewHolder.PurchasePlanViewHolder
import org.wordpress.android.ui.domains.DomainsDashboardViewHolder.SiteDomainsHeaderViewHolder
import org.wordpress.android.ui.domains.DomainsDashboardViewHolder.SiteDomainsViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class DomainsDashboardAdapter @Inject constructor(
    private val uiHelpers: UiHelpers
) : ListAdapter<DomainsDashboardItem, DomainsDashboardViewHolder<*>>(DomainsDashboardDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DomainsDashboardViewHolder<*> {
        return when (Type.entries[viewType]) {
            SITE_DOMAINS_HEADER -> SiteDomainsHeaderViewHolder(parent, uiHelpers)
            SITE_DOMAINS -> SiteDomainsViewHolder(parent, uiHelpers)
            ADD_DOMAIN -> AddDomainViewHolder(parent)
            PURCHASE_DOMAIN -> PurchaseDomainViewHolder(parent, uiHelpers)
            PURCHASE_PLAN -> PurchasePlanViewHolder(parent, uiHelpers)
        }
    }

    override fun onBindViewHolder(holder: DomainsDashboardViewHolder<*>, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SiteDomainsHeaderViewHolder -> holder.onBind(item as SiteDomainsHeader)
            is SiteDomainsViewHolder -> holder.onBind(item as SiteDomains)
            is AddDomainViewHolder -> holder.onBind(item as AddDomain)
            is PurchaseDomainViewHolder -> holder.onBind(item as PurchaseDomain)
            is PurchasePlanViewHolder -> holder.onBind(item as PurchasePlan)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }
}

object DomainsDashboardDiffCallback : DiffUtil.ItemCallback<DomainsDashboardItem>() {
    override fun areItemsTheSame(oldItem: DomainsDashboardItem, newItem: DomainsDashboardItem): Boolean {
        return oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: DomainsDashboardItem, newItem: DomainsDashboardItem): Boolean {
        return oldItem == newItem
    }
}
