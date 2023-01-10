package org.wordpress.android.ui.domains

import org.wordpress.android.fluxc.model.SiteModel

sealed class DomainRegistrationNavigationAction {
    object OpenDomainSuggestions : DomainRegistrationNavigationAction()

    data class OpenDomainRegistrationCheckout(val site: SiteModel, val details: DomainProductDetails) :
        DomainRegistrationNavigationAction()

    data class OpenDomainRegistrationDetails(val details: DomainProductDetails) :
        DomainRegistrationNavigationAction()

    data class OpenDomainRegistrationResult(val event: DomainRegistrationCompletedEvent) :
        DomainRegistrationNavigationAction()

    data class FinishDomainRegistration(val event: DomainRegistrationCompletedEvent) :
        DomainRegistrationNavigationAction()
}
