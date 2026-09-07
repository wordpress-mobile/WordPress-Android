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

            // The debouncer runs on its own scheduler thread. The query is read
            // there, so it is the one the search was scheduled for, and the
            // rest hops to the main thread, where the row taps and the response
            // handler already write the same state.
            debouncer.debounce(Void::class.java, {
                val query = searchQuery
                launch(uiDispatcher) { fetchSuggestions(query) }
            }, SEARCH_QUERY_DELAY_MS, TimeUnit.MILLISECONDS)
        }
    }

    companion object {
        private const val SEARCH_QUERY_DELAY_MS = 250L
        private const val SUGGESTIONS_REQUEST_COUNT = 20u
        private const val BLOG_DOMAIN_TLDS = "blog"
        private const val ERROR_CODE_EMPTY_RESULTS = "empty_results"
    }

    /**
     * Null when there is no WordPress.com account to make the request as.
     *
     * `AccountStore.accessToken` is typed nullable but reads `""` when signed
     * out, and is only null between an in-process sign out and the next
     * launch, so both have to be treated as no token.
     */
    @Synchronized
    private fun getOrCreateClient(): WpComApiClient? {
        val token = accountStore.accessToken?.takeIf { it.isNotEmpty() } ?: return null
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
            val client = getOrCreateClient()
            if (client == null) {
                AppLog.e(
                    T.DOMAIN_REGISTRATION,
                    "Cannot fetch domain products without a WP.com access token"
                )
                initializeDefaultSuggestions()
                return@launch
            }
            val params = buildProductsParams()
            val result = client.request { it.products().list(params).data }
            when (result) {
                is WpRequestResult.Success -> products = result.response.values.toList()
                else -> AppLog.e(
                    T.DOMAIN_REGISTRATION,
                    "An error occurred while fetching domain products"
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

    private fun fetchSuggestions(query: String) {
        if (query.isBlank()) {
            // A site with no name leaves nothing to search for on open, and
            // nothing to fall back to when the field is emptied. The API
            // rejects a blank query, and reporting that is not useful when the
            // field is showing its placeholder.
            suggestions = ListState.Init()
            onDomainSuggestionSelected(null)
            return
        }

        val client = getOrCreateClient()
        if (client == null) {
            AppLog.e(
                T.DOMAIN_REGISTRATION,
                "Cannot fetch domain suggestions without a WP.com access token"
            )
            suggestions = ListState.Error(suggestions.transform { emptyList() })
            onDomainSuggestionSelected(null)
            return
        }

        suggestions = ListState.Loading(suggestions)

        // Reset the selected suggestion, if list is updated
        onDomainSuggestionSelected(null)

        val params = buildSuggestionsParams(query, SiteUtils.onBloggerPlan(site))

        launch {
            val result = client.request { it.domains().suggestions(params).data }
            // Back onto the thread the rest of the state is written from, as
            // FluxC's `ThreadMode.MAIN` subscription did.
            withContext(uiDispatcher) { onDomainSuggestionsFetched(query, result) }
        }
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
            "An error occurred while fetching the domain suggestions"
        )
        // The list answers the query in the search field. A search that failed
        // has no results, so carrying the previous ones over would leave the
        // screen answering a query the user has already replaced.
        suggestions = ListState.Error(
            suggestions.transform { emptyList() },
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
            // What is on screen answers a query that is no longer in the field,
            // including an error describing how it was written. Leaving the
            // state alone when the query has not moved keeps the default
            // suggestions through a configuration change, where the field is
            // restored empty and reports itself as changed.
            suggestions = ListState.Init()
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

    private suspend fun showLoadingButton(isLoading: Boolean) = withContext(uiDispatcher) {
        _isButtonProgressBarVisible.value = isLoading
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
