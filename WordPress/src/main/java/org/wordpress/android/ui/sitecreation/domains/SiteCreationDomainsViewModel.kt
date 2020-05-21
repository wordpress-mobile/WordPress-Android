package org.wordpress.android.ui.sitecreation.domains

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainErrorType
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.models.networkresource.ListState.Error
import org.wordpress.android.models.networkresource.ListState.Loading
import org.wordpress.android.models.networkresource.ListState.Ready
import org.wordpress.android.models.networkresource.ListState.Success
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainSuggestionsQuery.UserQuery
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsListItemUiState.DomainsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsListItemUiState.DomainsModelUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsListItemUiState.DomainsModelUiState.DomainsModelAvailableUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsListItemUiState.DomainsModelUiState.DomainsModelUnavailabilityUiState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsUiState.DomainsUiContentState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType
import org.wordpress.android.ui.sitecreation.misc.SiteCreationHeaderUiState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSearchInputUiState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.usecases.FetchDomainsUseCase
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

private const val THROTTLE_DELAY = 500L
private const val ERROR_CONTEXT = "domains"

class SiteCreationDomainsViewModel @Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val domainSanitizer: SiteCreationDomainSanitizer,
    private val fetchDomainsUseCase: FetchDomainsUseCase,
    private val tracker: SiteCreationTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val job = Job()
    private var fetchDomainsJob: Job? = null
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job
    private var isStarted = false
    private var segmentId by Delegates.notNull<Long>()

    private val _uiState: MutableLiveData<DomainsUiState> = MutableLiveData()
    val uiState: LiveData<DomainsUiState> = _uiState

    private var currentQuery: DomainSuggestionsQuery? = null
    private var listState: ListState<String> = ListState.Init()
    private var selectedDomain by Delegates.observable<String?>(null) { _, old, new ->
        if (old != new) {
            updateUiStateToContent(currentQuery, listState)
        }
    }

    private val _createSiteBtnClicked = SingleLiveEvent<String>()
    val createSiteBtnClicked: LiveData<String> = _createSiteBtnClicked

    private val _clearBtnClicked = SingleLiveEvent<Unit>()
    val clearBtnClicked = _clearBtnClicked

    private val _onHelpClicked = SingleLiveEvent<Unit>()
    val onHelpClicked: LiveData<Unit> = _onHelpClicked

    init {
        dispatcher.register(fetchDomainsUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchDomainsUseCase)
    }

    fun start(segmentId: Long) {
        if (isStarted) {
            return
        }
        this.segmentId = segmentId
        isStarted = true
        tracker.trackDomainsAccessed()
        resetUiState()
    }

    fun createSiteBtnClicked() {
        val domain = requireNotNull(selectedDomain) {
            "Create site button should not be visible if a domain is not selected"
        }
        tracker.trackDomainSelected(domain, currentQuery?.value ?: "")
        _createSiteBtnClicked.value = domain
    }

    fun onClearTextBtnClicked() {
        _clearBtnClicked.call()
    }

    fun onHelpClicked() {
        _onHelpClicked.call()
    }

    fun updateQuery(query: String) {
        updateQueryInternal(UserQuery(query))
    }

    private fun updateQueryInternal(query: DomainSuggestionsQuery?) {
        currentQuery = query
        selectedDomain = null
        fetchDomainsJob?.cancel() // cancel any previous requests
        if (query != null && !query.value.isBlank()) {
            fetchDomains(query)
        } else {
            resetUiState()
        }
    }

    private fun resetUiState() {
        updateUiStateToContent(null, Ready(emptyList()))
    }

    private fun fetchDomains(query: DomainSuggestionsQuery) {
        if (networkUtils.isNetworkAvailable()) {
            updateUiStateToContent(query, Loading(Ready(emptyList()), false))
            fetchDomainsJob = launch {
                delay(THROTTLE_DELAY)
                val onSuggestedDomains = fetchDomainsUseCase.fetchDomains(query.value, segmentId)
                withContext(mainDispatcher) {
                    onDomainsFetched(query, onSuggestedDomains)
                }
            }
        } else {
            tracker.trackErrorShown(ERROR_CONTEXT, SiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR)
            updateUiStateToContent(
                    query,
                    Error(listState, errorMessageResId = R.string.no_network_message)
            )
        }
    }

    private fun onDomainsFetched(query: DomainSuggestionsQuery, event: OnSuggestedDomains) {
        // We want to treat `INVALID_QUERY` as if it's an empty result, so we'll ignore it
        if (event.isError && event.error.type != SuggestDomainErrorType.INVALID_QUERY) {
            tracker.trackErrorShown(
                    ERROR_CONTEXT,
                    event.error.type?.toString() ?: SiteCreationErrorType.UNKNOWN.toString(),
                    event.error.message
            )
            updateUiStateToContent(
                    query,
                    Error(
                            listState,
                            errorMessageResId = R.string.site_creation_fetch_suggestions_error_unknown
                    )
            )
        } else {
            /**
             * We would like to show the domains that matches the current query at the top. For this, we split the
             * domain names into two, one part for the domain names that start with the current query plus `.` and the
             * other part for the others. We then combine them back again into a single list.
             */
            val domainNames = event.suggestions.map { it.domain_name }
                    .partition { it.startsWith("${query.value}.") }
                    .toList().flatten()
            updateUiStateToContent(query, Success(domainNames))
        }
    }

    private fun updateUiStateToContent(query: DomainSuggestionsQuery?, state: ListState<String>) {
        listState = state
        val isNonEmptyUserQuery = isNonEmptyUserQuery(query)
        updateUiState(
                DomainsUiState(
                        headerUiState = createHeaderUiState(
                                !isNonEmptyUserQuery
                        ),
                        searchInputUiState = createSearchInputUiState(
                                showProgress = state is Loading,
                                showClearButton = isNonEmptyUserQuery,
                                showDivider = state.data.isNotEmpty()
                        ),
                        contentState = createDomainsUiContentState(query, state),
                        createSiteButtonContainerVisibility = selectedDomain != null
                )
        )
    }

    private fun updateUiState(uiState: DomainsUiState) {
        _uiState.value = uiState
    }

    private fun createDomainsUiContentState(
        query: DomainSuggestionsQuery?,
        state: ListState<String>
    ): DomainsUiContentState {
        // Only treat it as an error if the search is user initiated
        val isError = isNonEmptyUserQuery(query) && state is Error

        val items = createSuggestionsUiStates(
                onRetry = { updateQueryInternal(query) },
                query = query?.value,
                data = state.data,
                errorFetchingSuggestions = isError,
                errorResId = if (isError) (state as Error).errorMessageResId else null
        )
        return if (items.isEmpty()) {
            if (isNonEmptyUserQuery(query) && (state is Success || state is Ready)) {
                DomainsUiContentState.Empty
            } else DomainsUiContentState.Initial
        } else {
            DomainsUiContentState.VisibleItems(items)
        }
    }

    private fun createSuggestionsUiStates(
        onRetry: () -> Unit,
        query: String?,
        data: List<String>,
        errorFetchingSuggestions: Boolean,
        @StringRes errorResId: Int?
    ): List<DomainsListItemUiState> {
        val items: ArrayList<DomainsListItemUiState> = ArrayList()
        if (errorFetchingSuggestions) {
            val errorUiState = DomainsFetchSuggestionsErrorUiState(
                    messageResId = errorResId ?: R.string.site_creation_fetch_suggestions_error_unknown,
                    retryButtonResId = R.string.button_retry
            )
            errorUiState.onItemTapped = onRetry
            items.add(errorUiState)
        } else {
            query?.let { value ->
                getDomainUnavailableUiState(value, data)?.let {
                    items.add(it)
                }
            }

            data.forEach { domainName ->
                val itemUiState = DomainsModelAvailableUiState(
                        domainName,
                        checked = domainName == selectedDomain
                )
                itemUiState.onItemTapped = { setSelectedDomainName(domainName) }
                items.add(itemUiState)
            }
        }
        return items
    }

    private fun getDomainUnavailableUiState(
        query: String,
        domains: List<String>
    ): DomainsModelUiState? {
        if (domains.isEmpty()) {
            return null
        }

        val sanitizedQuery = domainSanitizer.sanitizeDomainQuery(query)

        val isDomainUnavailable = (domains.find { domain ->
            domain.startsWith("$sanitizedQuery.")
        }).isNullOrEmpty()

        return if (isDomainUnavailable) {
            DomainsModelUnavailabilityUiState(
                    "$sanitizedQuery.wordpress.com",
                    UiStringRes(R.string.new_site_creation_unavailable_domain)
            )
        } else {
            null
        }
    }

    private fun createHeaderUiState(
        isVisible: Boolean
    ): SiteCreationHeaderUiState? {
        return if (isVisible) SiteCreationHeaderUiState(
                UiStringRes(R.string.new_site_creation_domain_header_title),
                UiStringRes(R.string.new_site_creation_domain_header_subtitle)
        ) else null
    }

    private fun createSearchInputUiState(
        showProgress: Boolean,
        showClearButton: Boolean,
        showDivider: Boolean
    ): SiteCreationSearchInputUiState {
        return SiteCreationSearchInputUiState(
                hint = UiStringRes(R.string.new_site_creation_search_domain_input_hint),
                showProgress = showProgress,
                showClearButton = showClearButton,
                showDivider = showDivider
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun setSelectedDomainName(domainName: String) {
        selectedDomain = domainName
    }

    private fun isNonEmptyUserQuery(query: DomainSuggestionsQuery?): Boolean =
            query is UserQuery && !query.value.isBlank()

    data class DomainsUiState(
        val headerUiState: SiteCreationHeaderUiState?,
        val searchInputUiState: SiteCreationSearchInputUiState,
        val contentState: DomainsUiContentState = DomainsUiContentState.Initial,
        val createSiteButtonContainerVisibility: Boolean
    ) {
        sealed class DomainsUiContentState(
            val emptyViewVisibility: Boolean,
            val items: List<DomainsListItemUiState>
        ) {
            object Initial : DomainsUiContentState(
                    emptyViewVisibility = false,
                    items = emptyList()
            )

            object Empty : DomainsUiContentState(
                    emptyViewVisibility = true,
                    items = emptyList()
            )

            class VisibleItems(items: List<DomainsListItemUiState>) : DomainsUiContentState(
                    emptyViewVisibility = false,
                    items = items
            )
        }
    }

    sealed class DomainsListItemUiState {
        var onItemTapped: (() -> Unit)? = null
        open val clickable: Boolean = false

        sealed class DomainsModelUiState(
            open val name: String,
            open val checked: Boolean,
            val radioButtonVisibility: Boolean,
            open val subTitle: UiString? = null,
            override val clickable: Boolean
        ) : DomainsListItemUiState() {
            data class DomainsModelAvailableUiState(
                override val name: String,
                override val checked: Boolean
            ) : DomainsModelUiState(name, checked, true, clickable = true)

            data class DomainsModelUnavailabilityUiState(
                override val name: String,
                override val subTitle: UiString
            ) : DomainsModelUiState(name, false, false, subTitle, false)
        }

        data class DomainsFetchSuggestionsErrorUiState(
            @StringRes val messageResId: Int,
            @StringRes val retryButtonResId: Int
        ) : DomainsListItemUiState()
    }

    /**
     * An organized way to separate user initiated searches from automatic searches so we can handle them differently.
     */
    private sealed class DomainSuggestionsQuery(val value: String) {
        /**
         * User initiated search.
         */
        class UserQuery(value: String) : DomainSuggestionsQuery(value)
    }
}
