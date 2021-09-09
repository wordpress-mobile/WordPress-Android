package org.wordpress.android.ui.domains

import androidx.annotation.DrawableRes
import org.wordpress.android.ui.domains.DomainsListItem.Type.ADD_DOMAIN
import org.wordpress.android.ui.domains.DomainsListItem.Type.DOMAIN_BLURB
import org.wordpress.android.ui.domains.DomainsListItem.Type.MANAGE_DOMAINS
import org.wordpress.android.ui.domains.DomainsListItem.Type.PRIMARY_DOMAIN
import org.wordpress.android.ui.domains.DomainsListItem.Type.PURCHASE_DOMAIN
import org.wordpress.android.ui.domains.DomainsListItem.Type.SITE_DOMAINS
import org.wordpress.android.ui.domains.DomainsListItem.Type.SITE_DOMAINS_HEADER
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

sealed class DomainsListItem(val type: Type) {
    enum class Type {
        PRIMARY_DOMAIN,
        SITE_DOMAINS_HEADER,
        SITE_DOMAINS,
        ADD_DOMAIN,
        MANAGE_DOMAINS,
        PURCHASE_DOMAIN,
        DOMAIN_BLURB
    }

    data class PrimaryDomain(val domain: UiString, val onClick: ListItemInteraction) : DomainsListItem(PRIMARY_DOMAIN)

    data class SiteDomainsHeader(val title: UiString) : DomainsListItem(SITE_DOMAINS_HEADER)

    data class SiteDomains(val domain: UiString, val expiry: UiString) : DomainsListItem(SITE_DOMAINS)

    data class AddDomain(val onClick: ListItemInteraction) : DomainsListItem(ADD_DOMAIN)

    data class ManageDomains(val onClick: ListItemInteraction) : DomainsListItem(MANAGE_DOMAINS)

    data class PurchaseDomain(
        @DrawableRes val image: Int,
        val title: UiString,
        val body: UiString,
        val onClick: ListItemInteraction
    ) : DomainsListItem(PURCHASE_DOMAIN)

    data class DomainBlurb(val blurb: UiString) : DomainsListItem(DOMAIN_BLURB)
}
