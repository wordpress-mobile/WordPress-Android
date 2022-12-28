package org.wordpress.android.ui.domains

import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.wordpress.android.R
import org.wordpress.android.databinding.DomainAddDomainCtaBinding
import org.wordpress.android.databinding.DomainFreeSiteAddressCardBinding
import org.wordpress.android.databinding.DomainManageDomainsCtaBinding
import org.wordpress.android.databinding.DomainPurchaseCardBinding
import org.wordpress.android.databinding.DomainSiteDomainsBlurbBinding
import org.wordpress.android.databinding.DomainSiteDomainsCardBinding
import org.wordpress.android.databinding.DomainSiteDomainsHeaderBinding
import org.wordpress.android.ui.domains.DomainsDashboardItem.AddDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.DomainBlurb
import org.wordpress.android.ui.domains.DomainsDashboardItem.FreeDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.ManageDomains
import org.wordpress.android.ui.domains.DomainsDashboardItem.PurchaseDomain
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomains
import org.wordpress.android.ui.domains.DomainsDashboardItem.SiteDomainsHeader
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

sealed class DomainsDashboardViewHolder<T : ViewBinding>(
    protected val binding: T
) : RecyclerView.ViewHolder(binding.root) {
    class FreeDomainViewHolder(
        parent: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : DomainsDashboardViewHolder<DomainFreeSiteAddressCardBinding>(
        parent.viewBinding(DomainFreeSiteAddressCardBinding::inflate)
    ) {
        fun onBind(item: FreeDomain) = with(binding) {
            uiHelpers.setTextOrHide(primarySiteAddress, item.domain)
            primarySiteAddressChip.isVisible = item.isPrimary
            primarySiteAddressActions.setOnClickListener { view -> popupMenuClick(item, view) }
        }

        private fun popupMenuClick(item: FreeDomain, v: View) {
            val popup = PopupMenu(v.context, v)
            popup.setOnMenuItemClickListener { menuItem ->
                val action = DomainsDashboardItem.Action.fromItemId(menuItem.itemId)
                item.onPopupMenuClick(action)
            }
            popup.menuInflater.inflate(R.menu.domains_more, popup.menu)
            popup.show()
        }
    }

    class SiteDomainsHeaderViewHolder(
        parent: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : DomainsDashboardViewHolder<DomainSiteDomainsHeaderBinding>(
        parent.viewBinding(DomainSiteDomainsHeaderBinding::inflate)
    ) {
        fun onBind(item: SiteDomainsHeader) = with(binding) {
            uiHelpers.setTextOrHide(siteDomainsHeader, item.title)
        }
    }

    class SiteDomainsViewHolder(
        parent: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : DomainsDashboardViewHolder<DomainSiteDomainsCardBinding>(
        parent.viewBinding(DomainSiteDomainsCardBinding::inflate)
    ) {
        fun onBind(item: SiteDomains) = with(binding) {
            uiHelpers.setTextOrHide(siteDomain, item.domain)
            uiHelpers.setTextOrHide(siteDomainExpiryDate, item.expiry)
            primarySiteAddressChip.isVisible = item.isPrimary
        }
    }

    class AddDomainViewHolder(
        parent: ViewGroup
    ) : DomainsDashboardViewHolder<DomainAddDomainCtaBinding>(
        parent.viewBinding(DomainAddDomainCtaBinding::inflate)
    ) {
        fun onBind(item: AddDomain) = with(binding) {
            addDomainButton.setOnClickListener { item.onClick.click() }
        }
    }

    class ManageDomainsViewHolder(
        parent: ViewGroup
    ) : DomainsDashboardViewHolder<DomainManageDomainsCtaBinding>(
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
    ) : DomainsDashboardViewHolder<DomainPurchaseCardBinding>(
        parent.viewBinding(DomainPurchaseCardBinding::inflate)
    ) {
        fun onBind(item: PurchaseDomain) = with(binding) {
            uiHelpers.setImageOrHide(purchaseDomainImage, item.image)
            uiHelpers.setTextOrHide(purchaseDomainTitle, item.title)
            uiHelpers.setTextOrHide(purchaseDomainCaption, item.body)
            searchDomainsButton.setOnClickListener { item.onClick.click() }
        }
    }

    class DomainBlurbViewHolder(
        parent: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : DomainsDashboardViewHolder<DomainSiteDomainsBlurbBinding>(
        parent.viewBinding(DomainSiteDomainsBlurbBinding::inflate)
    ) {
        fun onBind(item: DomainBlurb) = with(binding) {
            uiHelpers.setTextOrHide(primarySiteRedirectBlurb, item.blurb)
        }
    }
}
