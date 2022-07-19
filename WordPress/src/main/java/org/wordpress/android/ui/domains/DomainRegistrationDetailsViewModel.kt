package org.wordpress.android.ui.domains

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker.Stat
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
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainPayload
import org.wordpress.android.fluxc.store.SiteStore.OnDomainSupportedStatesFetched
import org.wordpress.android.fluxc.store.SiteStore.OnPrimaryDomainDesignated
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.TransactionsStore
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartRedeemed
import org.wordpress.android.fluxc.store.TransactionsStore.OnSupportedCountriesFetched
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartError
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartPayload
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.DomainPhoneNumberUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

const val SITE_CHECK_DELAY_MS = 5000L
const val MAX_SITE_CHECK_TRIES = 10

class DomainRegistrationDetailsViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused") private val transactionsStore: TransactionsStore, // needed for events to work
    private val siteStore: SiteStore,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @param:Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher
) : ScopedViewModel(uiDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var domainProductDetails: DomainProductDetails

    private var isStarted = false

    private var siteCheckTries = 0

    private var supportedCountries: List<SupportedDomainCountry>? = null
    private val _supportedStates = MutableLiveData<List<SupportedStateResponse>?>()

    private val _uiState = MutableLiveData<DomainRegistrationDetailsUiState>()
    val uiState: LiveData<DomainRegistrationDetailsUiState>
        get() = _uiState

    private val _showErrorMessage = SingleLiveEvent<String>()
    val showErrorMessage: LiveData<String>
        get() = _showErrorMessage

    private val _formError = SingleLiveEvent<RedeemShoppingCartError>()
    val formError: LiveData<RedeemShoppingCartError>
        get() = _formError

    private val _showCountryPickerDialog = SingleLiveEvent<List<SupportedDomainCountry>>()
    val showCountryPickerDialog: LiveData<List<SupportedDomainCountry>>
        get() = _showCountryPickerDialog

    private val _showStatePickerDialog = SingleLiveEvent<List<SupportedStateResponse>>()
    val showStatePickerDialog: LiveData<List<SupportedStateResponse>>
        get() = _showStatePickerDialog

    private val _domainContactForm = MutableLiveData<DomainContactFormModel>()
    val domainContactForm: LiveData<DomainContactFormModel>
        get() = _domainContactForm

    private val _handleCompletedDomainRegistration = SingleLiveEvent<DomainRegistrationCompletedEvent>()
    val handleCompletedDomainRegistration: LiveData<DomainRegistrationCompletedEvent>
        get() = _handleCompletedDomainRegistration

    private val _showTos = SingleLiveEvent<Unit>()
    val showTos: LiveData<Unit>
        get() = _showTos

    data class DomainRegistrationDetailsUiState(
        val isFormProgressIndicatorVisible: Boolean = false,
        val isStateProgressIndicatorVisible: Boolean = false,
        val isRegistrationProgressIndicatorVisible: Boolean = false,
        val isDomainRegistrationButtonEnabled: Boolean = false,
        val isPrivacyProtectionEnabled: Boolean = true,
        val selectedState: SupportedStateResponse? = null,
        val selectedCountry: SupportedDomainCountry? = null,
        val isStateInputEnabled: Boolean = false
    )

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
        // default state
        _uiState.value = DomainRegistrationDetailsUiState()

        fetchSupportedCountries()

        isStarted = true
    }

    private fun fetchSupportedCountries() {
        _uiState.value = _uiState.value?.copy(isFormProgressIndicatorVisible = true)
        dispatcher.dispatch(TransactionActionBuilder.generateNoPayloadAction(FETCH_SUPPORTED_COUNTRIES))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSupportedCountriesFetched(event: OnSupportedCountriesFetched) {
        if (event.isError) {
            _uiState.value = _uiState.value?.copy(isFormProgressIndicatorVisible = false)
            _showErrorMessage.value = event.error.message
            AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching supported countries")
        } else {
            supportedCountries = event.countries?.toCollection(ArrayList())
            dispatcher.dispatch(AccountActionBuilder.newFetchDomainContactAction())
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDomainContactFetched(event: OnDomainContactFetched) {
        if (event.isError) {
            _uiState.value = _uiState.value?.copy(isFormProgressIndicatorVisible = false)
            _showErrorMessage.value = event.error.message
            AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching domain contact details")
        } else {
            _domainContactForm.value = DomainContactFormModel.fromDomainContactModel(event.contactModel)
            _uiState.value = _uiState.value?.copy(isFormProgressIndicatorVisible = false)

            val countryCode = event.contactModel?.countryCode

            if (event.contactModel != null && !TextUtils.isEmpty(countryCode)) {
                _uiState.value =
                        uiState.value?.copy(
                                selectedCountry = supportedCountries?.firstOrNull {
                                    it.code == event.contactModel?.countryCode
                                },
                                isStateProgressIndicatorVisible = true,
                                isDomainRegistrationButtonEnabled = false
                        )

                // if customer does not have a phone number we will try to prefill a country code
                if (TextUtils.isEmpty(event.contactModel?.phone)) {
                    val countryCodePrefix = DomainPhoneNumberUtils.getPhoneNumberPrefix(countryCode!!)
                    _domainContactForm.value = _domainContactForm.value?.copy(
                            phoneNumberPrefix = countryCodePrefix
                    )
                }

                dispatcher.dispatch(
                        SiteActionBuilder.newFetchDomainSupportedStatesAction(event.contactModel?.countryCode)
                )
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDomainSupportedStatesFetched(event: OnDomainSupportedStatesFetched) {
        if (event.isError) {
            _uiState.value =
                    uiState.value?.copy(
                            isStateProgressIndicatorVisible = false,
                            isDomainRegistrationButtonEnabled = true
                    )
            event.error?.message?.let { _showErrorMessage.value = it }
            AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching supported countries")
        } else {
            _uiState.value = uiState.value?.copy(
                    selectedState = event.supportedStates?.firstOrNull { it.code == domainContactForm.value?.state },
                    isStateProgressIndicatorVisible = false,
                    isDomainRegistrationButtonEnabled = true,
                    isStateInputEnabled = !event.supportedStates.isNullOrEmpty()
            )
            _supportedStates.value = event.supportedStates
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShoppingCartCreated(event: OnShoppingCartCreated) {
        if (event.isError) {
            _uiState.value = uiState.value?.copy(isRegistrationProgressIndicatorVisible = false)
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
                                event.cartDetails!!,
                                DomainContactFormModel.toDomainContactModel(domainContactForm.value)!!
                        )
                )
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCartRedeemed(event: OnShoppingCartRedeemed) {
        if (event.isError) {
            analyticsTracker.track(Stat.AUTOMATED_TRANSFER_CUSTOM_DOMAIN_PURCHASE_FAILED)
            _uiState.value = uiState.value?.copy(isRegistrationProgressIndicatorVisible = false)
            _formError.value = event.error
            _showErrorMessage.value = event.error.message
            AppLog.e(
                    T.DOMAIN_REGISTRATION,
                    "An error occurred while redeeming a shopping cart : " + event.error.type +
                            " " + event.error.message
            )
            return
        }

        // after cart is redeemed, wait for a bit before manually setting domain as primary
        launch {
            delay(SITE_CHECK_DELAY_MS)
            dispatcher.dispatch(
                    SiteActionBuilder.newDesignatePrimaryDomainAction(
                            DesignatePrimaryDomainPayload(
                                    site,
                                    domainProductDetails.domainName
                            )
                    )
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPrimaryDomainDesignated(event: OnPrimaryDomainDesignated) {
        if (event.isError) { // in case of error we notify used and proceed to next step
            event.error?.message?.let { _showErrorMessage.value = it }
            AppLog.e(
                    T.DOMAIN_REGISTRATION,
                    "An error occurred while redeeming a shopping cart : " + event.error.type +
                            " " + event.error.message
            )
        }

        dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.isError) {
            AppLog.e(
                    T.DOMAIN_REGISTRATION,
                    "An error occurred while updating site details : " + event.error.message
            )
            event.error?.message?.let { _showErrorMessage.value = it }
            finishRegistration()
            return
        }

        val updatedSite = siteStore.getSiteByLocalId(site.id)

        // New domain is not is not reflected in SiteModel yet, try refreshing a site until we get it
        if (updatedSite?.url?.endsWith(".wordpress.com") == true && siteCheckTries < MAX_SITE_CHECK_TRIES) {
            AppLog.v(
                    T.DOMAIN_REGISTRATION,
                    "Newly registered domain is still not reflected in site model. Refreshing site model..."
            )
            launch {
                delay(SITE_CHECK_DELAY_MS)
                dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site))
                siteCheckTries++
            }
        } else {
            // Everything looks good! Let's wait a bit before moving on
            launch {
                AppLog.v(T.DOMAIN_REGISTRATION, "Finishing registration...")
                delay(SITE_CHECK_DELAY_MS)
                finishRegistration()
            }
        }
    }

    private fun finishRegistration() {
        _uiState.value = uiState.value?.copy(isRegistrationProgressIndicatorVisible = false)
        _handleCompletedDomainRegistration.postValue(
                DomainRegistrationCompletedEvent(
                        domainProductDetails.domainName,
                        domainContactForm.value!!.email!!
                )
        )
    }

    fun onCountrySelectorClicked() {
        _showCountryPickerDialog.value = supportedCountries!!
    }

    fun onStateSelectorClicked() {
        _showStatePickerDialog.value = _supportedStates.value
    }

    fun onRegisterDomainButtonClicked() {
        _uiState.value = uiState.value?.copy(isRegistrationProgressIndicatorVisible = true)
        _domainContactForm.value = _domainContactForm.value?.copy(
                countryCode = uiState.value?.selectedCountry?.code,
                state = uiState.value?.selectedState?.code
        )
        dispatcher.dispatch(
                TransactionActionBuilder.newCreateShoppingCartAction(
                        CreateShoppingCartPayload(
                                site,
                                domainProductDetails.productId,
                                domainProductDetails.domainName,
                                uiState.value?.isPrivacyProtectionEnabled!!
                        )
                )
        )
    }

    fun onCountrySelected(country: SupportedDomainCountry) {
        if (country != uiState.value?.selectedCountry) {
            _supportedStates.value = null
            _uiState.value =
                    uiState.value?.copy(
                            selectedCountry = country,
                            selectedState = null,
                            isStateProgressIndicatorVisible = true,
                            isDomainRegistrationButtonEnabled = false,
                            isStateInputEnabled = false
                    )

            _domainContactForm.value = _domainContactForm.value?.copy(
                    countryCode = country.code,
                    state = null,
                    phoneNumberPrefix = DomainPhoneNumberUtils.getPhoneNumberPrefix(country.code)
            )
            dispatcher.dispatch(SiteActionBuilder.newFetchDomainSupportedStatesAction(country.code))
        }
    }

    fun onStateSelected(state: SupportedStateResponse) {
        _uiState.value = uiState.value?.copy(selectedState = state)
    }

    fun onTosLinkClicked() {
        _showTos.call()
    }

    fun onDomainContactDetailsChanged(domainContacFormModel: DomainContactFormModel) {
        val isFormBusy = uiState.value?.isFormProgressIndicatorVisible == true ||
                uiState.value?.isRegistrationProgressIndicatorVisible == true

        if (!isFormBusy) {
            _domainContactForm.value = domainContacFormModel
        }
    }

    fun togglePrivacyProtection(isEnabled: Boolean) {
        _uiState.value = uiState.value?.copy(isPrivacyProtectionEnabled = isEnabled)
    }

    data class DomainContactFormModel(
        val firstName: String?,
        val lastName: String?,
        val organization: String?,
        val addressLine1: String?,
        val addressLine2: String?,
        val postalCode: String?,
        val city: String?,
        val state: String?,
        val countryCode: String?,
        val email: String?,
        val phoneNumberPrefix: String?,
        val phoneNumber: String?
    ) {
        companion object {
            fun toDomainContactModel(domainContactFormModel: DomainContactFormModel?): DomainContactModel? {
                if (domainContactFormModel == null) {
                    return null
                }

                return DomainContactModel(
                        firstName = domainContactFormModel.firstName,
                        lastName = domainContactFormModel.lastName,
                        organization = domainContactFormModel.organization,
                        addressLine1 = domainContactFormModel.addressLine1,
                        addressLine2 = domainContactFormModel.addressLine2,
                        postalCode = domainContactFormModel.postalCode,
                        city = domainContactFormModel.city,
                        state = domainContactFormModel.state,
                        countryCode = domainContactFormModel.countryCode,
                        email = domainContactFormModel.email,
                        phone = DomainPhoneNumberUtils.formatPhoneNumberandPrefix(
                                domainContactFormModel.phoneNumberPrefix,
                                domainContactFormModel.phoneNumber
                        ),
                        fax = null
                )
            }

            fun fromDomainContactModel(domainContactModel: DomainContactModel?): DomainContactFormModel? {
                if (domainContactModel == null) {
                    return null
                }

                return DomainContactFormModel(
                        firstName = domainContactModel.firstName,
                        lastName = domainContactModel.lastName,
                        organization = domainContactModel.organization,
                        addressLine1 = domainContactModel.addressLine1,
                        addressLine2 = domainContactModel.addressLine2,
                        postalCode = domainContactModel.postalCode,
                        city = domainContactModel.city,
                        state = domainContactModel.state,
                        countryCode = domainContactModel.countryCode,
                        email = domainContactModel.email,
                        phoneNumberPrefix = DomainPhoneNumberUtils.getPhoneNumberPrefixFromFullPhoneNumber(
                                domainContactModel.phone
                        ),
                        phoneNumber = DomainPhoneNumberUtils.getPhoneNumberWithoutPrefix(domainContactModel.phone)
                )
            }
        }
    }
}
