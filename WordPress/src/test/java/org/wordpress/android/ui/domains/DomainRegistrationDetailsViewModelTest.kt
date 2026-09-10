package org.wordpress.android.ui.domains

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.action.TransactionAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.DomainContactModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse.Extra
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse.Product
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainError
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainErrorType
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainPayload
import org.wordpress.android.fluxc.store.SiteStore.OnPrimaryDomainDesignated
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.fluxc.store.TransactionsStore
import org.wordpress.android.fluxc.store.TransactionsStore.CreateCartErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartError
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartWithDomainAndPlanPayload
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartRedeemed
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartError
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.PHONE
import org.wordpress.android.ui.domains.DomainRegistrationDetailsViewModel.DomainContactFormModel
import org.wordpress.android.ui.domains.DomainRegistrationDetailsViewModel.DomainRegistrationDetailsUiState
import org.wordpress.android.ui.domains.usecases.DomainContactResult
import org.wordpress.android.ui.domains.usecases.FetchDomainContactUseCase
import org.wordpress.android.ui.domains.usecases.FetchSupportedCountriesUseCase
import org.wordpress.android.ui.domains.usecases.FetchSupportedStatesUseCase
import org.wordpress.android.ui.domains.usecases.SupportedCountriesResult
import org.wordpress.android.ui.domains.usecases.SupportedStatesResult
import org.wordpress.android.viewmodel.ResourceProvider
import uniffi.wp_api.DomainContactInformation
import uniffi.wp_api.SupportedCountry
import uniffi.wp_api.SupportedState
import org.wordpress.android.util.NoDelayCoroutineDispatcher
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class DomainRegistrationDetailsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var transactionsStore: TransactionsStore

    @Mock
    private lateinit var siteStore: SiteStore

    @Mock
    private lateinit var dispatcher: Dispatcher

    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Mock
    private lateinit var fetchSupportedCountriesUseCase: FetchSupportedCountriesUseCase

    @Mock
    private lateinit var fetchDomainContactUseCase: FetchDomainContactUseCase

    @Mock
    private lateinit var fetchSupportedStatesUseCase: FetchSupportedStatesUseCase

    @Mock
    private lateinit var resourceProvider: ResourceProvider
    private var site: SiteModel = SiteModel()

    @Mock
    private lateinit var domainContactDetailsObserver: Observer<DomainContactFormModel>

    @Mock
    private lateinit var countryPickerDialogObserver: Observer<List<SupportedCountry>>

    @Mock
    private lateinit var statePickerDialogObserver: Observer<List<SupportedState>>

    @Mock
    private lateinit var tosLinkObserver: Observer<Unit?>

    @Mock
    private lateinit var completedDomainRegistrationObserver: Observer<DomainRegistrationCompletedEvent>

    @Mock
    private lateinit var errorMessageObserver: Observer<String>

    private val uiStateResults = mutableListOf<DomainRegistrationDetailsUiState>()

    private lateinit var viewModel: DomainRegistrationDetailsViewModel

    private val primaryCountry = testSupportedCountry("US", "United States")
    private val secondaryCountry = testSupportedCountry("AU", "Australia")
    private val countries = listOf(primaryCountry, secondaryCountry)

    private val primaryState = SupportedState("CA", "California")
    private val secondaryState = SupportedState("NSW", "New South Wales")
    private val states = listOf(primaryState, secondaryState)

    private val siteId = 1234L
    private val productId = 76
    private val testDomainName = "testdomain.blog"
    private val cartId = "123"

    private val domainContactModel = DomainContactModel(
        "John",
        "Smith",
        "",
        "Street 1",
        "Apt 1",
        "10018",
        "First City",
        "CA",
        "US",
        "email@wordpress.org",
        "+1.3124567890",
        null
    )

    private val domainContactInformation = DomainContactInformation(
        firstName = "John",
        lastName = "Smith",
        organization = "",
        address1 = "Street 1",
        address2 = "Apt 1",
        postalCode = "10018",
        city = "First City",
        state = "CA",
        countryCode = "US",
        email = "email@wordpress.org",
        phone = "+1.3124567890",
        fax = null,
        extra = null,
    )

    private val domainContactFormModel = DomainContactFormModel(
        "John",
        "Smith",
        "",
        "Street 1",
        "Apt 1",
        "10018",
        "First City",
        "CA",
        "US",
        "email@wordpress.org",
        "1",
        "3124567890"
    )

    private val domainRegistrationCompletedEvent = DomainRegistrationCompletedEvent(
        "testdomain.blog",
        "email@wordpress.org"
    )

    private val shoppingCartCreateError = CreateShoppingCartError(GENERIC_ERROR, "Error Creating Cart")
    private val shoppingCartRedeemError = RedeemShoppingCartError(PHONE, "Wrong phone number")
    private val siteChangedError = SiteError(SiteErrorType.GENERIC_ERROR, "Error fetching site")
    private val primaryDomainError = DesignatePrimaryDomainError(
        DesignatePrimaryDomainErrorType.GENERIC_ERROR,
        "Error designating primary domain"
    )
    private val domainContactInformationFetchErrorMessage = "Error fetching domain contact information"
    private val domainSupportedStatesFetchErrorMessage = "Error fetching domain supported states"
    private val fetchSupportedCountriesErrorMessage = "Error fetching countries"
    private val offlineMessage = "Check your network connection and try again"
    private val requestFailedMessage = "There was a problem handling the request. Please try again later."

    private val createShoppingCartResponse = CreateShoppingCartResponse(
        siteId.toInt(),
        cartId,
        listOf(Product(productId, testDomainName, Extra(privacy = true)))
    )

    private val domainProductDetails = DomainProductDetails(productId, testDomainName)

    @Before
    fun setUp() {
        site.siteId = siteId
        site.url = testDomainName

        whenever(siteStore.getSiteByLocalId(any())).doReturn(site)

        viewModel = DomainRegistrationDetailsViewModel(
            dispatcher,
            transactionsStore,
            siteStore,
            analyticsTracker,
            fetchSupportedCountriesUseCase,
            fetchDomainContactUseCase,
            fetchSupportedStatesUseCase,
            resourceProvider,
            NoDelayCoroutineDispatcher()
        )
        // Setting up chain of actions
        test {
            setupFetchSupportedCountries(false)
            setupFetchDomainContact(false)
            setupFetchStates(false)
        }
        setupCreateShoppingCartDispatcher(false)
        setupRedeemShoppingCartDispatcher(false)
        setupFetchSiteDispatcher(false)
        setupPrimaryDomainDispatcher(false)

        uiStateResults.clear()
        viewModel.uiState.observeForever { if (it != null) uiStateResults.add(it) }
        viewModel.domainContactForm.observeForever(domainContactDetailsObserver)
        viewModel.showCountryPickerDialog.observeForever(countryPickerDialogObserver)
        viewModel.showStatePickerDialog.observeForever(statePickerDialogObserver)
        viewModel.showTos.observeForever(tosLinkObserver)
        viewModel.handleCompletedDomainRegistration.observeForever(completedDomainRegistrationObserver)
        viewModel.showErrorMessage.observeForever(errorMessageObserver)
    }

    @Test
    fun contactDetailsPreload() = test {
        viewModel.start(site, domainProductDetails)

        verify(dispatcher, never()).dispatch(any())
        verify(fetchSupportedStatesUseCase).execute(primaryCountry.code)

        assertThat(uiStateResults.size).isEqualTo(5)

        val initialState = uiStateResults[0]

        assertThat(initialState.isFormProgressIndicatorVisible).isEqualTo(false)
        assertThat(initialState.isDomainRegistrationButtonEnabled).isEqualTo(false)
        assertThat(initialState.isStateProgressIndicatorVisible).isEqualTo(false)
        assertThat(initialState.isPrivacyProtectionEnabled).isEqualTo(true)
        assertThat(initialState.isRegistrationProgressIndicatorVisible).isEqualTo(false)
        assertThat(initialState.isStateInputEnabled).isEqualTo(false)
        assertThat(initialState.selectedState).isNull()
        assertThat(initialState.selectedCountry).isNull()

        val fetchingCountriesAndDomainContactState = uiStateResults[1]

        assertThat(fetchingCountriesAndDomainContactState.isFormProgressIndicatorVisible).isEqualTo(true)
        assertThat(fetchingCountriesAndDomainContactState.isDomainRegistrationButtonEnabled).isEqualTo(false)
        assertThat(fetchingCountriesAndDomainContactState.isStateProgressIndicatorVisible).isEqualTo(false)
        assertThat(fetchingCountriesAndDomainContactState.isPrivacyProtectionEnabled).isEqualTo(true)
        assertThat(fetchingCountriesAndDomainContactState.isRegistrationProgressIndicatorVisible).isEqualTo(false)
        assertThat(fetchingCountriesAndDomainContactState.isStateInputEnabled).isEqualTo(false)
        assertThat(fetchingCountriesAndDomainContactState.selectedState).isNull()
        assertThat(fetchingCountriesAndDomainContactState.selectedCountry).isNull()

        // hiding form progress after domain contact details fetched
        val domainContactDetailsFetchedState = uiStateResults[2]

        assertThat(domainContactDetailsFetchedState.isFormProgressIndicatorVisible).isEqualTo(false)
        assertThat(domainContactDetailsFetchedState.isDomainRegistrationButtonEnabled).isEqualTo(false)
        assertThat(domainContactDetailsFetchedState.isStateProgressIndicatorVisible).isEqualTo(false)
        assertThat(domainContactDetailsFetchedState.isPrivacyProtectionEnabled).isEqualTo(true)
        assertThat(domainContactDetailsFetchedState.isRegistrationProgressIndicatorVisible).isEqualTo(false)
        assertThat(domainContactDetailsFetchedState.isStateInputEnabled).isEqualTo(false)
        assertThat(domainContactDetailsFetchedState.selectedState).isNull()
        assertThat(domainContactDetailsFetchedState.selectedCountry).isNull()

        verify(domainContactDetailsObserver).onChanged(domainContactFormModel)

        val fetchingStatesState = uiStateResults[3]

        assertThat(fetchingStatesState.isFormProgressIndicatorVisible).isEqualTo(false)
        assertThat(fetchingStatesState.isDomainRegistrationButtonEnabled).isEqualTo(false)
        assertThat(fetchingStatesState.isStateProgressIndicatorVisible).isEqualTo(true)
        assertThat(fetchingStatesState.isPrivacyProtectionEnabled).isEqualTo(true)
        assertThat(fetchingStatesState.isRegistrationProgressIndicatorVisible).isEqualTo(false)
        assertThat(fetchingStatesState.isStateInputEnabled).isEqualTo(false)
        assertThat(fetchingStatesState.selectedState).isNull()
        assertThat(fetchingStatesState.selectedCountry).isEqualTo(primaryCountry)

        //  ending preload
        val fetchedStatesState = uiStateResults[4]

        assertThat(fetchedStatesState.isFormProgressIndicatorVisible).isEqualTo(false)
        assertThat(fetchedStatesState.isDomainRegistrationButtonEnabled).isEqualTo(true)
        assertThat(fetchedStatesState.isStateProgressIndicatorVisible).isEqualTo(false)
        assertThat(fetchedStatesState.isPrivacyProtectionEnabled).isEqualTo(true)
        assertThat(fetchedStatesState.isRegistrationProgressIndicatorVisible).isEqualTo(false)
        assertThat(fetchedStatesState.isStateInputEnabled).isEqualTo(true)
        assertThat(fetchedStatesState.selectedState).isEqualTo(primaryState)
        assertThat(fetchedStatesState.selectedCountry).isEqualTo(primaryCountry)
    }

    @Test
    fun phoneNumberPrefixIsPrefilledDuringPreload() = test {
        setupFetchDomainContact(false, domainContactInformation.copy(phone = null))
        viewModel.start(site, domainProductDetails)

        var domainContactModelWithPrefilledPhonePrefix: DomainContactFormModel? = null

        viewModel.domainContactForm.observeForever {
            domainContactModelWithPrefilledPhonePrefix = it
        }

        assertThat(domainContactModelWithPrefilledPhonePrefix).isNotNull()
        assertThat(domainContactModelWithPrefilledPhonePrefix?.phoneNumberPrefix).isEqualTo("1")
    }

    @Test
    fun phoneNumberPrefixIsNotPrefiledWhenCountryCodeIsMissingDuringPreload() = test {
        setupFetchDomainContact(false, domainContactInformation.copy(phone = null, countryCode = null))
        viewModel.start(site, domainProductDetails)

        var domainContactModelWithPrefilledPhonePrefix: DomainContactFormModel? = null

        viewModel.domainContactForm.observeForever {
            domainContactModelWithPrefilledPhonePrefix = it
        }

        assertThat(domainContactModelWithPrefilledPhonePrefix).isNotNull()
        assertThat(domainContactModelWithPrefilledPhonePrefix?.phoneNumberPrefix).isNull()
    }

    @Test
    fun offlineFetchingCountriesDuringPreload() = test {
        whenever(resourceProvider.getString(R.string.error_network_connection)).thenReturn(offlineMessage)
        whenever(fetchSupportedCountriesUseCase.execute())
            .thenReturn(SupportedCountriesResult.Error(message = null, isDeviceOffline = true))

        viewModel.start(site, domainProductDetails)

        verify(errorMessageObserver).onChanged(offlineMessage)
    }

    @Test
    fun offlineFetchingStates() = test {
        whenever(resourceProvider.getString(R.string.error_network_connection)).thenReturn(offlineMessage)
        whenever(fetchSupportedStatesUseCase.execute(any()))
            .thenReturn(SupportedStatesResult.Error(message = null, isDeviceOffline = true))

        viewModel.start(site, domainProductDetails)

        verify(errorMessageObserver).onChanged(offlineMessage)
    }

    @Test
    fun failureWithNoMessageFetchingCountriesDuringPreload() = test {
        whenever(resourceProvider.getString(R.string.request_failed_message)).thenReturn(requestFailedMessage)
        whenever(fetchSupportedCountriesUseCase.execute())
            .thenReturn(SupportedCountriesResult.Error(message = null, isDeviceOffline = false))

        viewModel.start(site, domainProductDetails)

        verify(errorMessageObserver).onChanged(requestFailedMessage)
    }

    @Test
    fun failureWithBlankMessageFetchingCountriesDuringPreload() = test {
        whenever(resourceProvider.getString(R.string.request_failed_message)).thenReturn(requestFailedMessage)
        whenever(fetchSupportedCountriesUseCase.execute())
            .thenReturn(SupportedCountriesResult.Error(message = "", isDeviceOffline = false))

        viewModel.start(site, domainProductDetails)

        verify(errorMessageObserver).onChanged(requestFailedMessage)
    }

    @Test
    fun errorFetchingCountriesDuringPreload() = test {
        setupFetchSupportedCountries(true)

        viewModel.start(site, domainProductDetails)

        verify(dispatcher, never()).dispatch(any())

        assertThat(uiStateResults.size).isEqualTo(3)

        val errorFetchingCountriesState = uiStateResults[2]

        assertThat(errorFetchingCountriesState.isFormProgressIndicatorVisible).isEqualTo(false)
        assertThat(errorFetchingCountriesState.isDomainRegistrationButtonEnabled).isEqualTo(false)
        assertThat(errorFetchingCountriesState.isStateProgressIndicatorVisible).isEqualTo(false)
        assertThat(errorFetchingCountriesState.isPrivacyProtectionEnabled).isEqualTo(true)
        assertThat(errorFetchingCountriesState.isRegistrationProgressIndicatorVisible).isEqualTo(false)
        assertThat(errorFetchingCountriesState.selectedState).isNull()
        assertThat(errorFetchingCountriesState.selectedCountry).isNull()

        verify(errorMessageObserver).onChanged(fetchSupportedCountriesErrorMessage)
    }

    @Test
    fun errorFetchingDomainContactInformationDuringPreload() = test {
        setupFetchDomainContact(true)

        viewModel.start(site, domainProductDetails)

        verify(dispatcher, never()).dispatch(any())

        verify(errorMessageObserver).onChanged(domainContactInformationFetchErrorMessage)

        assertThat(uiStateResults.size).isEqualTo(3)

        val errorFetchingDomainContactDetailsState = uiStateResults[2]

        assertThat(errorFetchingDomainContactDetailsState.isFormProgressIndicatorVisible).isEqualTo(false)
        assertThat(errorFetchingDomainContactDetailsState.isDomainRegistrationButtonEnabled).isEqualTo(false)
        assertThat(errorFetchingDomainContactDetailsState.isStateProgressIndicatorVisible).isEqualTo(false)
        assertThat(errorFetchingDomainContactDetailsState.isPrivacyProtectionEnabled).isEqualTo(true)
        assertThat(errorFetchingDomainContactDetailsState.isRegistrationProgressIndicatorVisible).isEqualTo(false)
        assertThat(errorFetchingDomainContactDetailsState.isStateInputEnabled).isEqualTo(false)
        assertThat(errorFetchingDomainContactDetailsState.selectedState).isNull()
        assertThat(errorFetchingDomainContactDetailsState.selectedCountry).isNull()

        verify(domainContactDetailsObserver, times(0)).onChanged(any())
        assertThat(viewModel.domainContactForm.value).isNull()
    }

    @Test
    fun errorFetchingStatesDuringPreload() = test {
        setupFetchStates(true)

        viewModel.start(site, domainProductDetails)

        verify(dispatcher, never()).dispatch(any())

        verify(fetchSupportedStatesUseCase).execute(primaryCountry.code)
        verify(errorMessageObserver).onChanged(domainSupportedStatesFetchErrorMessage)

        assertThat(uiStateResults.size).isEqualTo(5)

        val errorFetchingStatesState = uiStateResults[4]

        assertThat(errorFetchingStatesState.isFormProgressIndicatorVisible).isEqualTo(false)
        assertThat(errorFetchingStatesState.isDomainRegistrationButtonEnabled).isEqualTo(true)
        assertThat(errorFetchingStatesState.isStateProgressIndicatorVisible).isEqualTo(false)
        assertThat(errorFetchingStatesState.isPrivacyProtectionEnabled).isEqualTo(true)
        assertThat(errorFetchingStatesState.isRegistrationProgressIndicatorVisible).isEqualTo(false)
        assertThat(errorFetchingStatesState.isStateInputEnabled).isEqualTo(false)
        assertThat(errorFetchingStatesState.selectedState).isNull()
        assertThat(errorFetchingStatesState.selectedCountry).isEqualTo(primaryCountry)
    }

    @Test
    fun onCountrySelectorClicked() = test {
        viewModel.start(site, domainProductDetails)

        viewModel.onCountrySelectorClicked()

        verify(countryPickerDialogObserver).onChanged(countries)
    }

    @Test
    fun onStateSelectorClicked() = test {
        viewModel.start(site, domainProductDetails)

        viewModel.onStateSelectorClicked()

        verify(statePickerDialogObserver).onChanged(states)
    }

    @Test
    fun onCountrySelected() = test {
        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onCountrySelected(secondaryCountry)

        verify(dispatcher, never()).dispatch(any())
        verify(fetchSupportedStatesUseCase).execute(secondaryCountry.code)

        assertThat(viewModel.domainContactForm.value?.countryCode).isEqualTo("AU")
        assertThat(viewModel.domainContactForm.value?.state).isNull()
        // phone number preffix was correctly switched to AU one
        assertThat(viewModel.domainContactForm.value?.phoneNumberPrefix).isEqualTo("61")
        assertThat(viewModel.domainContactForm.value?.phoneNumber).isEqualTo("3124567890")

        assertThat(uiStateResults.size).isEqualTo(2)

        val countrySelectedState = uiStateResults[0]

        assertThat(countrySelectedState.isFormProgressIndicatorVisible).isEqualTo(false)
        assertThat(countrySelectedState.isDomainRegistrationButtonEnabled).isEqualTo(false)
        assertThat(countrySelectedState.isStateProgressIndicatorVisible).isEqualTo(true)
        assertThat(countrySelectedState.isRegistrationProgressIndicatorVisible).isEqualTo(false)
        assertThat(countrySelectedState.isStateInputEnabled).isEqualTo(false)
        assertThat(countrySelectedState.selectedState).isNull()
        assertThat(countrySelectedState.selectedCountry).isEqualTo(secondaryCountry)

        val statesFetchedState = uiStateResults[1]

        assertThat(statesFetchedState.isFormProgressIndicatorVisible).isEqualTo(false)
        assertThat(statesFetchedState.isDomainRegistrationButtonEnabled).isEqualTo(true)
        assertThat(statesFetchedState.isStateProgressIndicatorVisible).isEqualTo(false)
        assertThat(statesFetchedState.isRegistrationProgressIndicatorVisible).isEqualTo(false)
        assertThat(statesFetchedState.isStateInputEnabled).isEqualTo(true)
        assertThat(statesFetchedState.selectedState).isNull()
        assertThat(statesFetchedState.selectedCountry).isEqualTo(secondaryCountry)
    }

    @Test
    fun onSameCountrySelected() = test {
        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        assertThat(viewModel.uiState.value?.selectedCountry).isEqualTo(primaryCountry)
        viewModel.onCountrySelected(primaryCountry)

        // The one call is the preload's, so selecting the same country refetched nothing
        verify(fetchSupportedStatesUseCase, times(1)).execute(any())

        assertThat(viewModel.uiState.value?.selectedCountry).isEqualTo(primaryCountry)
        assertThat(uiStateResults.size).isEqualTo(0)
    }

    @Test
    fun onStateSelected() = test {
        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onStateSelected(primaryState)
        assertThat(uiStateResults.size).isEqualTo(1)

        val stateSelectedState = uiStateResults[0]

        assertThat(stateSelectedState.selectedState).isEqualTo(primaryState)
        assertThat(viewModel.uiState.value?.selectedState).isEqualTo(primaryState)
    }

    @Test
    fun onRegisterDomainButtonClicked() = test {
        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onRegisterDomainButtonClicked()

        assertThat(viewModel.domainContactForm.value?.countryCode).isEqualTo(primaryCountry.code)
        assertThat(viewModel.domainContactForm.value?.state).isEqualTo(primaryState.code)

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(4)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues

        validateCreateCartAction(actionsDispatched[0])
        validateRedeemCartAction(actionsDispatched[1])
        validateDesignatePrimaryDomainActions(actionsDispatched[2])
        validateFetchSiteAction(actionsDispatched[3])

        assertThat(uiStateResults.size).isEqualTo(2)

        val domainRegisteringState = uiStateResults[0]
        assertThat(domainRegisteringState.isRegistrationProgressIndicatorVisible).isEqualTo(true)

        val domainRegisteredState = uiStateResults[1]
        assertThat(domainRegisteredState.isRegistrationProgressIndicatorVisible).isEqualTo(false)

        verify(completedDomainRegistrationObserver).onChanged(domainRegistrationCompletedEvent)
    }

    @Test
    fun onErrorCreatingCart() = test {
        setupCreateShoppingCartDispatcher(true)

        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onRegisterDomainButtonClicked()

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(1)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateCreateCartAction(actionsDispatched[0])

        assertThat(uiStateResults.size).isEqualTo(2)

        val domainRegisteringState = uiStateResults[0]
        assertThat(domainRegisteringState.isRegistrationProgressIndicatorVisible).isEqualTo(true)

        val errorCreatingCartState = uiStateResults[1]
        assertThat(errorCreatingCartState.isRegistrationProgressIndicatorVisible).isEqualTo(false)

        verify(errorMessageObserver).onChanged(shoppingCartCreateError.message)
    }

    @Test
    fun onErrorRedeemingCart() = test {
        setupRedeemShoppingCartDispatcher(true)

        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onRegisterDomainButtonClicked()

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(2)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateCreateCartAction(actionsDispatched[0])
        validateRedeemCartAction(actionsDispatched[1])

        assertThat(uiStateResults.size).isEqualTo(2)

        val domainRegisteringState = uiStateResults[0]
        assertThat(domainRegisteringState.isRegistrationProgressIndicatorVisible).isEqualTo(true)

        val errorRedeemingCartState = uiStateResults[1]
        assertThat(errorRedeemingCartState.isRegistrationProgressIndicatorVisible).isEqualTo(false)

        verify(errorMessageObserver).onChanged(shoppingCartRedeemError.message)
        verify(analyticsTracker).track(Stat.AUTOMATED_TRANSFER_CUSTOM_DOMAIN_PURCHASE_FAILED)
    }

    @Test
    fun onErrorFetchingSite() = test {
        setupFetchSiteDispatcher(true)

        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onRegisterDomainButtonClicked()

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(4)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues

        validateCreateCartAction(actionsDispatched[0])
        validateRedeemCartAction(actionsDispatched[1])
        validateDesignatePrimaryDomainActions(actionsDispatched[2])
        validateFetchSiteAction(actionsDispatched[3])

        assertThat(uiStateResults.size).isEqualTo(2)

        val domainRegisteringState = uiStateResults[0]
        assertThat(domainRegisteringState.isRegistrationProgressIndicatorVisible).isEqualTo(true)

        val errorFetchingSiteState = uiStateResults[1]
        assertThat(errorFetchingSiteState.isRegistrationProgressIndicatorVisible).isEqualTo(false)

        verify(errorMessageObserver).onChanged(siteChangedError.message ?: "")

        verify(completedDomainRegistrationObserver).onChanged(domainRegistrationCompletedEvent)
    }

    @Test
    fun onTosLinkClicked() = test {
        viewModel.start(site, domainProductDetails)

        viewModel.onTosLinkClicked()

        verify(tosLinkObserver).onChanged(null)
    }

    @Test
    fun onDomainContactDetailsChanged() = test {
        viewModel.start(site, domainProductDetails)

        val updatedDomainContactDetails = domainContactFormModel.copy(firstName = "Peter")
        viewModel.onDomainContactDetailsChanged(updatedDomainContactDetails)

        verify(domainContactDetailsObserver).onChanged(updatedDomainContactDetails)
        assertThat(viewModel.domainContactForm.value).isEqualTo(updatedDomainContactDetails)
    }

    @Test
    fun togglePrivacyProtection() = test {
        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        assertThat(viewModel.uiState.value?.isPrivacyProtectionEnabled).isTrue()

        viewModel.togglePrivacyProtection(true)
        val privacyProtectionOnState = uiStateResults[0]
        assertThat(privacyProtectionOnState.isPrivacyProtectionEnabled).isEqualTo(true)

        viewModel.togglePrivacyProtection(false)
        val privacyProtectionOffState = uiStateResults[1]
        assertThat(privacyProtectionOffState.isPrivacyProtectionEnabled).isEqualTo(false)

        assertThat(uiStateResults.size).isEqualTo(2)
    }

    @Test
    fun mappingOfDomainContactDetailModels() = test {
        val convertedDomainContactModel = DomainContactFormModel.toDomainContactModel(domainContactFormModel)
        assertThat(convertedDomainContactModel).isEqualTo(domainContactModel)

        val convertedDomainContactFormModel =
            DomainContactFormModel.fromDomainContactInformation(domainContactInformation)
        assertThat(convertedDomainContactFormModel).isEqualTo(domainContactFormModel)
    }

    private suspend fun setupFetchSupportedCountries(isError: Boolean) {
        val result = if (isError) {
            SupportedCountriesResult.Error(fetchSupportedCountriesErrorMessage)
        } else {
            SupportedCountriesResult.Success(countries)
        }
        whenever(fetchSupportedCountriesUseCase.execute()).thenReturn(result)
    }

    private suspend fun setupFetchStates(isError: Boolean) {
        val result = if (isError) {
            SupportedStatesResult.Error(domainSupportedStatesFetchErrorMessage)
        } else {
            SupportedStatesResult.Success(states)
        }
        whenever(fetchSupportedStatesUseCase.execute(any())).thenReturn(result)
    }

    private suspend fun setupFetchDomainContact(
        isError: Boolean,
        contact: DomainContactInformation = domainContactInformation
    ) {
        val result = if (isError) {
            DomainContactResult.Error(domainContactInformationFetchErrorMessage)
        } else {
            DomainContactResult.Success(contact)
        }
        whenever(fetchDomainContactUseCase.execute()).thenReturn(result)
    }

    private fun setupCreateShoppingCartDispatcher(isError: Boolean) {
        val event = if (isError) {
            OnShoppingCartCreated(shoppingCartCreateError)
        } else {
            OnShoppingCartCreated(createShoppingCartResponse)
        }
        whenever(dispatcher.dispatch(argWhere<Action<Void>> {
            it.type == TransactionAction.CREATE_SHOPPING_CART_WITH_DOMAIN_AND_PLAN
        })).then {
            viewModel.onShoppingCartCreated(event)
        }
    }

    private fun setupRedeemShoppingCartDispatcher(isError: Boolean) {
        val event = if (isError) {
            OnShoppingCartRedeemed(shoppingCartRedeemError)
        } else {
            OnShoppingCartRedeemed(true)
        }
        whenever(dispatcher.dispatch(argWhere<Action<Void>> {
            it.type == TransactionAction.REDEEM_CART_WITH_CREDITS
        })).then {
            viewModel.onCartRedeemed(event)
        }
    }

    private fun setupFetchSiteDispatcher(isError: Boolean) {
        val event = OnSiteChanged(1)
        if (isError) {
            event.error = siteChangedError
        }
        whenever(dispatcher.dispatch(argWhere<Action<Void>> { it.type == SiteAction.FETCH_SITE })).then {
            viewModel.onSiteChanged(event)
        }
    }

    private fun setupPrimaryDomainDispatcher(isError: Boolean) {
        val event = OnPrimaryDomainDesignated(site, isError)
        if (isError) {
            event.error = primaryDomainError
        }
        whenever(dispatcher.dispatch(argWhere<Action<Void>> {
            it.type == SiteAction.DESIGNATE_PRIMARY_DOMAIN
        })).then {
            viewModel.onPrimaryDomainDesignated(event)
        }
    }

    private fun validateCreateCartAction(action: Action<*>) {
        assertThat(action.type).isEqualTo(TransactionAction.CREATE_SHOPPING_CART_WITH_DOMAIN_AND_PLAN)
        assertThat(action.payload).isNotNull
        assertThat(action.payload).isInstanceOf(CreateShoppingCartWithDomainAndPlanPayload::class.java)

        val createShoppingCartPayload = action.payload as CreateShoppingCartWithDomainAndPlanPayload
        assertThat(createShoppingCartPayload.site).isEqualTo(site)
        assertThat(createShoppingCartPayload.domainName).isEqualTo(testDomainName)
        assertThat(createShoppingCartPayload.domainProductId).isEqualTo(productId)
        assertThat(createShoppingCartPayload.isDomainPrivacyEnabled).isEqualTo(true)
    }

    private fun validateRedeemCartAction(action: Action<*>) {
        assertThat(action.type).isEqualTo(TransactionAction.REDEEM_CART_WITH_CREDITS)
        assertThat(action.payload).isNotNull
        assertThat(action.payload).isInstanceOf(RedeemShoppingCartPayload::class.java)

        val redeemShoppingCartPayload = action.payload as RedeemShoppingCartPayload
        assertThat(redeemShoppingCartPayload.cartDetails).isEqualTo(createShoppingCartResponse)
        assertThat(redeemShoppingCartPayload.domainContactModel).isEqualTo(domainContactModel)
    }

    private fun validateFetchSiteAction(action: Action<*>) {
        assertThat(action.type).isEqualTo(SiteAction.FETCH_SITE)
        assertThat(action.payload).isNotNull
        assertThat(action.payload).isInstanceOf(SiteModel::class.java)

        val fetchSitePayload = action.payload as SiteModel
        assertThat(fetchSitePayload).isEqualTo(site)
    }

    private fun validateDesignatePrimaryDomainActions(action: Action<*>) {
        assertThat(action.type).isEqualTo(SiteAction.DESIGNATE_PRIMARY_DOMAIN)
        assertThat(action.payload).isNotNull
        assertThat(action.payload).isInstanceOf(DesignatePrimaryDomainPayload::class.java)

        val designatePrimaryDomainPayload = action.payload as DesignatePrimaryDomainPayload
        assertThat(designatePrimaryDomainPayload.site).isEqualTo(site)
        assertThat(designatePrimaryDomainPayload.domain).isEqualTo(testDomainName)
    }

    private fun clearPreLoadUiStateResult() {
        uiStateResults.clear()
    }
}
