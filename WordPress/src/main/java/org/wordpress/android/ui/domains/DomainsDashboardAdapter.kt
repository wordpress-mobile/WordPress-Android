package org.wordpress.android.ui.domains

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.ui.domains.DomainsDashboardItem.AddDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.DomainBlurb
import org.wordpress.android.ui.domains.DomainsDashboardItem.ManageDomains
import org.wordpress.android.ui.domains.DomainsDashboardItem.PrimaryDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.PurchaseDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomains
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomainsHeader
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.ADD_DOMAIN
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.DOMAIN_BLURB
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.MANAGE_DOMAINS
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.PRIMARY_DOMAIN
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.PURCHASE_DOMAIN
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.SITE_DOMAINS
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.SITE_DOMAINS_HEADER
import org.wordpress.android.ui.domains.DomainsDashboardViewHolder.AddDomainViewHolder
import org.wordpress.android.ui.domains.DomainsDashboardViewHolder.DomainBlurbViewHolder
import org.wordpress.android.ui.domains.DomainsDashboardViewHolder.ManageDomainsViewHolder
import org.wordpress.android.ui.domains.DomainsDashboardViewHolder.PrimaryDomainViewHolder
import org.wordpress.android.ui.domains.DomainsDashboardViewHolder.PurchaseDomainViewHolder
import org.wordpress.android.ui.domains.DomainsDashboardViewHolder.SiteDomainsHeaderViewHolder
import org.wordpress.android.ui.domains.DomainsDashboardViewHolder.SiteDomainsViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class DomainsDashboardAdapter @Inject constructor(
    private val uiHelpers: UiHelpers
) : ListAdapter<DomainsDashboardItem, DomainsDashboardViewHolder<*>>(DomainsDashboardDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DomainsDashboardViewHolder<*> {
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

    override fun onBindViewHolder(holder: DomainsDashboardViewHolder<*>, position: Int) {
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

object DomainsDashboardDiffCallback : DiffUtil.ItemCallback<DomainsDashboardItem>() {
    override fun areItemsTheSame(oldItem: DomainsDashboardItem, newItem: DomainsDashboardItem): Boolean {
        return oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: DomainsDashboardItem, newItem: DomainsDashboardItem): Boolean {
        return oldItem == newItem
    }
}
