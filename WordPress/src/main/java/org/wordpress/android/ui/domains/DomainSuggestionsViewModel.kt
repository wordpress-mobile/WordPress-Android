package org.wordpress.android.ui.domains

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.CoroutineDispatcher
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_PURCHASE_WEBVIEW_VIEWED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAINS_SEARCH_SELECT_DOMAIN_TAPPED
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.products.Product
import org.wordpress.android.fluxc.store.ProductsStore
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.DOMAIN_PURCHASE
import org.wordpress.android.ui.domains.usecases.CreateCartUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.SiteDomainsFeatureConfig
import org.wordpress.android.util.helpers.Debouncer
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import kotlin.properties.Delegates

class DomainSuggestionsViewModel @Inject constructor(
    private val productsStore: ProductsStore,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val dispatcher: Dispatcher,
    private val debouncer: Debouncer,
    private val siteDomainsFeatureConfig: SiteDomainsFeatureConfig,
    private val createCartUseCase: CreateCartUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    lateinit var site: SiteModel
    lateinit var domainRegistrationPurpose: DomainRegistrationPurpose
    var products: List<Product>? = null

    private var isStarted = false
    private var isQueryTrackingCompleted = false

    private val _suggestions = MutableLiveData<ListState<DomainSuggestionItem>>()
    val suggestionsLiveData: LiveData<ListState<DomainSuggestionItem>> = _suggestions

    private var suggestions: ListState<DomainSuggestionItem>
            by Delegates.observable(ListState.Init()) { _, _, new ->
                _suggestions.postValue(new)
            }

    private val _selectedSuggestion = MutableLiveData<DomainSuggestionItem?>()

    val selectDomainButtonEnabledState = Transformations.map(_selectedSuggestion) { it is DomainSuggestionItem }

    private val _isIntroVisible = MutableLiveData(true)
    val isIntroVisible: LiveData<Boolean> = _isIntroVisible

    private val _showRedirectMessage = MutableLiveData<String?>()
    val showRedirectMessage: LiveData<String?> = _showRedirectMessage

    private val _isButtonProgressBarVisible = MutableLiveData(false)
    val isButtonProgressBarVisible: LiveData<Boolean> = _isButtonProgressBarVisible

    private val _onDomainSelected = MutableLiveData<Event<DomainProductDetails>>()
    val onDomainSelected: LiveData<Event<DomainProductDetails>> = _onDomainSelected

    private var searchQuery: String by Delegates.observable("") { _, oldValue, newValue ->
        if (newValue != oldValue) {
            if (isStarted && !isQueryTrackingCompleted) {
                isQueryTrackingCompleted = true
                analyticsTracker.track(Stat.DOMAIN_CREDIT_SUGGESTION_QUERIED)
            }

            debouncer.debounce(Void::class.java, {
                fetchSuggestions()
            }, SEARCH_QUERY_DELAY_MS, TimeUnit.MILLISECONDS)
        }
    }

    companion object {
        private const val SEARCH_QUERY_DELAY_MS = 250L
        private const val SUGGESTIONS_REQUEST_COUNT = 20
        private const val BLOG_DOMAIN_TLDS = "blog"
    }

    // Bind Dispatcher to Lifecycle

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
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
            val result = productsStore.fetchProducts()
            when {
                result.isError -> {
                    AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching site domains")
                    initializeDefaultSuggestions()
                }
                else -> {
                    AppLog.d(T.DOMAIN_REGISTRATION, result.products.toString())
                    result.products?.let { products = it }
                    initializeDefaultSuggestions()
                }
            }
        }
    }

    private fun fetchSuggestions() {
        suggestions = ListState.Loading(suggestions)

        val suggestDomainsPayload = if (SiteUtils.onBloggerPlan(site)) {
            SuggestDomainsPayload(searchQuery, SUGGESTIONS_REQUEST_COUNT, BLOG_DOMAIN_TLDS)
        } else {
            SuggestDomainsPayload(searchQuery, false, false, true, SUGGESTIONS_REQUEST_COUNT, false)
        }

        dispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(suggestDomainsPayload))

        // Reset the selected suggestion, if list is updated
        onDomainSuggestionSelected(null)
    }

    // Network Callback

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDomainSuggestionsFetched(event: OnSuggestedDomains) {
        if (searchQuery != event.query) {
            return
        }
        if (event.isError) {
            AppLog.e(
                T.DOMAIN_REGISTRATION,
                "An error occurred while fetching the domain suggestions with type: " + event.error.type
            )
            suggestions = ListState.Error(suggestions, event.error.message)
            return
        }

        event.suggestions
            .filter { !it.is_free }
            .map {
                val product = products?.firstOrNull { product -> product.productId == it.product_id }
                DomainSuggestionItem(
                    domainName = it.domain_name,
                    cost = it.cost,
                    isOnSale = product?.isSaleDomain() ?: false,
                    saleCost = product?.saleCostForDisplay().toString(),
                    isFree = it.is_free,
                    supportsPrivacy = it.supports_privacy,
                    productId = it.product_id,
                    productSlug = it.product_slug,
                    vendor = it.vendor,
                    relevance = it.relevance,
                    isSelected = _selectedSuggestion.value?.domainName == it.domain_name,
                    isCostVisible = siteDomainsFeatureConfig.isEnabled(),
                    isFreeWithCredits = domainRegistrationPurpose == CTA_DOMAIN_CREDIT_REDEMPTION,
                    isEnabled = true
                )
            }
            .sortedBy { it.relevance }
            .asReversed()
            .let {
                suggestions = ListState.Success(it)
            }
    }

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
            DOMAIN_PURCHASE -> createCart(selectedSuggestion)
            else -> selectDomain(selectedSuggestion)
        }

        analyticsTracker.track(DOMAINS_SEARCH_SELECT_DOMAIN_TAPPED, site)
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

    internal fun Product.isSaleDomain(): Boolean = this.saleCost?.let { it.compareTo(0.0) > 0 } == true

    internal fun Product.saleCostForDisplay(): String = this.currencyCode + "%.2f".format(this.saleCost)

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
            selectDomain(selectedSuggestion)
        }
    }

    private fun selectDomain(selectedSuggestion: DomainSuggestionItem) {
        val domainProductDetails = DomainProductDetails(selectedSuggestion.productId, selectedSuggestion.domainName)
        _onDomainSelected.postValue(Event(domainProductDetails))
        analyticsTracker.track(DOMAINS_PURCHASE_WEBVIEW_VIEWED, site)
    }

    private fun showLoadingButton(isLoading: Boolean) {
        _isButtonProgressBarVisible.postValue(isLoading)
        suggestions = suggestions.transform { list ->
            list.map { it.copy(isEnabled = !isLoading) }
        }
    }
}
