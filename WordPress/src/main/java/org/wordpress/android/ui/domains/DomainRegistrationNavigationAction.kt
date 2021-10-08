package org.wordpress.android.ui.domains

sealed class DomainRegistrationNavigationAction {
    object OpenDomainSuggestions : DomainRegistrationNavigationAction()

    data class OpenDomainRegistrationDetails(val details: DomainProductDetails) :
            DomainRegistrationNavigationAction()

    data class OpenDomainRegistrationResult(val event: DomainRegistrationCompletedEvent) :
            DomainRegistrationNavigationAction()

    data class FinishDomainRegistration(val event: DomainRegistrationCompletedEvent) :
            DomainRegistrationNavigationAction()
}
