package org.wordpress.android.ui.domains

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.ui.domains.DomainDashboardViewHolder.AddDomainViewHolder
import org.wordpress.android.ui.domains.DomainDashboardViewHolder.DomainBlurbViewHolder
import org.wordpress.android.ui.domains.DomainDashboardViewHolder.ManageDomainsViewHolder
import org.wordpress.android.ui.domains.DomainDashboardViewHolder.PrimaryDomainViewHolder
import org.wordpress.android.ui.domains.DomainDashboardViewHolder.PurchaseDomainViewHolder
import org.wordpress.android.ui.domains.DomainDashboardViewHolder.SiteDomainsHeaderViewHolder
import org.wordpress.android.ui.domains.DomainDashboardViewHolder.SiteDomainsViewHolder
import org.wordpress.android.ui.domains.DomainsListItem.AddDomain
import org.wordpress.android.ui.domains.DomainsListItem.DomainBlurb
import org.wordpress.android.ui.domains.DomainsListItem.ManageDomains
import org.wordpress.android.ui.domains.DomainsListItem.PrimaryDomain
import org.wordpress.android.ui.domains.DomainsListItem.PurchaseDomain
import org.wordpress.android.ui.domains.DomainsListItem.SiteDomains
import org.wordpress.android.ui.domains.DomainsListItem.SiteDomainsHeader
import org.wordpress.android.ui.domains.DomainsListItem.Type
import org.wordpress.android.ui.domains.DomainsListItem.Type.ADD_DOMAIN
import org.wordpress.android.ui.domains.DomainsListItem.Type.DOMAIN_BLURB
import org.wordpress.android.ui.domains.DomainsListItem.Type.MANAGE_DOMAINS
import org.wordpress.android.ui.domains.DomainsListItem.Type.PRIMARY_DOMAIN
import org.wordpress.android.ui.domains.DomainsListItem.Type.PURCHASE_DOMAIN
import org.wordpress.android.ui.domains.DomainsListItem.Type.SITE_DOMAINS
import org.wordpress.android.ui.domains.DomainsListItem.Type.SITE_DOMAINS_HEADER
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class DomainsDashboardAdapter @Inject constructor(
    private val uiHelpers: UiHelpers
) : ListAdapter<DomainsListItem, DomainDashboardViewHolder<*>>(DomainsDashboardDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DomainDashboardViewHolder<*> {
        return when (Type.values()[viewType]) {
            PRIMARY_DOMAIN -> PrimaryDomainViewHolder(parent, uiHelpers)
            SITE_DOMAINS_HEADER -> SiteDomainsHeaderViewHolder(parent, uiHelpers)
            SITE_DOMAINS -> SiteDomainsViewHolder(parent, uiHelpers)
            ADD_DOMAIN -> AddDomainViewHolder(parent)
            MANAGE_DOMAINS -> ManageDomainsViewHolder(parent)
            PURCHASE_DOMAIN -> PurchaseDomainViewHolder(parent, uiHelpers)
            DOMAIN_BLURB -> DomainBlurbViewHolder(parent, uiHelpers)
        }
    }

    override fun onBindViewHolder(holder: DomainDashboardViewHolder<*>, position: Int) {
        val item = getItem(position)
        when (holder) {
            is PrimaryDomainViewHolder -> holder.onBind(item as PrimaryDomain)
            is SiteDomainsHeaderViewHolder -> holder.onBind(item as SiteDomainsHeader)
            is SiteDomainsViewHolder -> holder.onBind(item as SiteDomains)
            is AddDomainViewHolder -> holder.onBind(item as AddDomain)
            is ManageDomainsViewHolder -> holder.onBind(item as ManageDomains)
            is PurchaseDomainViewHolder -> holder.onBind(item as PurchaseDomain)
            is DomainBlurbViewHolder -> holder.onBind(item as DomainBlurb)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }
}

object DomainsDashboardDiffCallback : DiffUtil.ItemCallback<DomainsListItem>() {
    override fun areItemsTheSame(oldItem: DomainsListItem, newItem: DomainsListItem): Boolean {
        return oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: DomainsListItem, newItem: DomainsListItem): Boolean {
        return oldItem == newItem
    }
}
