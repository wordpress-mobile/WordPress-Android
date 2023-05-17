package org.wordpress.android.ui.domains

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.DOMAIN_PURCHASE
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.FinishDomainRegistration
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationCheckout
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationDetails
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationResult
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainSuggestions
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class DomainRegistrationMainViewModel @Inject constructor(
    private val analyticsTracker: DomainsRegistrationTracker
) : ViewModel() {
    private var isStarted: Boolean = false

    private lateinit var site: SiteModel
    private lateinit var domainRegistrationPurpose: DomainRegistrationPurpose

    private val _onNavigation = MutableLiveData<Event<DomainRegistrationNavigationAction>>()
    val onNavigation: LiveData<Event<DomainRegistrationNavigationAction>> = _onNavigation

    fun start(site: SiteModel, domainRegistrationPurpose: DomainRegistrationPurpose) {
        if (isStarted) {
            return
        }

        this.site = site
        this.domainRegistrationPurpose = domainRegistrationPurpose

        _onNavigation.value = Event(OpenDomainSuggestions)

        isStarted = true
    }

    fun selectDomain(domainProductDetails: DomainProductDetails) {
        _onNavigation.value = when (domainRegistrationPurpose) {
            DOMAIN_PURCHASE -> {
                analyticsTracker.trackDomainsRegistrationFormViewed()
                Event(OpenDomainRegistrationCheckout(site, domainProductDetails))
            }
            else -> {
                analyticsTracker.trackDomainCreditNameSelected()
                Event(OpenDomainRegistrationDetails(domainProductDetails))
            }
        }
    }

    fun completeDomainRegistration(event: DomainRegistrationCompletedEvent) {
        // Called on checkout result
        _onNavigation.value = when (domainRegistrationPurpose) {
            CTA_DOMAIN_CREDIT_REDEMPTION, DOMAIN_PURCHASE -> {
                analyticsTracker.trackDomainsRegistrationFormSubmitted()
                Event(OpenDomainRegistrationResult(event))
            }
            else -> Event(FinishDomainRegistration(event))
        }
    }

    fun finishDomainRegistration(event: DomainRegistrationCompletedEvent) {
        // In Menu→ Domains this gets called either on continue btn click or on back press
        analyticsTracker.trackDomainsPurchaseDomainSuccess(isSiteCreation = false)
        _onNavigation.value = Event(FinishDomainRegistration(event))
    }
}
