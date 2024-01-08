package org.wordpress.android.ui.domains

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import org.wordpress.android.R
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.ADD_DOMAIN
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.DOMAIN_BLURB
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.MANAGE_DOMAINS
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.PRIMARY_DOMAIN
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.PURCHASE_DOMAIN
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.SITE_DOMAINS
import org.wordpress.android.ui.domains.DomainsDashboardItem.Type.SITE_DOMAINS_HEADER
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

sealed class DomainsDashboardItem(val type: Type) {
    enum class Type {
        PRIMARY_DOMAIN,
        SITE_DOMAINS_HEADER,
        SITE_DOMAINS,
        ADD_DOMAIN,
        MANAGE_DOMAINS,
        PURCHASE_DOMAIN,
        DOMAIN_BLURB
    }

    data class FreeDomain(
        val domain: UiString,
        val isPrimary: Boolean,
        val onPopupMenuClick: (Action) -> Boolean
    ) : DomainsDashboardItem(PRIMARY_DOMAIN)

    data class SiteDomainsHeader(val title: UiString) : DomainsDashboardItem(SITE_DOMAINS_HEADER)

    data class SiteDomains(
        val domain: UiString,
        val expiry: UiString,
        val isPrimary: Boolean
    ) : DomainsDashboardItem(SITE_DOMAINS)

    data class AddDomain(val onClick: ListItemInteraction) : DomainsDashboardItem(ADD_DOMAIN)

    data class ManageDomains(val onClick: ListItemInteraction) : DomainsDashboardItem(MANAGE_DOMAINS)

    data class PurchaseDomain(
        @DrawableRes val image: Int?,
        val title: UiString,
        val body: UiString,
        val onClick: ListItemInteraction
    ) : DomainsDashboardItem(PURCHASE_DOMAIN)

    data class DomainBlurb(val blurb: UiString) : DomainsDashboardItem(DOMAIN_BLURB)

    enum class Action(@IdRes val itemId: Int) {
        CHANGE_SITE_ADDRESS(R.id.change_site_address);

        companion object {
            fun fromItemId(itemId: Int): Action {
                return values().firstOrNull { it.itemId == itemId }
                    ?: throw IllegalArgumentException("Unexpected item ID in context menu")
            }
        }
    }
}
