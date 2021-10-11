package org.wordpress.android.ui.domains

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.SiteDomainsFeatureConfig
import org.wordpress.android.util.helpers.Debouncer
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

class DomainSuggestionsViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val dispatcher: Dispatcher,
    private val debouncer: Debouncer,
    private val siteDomainsFeatureConfig: SiteDomainsFeatureConfig
) : ViewModel() {
    lateinit var site: SiteModel
    lateinit var domainRegistrationPurpose: DomainRegistrationPurpose

    private var isStarted = false
    private var isQueryTrackingCompleted = false

    private val _suggestions = MutableLiveData<ListState<DomainSuggestionItem>>()
    val suggestionsLiveData: LiveData<ListState<DomainSuggestionItem>> = _suggestions

    private var suggestions: ListState<DomainSuggestionItem>
            by Delegates.observable(ListState.Init()) { _, _, new ->
                _suggestions.postValue(new)
            }

    private val _selectedSuggestion = MutableLiveData<DomainSuggestionItem?>()
    val selectedSuggestion: LiveData<DomainSuggestionItem?> = _selectedSuggestion

    val choseDomainButtonEnabledState = Transformations.map(_selectedSuggestion) { it is DomainSuggestionItem }

    private val _isIntroVisible = MutableLiveData(true)
    val isIntroVisible: LiveData<Boolean> = _isIntroVisible

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
        super.onCleared()
    }

    fun start(site: SiteModel, domainRegistrationPurpose: DomainRegistrationPurpose) {
        if (isStarted) {
            return
        }
        this.site = site
        this.domainRegistrationPurpose = domainRegistrationPurpose
        initializeDefaultSuggestions()
        isStarted = true
    }

    private fun initializeDefaultSuggestions() {
        searchQuery = site.name
    }

    // Network Request

    private fun fetchSuggestions() {
        suggestions = ListState.Loading(suggestions)

        val suggestDomainsPayload = if (SiteUtils.onBloggerPlan(site)) {
            SuggestDomainsPayload(searchQuery, SUGGESTIONS_REQUEST_COUNT, BLOG_DOMAIN_TLDS)
        } else {
            SuggestDomainsPayload(searchQuery, false, false, true, SUGGESTIONS_REQUEST_COUNT, false)
        }

        dispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(suggestDomainsPayload))

        // Reset the selected suggestion, if list is updated
        onDomainSuggestionsSelected(null)
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
                .map {
                    DomainSuggestionItem(
                            domainName = it.domain_name,
                            cost = it.cost,
                            isFree = it.is_free,
                            supportsPrivacy = it.supports_privacy,
                            productId = it.product_id,
                            productSlug = it.product_slug,
                            vendor = it.vendor,
                            relevance = it.relevance,
                            isSelected = _selectedSuggestion.value?.domainName == it.domain_name,
                            isCostVisible = siteDomainsFeatureConfig.isEnabled(),
                            isFreeWithCredits = domainRegistrationPurpose == CTA_DOMAIN_CREDIT_REDEMPTION
                    )
                }
                .sortedBy { it.relevance }
                .asReversed()
                .let {
                    suggestions = ListState.Success(it)
                }
    }

    fun onDomainSuggestionsSelected(selectedSuggestion: DomainSuggestionItem?) {
        _selectedSuggestion.postValue(selectedSuggestion)
        suggestions = suggestions.transform { list ->
            list.map { it.copy(isSelected = selectedSuggestion?.domainName == it.domainName) }
        }
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
}
