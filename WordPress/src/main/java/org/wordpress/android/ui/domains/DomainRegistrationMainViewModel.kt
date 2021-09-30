package org.wordpress.android.ui.domains

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.TransactionActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TransactionsStore
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.SiteDomainsFeatureConfig
import javax.inject.Inject

class DomainRegistrationMainViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val siteDomainsFeatureConfig: SiteDomainsFeatureConfig,
    private val dispatcher: Dispatcher,
    private val transactionsStore: TransactionsStore, // needed for events to work
) : ViewModel() {
    private val _domainSuggestionsVisible = MutableLiveData<Boolean>()
    val domainSuggestionsVisible: LiveData<Boolean> = _domainSuggestionsVisible

    private val _selectedDomain = MutableLiveData<DomainProductDetails>()
    val selectedDomain: LiveData<DomainProductDetails> = _selectedDomain

    private val _domainRegistrationCompleted = MutableLiveData<DomainRegistrationCompletedEvent>()
    val domainRegistrationCompleted: LiveData<DomainRegistrationCompletedEvent> = _domainRegistrationCompleted

    private val _cartCreated = MutableLiveData<CartCreatedEvent>()
    val cartCreated: LiveData<CartCreatedEvent> = _cartCreated

    val isSiteDomainsEnabled = siteDomainsFeatureConfig.isEnabled()
    private lateinit var site: SiteModel

    private var isStarted: Boolean = false
    fun start(site: SiteModel) {
        if (isStarted) {
            return
        }
        this.site = site
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

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    fun createCart(domainProductDetails: DomainProductDetails, isPrivacyProtectionEnabled: Boolean = true) {
        AppLog.d(T.DOMAIN_REGISTRATION, "Create cart: $domainProductDetails")
        dispatcher.dispatch(
                TransactionActionBuilder.newCreateShoppingCartAction(
                        CreateShoppingCartPayload(
                                site,
                                domainProductDetails.productId,
                                domainProductDetails.domainName,
                                isPrivacyProtectionEnabled
                        )
                )
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShoppingCartCreated(event: OnShoppingCartCreated) {
        if (event.isError) {
            AppLog.e(T.DOMAIN_REGISTRATION, "Error creating cart: ${event.error.message}")
            // TODO Handle shopping creation failure
        } else {
            AppLog.d(T.DOMAIN_REGISTRATION, "Cart created: ${event.cartDetails}")
            _cartCreated.value = CartCreatedEvent(site)
        }
    }

    fun handleSuccessfulRegistration() {
        completeDomainRegistration(DomainRegistrationCompletedEvent(_selectedDomain.value?.domainName.orEmpty(), ""))
    }

    data class CartCreatedEvent(val site: SiteModel)
}
