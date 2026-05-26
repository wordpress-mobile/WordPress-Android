package org.wordpress.android.ui.domains.management

import uniffi.wp_api.AllDomainItem
import uniffi.wp_api.DomainSubtypeId

private const val REDIRECT_SUBTYPE = "redirect"

fun AllDomainItem.getDomainDetailsUrl(): String? {
    if (domain.isEmpty() || siteSlug.isEmpty()) return null
    return when (subtype.id) {
        is DomainSubtypeId.DomainTransfer ->
            "https://wordpress.com/domains/manage/all/$domain/transfer/in/$siteSlug"
        is DomainSubtypeId.Other ->
            if ((subtype.id as DomainSubtypeId.Other).v1 == REDIRECT_SUBTYPE) {
                "https://wordpress.com/domains/manage/all/$domain/redirect/$siteSlug"
            } else {
                "https://wordpress.com/domains/manage/all/$domain/edit/$siteSlug"
            }
        else ->
            "https://wordpress.com/domains/manage/all/$domain/edit/$siteSlug"
    }
}
