package org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher

data class ProposedDomain(
    val productId: Int,
    val domain: String,
    val price: String,
    val salePrice: String?,
    val supportsPrivacy: Boolean,
) {
    val domainSuffix: String
        get() = domain.split('.').last()

    val domainPrefix: String
        get() = domain.removeSuffix(domainSuffix)
}
