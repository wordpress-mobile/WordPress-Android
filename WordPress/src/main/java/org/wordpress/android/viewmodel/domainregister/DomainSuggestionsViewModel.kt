package org.wordpress.android.viewmodel.domainregister

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.support.annotation.VisibleForTesting
import android.text.TextUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.helpers.Debouncer
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

typealias DomainSuggestionsListState = ListState<DomainSuggestionResponse>

class DomainSuggestionsViewModel @Inject constructor(
    private val dispatcher: Dispatcher
) : ViewModel() {
    lateinit var site: SiteModel
    private var isStarted = false
    private var debouncer = Debouncer()

    @VisibleForTesting
    constructor(dispatcher: Dispatcher, debouncer: Debouncer) : this(dispatcher = dispatcher) {
        this.debouncer = debouncer
    }

    private val _suggestions = MutableLiveData<DomainSuggestionsListState>()
    val suggestionsLiveData: LiveData<List<DomainSuggestionResponse>>
        get() = Transformations.map(_suggestions) { it?.data ?: emptyList() }

    val isLoadingInProgress: LiveData<Boolean>
        get() = Transformations.map(_suggestions) { it?.isFetchingFirstPage() == true }

    private var suggestions: ListState<DomainSuggestionResponse>
            by Delegates.observable(ListState.Init()) { _, _, new ->
                _suggestions.postValue(new)
            }

    private val _selectedSuggestion = MutableLiveData<DomainSuggestionResponse?>()
    val selectedSuggestion: LiveData<DomainSuggestionResponse?>
        get() = _selectedSuggestion
    val shouldEnableChooseDomain: LiveData<Boolean>
        get() = Transformations.map(_selectedSuggestion) { it is DomainSuggestionResponse }

    private val _selectedPosition = MutableLiveData<Int>()
    val selectedPosition: LiveData<Int>
        get() = _selectedPosition

    private var searchQuery: String by Delegates.observable("") { _, oldValue, newValue ->
        if (newValue != oldValue) {
            debouncer.debounce(Void::class.java, {
                fetchSuggestions()
            }, GET_SUGGESTIONS_INTERVAL_MS, TimeUnit.MILLISECONDS)
        }
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
        isStarted = true
        initializeDefaultSuggestions()
    }

    private fun initializeDefaultSuggestions() {
        searchQuery = site.name
    }

    // Network Request

    private fun fetchSuggestions() {
        suggestions = ListState.Loading(suggestions)

        val suggestDomainsPayload =
                SuggestDomainsPayload(searchQuery, false, false, true, SUGGESTIONS_REQUEST_COUNT)
        dispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(suggestDomainsPayload))

        // Reset the selected suggestion, if list is updated
        onDomainSuggestionsSelected(null, -1)
    }

    // Network Callback

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onDomainSuggestionsFetched(event: OnSuggestedDomains) {
        if (searchQuery != event.query) {
            return
        }
        if (event.isError) {
            AppLog.e(T.DOMAIN_REGISTRATION,
                    "An error occurred while fetching the domain suggestions with type: " + event.error.type)
            suggestions = ListState.Error(suggestions, event.error.message)
            return
        }
        suggestions = ListState.Success(event.suggestions)
    }

    fun onDomainSuggestionsSelected(selectedSuggestion: DomainSuggestionResponse?, selectedPosition: Int) {
        _selectedPosition.postValue(selectedPosition)
        _selectedSuggestion.postValue(selectedSuggestion)
    }

    fun updateSearchQuery(query: String) {
        if (!TextUtils.isEmpty(query)) {
            searchQuery = query
        } else if (searchQuery != site.name) {
            // Only reinitialize the search query, if it has changed.
            initializeDefaultSuggestions()
        }
    }

    companion object {
        private const val SUGGESTIONS_REQUEST_COUNT = 30
        private const val GET_SUGGESTIONS_INTERVAL_MS = 250L
    }
}
