package org.wordpress.android.viewmodel.domains

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
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

@RunWith(MockitoJUnitRunner::class)
class DomainRegistrationDetailsViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()
    @Mock private lateinit var store: TransactionsStore
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var site: SiteModel

    @Mock private lateinit var formProgressIndicatorObserver: Observer<Boolean>
    @Mock private lateinit var domainContactDetailsObserver: Observer<DomainContactModel>
    @Mock private lateinit var stateProgressIndicatorObserver: Observer<Boolean>
    @Mock private lateinit var domainRegistrationButtonObserver: Observer<Boolean>
    @Mock private lateinit var selectedStateObserver: Observer<SupportedStateResponse>
    @Mock private lateinit var selectedCountryObserver: Observer<SupportedDomainCountry>
    @Mock private lateinit var countryPickerDialogObserver: Observer<List<SupportedDomainCountry>>
    @Mock private lateinit var statePickerDialogObserver: Observer<List<SupportedStateResponse>>
    @Mock private lateinit var tosLinkObserver: Observer<Unit>
    @Mock private lateinit var privacyProtectionObserver: Observer<Boolean>
    @Mock private lateinit var domainRegistrationProgressIndicatorObserver: Observer<Boolean>
    @Mock private lateinit var completedDomainRegistrationObserver: Observer<Unit>
    @Mock private lateinit var stateInputVisibleObserver: Observer<Boolean>
    @Mock private lateinit var errorMessageObserver: Observer<String>

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

        viewModel.formProgressIndicatorVisible.observeForever(formProgressIndicatorObserver)
        viewModel.domainContactDetails.observeForever(domainContactDetailsObserver)
        viewModel.statesProgressIndicatorVisible.observeForever(stateProgressIndicatorObserver)
        viewModel.domainRegistrationButtonEnabled.observeForever(domainRegistrationButtonObserver)
        viewModel.selectedState.observeForever(selectedStateObserver)
        viewModel.selectedCountry.observeForever(selectedCountryObserver)
        viewModel.showCountryPickerDialog.observeForever(countryPickerDialogObserver)
        viewModel.showStatePickerDialog.observeForever(statePickerDialogObserver)
        viewModel.showTos.observeForever(tosLinkObserver)
        viewModel.privacyProtectionState.observeForever(privacyProtectionObserver)
        viewModel.registrationProgressIndicatorVisible.observeForever(domainRegistrationProgressIndicatorObserver)
        viewModel.handleCompletedDomainRegistration.observeForever(completedDomainRegistrationObserver)
        viewModel.stateInputEnabled.observeForever(stateInputVisibleObserver)
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

        // form progress indicator was shown and dismissed
        verify(formProgressIndicatorObserver, times(1)).onChanged(true)
        verify(formProgressIndicatorObserver, times(1)).onChanged(false)
        Assertions.assertThat(viewModel.formProgressIndicatorVisible.value).isEqualTo(false)

        // states progress indicator was shown and dismissed
        verify(stateProgressIndicatorObserver, times(1)).onChanged(true)
        verify(stateProgressIndicatorObserver, times(1)).onChanged(false)
        Assertions.assertThat(viewModel.statesProgressIndicatorVisible.value).isEqualTo(false)

        // domain registration button was disabled and then enabled
        verify(domainRegistrationButtonObserver, times(1)).onChanged(true)
        verify(domainRegistrationButtonObserver, times(1)).onChanged(false)
        Assertions.assertThat(viewModel.domainRegistrationButtonEnabled.value).isEqualTo(true)

        verify(domainContactDetailsObserver, times(1)).onChanged(domainContactModel)

        verify(selectedStateObserver, times(1)).onChanged(primaryState)
        Assertions.assertThat(viewModel.selectedState.value).isEqualTo(primaryState)

        verify(selectedCountryObserver, times(1)).onChanged(primaryCountry)
        Assertions.assertThat(viewModel.selectedCountry.value).isEqualTo(primaryCountry)

        verify(stateInputVisibleObserver, times(1)).onChanged(true)
        Assertions.assertThat(viewModel.stateInputEnabled.value).isEqualTo(true)
    }

    @Test
    fun errorFetchingCountriesDuringPreload() = test {
        setupFetchSupportedCountriesDispatcher(true)

        viewModel.start(site, domainProductDetails)

        // Verifying that correct actions with expected payloads were dispatched
        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(1)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateFetchSupportedCountriesAction(actionsDispatched[0])

        verify(formProgressIndicatorObserver, times(1)).onChanged(true)
        verify(formProgressIndicatorObserver, times(1)).onChanged(false)
        Assertions.assertThat(viewModel.formProgressIndicatorVisible.value).isEqualTo(false)

        verify(errorMessageObserver, times(1)).onChanged(fetchSupportedCountriesError.message)
    }

    @Test
    fun errorFetchingDomainContactInformationDuringPreload() = test {
        setupFetchSupportedCountriesDispatcher(false)
        setupFetchDomainContactInformationDispatcher(true)

        viewModel.start(site, domainProductDetails)

        // Verifying that correct actions with expected payloads were dispatched
        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(2)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateFetchSupportedCountriesAction(actionsDispatched[0])
        validateFetchDomainContactAction(actionsDispatched[1])

        verify(errorMessageObserver, times(1)).onChanged(domainContactInformationFetchError.message)

        verify(formProgressIndicatorObserver, times(1)).onChanged(true)
        verify(formProgressIndicatorObserver, times(1)).onChanged(false)
        Assertions.assertThat(viewModel.formProgressIndicatorVisible.value).isEqualTo(false)

        verify(domainContactDetailsObserver, times(0)).onChanged(any())
        Assertions.assertThat(viewModel.domainContactDetails.value).isNull()
    }

    @Test
    fun errorFetchingStatesDuringPreload() = test {
        setupFetchSupportedCountriesDispatcher(false)
        setupFetchDomainContactInformationDispatcher(false)
        setupFetchStatesDispatcher(true)

        viewModel.start(site, domainProductDetails)

        // Verifying that correct actions with expected payloads were dispatched
        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(3)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateFetchSupportedCountriesAction(actionsDispatched[0])
        validateFetchDomainContactAction(actionsDispatched[1])
        validateFetchStatesAction(actionsDispatched[2], primaryCountry.code)

        verify(errorMessageObserver, times(1)).onChanged(domainSupportedStatesFetchError.message)

        verify(formProgressIndicatorObserver, times(1)).onChanged(true)
        verify(formProgressIndicatorObserver, times(1)).onChanged(false)
        Assertions.assertThat(viewModel.formProgressIndicatorVisible.value).isEqualTo(false)

        verify(stateProgressIndicatorObserver, times(1)).onChanged(true)
        verify(stateProgressIndicatorObserver, times(1)).onChanged(false)
        Assertions.assertThat(viewModel.statesProgressIndicatorVisible.value).isEqualTo(false)

        // domain registration button was disabled and then enabled
        verify(domainRegistrationButtonObserver, times(1)).onChanged(true)
        verify(domainRegistrationButtonObserver, times(1)).onChanged(false)
        Assertions.assertThat(viewModel.domainRegistrationButtonEnabled.value).isEqualTo(true)
    }

    @Test
    fun onCountrySelectorClicked() = test {
        viewModel.start(site, domainProductDetails)

        viewModel.onCountrySelectorClicked()

        verify(countryPickerDialogObserver, times(1)).onChanged(countries)
    }

    @Test
    fun onStateSelectorClicked() = test {
        viewModel.start(site, domainProductDetails)

        viewModel.onStateSelectorClicked()

        verify(statePickerDialogObserver, times(1)).onChanged(states)
    }

    @Test
    fun onCountrySelected() = test {
        viewModel.start(site, domainProductDetails)

        viewModel.onCountrySelected(secondaryCountry)

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(4)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateFetchStatesAction(actionsDispatched[3], secondaryCountry.code)

        Assertions.assertThat(viewModel.domainContactDetails.value?.countryCode).isEqualTo("AU")

        verify(selectedStateObserver, times(2)).onChanged(null)
        Assertions.assertThat(viewModel.selectedState.value).isNull()

        verify(selectedCountryObserver, times(1)).onChanged(secondaryCountry)

        // states progress indicator was shown and dismissed
        verify(stateProgressIndicatorObserver, times(2)).onChanged(true)
        verify(stateProgressIndicatorObserver, times(2)).onChanged(false)
        Assertions.assertThat(viewModel.statesProgressIndicatorVisible.value).isEqualTo(false)

        // domain registration button was disabled and then enabled
        verify(domainRegistrationButtonObserver, times(2)).onChanged(true)
        verify(domainRegistrationButtonObserver, times(2)).onChanged(false)
        Assertions.assertThat(viewModel.domainRegistrationButtonEnabled.value).isEqualTo(true)

        verify(stateInputVisibleObserver, times(2)).onChanged(true)
        Assertions.assertThat(viewModel.stateInputEnabled.value).isEqualTo(true)
    }

    @Test
    fun onStateSelected() = test {
        viewModel.start(site, domainProductDetails)

        viewModel.onStateSelected(primaryState)

        verify(selectedStateObserver, times(2)).onChanged(primaryState)
        Assertions.assertThat(viewModel.selectedState.value).isEqualTo(primaryState)
    }

    @Test
    fun onRegisterDomainButtonClicked() = test {
        setupCreateShoppingCartDispatcher(false)
        setupRedeemShoppingCartDispatcher(false)
        setupFetchSiteDispatcher(false)

        viewModel.start(site, domainProductDetails)

        viewModel.onRegisterDomainButtonClicked()

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(6)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues

        validateCreateCartAction(actionsDispatched[3])
        validateRedeemCartAction(actionsDispatched[4])
        validateFetchSiteAction(actionsDispatched[5])

        verify(domainRegistrationProgressIndicatorObserver, times(1)).onChanged(true)
        verify(domainRegistrationProgressIndicatorObserver, times(1)).onChanged(false)
        Assertions.assertThat(viewModel.registrationProgressIndicatorVisible.value).isEqualTo(false)

        verify(completedDomainRegistrationObserver, times(1)).onChanged(null)
    }

    @Test
    fun onErrorCreatingCart() = test {
        setupCreateShoppingCartDispatcher(true)

        viewModel.start(site, domainProductDetails)

        viewModel.onRegisterDomainButtonClicked()

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(4)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateCreateCartAction(actionsDispatched[3])

        verify(domainRegistrationProgressIndicatorObserver, times(1)).onChanged(true)
        verify(domainRegistrationProgressIndicatorObserver, times(1)).onChanged(false)
        Assertions.assertThat(viewModel.registrationProgressIndicatorVisible.value).isEqualTo(false)

        verify(errorMessageObserver, times(1)).onChanged(shoppingCartCreateError.message)
    }

    @Test
    fun onErrorRedeemingCart() = test {
        setupCreateShoppingCartDispatcher(false)
        setupRedeemShoppingCartDispatcher(true)

        viewModel.start(site, domainProductDetails)

        viewModel.onRegisterDomainButtonClicked()

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(5)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues
        validateCreateCartAction(actionsDispatched[3])
        validateRedeemCartAction(actionsDispatched[4])

        verify(domainRegistrationProgressIndicatorObserver, times(1)).onChanged(true)
        verify(domainRegistrationProgressIndicatorObserver, times(1)).onChanged(false)
        Assertions.assertThat(viewModel.registrationProgressIndicatorVisible.value).isEqualTo(false)

        verify(errorMessageObserver, times(1)).onChanged(shoppingCartRedeemError.message)
    }

    @Test
    fun onErrorFetchingSite() = test {
        setupCreateShoppingCartDispatcher(false)
        setupRedeemShoppingCartDispatcher(false)
        setupFetchSiteDispatcher(true)

        viewModel.start(site, domainProductDetails)

        viewModel.onRegisterDomainButtonClicked()

        val captor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher, times(6)).dispatch(captor.capture())

        val actionsDispatched = captor.allValues

        validateCreateCartAction(actionsDispatched[3])
        validateRedeemCartAction(actionsDispatched[4])
        validateFetchSiteAction(actionsDispatched[5])

        verify(domainRegistrationProgressIndicatorObserver, times(1)).onChanged(true)
        verify(domainRegistrationProgressIndicatorObserver, times(1)).onChanged(false)
        Assertions.assertThat(viewModel.registrationProgressIndicatorVisible.value).isEqualTo(false)

        verify(errorMessageObserver, times(1)).onChanged(siteChangedError.message)

        verify(completedDomainRegistrationObserver, times(1)).onChanged(null)
    }

    @Test
    fun onTosLinkClicked() = test {
        viewModel.start(site, domainProductDetails)

        viewModel.onTosLinkClicked()

        verify(tosLinkObserver, times(1)).onChanged(null)
    }

    @Test
    fun onDomainContactDetailsChanged() = test {
        viewModel.start(site, domainProductDetails)

        val updatedDomainContactDetails = domainContactModel.copy(firstName = "Peter")
        viewModel.onDomainContactDetailsChanged(updatedDomainContactDetails)

        verify(domainContactDetailsObserver, times(1)).onChanged(updatedDomainContactDetails)
        Assertions.assertThat(viewModel.domainContactDetails.value).isEqualTo(updatedDomainContactDetails)
    }

    @Test
    fun togglePrivacyProtection() = test {
        viewModel.start(site, domainProductDetails)

        Assertions.assertThat(viewModel.privacyProtectionState.value).isTrue()

        viewModel.togglePrivacyProtection(true)
        verify(privacyProtectionObserver, times(2)).onChanged(true)

        viewModel.togglePrivacyProtection(false)
        verify(privacyProtectionObserver, times(1)).onChanged(false)
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
        whenever(dispatcher.dispatch(argWhere<Action<Void>> { it.type == SiteAction.FETCH_DOMAIN_SUPPORTED_STATES })).then {
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
        whenever(dispatcher.dispatch(argWhere<Action<Void>> { it.type == TransactionAction.CREATE_SHOPPING_CART })).then {
            viewModel.onShoppingCartCreated(event)
        }
    }

    private fun setupRedeemShoppingCartDispatcher(isError: Boolean) {
        val event = if (isError) {
            OnShoppingCartRedeemed(shoppingCartRedeemError)
        } else {
            OnShoppingCartRedeemed(true)
        }
        whenever(dispatcher.dispatch(argWhere<Action<Void>> { it.type == TransactionAction.REDEEM_CART_WITH_CREDITS })).then {
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
}
