package org.wordpress.android.viewmodel.domains

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationHandler
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.helpers.Debouncer
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

typealias DomainSuggestionsListState = ListState<DomainSuggestionResponse>

class DomainSuggestionsViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val dispatcher: Dispatcher,
    private val debouncer: Debouncer,
    private val domainRegistrationHandler: DomainRegistrationHandler
) : ViewModel() {
    lateinit var site: SiteModel
    var isDomainCreditAvailable: Boolean = false
    private var isStarted = false
    private var isQueryTrackingCompleted = false

    private val _suggestions = MutableLiveData<DomainSuggestionsListState>()
    val suggestionsLiveData: LiveData<DomainSuggestionsListState>
        get() = _suggestions

    private var suggestions: ListState<DomainSuggestionResponse>
            by Delegates.observable(ListState.Init()) { _, _, new ->
                _suggestions.postValue(new)
            }

    private val _selectedSuggestion = MutableLiveData<DomainSuggestionResponse?>()
    val selectedSuggestion: LiveData<DomainSuggestionResponse?>
        get() = _selectedSuggestion

    val choseDomainButtonEnabledState: LiveData<Boolean>
        get() = Transformations.map(_selectedSuggestion) { it is DomainSuggestionResponse }

    private val _selectedPosition = MutableLiveData<Int>()
    val selectedPosition: LiveData<Int>
        get() = _selectedPosition

    private val _isIntroVisible = MutableLiveData<Boolean>().apply { value = true }
    val isIntroVisible: LiveData<Boolean>
        get() = _isIntroVisible

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

    fun start(site: SiteModel) {
        if (isStarted) {
            return
        }
        this.site = site
        checkDomainCreditAvailability()
        initializeDefaultSuggestions()
        isStarted = true
    }

    private fun initializeDefaultSuggestions() {
        searchQuery = site.name
    }

    private fun checkDomainCreditAvailability() {
        val domainCreditAvailable =
                domainRegistrationHandler.buildSource(viewModelScope, site.id).distinctUntilChanged()
        isDomainCreditAvailable = domainCreditAvailable.value?.isDomainCreditAvailable == true
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
        onDomainSuggestionsSelected(null, -1)
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

        val sortedDomainSuggestions = event.suggestions.sortedBy { it.relevance }.asReversed()
        suggestions = ListState.Success(sortedDomainSuggestions)
    }

    fun onDomainSuggestionsSelected(selectedSuggestion: DomainSuggestionResponse?, selectedPosition: Int) {
        _selectedPosition.postValue(selectedPosition)
        _selectedSuggestion.postValue(selectedSuggestion)
    }

    fun updateSearchQuery(query: String) {
        _isIntroVisible.value = query.isEmpty()

        if (!TextUtils.isEmpty(query)) {
            searchQuery = query
        } else if (searchQuery != site.name) {
            // Only reinitialize the search query, if it has changed.
            initializeDefaultSuggestions()
        }
    }
}
