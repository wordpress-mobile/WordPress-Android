package org.wordpress.android.viewmodel.domains

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.text.TextUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TransactionAction.FETCH_SUPPORTED_COUNTRIES
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.generated.TransactionActionBuilder
import org.wordpress.android.fluxc.model.DomainContactModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedStateResponse
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.SupportedDomainCountry
import org.wordpress.android.fluxc.store.AccountStore.OnDomainContactFetched
import org.wordpress.android.fluxc.store.SiteStore.OnDomainSupportedStatesFetched
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.TransactionsStore
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartRedeemed
import org.wordpress.android.fluxc.store.TransactionsStore.OnSupportedCountriesFetched
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartError
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartPayload
import org.wordpress.android.ui.domains.DomainProductDetails
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class DomainRegistrationDetailsViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val transactionsStore: TransactionsStore
) : ViewModel() {
    private lateinit var site: SiteModel
    private lateinit var domainProductDetails: DomainProductDetails
    private var isStarted = false

    private var supportedCountries: List<SupportedDomainCountry>? = null
    private val _supportedStates = MutableLiveData<List<SupportedStateResponse>>()

    val stateInputEnabled: LiveData<Boolean> = Transformations.map(_supportedStates) { it?.isNotEmpty() ?: false }

    private val _selectedCountry = MutableLiveData<SupportedDomainCountry>()
    val selectedCountry: LiveData<SupportedDomainCountry>
        get() = _selectedCountry

    private val _selectedState = MutableLiveData<SupportedStateResponse>()
    val selectedState: LiveData<SupportedStateResponse>
        get() = _selectedState

    private val _showErrorMessage = SingleLiveEvent<String>()
    val showErrorMessage: LiveData<String>
        get() = _showErrorMessage

    private val _formError = SingleLiveEvent<RedeemShoppingCartError>()
    val formError: LiveData<RedeemShoppingCartError>
        get() = _formError

    private val _formProgressIndicatorVisible = MutableLiveData<Boolean>()
    val formProgressIndicatorVisible: LiveData<Boolean>
        get() = _formProgressIndicatorVisible

    private val _statesProgressIndicatorVisible = MutableLiveData<Boolean>()
    val statesProgressIndicatorVisible: LiveData<Boolean>
        get() = _statesProgressIndicatorVisible

    private val _registrationProgressIndicatorVisible = MutableLiveData<Boolean>()
    val registrationProgressIndicatorVisible: LiveData<Boolean>
        get() = _registrationProgressIndicatorVisible

    private val _domainRegistrationButtonEnabled = MutableLiveData<Boolean>()
    val domainRegistrationButtonEnabled: LiveData<Boolean>
        get() = _domainRegistrationButtonEnabled

    private val _privacyProtectionState = MutableLiveData<Boolean>()
    val privacyProtectionState: LiveData<Boolean>
        get() = _privacyProtectionState

    private val _showCountryPickerDialog = SingleLiveEvent<List<SupportedDomainCountry>>()
    val showCountryPickerDialog: LiveData<List<SupportedDomainCountry>>
        get() = _showCountryPickerDialog

    private val _showStatePickerDialog = SingleLiveEvent<List<SupportedStateResponse>>()
    val showStatePickerDialog: LiveData<List<SupportedStateResponse>>
        get() = _showStatePickerDialog

    private val _domainContactDetails = MutableLiveData<DomainContactModel>()
    val domainContactDetails: LiveData<DomainContactModel>
        get() = _domainContactDetails

    private val _handleCompletedDomainRegistration = SingleLiveEvent<Unit>()
    val handleCompletedDomainRegistration: LiveData<Unit>
        get() = _handleCompletedDomainRegistration

    private val _showTos = SingleLiveEvent<Unit>()
    val showTos: LiveData<Unit>
        get() = _showTos

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    fun start(site: SiteModel, domainProductDetails: DomainProductDetails) {
        if (isStarted) {
            return
        }
        this.site = site
        this.domainProductDetails = domainProductDetails
        fetchSupportedCountries()

        if (privacyProtectionState.value == null) {
            _privacyProtectionState.value = true
        }
        isStarted = true
    }

    private fun fetchSupportedCountries() {
        _formProgressIndicatorVisible.value = true
        dispatcher.dispatch(TransactionActionBuilder.generateNoPayloadAction(FETCH_SUPPORTED_COUNTRIES))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSupportedCountriesFetched(event: OnSupportedCountriesFetched) {
        if (event.isError) {
            _formProgressIndicatorVisible.value = false
            _showErrorMessage.value = event.error.message
            AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching supported countries")
        } else {
            supportedCountries = event.countries?.toCollection(ArrayList())
            dispatcher.dispatch(AccountActionBuilder.newFetchDomainContactAction())
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDomainContactFetched(event: OnDomainContactFetched) {
        _formProgressIndicatorVisible.value = false
        if (event.isError) {
            _showErrorMessage.value = event.error.message
            AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching domain contact details")
        } else {
            _domainContactDetails.value = event.contactModel

            if (event.contactModel != null && !TextUtils.isEmpty(event.contactModel!!.countryCode)) {
                _selectedCountry.value = supportedCountries!!.firstOrNull { it.code == event.contactModel!!.countryCode }
                _statesProgressIndicatorVisible.value = true
                _domainRegistrationButtonEnabled.value = false
                dispatcher.dispatch(SiteActionBuilder.newFetchDomainSupportedStatesAction(event.contactModel!!.countryCode))
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDomainSupportedStatesFetched(event: OnDomainSupportedStatesFetched) {
        _domainRegistrationButtonEnabled.value = true
        _statesProgressIndicatorVisible.value = false
        if (event.isError) {
            _showErrorMessage.value = event.error.message
            AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching supported countries")
        } else {
            _selectedState.value = event.supportedStates?.firstOrNull { it.code == domainContactDetails.value!!.state }
            _supportedStates.value = event.supportedStates
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShoppingCartCreated(event: OnShoppingCartCreated) {
        if (event.isError) {
            _registrationProgressIndicatorVisible.value = false
            AppLog.e(
                    T.DOMAIN_REGISTRATION,
                    "An error occurred while creating a shopping cart : " + event.error.message
            )
            _showErrorMessage.value = event.error.message
            return
        }

        dispatcher.dispatch(
                TransactionActionBuilder.newRedeemCartWithCreditsAction(
                        RedeemShoppingCartPayload(
                                event.cartDetails!!, domainContactDetails.value!!
                        )
                )
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCartRedeemed(event: OnShoppingCartRedeemed) {
        if (event.isError) {
            _registrationProgressIndicatorVisible.value = false
            _formError.value = event.error
            _showErrorMessage.value = event.error.message
            AppLog.e(
                    T.DOMAIN_REGISTRATION,
                    "An error occurred while redeeming a shopping cart : " + event.error.type +
                            " " + event.error.message
            )
            return
        }

        dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        _registrationProgressIndicatorVisible.value = false
        if (event.isError) {
            AppLog.e(
                    T.DOMAIN_REGISTRATION,
                    "An error occurred while updating site details : " + event.error.message
            )
            _showErrorMessage.value = event.error.message
        }

        _handleCompletedDomainRegistration.call()
    }

    fun onCountrySelectorClicked() {
        _showCountryPickerDialog.value = supportedCountries!!
    }

    fun onStateSelectorClicked() {
        _showStatePickerDialog.value = _supportedStates.value
    }

    fun onRegisterDomainButtonClicked() {
        _registrationProgressIndicatorVisible.value = true
        dispatcher.dispatch(
                TransactionActionBuilder.newCreateShoppingCartAction(
                        CreateShoppingCartPayload(
                                site,
                                domainProductDetails.productId,
                                domainProductDetails.domainName,
                                privacyProtectionState.value!!
                        )
                )
        )
    }

    fun onCountrySelected(country: SupportedDomainCountry) {
        if (country != _selectedCountry.value) {
            _selectedCountry.value = country
            _domainContactDetails.value = _domainContactDetails.value?.copy(countryCode = country.code, state = null)
            _selectedState.value = null
            _statesProgressIndicatorVisible.value = true
            _domainRegistrationButtonEnabled.value = false
            dispatcher.dispatch(SiteActionBuilder.newFetchDomainSupportedStatesAction(country.code))
        }
    }

    fun onStateSelected(state: SupportedStateResponse) {
        _selectedState.value = state
    }

    fun onTosLinkClicked() {
        _showTos.call()
    }

    fun onDomainContactDetailsChanged(domainContactModel: DomainContactModel) {
        if (formProgressIndicatorVisible.value == false) {
            _domainContactDetails.value = domainContactModel
        }
    }

    fun togglePrivacyProtection(isEnabled: Boolean) {
        _privacyProtectionState.value = isEnabled
    }
}
