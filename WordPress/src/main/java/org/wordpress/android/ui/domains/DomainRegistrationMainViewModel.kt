package org.wordpress.android.ui.domains

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class DomainRegistrationMainViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {
    private val _domainSuggestionsVisible = MutableLiveData<Boolean>()
    val domainSuggestionsVisible: LiveData<Boolean> = _domainSuggestionsVisible

    private val _selectedDomain = MutableLiveData<DomainProductDetails>()
    val selectedDomain: LiveData<DomainProductDetails> = _selectedDomain

    private val _domainRegistrationCompleted = MutableLiveData<DomainRegistrationCompletedEvent>()
    val domainRegistrationCompleted: LiveData<DomainRegistrationCompletedEvent> = _domainRegistrationCompleted

    private var isStarted: Boolean = false

    private lateinit var site: SiteModel
    private lateinit var domainRegistrationPurpose: DomainRegistrationPurpose

    fun start(site: SiteModel, domainRegistrationPurpose: DomainRegistrationPurpose) {
        if (isStarted) {
            return
        }

        this.site = site
        this.domainRegistrationPurpose = domainRegistrationPurpose

        _domainSuggestionsVisible.value = true

        isStarted = true
    }

    fun selectDomain(domainProductDetails: DomainProductDetails) {
        analyticsTracker.track(Stat.DOMAIN_CREDIT_NAME_SELECTED)
        _domainSuggestionsVisible.value = false
        _selectedDomain.value = domainProductDetails
    }

    fun completeDomainRegistration(domainRegistrationCompletedEvent: DomainRegistrationCompletedEvent) {
        _selectedDomain.value = null
        _domainRegistrationCompleted.value = domainRegistrationCompletedEvent
    }
}
