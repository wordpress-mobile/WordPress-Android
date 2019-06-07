package org.wordpress.android.ui.domains

interface DomainRegistrationStepsListener {
    fun onDomainSelected(domainProductDetails: DomainProductDetails)
    fun onDomainRegistered(domainName: String)
}
