package org.wordpress.android.ui.domains

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.DOMAIN_PURCHASE
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.FinishDomainRegistration
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationCheckout
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationDetails
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationResult
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainSuggestions
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class DomainRegistrationMainViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
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
                analyticsTracker.track(Stat.DOMAINS_REGISTRATION_FORM_VIEWED)
                Event(OpenDomainRegistrationCheckout(site, domainProductDetails))
            }
            else -> {
                analyticsTracker.track(Stat.DOMAIN_CREDIT_NAME_SELECTED)
                Event(OpenDomainRegistrationDetails(domainProductDetails))
            }
        }
    }

    fun completeDomainRegistration(event: DomainRegistrationCompletedEvent) {
        _onNavigation.value = when (domainRegistrationPurpose) {
            CTA_DOMAIN_CREDIT_REDEMPTION, DOMAIN_PURCHASE -> {
                analyticsTracker.track(Stat.DOMAINS_REGISTRATION_FORM_SUBMITTED)
                Event(OpenDomainRegistrationResult(event))
            }
            else -> Event(FinishDomainRegistration(event))
        }
    }

    @Suppress("ForbiddenComment")
    fun finishDomainRegistration(event: DomainRegistrationCompletedEvent) {
        analyticsTracker.track(Stat.DOMAINS_PURCHASE_DOMAIN_SUCCESS) // TODO: is it a success or just a back press
        _onNavigation.value = Event(FinishDomainRegistration(event))
    }
}
