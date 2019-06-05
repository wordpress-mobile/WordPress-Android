package org.wordpress.android.viewmodel.domains

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.action.TransactionAction
import org.wordpress.android.fluxc.action.TransactionAction.FETCH_SUPPORTED_COUNTRIES
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.DomainContactModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedStateResponse
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.SupportedDomainCountry
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse.Product
import org.wordpress.android.fluxc.store.AccountStore.DomainContactError
import org.wordpress.android.fluxc.store.AccountStore.DomainContactErrorType
import org.wordpress.android.fluxc.store.AccountStore.OnDomainContactFetched
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesError
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesErrorType
import org.wordpress.android.fluxc.store.SiteStore.OnDomainSupportedStatesFetched
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.fluxc.store.TransactionsStore
import org.wordpress.android.fluxc.store.TransactionsStore.CreateCartErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartError
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.FetchSupportedCountriesError
import org.wordpress.android.fluxc.store.TransactionsStore.FetchSupportedCountriesErrorType
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartRedeemed
import org.wordpress.android.fluxc.store.TransactionsStore.OnSupportedCountriesFetched
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartError
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.PHONE
import org.wordpress.android.test
import org.wordpress.android.ui.domains.DomainProductDetails
import org.wordpress.android.viewmodel.domains.DomainRegistrationDetailsViewModel.DomainRegistrationDetailsUiState

class DomainRegistrationDetailsViewModelTest : BaseUnitTest() {
    @Mock private lateinit var store: TransactionsStore
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var site: SiteModel

    @Mock private lateinit var domainContactDetailsObserver: Observer<DomainContactModel>
    @Mock private lateinit var countryPickerDialogObserver: Observer<List<SupportedDomainCountry>>
    @Mock private lateinit var statePickerDialogObserver: Observer<List<SupportedStateResponse>>
    @Mock private lateinit var tosLinkObserver: Observer<Unit>
    @Mock private lateinit var completedDomainRegistrationObserver: Observer<String>
    @Mock private lateinit var errorMessageObserver: Observer<String>

    private val uiStateResults = mutableListOf<DomainRegistrationDetailsUiState>()

    private lateinit var viewModel: DomainRegistrationDetailsViewModel

    private val primaryCountry = SupportedDomainCountry("US", "United States")
    private val secondaryCountry = SupportedDomainCountry("AU", "Australia")
    private val countries = listOf(primaryCountry, secondaryCountry)

    private val primaryState = SupportedStateResponse("CA", "California")
    private val secondaryState = SupportedStateResponse("NSW", "New South Wales")
    private val states = listOf(primaryState, secondaryState)

    private val siteId = 1234L
    private val productId = "76"
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
            "3124567890",
            ""
    )

    private val shoppingCartCreateError = CreateShoppingCartError(GENERIC_ERROR, "Error Creating Cart")
    private val shoppingCartRedeemError = RedeemShoppingCartError(PHONE, "Wrong phone number")
    private val siteChangedError = SiteError(SiteErrorType.GENERIC_ERROR, "Error fetching site")
    private val domainContactInformationFetchError = DomainContactError(
            DomainContactErrorType.GENERIC_ERROR,
            "Error fetching domain contact information"
    )
    private val domainSupportedStatesFetchError = DomainSupportedStatesError(
            DomainSupportedStatesErrorType.GENERIC_ERROR,
            "Error fetching domain supported states"
    )
    private val fetchSupportedCountriesError = FetchSupportedCountriesError(
            FetchSupportedCountriesErrorType.GENERIC_ERROR,
            "Error fetching countries"
    )

    private val createShoppingCartResponse = CreateShoppingCartResponse(
            siteId.toInt(),
            cartId,
            listOf(Product(productId, testDomainName))
    )

    private val domainProductDetails = DomainProductDetails(productId, testDomainName)

    @Before
    fun setUp() {
        site.siteId = siteId

        viewModel = DomainRegistrationDetailsViewModel(dispatcher, store)
        // Setting up chain of actions
        setupFetchSupportedCountriesDispatcher(false)
        setupFetchDomainContactInformationDispatcher(false)
        setupFetchStatesDispatcher(false)
        setupCreateShoppingCartDispatcher(false)
        setupRedeemShoppingCartDispatcher(false)
        setupFetchSiteDispatcher(false)

        uiStateResults.clear()
        viewModel.uiState.observeForever { if (it != null) uiStateResults.add(it) }
        viewModel.domainContactDetails.observeForever(domainContactDetailsObserver)
        viewModel.showCountryPickerDialog.observeForever(countryPickerDialogObserver)
        viewModel.showStatePickerDialog.observeForever(statePickerDialogObserver)
        viewModel.showTos.observeForever(tosLinkObserver)
        viewModel.handleCompletedDomainRegistration.observeForever(completedDomainRegistrationObserver)
        viewModel.showErrorMessage.observeForever(errorMessageObserver)
    }

    @Test
    fun contactDetailsPreload() = test {
        viewModel.start(site, domainProductDetails)

        // Verifying that correct actions with expected payloads were dispatched
        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(3)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateFetchSupportedCountriesAction(actionsDispatched[0])
        validateFetchDomainContactAction(actionsDispatched[1])
        validateFetchStatesAction(actionsDispatched[2], primaryCountry.code)

        var preloadStep = 0

        Assertions.assertThat(uiStateResults.size).isEqualTo(5)

        // initial state
        Assertions.assertThat(uiStateResults[preloadStep].isFormProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isDomainRegistrationButtonEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isStateProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isPrivacyProtectionEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[preloadStep].isRegistrationProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isStateInputEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].selectedState).isNull()
        Assertions.assertThat(uiStateResults[preloadStep].selectedCountry).isNull()

        preloadStep++

        // fetching countries and domain contact details
        Assertions.assertThat(uiStateResults[preloadStep].isFormProgressIndicatorVisible).isEqualTo(true)
        Assertions.assertThat(uiStateResults[preloadStep].isDomainRegistrationButtonEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isStateProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isPrivacyProtectionEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[preloadStep].isRegistrationProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isStateInputEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].selectedState).isNull()
        Assertions.assertThat(uiStateResults[preloadStep].selectedCountry).isNull()

        preloadStep++

        // hiding form progress after domain contact details fetched
        Assertions.assertThat(uiStateResults[preloadStep].isFormProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isDomainRegistrationButtonEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isStateProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isPrivacyProtectionEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[preloadStep].isRegistrationProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isStateInputEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].selectedState).isNull()
        Assertions.assertThat(uiStateResults[preloadStep].selectedCountry).isNull()

        verify(domainContactDetailsObserver).onChanged(domainContactModel)

        preloadStep++

        // fetching states using country code in domain contact details
        Assertions.assertThat(uiStateResults[preloadStep].isFormProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isDomainRegistrationButtonEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isStateProgressIndicatorVisible).isEqualTo(true)
        Assertions.assertThat(uiStateResults[preloadStep].isPrivacyProtectionEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[preloadStep].isRegistrationProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isStateInputEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].selectedState).isNull()
        Assertions.assertThat(uiStateResults[preloadStep].selectedCountry).isEqualTo(primaryCountry)

        preloadStep++

        // fetched states, ending preload

        Assertions.assertThat(uiStateResults[preloadStep].isFormProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isDomainRegistrationButtonEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[preloadStep].isStateProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isPrivacyProtectionEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[preloadStep].isRegistrationProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[preloadStep].isStateInputEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[preloadStep].selectedState).isEqualTo(primaryState)
        Assertions.assertThat(uiStateResults[preloadStep].selectedCountry).isEqualTo(primaryCountry)
    }

    @Test
    fun errorFetchingCountriesDuringPreload() = test {
        setupFetchSupportedCountriesDispatcher(true)

        viewModel.start(site, domainProductDetails)

        // Verifying that correct actions with expected payloads were dispatched
        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateFetchSupportedCountriesAction(actionsDispatched[0])

        Assertions.assertThat(uiStateResults.size).isEqualTo(3)

        // error fetching countries
        Assertions.assertThat(uiStateResults[2].isFormProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[2].isDomainRegistrationButtonEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[2].isStateProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[2].isPrivacyProtectionEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[2].isRegistrationProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[2].selectedState).isNull()
        Assertions.assertThat(uiStateResults[2].selectedCountry).isNull()

        verify(errorMessageObserver).onChanged(fetchSupportedCountriesError.message)
    }

    @Test
    fun errorFetchingDomainContactInformationDuringPreload() = test {
        setupFetchDomainContactInformationDispatcher(true)

        viewModel.start(site, domainProductDetails)

        // Verifying that correct actions with expected payloads were dispatched
        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(2)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateFetchSupportedCountriesAction(actionsDispatched[0])
        validateFetchDomainContactAction(actionsDispatched[1])

        verify(errorMessageObserver).onChanged(domainContactInformationFetchError.message)

        Assertions.assertThat(uiStateResults.size).isEqualTo(3)

        // error fetching domain contact details
        Assertions.assertThat(uiStateResults[2].isFormProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[2].isDomainRegistrationButtonEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[2].isStateProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[2].isPrivacyProtectionEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[2].isRegistrationProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[2].isStateInputEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[2].selectedState).isNull()
        Assertions.assertThat(uiStateResults[2].selectedCountry).isNull()

        verify(domainContactDetailsObserver, times(0)).onChanged(any())
        Assertions.assertThat(viewModel.domainContactDetails.value).isNull()
    }

    @Test
    fun errorFetchingStatesDuringPreload() = test {
        setupFetchStatesDispatcher(true)

        viewModel.start(site, domainProductDetails)

        // Verifying that correct actions with expected payloads were dispatched
        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(3)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateFetchSupportedCountriesAction(actionsDispatched[0])
        validateFetchDomainContactAction(actionsDispatched[1])
        validateFetchStatesAction(actionsDispatched[2], primaryCountry.code)

        verify(errorMessageObserver).onChanged(domainSupportedStatesFetchError.message)

        Assertions.assertThat(uiStateResults.size).isEqualTo(5)

        Assertions.assertThat(uiStateResults[4].isFormProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[4].isDomainRegistrationButtonEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[4].isStateProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[4].isPrivacyProtectionEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[4].isRegistrationProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[4].isStateInputEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[4].selectedState).isNull()
        Assertions.assertThat(uiStateResults[4].selectedCountry).isEqualTo(primaryCountry)
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

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(4)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateFetchStatesAction(actionsDispatched[3], secondaryCountry.code)

        Assertions.assertThat(viewModel.domainContactDetails.value?.countryCode).isEqualTo("AU")
        Assertions.assertThat(viewModel.domainContactDetails.value?.state).isNull()

        Assertions.assertThat(uiStateResults.size).isEqualTo(2)

        var loadingStep = 0

        // county selected
        Assertions.assertThat(uiStateResults[loadingStep].isFormProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[loadingStep].isDomainRegistrationButtonEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[loadingStep].isStateProgressIndicatorVisible).isEqualTo(true)
        Assertions.assertThat(uiStateResults[loadingStep].isRegistrationProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[loadingStep].isStateInputEnabled).isEqualTo(false)
        Assertions.assertThat(uiStateResults[loadingStep].selectedState).isNull()
        Assertions.assertThat(uiStateResults[loadingStep].selectedCountry).isEqualTo(secondaryCountry)

        loadingStep++

        // states fetched
        Assertions.assertThat(uiStateResults[loadingStep].isFormProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[loadingStep].isDomainRegistrationButtonEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[loadingStep].isStateProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[loadingStep].isRegistrationProgressIndicatorVisible).isEqualTo(false)
        Assertions.assertThat(uiStateResults[loadingStep].isStateInputEnabled).isEqualTo(true)
        Assertions.assertThat(uiStateResults[loadingStep].selectedState).isNull()
        Assertions.assertThat(uiStateResults[loadingStep].selectedCountry).isEqualTo(secondaryCountry)
    }

    @Test
    fun onSameCountrySelected() = test {
        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        Assertions.assertThat(viewModel.uiState.value?.selectedCountry).isEqualTo(primaryCountry)
        viewModel.onCountrySelected(primaryCountry)

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(3)).dispatch(captor.capture())

        Assertions.assertThat(viewModel.uiState.value?.selectedCountry).isEqualTo(primaryCountry)
        Assertions.assertThat(uiStateResults.size).isEqualTo(0)
    }

    @Test
    fun onStateSelected() = test {
        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onStateSelected(primaryState)
        Assertions.assertThat(uiStateResults.size).isEqualTo(1)

        Assertions.assertThat(uiStateResults[0].selectedState).isEqualTo(primaryState)
        Assertions.assertThat(viewModel.uiState.value?.selectedState).isEqualTo(primaryState)
    }

    @Test
    fun onRegisterDomainButtonClicked() = test {
        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onRegisterDomainButtonClicked()

        Assertions.assertThat(viewModel.domainContactDetails.value?.countryCode).isEqualTo(primaryCountry.code)
        Assertions.assertThat(viewModel.domainContactDetails.value?.state).isEqualTo(primaryState.code)

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(6)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues

        validateCreateCartAction(actionsDispatched[3])
        validateRedeemCartAction(actionsDispatched[4])
        validateFetchSiteAction(actionsDispatched[5])

        Assertions.assertThat(uiStateResults.size).isEqualTo(2)

        Assertions.assertThat(uiStateResults[0].isRegistrationProgressIndicatorVisible).isEqualTo(true)
        Assertions.assertThat(uiStateResults[1].isRegistrationProgressIndicatorVisible).isEqualTo(false)

        verify(completedDomainRegistrationObserver).onChanged(domainProductDetails.domainName)
    }

    @Test
    fun onErrorCreatingCart() = test {
        setupCreateShoppingCartDispatcher(true)

        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onRegisterDomainButtonClicked()

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(4)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateCreateCartAction(actionsDispatched[3])

        Assertions.assertThat(uiStateResults.size).isEqualTo(2)

        Assertions.assertThat(uiStateResults[0].isRegistrationProgressIndicatorVisible).isEqualTo(true)
        Assertions.assertThat(uiStateResults[1].isRegistrationProgressIndicatorVisible).isEqualTo(false)

        verify(errorMessageObserver).onChanged(shoppingCartCreateError.message)
    }

    @Test
    fun onErrorRedeemingCart() = test {
        setupRedeemShoppingCartDispatcher(true)

        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onRegisterDomainButtonClicked()

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(5)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateCreateCartAction(actionsDispatched[3])
        validateRedeemCartAction(actionsDispatched[4])

        Assertions.assertThat(uiStateResults.size).isEqualTo(2)

        Assertions.assertThat(uiStateResults[0].isRegistrationProgressIndicatorVisible).isEqualTo(true)
        Assertions.assertThat(uiStateResults[1].isRegistrationProgressIndicatorVisible).isEqualTo(false)

        verify(errorMessageObserver).onChanged(shoppingCartRedeemError.message)
    }

    @Test
    fun onErrorFetchingSite() = test {
        setupFetchSiteDispatcher(true)

        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        viewModel.onRegisterDomainButtonClicked()

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(6)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues

        validateCreateCartAction(actionsDispatched[3])
        validateRedeemCartAction(actionsDispatched[4])
        validateFetchSiteAction(actionsDispatched[5])

        Assertions.assertThat(uiStateResults.size).isEqualTo(2)

        Assertions.assertThat(uiStateResults[0].isRegistrationProgressIndicatorVisible).isEqualTo(true)
        Assertions.assertThat(uiStateResults[1].isRegistrationProgressIndicatorVisible).isEqualTo(false)

        verify(errorMessageObserver).onChanged(siteChangedError.message)

        verify(completedDomainRegistrationObserver).onChanged(domainProductDetails.domainName)
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

        val updatedDomainContactDetails = domainContactModel.copy(firstName = "Peter")
        viewModel.onDomainContactDetailsChanged(updatedDomainContactDetails)

        verify(domainContactDetailsObserver).onChanged(updatedDomainContactDetails)
        Assertions.assertThat(viewModel.domainContactDetails.value).isEqualTo(updatedDomainContactDetails)
    }

    @Test
    fun togglePrivacyProtection() = test {
        viewModel.start(site, domainProductDetails)
        clearPreLoadUiStateResult()

        Assertions.assertThat(viewModel.uiState.value?.isPrivacyProtectionEnabled).isTrue()

        viewModel.togglePrivacyProtection(true)
        Assertions.assertThat(uiStateResults[0].isPrivacyProtectionEnabled).isEqualTo(true)

        viewModel.togglePrivacyProtection(false)
        Assertions.assertThat(uiStateResults[1].isPrivacyProtectionEnabled).isEqualTo(false)

        Assertions.assertThat(uiStateResults.size).isEqualTo(2)
    }

    private fun setupFetchSupportedCountriesDispatcher(isError: Boolean) {
        val event = if (isError) {
            OnSupportedCountriesFetched(fetchSupportedCountriesError)
        } else {
            OnSupportedCountriesFetched(countries)
        }
        whenever(dispatcher.dispatch(argWhere<Action<Void>> { it.type == FETCH_SUPPORTED_COUNTRIES })).then {
            viewModel.onSupportedCountriesFetched(event)
        }
    }

    private fun setupFetchStatesDispatcher(isError: Boolean) {
        val event = if (isError) {
            OnDomainSupportedStatesFetched(null, domainSupportedStatesFetchError)
        } else {
            OnDomainSupportedStatesFetched(states, null)
        }
        whenever(dispatcher.dispatch(argWhere<Action<Void>> {
            it.type == SiteAction.FETCH_DOMAIN_SUPPORTED_STATES
        })).then {
            viewModel.onDomainSupportedStatesFetched(event)
        }
    }

    private fun setupFetchDomainContactInformationDispatcher(isError: Boolean) {
        val event = if (isError) {
            OnDomainContactFetched(null, domainContactInformationFetchError)
        } else {
            OnDomainContactFetched(domainContactModel, null)
        }
        whenever(dispatcher.dispatch(argWhere<Action<Void>> { it.type == AccountAction.FETCH_DOMAIN_CONTACT })).then {
            viewModel.onDomainContactFetched(event)
        }
    }

    private fun setupCreateShoppingCartDispatcher(isError: Boolean) {
        val event = if (isError) {
            OnShoppingCartCreated(shoppingCartCreateError)
        } else {
            OnShoppingCartCreated(createShoppingCartResponse)
        }
        whenever(dispatcher.dispatch(argWhere<Action<Void>> {
            it.type == TransactionAction.CREATE_SHOPPING_CART
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

    private fun validateFetchSupportedCountriesAction(action: Action<*>) {
        Assertions.assertThat(action.type).isEqualTo(FETCH_SUPPORTED_COUNTRIES)
        Assertions.assertThat(action.payload).isNull()
    }

    private fun validateFetchDomainContactAction(action: Action<*>) {
        Assertions.assertThat(action.type).isEqualTo(AccountAction.FETCH_DOMAIN_CONTACT)
        Assertions.assertThat(action.payload).isNull()
    }

    private fun validateFetchStatesAction(action: Action<*>, targetCountryCode: String) {
        Assertions.assertThat(action.type).isEqualTo(SiteAction.FETCH_DOMAIN_SUPPORTED_STATES)
        Assertions.assertThat(action.payload).isEqualTo(targetCountryCode)
    }

    private fun validateCreateCartAction(action: Action<*>) {
        Assertions.assertThat(action.type).isEqualTo(TransactionAction.CREATE_SHOPPING_CART)
        Assertions.assertThat(action.payload).isNotNull
        Assertions.assertThat(action.payload).isInstanceOf(CreateShoppingCartPayload::class.java)

        val createShoppingCartPayload = action.payload as CreateShoppingCartPayload
        Assertions.assertThat(createShoppingCartPayload.site).isEqualTo(site)
        Assertions.assertThat(createShoppingCartPayload.domainName).isEqualTo(testDomainName)
        Assertions.assertThat(createShoppingCartPayload.productId).isEqualTo(productId)
        Assertions.assertThat(createShoppingCartPayload.isPrivacyEnabled).isEqualTo(true)
    }

    private fun validateRedeemCartAction(action: Action<*>) {
        Assertions.assertThat(action.type).isEqualTo(TransactionAction.REDEEM_CART_WITH_CREDITS)
        Assertions.assertThat(action.payload).isNotNull
        Assertions.assertThat(action.payload).isInstanceOf(RedeemShoppingCartPayload::class.java)

        val redeemShoppingCartPayload = action.payload as RedeemShoppingCartPayload
        Assertions.assertThat(redeemShoppingCartPayload.cartDetails).isEqualTo(createShoppingCartResponse)
        Assertions.assertThat(redeemShoppingCartPayload.domainContactModel).isEqualTo(domainContactModel)
    }

    private fun validateFetchSiteAction(action: Action<*>) {
        Assertions.assertThat(action.type).isEqualTo(SiteAction.FETCH_SITE)
        Assertions.assertThat(action.payload).isNotNull
        Assertions.assertThat(action.payload).isInstanceOf(SiteModel::class.java)

        val fetchSitePayload = action.payload as SiteModel
        Assertions.assertThat(fetchSitePayload).isEqualTo(site)
    }

    private fun clearPreLoadUiStateResult() {
        uiStateResults.clear()
    }
}
