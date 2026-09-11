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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainError
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainErrorType
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainPayload
import org.wordpress.android.fluxc.store.SiteStore.OnPrimaryDomainDesignated
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.ui.domains.DomainRegistrationDetailsViewModel.DomainContactFieldError
import org.wordpress.android.ui.domains.DomainRegistrationDetailsViewModel.DomainContactFormModel
import org.wordpress.android.ui.domains.DomainRegistrationDetailsViewModel.DomainRegistrationDetailsUiState
import org.wordpress.android.ui.domains.usecases.CreateCartResult
import org.wordpress.android.ui.domains.usecases.CreateCartUseCase
import org.wordpress.android.ui.domains.usecases.DomainContactField
import org.wordpress.android.ui.domains.usecases.DomainContactResult
import org.wordpress.android.ui.domains.usecases.FetchDomainContactUseCase
import org.wordpress.android.ui.domains.usecases.FetchSupportedCountriesUseCase
import org.wordpress.android.ui.domains.usecases.FetchSupportedStatesUseCase
import org.wordpress.android.ui.domains.usecases.RedeemCartResult
import org.wordpress.android.ui.domains.usecases.RedeemCartUseCase
import org.wordpress.android.ui.domains.usecases.SupportedCountriesResult
import org.wordpress.android.ui.domains.usecases.SupportedStatesResult
import org.wordpress.android.viewmodel.ResourceProvider
import uniffi.wp_api.CartKey
import uniffi.wp_api.DomainContactInformation
import uniffi.wp_api.SupportedCountry
import uniffi.wp_api.SupportedState
import org.wordpress.android.util.NoDelayCoroutineDispatcher
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class DomainRegistrationDetailsViewModelTest : BaseUnitTest() {
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
    private lateinit var createCartUseCase: CreateCartUseCase

    @Mock
    private lateinit var redeemCartUseCase: RedeemCartUseCase

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

    @Mock
    private lateinit var formErrorObserver: Observer<DomainContactFieldError>

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

    private val createCartErrorMessage = "Error Creating Cart"
    private val redeemCartErrorMessage = "Wrong phone number"
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

    private val shoppingCart = testShoppingCart(CartKey.Site(siteId.toULong()))

    private val domainProductDetails = DomainProductDetails(productId, testDomainName)

    @Before
    fun setUp() {
        site.siteId = siteId
        site.url = testDomainName

        whenever(siteStore.getSiteByLocalId(any())).doReturn(site)

        viewModel = DomainRegistrationDetailsViewModel(
            dispatcher,
            siteStore,
            analyticsTracker,
            fetchSupportedCountriesUseCase,
            fetchDomainContactUseCase,
            fetchSupportedStatesUseCase,
            createCartUseCase,
            redeemCartUseCase,
            resourceProvider,
            NoDelayCoroutineDispatcher()
        )
        // Setting up chain of actions
        test {
            setupFetchSupportedCountries(false)
            setupFetchDomainContact(false)
            setupFetchStates(false)
            setupCreateCart(false)
            setupRedeemCart(false)
        }
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
        viewModel.formError.observeForever(formErrorObserver)
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
    fun onCountrySelectorClickedAfterCountriesFailedToLoad() = test {
        setupFetchSupportedCountries(true)
        viewModel.start(site, domainProductDetails)

        viewModel.onCountrySelectorClicked()

        verify(countryPickerDialogObserver, never()).onChanged(any())
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

        verify(createCartUseCase).execute(site, productId, testDomainName, true, true, null)
        // The prefilled contact round-trips: the form holds the fetched details unchanged
        verify(redeemCartUseCase).execute(shoppingCart, domainContactInformation)

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(2)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues

        validateDesignatePrimaryDomainActions(actionsDispatched[0])
        validateFetchSiteAction(actionsDispatched[1])

        assertThat(uiStateResults.size).isEqualTo(2)

        val domainRegisteringState = uiStateResults[0]
        assertThat(domainRegisteringState.isRegistrationProgressIndicatorVisible).isEqualTo(true)

        val domainRegisteredState = uiStateResults[1]
        assertThat(domainRegisteredState.isRegistrationProgressIndicatorVisible).isEqualTo(false)

        verify(completedDomainRegistrationObserver).onChanged(domainRegistrationCompletedEvent)
    }

    @Test
    fun onErrorCreatingCart() = test {
        setupCreateCart(true)

        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onRegisterDomainButtonClicked()

        verifyNoInteractions(redeemCartUseCase)
        verify(dispatcher, never()).dispatch(any())

        assertThat(uiStateResults.size).isEqualTo(2)

        val domainRegisteringState = uiStateResults[0]
        assertThat(domainRegisteringState.isRegistrationProgressIndicatorVisible).isEqualTo(true)

        val errorCreatingCartState = uiStateResults[1]
        assertThat(errorCreatingCartState.isRegistrationProgressIndicatorVisible).isEqualTo(false)

        verify(errorMessageObserver).onChanged(createCartErrorMessage)
    }

    @Test
    fun onErrorRedeemingCart() = test {
        setupRedeemCart(true)

        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onRegisterDomainButtonClicked()

        verify(dispatcher, never()).dispatch(any())

        assertThat(uiStateResults.size).isEqualTo(2)

        val domainRegisteringState = uiStateResults[0]
        assertThat(domainRegisteringState.isRegistrationProgressIndicatorVisible).isEqualTo(true)

        val errorRedeemingCartState = uiStateResults[1]
        assertThat(errorRedeemingCartState.isRegistrationProgressIndicatorVisible).isEqualTo(false)

        verify(errorMessageObserver).onChanged(redeemCartErrorMessage)
        verify(analyticsTracker).track(Stat.AUTOMATED_TRANSFER_CUSTOM_DOMAIN_PURCHASE_FAILED)
    }

    @Test
    fun `a rejected contact field is reported against that field`() = test {
        setupRedeemCart(true)

        viewModel.start(site, domainProductDetails)
        viewModel.onRegisterDomainButtonClicked()

        verify(formErrorObserver).onChanged(
            DomainContactFieldError(DomainContactField.PHONE, redeemCartErrorMessage)
        )
    }

    @Test
    fun `a rejection naming no contact field leaves the form unmarked`() = test {
        whenever(redeemCartUseCase.execute(any(), any()))
            .thenReturn(RedeemCartResult.Error(field = null, message = "Not enough credits"))

        viewModel.start(site, domainProductDetails)
        viewModel.onRegisterDomainButtonClicked()

        verify(formErrorObserver, never()).onChanged(any())
        verify(errorMessageObserver).onChanged("Not enough credits")
    }

    @Test
    fun onErrorFetchingSite() = test {
        setupFetchSiteDispatcher(true)

        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onRegisterDomainButtonClicked()

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(2)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues

        validateDesignatePrimaryDomainActions(actionsDispatched[0])
        validateFetchSiteAction(actionsDispatched[1])

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
        val convertedDomainContactInformation =
            DomainContactFormModel.toDomainContactInformation(domainContactFormModel)
        assertThat(convertedDomainContactInformation).isEqualTo(domainContactInformation)

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

    private suspend fun setupCreateCart(isError: Boolean) {
        val result = if (isError) {
            CreateCartResult.Error(createCartErrorMessage)
        } else {
            CreateCartResult.Success(shoppingCart)
        }
        whenever(createCartUseCase.execute(any(), any(), any(), any(), any(), anyOrNull()))
            .thenReturn(result)
    }

    private suspend fun setupRedeemCart(isError: Boolean) {
        val result = if (isError) {
            RedeemCartResult.Error(DomainContactField.PHONE, redeemCartErrorMessage)
        } else {
            RedeemCartResult.Success
        }
        whenever(redeemCartUseCase.execute(any(), any())).thenReturn(result)
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
