package org.wordpress.android.ui.domains.management

import uniffi.wp_api.AllDomainItem
import uniffi.wp_api.DomainSubtypeId

fun AllDomainItem.getDomainDetailsUrl(): String? {
    if (domain.isEmpty() || siteSlug.isEmpty()) return null
    return when (subtype.id) {
        is DomainSubtypeId.DomainTransfer ->
            "https://wordpress.com/domains/manage/all/$domain/transfer/in/$siteSlug"
        is DomainSubtypeId.SiteRedirect ->
            "https://wordpress.com/domains/manage/all/$domain/redirect/$siteSlug"
        else ->
            "https://wordpress.com/domains/manage/all/$domain/edit/$siteSlug"
    }
}
