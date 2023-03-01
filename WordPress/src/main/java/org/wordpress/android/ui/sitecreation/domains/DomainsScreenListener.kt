package org.wordpress.android.ui.sitecreation.domains

interface DomainsScreenListener {
    fun onDomainSelected(domain: DomainModel)
}

data class DomainModel(
    val domainName: String,
    val isFree: Boolean,
    val cost: String,
    val productId: Int,
)
