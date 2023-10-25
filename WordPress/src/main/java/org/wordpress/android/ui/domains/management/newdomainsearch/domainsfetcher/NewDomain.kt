package org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher

data class NewDomain(
    val productId: Int,
    val domainPrefix: String,
    val domainSuffix: String,
    val price: String,
    val salePrice: String?
)
