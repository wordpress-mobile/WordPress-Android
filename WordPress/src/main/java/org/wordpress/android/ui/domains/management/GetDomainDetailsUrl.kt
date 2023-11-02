package org.wordpress.android.ui.domains.management

import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain

fun AllDomainsDomain.getDomainDetailsUrl(): String? =
    if (domain.isNullOrEmpty() || siteSlug.isNullOrEmpty()) null else when (type) {
        "transfer" -> "https://wordpress.com/domains/manage/all/$domain/transfer/in/$siteSlug"
        "redirect" -> "https://wordpress.com/domains/manage/all/$domain/redirect/$siteSlug"
        else -> "https://wordpress.com/domains/manage/all/$domain/edit/$siteSlug"
    }
