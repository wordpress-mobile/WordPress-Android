package org.wordpress.android.ui.domains

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.ADD_DOMAIN
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.PURCHASE_DOMAIN
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.PURCHASE_PLAN
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.SITE_DOMAINS
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.SITE_DOMAINS_HEADER
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

sealed class DomainsDashboardItem(val type: Type) {
    enum class Type {
        SITE_DOMAINS_HEADER,
        SITE_DOMAINS,
        ADD_DOMAIN,
        PURCHASE_DOMAIN,
        PURCHASE_PLAN
    }

    data class SiteDomainsHeader(val title: UiString) : DomainsDashboardItem(SITE_DOMAINS_HEADER)

    data class SiteDomains(
        val domain: UiString,
        val isPrimary: Boolean,
        val domainStatus: UiString,
        @ColorRes val domainStatusColor: Int,
        val expiry: UiString?,
        val onDomainClick: ListItemInteraction? = null
    ) : DomainsDashboardItem(SITE_DOMAINS)

    data class AddDomain(val onClick: ListItemInteraction) : DomainsDashboardItem(ADD_DOMAIN)

    data class PurchaseDomain(
        @DrawableRes val image: Int?,
        val title: UiString,
        val body: UiString,
        val onClick: ListItemInteraction
    ) : DomainsDashboardItem(PURCHASE_DOMAIN)

    data class PurchasePlan(
        @DrawableRes val image: Int?,
        val title: UiString,
        val body: UiString,
        val onUpgradeClick: ListItemInteraction,
        val onDomainClick: ListItemInteraction
    ) : DomainsDashboardItem(PURCHASE_PLAN)
}
