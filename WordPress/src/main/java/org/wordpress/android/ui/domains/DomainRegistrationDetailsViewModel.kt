package org.wordpress.android.ui.domains

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.domains.usecases.CreateCartResult
import org.wordpress.android.ui.domains.usecases.CreateCartUseCase
import org.wordpress.android.ui.domains.usecases.DesignatePrimaryDomainResult
import org.wordpress.android.ui.domains.usecases.DesignatePrimaryDomainUseCase
import org.wordpress.android.ui.domains.usecases.DomainContactField
import org.wordpress.android.ui.domains.usecases.DomainContactResult
import org.wordpress.android.ui.domains.usecases.FetchDomainContactUseCase
import org.wordpress.android.ui.domains.usecases.FetchSupportedCountriesUseCase
import org.wordpress.android.ui.domains.usecases.FetchSupportedStatesUseCase
import org.wordpress.android.ui.domains.usecases.RedeemCartResult
import org.wordpress.android.ui.domains.usecases.RedeemCartUseCase
import org.wordpress.android.ui.domains.usecases.SupportedCountriesResult
import org.wordpress.android.ui.domains.usecases.SupportedStatesResult
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.DomainPhoneNumberUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import uniffi.wp_api.DomainContactInformation
import uniffi.wp_api.ShoppingCart
import uniffi.wp_api.SupportedCountry
import uniffi.wp_api.SupportedState
import javax.inject.Inject
import javax.inject.Named

const val SITE_CHECK_DELAY_MS = 5000L
const val MAX_SITE_CHECK_TRIES = 10

class DomainRegistrationDetailsViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val fetchSupportedCountriesUseCase: FetchSupportedCountriesUseCase,
    private val fetchDomainContactUseCase: FetchDomainContactUseCase,
    private val fetchSupportedStatesUseCase: FetchSupportedStatesUseCase,
    private val createCartUseCase: CreateCartUseCase,
    private val redeemCartUseCase: RedeemCartUseCase,
    private val designatePrimaryDomainUseCase: DesignatePrimaryDomainUseCase,
    private val resourceProvider: ResourceProvider,
    @param:Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher
) : ScopedViewModel(uiDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var domainProductDetails: DomainProductDetails

    private var isStarted = false

    private var siteCheckTries = 0

    private var supportedStatesJob: Job? = null

    private var supportedCountries: List<SupportedCountry>? = null

    val countriesForPicker: List<SupportedCountry>
        get() = supportedCountries.orEmpty()
    private val _supportedStates = MutableLiveData<List<SupportedState>?>()

    val statesForPicker: List<SupportedState>
        get() = _supportedStates.value.orEmpty()

    private val _uiState = MutableLiveData<DomainRegistrationDetailsUiState>()
    val uiState: LiveData<DomainRegistrationDetailsUiState>
        get() = _uiState

    private val _showErrorMessage = SingleLiveEvent<String>()
    val showErrorMessage: LiveData<String>
        get() = _showErrorMessage

    private val _formError = SingleLiveEvent<DomainContactFieldError>()
    val formError: LiveData<DomainContactFieldError>
        get() = _formError

    private val _showCountryPickerDialog = SingleLiveEvent<List<SupportedCountry>>()
    val showCountryPickerDialog: LiveData<List<SupportedCountry>>
        get() = _showCountryPickerDialog

    private val _showStatePickerDialog = SingleLiveEvent<List<SupportedState>>()
    val showStatePickerDialog: LiveData<List<SupportedState>>
        get() = _showStatePickerDialog

    private val _domainContactForm = MutableLiveData<DomainContactFormModel>()
    val domainContactForm: LiveData<DomainContactFormModel>
        get() = _domainContactForm

    private val _handleCompletedDomainRegistration = SingleLiveEvent<DomainRegistrationCompletedEvent>()
    val handleCompletedDomainRegistration: LiveData<DomainRegistrationCompletedEvent>
        get() = _handleCompletedDomainRegistration

    private val _showTos = SingleLiveEvent<Unit?>()
    val showTos: LiveData<Unit?>
        get() = _showTos

    data class DomainRegistrationDetailsUiState(
        val isFormProgressIndicatorVisible: Boolean = false,
        val isStateProgressIndicatorVisible: Boolean = false,
        val isRegistrationProgressIndicatorVisible: Boolean = false,
        val isDomainRegistrationButtonEnabled: Boolean = false,
        val isPrivacyProtectionEnabled: Boolean = true,
        val selectedState: SupportedState? = null,
        val selectedCountry: SupportedCountry? = null,
        val isStateInputEnabled: Boolean = false
    )

    /** A contact field the server rejected, and what it said about it. */
    data class DomainContactFieldError(
        val field: DomainContactField,
        val message: String?
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

    private fun showError(message: String?, isDeviceOffline: Boolean) {
        _showErrorMessage.value = when {
            isDeviceOffline -> resourceProvider.getString(R.string.error_network_connection)
            !message.isNullOrBlank() -> message
            else -> resourceProvider.getString(R.string.request_failed_message)
        }
    }

    private fun fetchSupportedCountries() = launch {
        _uiState.value = _uiState.value?.copy(isFormProgressIndicatorVisible = true)
        when (val result = fetchSupportedCountriesUseCase.execute()) {
            is SupportedCountriesResult.Error -> {
                _uiState.value = _uiState.value?.copy(isFormProgressIndicatorVisible = false)
                showError(result.message, result.isDeviceOffline)
                AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching supported countries")
            }
            is SupportedCountriesResult.Success -> {
                supportedCountries = result.countries
                fetchDomainContact()
            }
        }
    }

    private suspend fun fetchDomainContact() {
        when (val result = fetchDomainContactUseCase.execute()) {
            is DomainContactResult.Error -> {
                _uiState.value = _uiState.value?.copy(isFormProgressIndicatorVisible = false)
                showError(result.message, result.isDeviceOffline)
                AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching domain contact details")
            }
            is DomainContactResult.Success -> {
                val contact = result.contact
                _domainContactForm.value = DomainContactFormModel.fromDomainContactInformation(contact)
                _uiState.value = _uiState.value?.copy(isFormProgressIndicatorVisible = false)

                val countryCode = contact.countryCode

                if (!countryCode.isNullOrEmpty()) {
                    _uiState.value =
                        uiState.value?.copy(
                            selectedCountry = supportedCountries?.firstOrNull {
                                it.code == countryCode
                            },
                            isStateProgressIndicatorVisible = true,
                            isDomainRegistrationButtonEnabled = false
                        )

                    // if customer does not have a phone number we will try to prefill a country code
                    if (TextUtils.isEmpty(contact.phone)) {
                        val countryCodePrefix = DomainPhoneNumberUtils.getPhoneNumberPrefix(countryCode)
                        _domainContactForm.value = _domainContactForm.value?.copy(
                            phoneNumberPrefix = countryCodePrefix
                        )
                    }

                    fetchSupportedStates(countryCode)
                }
            }
        }
    }

    /**
     * Cancels a states request still in flight, so that picking a country
     * while the one before it is still loading cannot leave the earlier
     * country's states standing against the later country.
     */
    private fun fetchSupportedStates(countryCode: String) {
        supportedStatesJob?.cancel()
        supportedStatesJob = launch { loadSupportedStates(countryCode) }
    }

    private suspend fun loadSupportedStates(countryCode: String) {
        when (val result = fetchSupportedStatesUseCase.execute(countryCode)) {
            is SupportedStatesResult.Error -> {
                _uiState.value =
                    uiState.value?.copy(
                        isStateProgressIndicatorVisible = false,
                        isDomainRegistrationButtonEnabled = true
                    )
                showError(result.message, result.isDeviceOffline)
                AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching supported states")
            }
            is SupportedStatesResult.Success -> {
                _uiState.value = uiState.value?.copy(
                    selectedState = result.states.firstOrNull { it.code == domainContactForm.value?.state },
                    isStateProgressIndicatorVisible = false,
                    isDomainRegistrationButtonEnabled = true,
                    isStateInputEnabled = result.states.isNotEmpty()
                )
                _supportedStates.value = result.states
            }
        }
    }

    private suspend fun createCart() {
        val result = createCartUseCase.execute(
            site,
            domainProductDetails.productId,
            domainProductDetails.domainName,
            uiState.value?.isPrivacyProtectionEnabled!!,
            isTemporary = true
        )
        when (result) {
            is CreateCartResult.Error -> {
                _uiState.value = uiState.value?.copy(isRegistrationProgressIndicatorVisible = false)
                showError(result.message, result.isDeviceOffline)
                AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while creating a shopping cart")
            }
            is CreateCartResult.Success -> redeemCart(result.cart)
        }
    }

    private suspend fun redeemCart(cart: ShoppingCart) {
        val contact = DomainContactFormModel.toDomainContactInformation(domainContactForm.value)!!
        when (val result = redeemCartUseCase.execute(cart, contact)) {
            is RedeemCartResult.Error -> {
                analyticsTracker.track(Stat.AUTOMATED_TRANSFER_CUSTOM_DOMAIN_PURCHASE_FAILED)
                _uiState.value = uiState.value?.copy(isRegistrationProgressIndicatorVisible = false)
                result.field?.let { _formError.value = DomainContactFieldError(it, result.message) }
                showError(result.message, result.isDeviceOffline)
                AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while redeeming a shopping cart")
            }
            is RedeemCartResult.PartialFailure -> {
                analyticsTracker.track(Stat.AUTOMATED_TRANSFER_CUSTOM_DOMAIN_PURCHASE_FAILED)
                _uiState.value = uiState.value?.copy(isRegistrationProgressIndicatorVisible = false)
                _showErrorMessage.value = resourceProvider.getString(
                    R.string.domain_registration_purchase_incomplete,
                    domainProductDetails.domainName
                )
                AppLog.e(
                    T.DOMAIN_REGISTRATION,
                    "The domain was paid for but could not be registered"
                )
            }
            is RedeemCartResult.Success -> {
                // after cart is redeemed, wait for a bit before manually setting domain as primary
                delay(SITE_CHECK_DELAY_MS)
                designatePrimaryDomain()
            }
        }
    }

    private suspend fun designatePrimaryDomain() {
        val result = designatePrimaryDomainUseCase.execute(site, domainProductDetails.domainName)
        // A failed designation is reported to the customer, but the registration carries on
        if (result is DesignatePrimaryDomainResult.Error) {
            showError(result.message, result.isDeviceOffline)
            AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while designating the primary domain")
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
        // The field is tappable while the countries load and after that load fails,
        // so there is not always a list to show.
        supportedCountries?.let { _showCountryPickerDialog.value = it }
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
        launch { createCart() }
    }

    fun onCountrySelected(country: SupportedCountry) {
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
            fetchSupportedStates(country.code)
        }
    }

    fun onStateSelected(state: SupportedState) {
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
            fun toDomainContactInformation(
                domainContactFormModel: DomainContactFormModel?
            ): DomainContactInformation? {
                if (domainContactFormModel == null) {
                    return null
                }

                return DomainContactInformation(
                    firstName = domainContactFormModel.firstName,
                    lastName = domainContactFormModel.lastName,
                    organization = domainContactFormModel.organization,
                    address1 = domainContactFormModel.addressLine1,
                    address2 = domainContactFormModel.addressLine2,
                    postalCode = domainContactFormModel.postalCode,
                    city = domainContactFormModel.city,
                    state = domainContactFormModel.state,
                    countryCode = domainContactFormModel.countryCode,
                    email = domainContactFormModel.email,
                    phone = DomainPhoneNumberUtils.formatPhoneNumberandPrefix(
                        domainContactFormModel.phoneNumberPrefix,
                        domainContactFormModel.phoneNumber
                    ),
                    fax = null,
                    extra = null
                )
            }

            fun fromDomainContactInformation(contact: DomainContactInformation) = DomainContactFormModel(
                firstName = contact.firstName,
                lastName = contact.lastName,
                organization = contact.organization,
                addressLine1 = contact.address1,
                addressLine2 = contact.address2,
                postalCode = contact.postalCode,
                city = contact.city,
                state = contact.state,
                countryCode = contact.countryCode,
                email = contact.email,
                phoneNumberPrefix = DomainPhoneNumberUtils.getPhoneNumberPrefixFromFullPhoneNumber(
                    contact.phone
                ),
                phoneNumber = DomainPhoneNumberUtils.getPhoneNumberWithoutPrefix(contact.phone)
            )
        }
    }
}
