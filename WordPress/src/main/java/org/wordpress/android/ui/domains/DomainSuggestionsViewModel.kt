package org.wordpress.android.ui.domains

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.DOMAIN_PURCHASE
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.FREE_DOMAIN_WITH_ANNUAL_PLAN
import org.wordpress.android.ui.domains.usecases.CreateCartUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.extensions.isOnSale
import org.wordpress.android.util.helpers.Debouncer
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import rs.wordpress.api.kotlin.toLogErrorString
import uniffi.wp_api.DomainSuggestion
import uniffi.wp_api.DomainSuggestionsParams
import uniffi.wp_api.Product
import uniffi.wp_api.ProductTypeFilter
import uniffi.wp_api.ProductsParams
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.WpErrorCode
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import kotlin.properties.Delegates

class DomainSuggestionsViewModel @Inject constructor(
    private val wpComApiClientProvider: WpComApiClientProvider,
    private val accountStore: AccountStore,
    private val domainsRegistrationTracker: DomainsRegistrationTracker,
    private val debouncer: Debouncer,
    private val createCartUseCase: CreateCartUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    lateinit var site: SiteModel
    lateinit var domainRegistrationPurpose: DomainRegistrationPurpose

    private var wpComApiClient: WpComApiClient? = null
    private var products: List<Product>? = null

    private var isStarted = false
    private var isQueryTrackingCompleted = false

    private val _suggestions = MutableLiveData<ListState<DomainSuggestionItem>>()
    val suggestionsLiveData: LiveData<ListState<DomainSuggestionItem>> = _suggestions

    private var suggestions: ListState<DomainSuggestionItem>
            by Delegates.observable(ListState.Init()) { _, _, new ->
                _suggestions.postValue(new)
            }

    private val _selectedSuggestion = MutableLiveData<DomainSuggestionItem?>()

    val selectDomainButtonEnabledState = _selectedSuggestion.map { it is DomainSuggestionItem }

    private val _isIntroVisible = MutableLiveData(true)
    val isIntroVisible: LiveData<Boolean> = _isIntroVisible

    private val _showRedirectMessage = MutableLiveData<String?>()
    val showRedirectMessage: LiveData<String?> = _showRedirectMessage

    private val _isButtonProgressBarVisible = MutableLiveData(false)
    val isButtonProgressBarVisible: LiveData<Boolean> = _isButtonProgressBarVisible

    private val _onDomainSelected = MutableLiveData<Event<DomainProductDetails>>()
    val onDomainSelected: LiveData<Event<DomainProductDetails>> = _onDomainSelected

    private val _onFreeDomainSelected = MutableLiveData<Event<DomainProductDetails>>()
    val onFreeDomainSelected: LiveData<Event<DomainProductDetails>> = _onFreeDomainSelected

    private var searchQuery: String by Delegates.observable("") { _, oldValue, newValue ->
        if (newValue != oldValue) {
            if (isStarted && !isQueryTrackingCompleted) {
                isQueryTrackingCompleted = true
                domainsRegistrationTracker.trackDomainCreditSuggestionQueried()
            }

            debouncer.debounce(Void::class.java, {
                fetchSuggestions()
            }, SEARCH_QUERY_DELAY_MS, TimeUnit.MILLISECONDS)
        }
    }

    companion object {
        private const val SEARCH_QUERY_DELAY_MS = 250L
        private const val SUGGESTIONS_REQUEST_COUNT = 20u
        private const val BLOG_DOMAIN_TLDS = "blog"
        private const val ERROR_CODE_EMPTY_RESULTS = "empty_results"
    }

    @Synchronized
    private fun getOrCreateClient(): WpComApiClient {
        val token = requireNotNull(accountStore.accessToken) {
            "WP.com access token is required"
        }
        return wpComApiClient
            ?: wpComApiClientProvider.getWpComApiClient(token)
                .also { wpComApiClient = it }
    }

    override fun onCleared() {
        debouncer.shutdown()
        createCartUseCase.clear()
        super.onCleared()
    }

    fun start(site: SiteModel, domainRegistrationPurpose: DomainRegistrationPurpose) {
        if (isStarted) {
            return
        }
        this.site = site
        this.domainRegistrationPurpose = domainRegistrationPurpose
        fetchProducts() // required for finding domains on sale
        shouldShowRedirectMessage()
        isStarted = true
    }

    private fun initializeDefaultSuggestions() {
        searchQuery = site.name
    }

    private fun shouldShowRedirectMessage() {
        if (this.domainRegistrationPurpose == DOMAIN_PURCHASE) {
            _showRedirectMessage.value = SiteUtils.getHomeURLOrHostName(site)
        }
    }

    // Network Request

    private fun fetchProducts() {
        launch {
            val params = buildProductsParams()
            val result = getOrCreateClient()
                .request { it.products().list(params).data }
            when (result) {
                is WpRequestResult.Success -> products = result.response.values.toList()
                else -> AppLog.e(
                    T.DOMAIN_REGISTRATION,
                    "An error occurred while fetching domain products: " +
                        result.toLogErrorString()
                )
            }
            initializeDefaultSuggestions()
        }
    }

    /**
     * Only the domain products carry the sale pricing the suggestion list
     * reads, so the request is filtered to them.
     */
    @VisibleForTesting
    internal fun buildProductsParams() = ProductsParams(productType = ProductTypeFilter.Domains)

    private fun fetchSuggestions() {
        suggestions = ListState.Loading(suggestions)

        val query = searchQuery
        val params = buildSuggestionsParams(query, SiteUtils.onBloggerPlan(site))

        launch {
            val result = getOrCreateClient()
                .request { it.domains().suggestions(params).data }
            // `suggestions` is read and written from the search field, the
            // debouncer and here. Handling the response on the main thread
            // keeps those writes on one thread, so a response that lands
            // while `fetchSuggestions` is still running cannot be overwritten
            // by the stale state the other thread is holding.
            withContext(uiDispatcher) { onDomainSuggestionsFetched(query, result) }
        }

        // Reset the selected suggestion, if list is updated
        onDomainSuggestionSelected(null)
    }

    /**
     * A site on the Blogger plan can only register a `.blog` domain, so its
     * search is restricted to that TLD and carries none of the other filters.
     */
    @VisibleForTesting
    internal fun buildSuggestionsParams(query: String, isOnBloggerPlan: Boolean) =
        if (isOnBloggerPlan) {
            DomainSuggestionsParams(
                query = query,
                quantity = SUGGESTIONS_REQUEST_COUNT,
                tlds = listOf(BLOG_DOMAIN_TLDS),
            )
        } else {
            DomainSuggestionsParams(
                query = query,
                quantity = SUGGESTIONS_REQUEST_COUNT,
                onlyWordpressdotcom = false, // checkstyle ignore
                includeWordpressdotcom = false, // checkstyle ignore
                includeDotblogsubdomain = true,
            )
        }

    // Network Callback

    private fun onDomainSuggestionsFetched(
        query: String,
        result: WpRequestResult<List<DomainSuggestion>>
    ) {
        if (searchQuery != query) {
            return
        }
        when (result) {
            is WpRequestResult.Success -> showSuggestions(result.response)
            is WpRequestResult.WpError -> when (result.apiErrorCode()) {
                // The API rejects a search that matches nothing rather than
                // returning an empty list. The request itself is fine.
                ERROR_CODE_EMPTY_RESULTS -> showSuggestions(emptyList())
                else -> showSuggestionsError(result)
            }
            else -> showSuggestionsError(result)
        }
    }

    private fun showSuggestions(fetched: List<DomainSuggestion>) {
        fetched
            .filterIsInstance<DomainSuggestion.Paid>()
            .map { paid ->
                val product = products?.firstOrNull { it.productId == paid.v1.productId }
                DomainSuggestionItem(
                    domainName = paid.v1.domainName,
                    cost = paid.v1.cost,
                    isOnSale = product.isOnSale(),
                    saleCost = product?.combinedSaleCostDisplay.orEmpty(),
                    isFree = false,
                    supportsPrivacy = paid.v1.supportsPrivacy,
                    productId = paid.v1.productId.toInt(),
                    productSlug = paid.v1.productSlug,
                    vendor = paid.v1.vendor,
                    relevance = paid.v1.relevance.toFloat(),
                    isSelected = _selectedSuggestion.value?.domainName == paid.v1.domainName,
                    isCostVisible = true,
                    isFreeWithCredits = domainRegistrationPurpose(),
                    isEnabled = true
                )
            }
            .sortedBy { it.relevance }
            .asReversed()
            .let {
                suggestions = ListState.Success(it)
            }
    }

    private fun showSuggestionsError(result: WpRequestResult<*>) {
        AppLog.e(
            T.DOMAIN_REGISTRATION,
            "An error occurred while fetching the domain suggestions: " +
                result.toLogErrorString()
        )
        suggestions = ListState.Error(
            suggestions,
            errorMessage = (result as? WpRequestResult.WpError)?.errorMessage,
            errorMessageResId = R.string.error_network_connection.takeIf { result.isDeviceOffline() }
        )
    }

    private fun domainRegistrationPurpose() = domainRegistrationPurpose == CTA_DOMAIN_CREDIT_REDEMPTION ||
                domainRegistrationPurpose == FREE_DOMAIN_WITH_ANNUAL_PLAN

    fun onDomainSuggestionSelected(selectedSuggestion: DomainSuggestionItem?) {
        _selectedSuggestion.postValue(selectedSuggestion)
        suggestions = suggestions.transform { list ->
            list.map { it.copy(isSelected = selectedSuggestion?.domainName == it.domainName) }
        }
    }

    @Suppress("UseCheckOrError")
    fun onSelectDomainButtonClicked() {
        val selectedSuggestion = _selectedSuggestion.value ?: throw IllegalStateException("Selected suggestion is null")
        when (domainRegistrationPurpose) {
            DOMAIN_PURCHASE, FREE_DOMAIN_WITH_ANNUAL_PLAN -> createCart(selectedSuggestion)
            else -> selectDomain(selectedSuggestion)
        }

        domainsRegistrationTracker.trackDomainsSearchSelectDomainTapped(site)
    }

    fun updateSearchQuery(query: String) {
        _isIntroVisible.value = query.isBlank()

        if (query.isNotBlank()) {
            searchQuery = query
        } else if (searchQuery != site.name) {
            // Only reinitialize the search query, if it has changed.
            initializeDefaultSuggestions()
        }
    }

    private fun createCart(selectedSuggestion: DomainSuggestionItem) = launch {
        AppLog.d(T.DOMAIN_REGISTRATION, "Creating cart: $selectedSuggestion")

        showLoadingButton(true)

        val event = createCartUseCase.execute(
            site,
            selectedSuggestion.productId,
            selectedSuggestion.domainName,
            selectedSuggestion.supportsPrivacy,
            false
        )

        showLoadingButton(false)

        if (event.isError) {
            AppLog.e(T.DOMAIN_REGISTRATION, "Failed cart creation: ${event.error.message}")
            // TODO Handle failed cart creation
        } else {
            AppLog.d(T.DOMAIN_REGISTRATION, "Successful cart creation: ${event.cartDetails}")
            if (domainRegistrationPurpose == FREE_DOMAIN_WITH_ANNUAL_PLAN) {
                openPlans(selectedSuggestion)
            } else {
                selectDomain(selectedSuggestion)
            }
        }
    }

    private fun openPlans(selectedSuggestion: DomainSuggestionItem) {
        val domainProductDetails = DomainProductDetails(selectedSuggestion.productId, selectedSuggestion.domainName)
        _onFreeDomainSelected.postValue(Event(domainProductDetails))
        domainsRegistrationTracker.trackDomainsPurchaseWebviewViewed(site, isSiteCreation = false)
    }

    private fun selectDomain(selectedSuggestion: DomainSuggestionItem) {
        val domainProductDetails = DomainProductDetails(selectedSuggestion.productId, selectedSuggestion.domainName)
        _onDomainSelected.postValue(Event(domainProductDetails))
        domainsRegistrationTracker.trackDomainsPurchaseWebviewViewed(site, isSiteCreation = false)
    }

    private fun showLoadingButton(isLoading: Boolean) {
        _isButtonProgressBarVisible.postValue(isLoading)
        suggestions = suggestions.transform { list ->
            list.map { it.copy(isEnabled = !isLoading) }
        }
    }
}

/**
 * The API's own error code, for the codes this endpoint defines. Codes the
 * WordPress REST API also uses are modelled as [WpErrorCode] variants and
 * return null.
 */
private fun WpRequestResult.WpError<*>.apiErrorCode(): String? =
    (errorCode as? WpErrorCode.CustomException)?.v1

/**
 * True when the request never reached the network because the device has no
 * connection, which is worth telling the user apart from a server refusal.
 */
private fun WpRequestResult<*>.isDeviceOffline(): Boolean =
    this is WpRequestResult.RequestExecutionFailed &&
            reason is RequestExecutionErrorReason.DeviceIsOfflineError
