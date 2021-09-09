package org.wordpress.android.ui.domains

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.wordpress.android.databinding.DomainAddDomainCtaBinding
import org.wordpress.android.databinding.DomainManageDomainsCtaBinding
import org.wordpress.android.databinding.DomainPrimarySiteAddressCardBinding
import org.wordpress.android.databinding.DomainPurchaseCardBinding
import org.wordpress.android.databinding.DomainSiteDomainsBlurbBinding
import org.wordpress.android.databinding.DomainSiteDomainsCardBinding
import org.wordpress.android.databinding.DomainSiteDomainsHeaderBinding
import org.wordpress.android.ui.domains.DomainsListItem.AddDomain
import org.wordpress.android.ui.domains.DomainsListItem.DomainBlurb
import org.wordpress.android.ui.domains.DomainsListItem.ManageDomains
import org.wordpress.android.ui.domains.DomainsListItem.PrimaryDomain
import org.wordpress.android.ui.domains.DomainsListItem.PurchaseDomain
import org.wordpress.android.ui.domains.DomainsListItem.SiteDomains
import org.wordpress.android.ui.domains.DomainsListItem.SiteDomainsHeader
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.viewBinding

sealed class DomainDashboardViewHolder<T : ViewBinding>(
    protected val binding: T
) : RecyclerView.ViewHolder(binding.root) {
    class PrimaryDomainViewHolder(
        parent: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : DomainDashboardViewHolder<DomainPrimarySiteAddressCardBinding>(
            parent.viewBinding(DomainPrimarySiteAddressCardBinding::inflate)
    ) {
        fun onBind(item: PrimaryDomain) = with(binding) {
            uiHelpers.setTextOrHide(primarySiteAddress, item.domain)
            primarySiteAdressActions.setOnClickListener { item.onClick.click() }
        }
    }

    class SiteDomainsHeaderViewHolder(
        parent: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : DomainDashboardViewHolder<DomainSiteDomainsHeaderBinding>(
            parent.viewBinding(DomainSiteDomainsHeaderBinding::inflate)
    ) {
        fun onBind(item: SiteDomainsHeader) = with(binding) {
            uiHelpers.setTextOrHide(siteDomainsHeader, item.title)
        }
    }

    class SiteDomainsViewHolder(
        parent: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : DomainDashboardViewHolder<DomainSiteDomainsCardBinding>(
            parent.viewBinding(DomainSiteDomainsCardBinding::inflate)
    ) {
        fun onBind(item: SiteDomains) = with(binding) {
            uiHelpers.setTextOrHide(siteDomain, item.domain)
            uiHelpers.setTextOrHide(siteDomainExpiryDate, item.expiry)
        }
    }

    class AddDomainViewHolder(
        parent: ViewGroup
    ) : DomainDashboardViewHolder<DomainAddDomainCtaBinding>(
            parent.viewBinding(DomainAddDomainCtaBinding::inflate)
    ) {
        fun onBind(item: AddDomain) = with(binding) {
            addDomainCard.setOnClickListener { item.onClick.click() }
            addDomainButton.setOnClickListener { item.onClick.click() }
        }
    }

    class ManageDomainsViewHolder(
        parent: ViewGroup
    ) : DomainDashboardViewHolder<DomainManageDomainsCtaBinding>(
            parent.viewBinding(DomainManageDomainsCtaBinding::inflate)
    ) {
        fun onBind(item: ManageDomains) = with(binding) {
            manageDomainsCard.setOnClickListener { item.onClick.click() }
            manageDomainsButton.setOnClickListener { item.onClick.click() }
        }
    }

    class PurchaseDomainViewHolder(
        parent: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : DomainDashboardViewHolder<DomainPurchaseCardBinding>(
            parent.viewBinding(DomainPurchaseCardBinding::inflate)
    ) {
        fun onBind(item: PurchaseDomain) = with(binding) {
            purchaseDomainImage.setImageResource(item.image)
            uiHelpers.setTextOrHide(purchaseDomainTitle, item.title)
            uiHelpers.setTextOrHide(purchaseDomainCaption, item.body)
            searchDomainsButton.setOnClickListener { item.onClick.click() }
        }
    }

    class DomainBlurbViewHolder(
        parent: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : DomainDashboardViewHolder<DomainSiteDomainsBlurbBinding>(
            parent.viewBinding(DomainSiteDomainsBlurbBinding::inflate)
    ) {
        fun onBind(item: DomainBlurb) = with(binding) {
            uiHelpers.setTextOrHide(primarySiteRedirectBlurb, item.blurb)
            uiHelpers.setTextOrHide(learnMore, item.learnMore)
            learnMore.setOnClickListener { item.onClick.click() }
        }
    }
}
