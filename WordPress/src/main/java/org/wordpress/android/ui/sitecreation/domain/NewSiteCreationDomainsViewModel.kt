package org.wordpress.android.ui.sitecreation.domain

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.StringRes
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.models.networkresource.ListState.Error
import org.wordpress.android.models.networkresource.ListState.Loading
import org.wordpress.android.models.networkresource.ListState.Ready
import org.wordpress.android.models.networkresource.ListState.Success
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.modules.MAIN_DISPATCHER
import org.wordpress.android.ui.sitecreation.SiteCreationHeaderUiState
import org.wordpress.android.ui.sitecreation.SiteCreationSearchInputUiState
import org.wordpress.android.ui.sitecreation.domain.NewSiteCreationDomainsViewModel.DomainSuggestionsQuery.TitleQuery
import org.wordpress.android.ui.sitecreation.domain.NewSiteCreationDomainsViewModel.DomainSuggestionsQuery.UserQuery
import org.wordpress.android.ui.sitecreation.domain.NewSiteCreationDomainsViewModel.DomainsListItemUiState.DomainsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.domain.NewSiteCreationDomainsViewModel.DomainsListItemUiState.DomainsModelUiState
import org.wordpress.android.ui.sitecreation.domain.NewSiteCreationDomainsViewModel.DomainsUiState.DomainsUiContentState
import org.wordpress.android.ui.sitecreation.usecases.FetchDomainsUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.properties.Delegates

private const val FETCH_DOMAINS_SHOULD_ONLY_FETCH_WORDPRESS_COM_DOMAINS = true
private const val FETCH_DOMAINS_SHOULD_INCLUDE_WORDPRESS_COM_DOMAINS = true
private const val FETCH_DOMAINS_SHOULD_INCLUDE_DOT_BLOG_SUB_DOMAINS = false
private const val FETCH_DOMAINS_SIZE = 20

private const val THROTTLE_DELAY: Int = 500

class NewSiteCreationDomainsViewModel @Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchDomainsUseCase: FetchDomainsUseCase,
    @Named(IO_DISPATCHER) private val IO: CoroutineContext,
    @Named(MAIN_DISPATCHER) private val MAIN: CoroutineContext
) : ViewModel(), CoroutineScope {
    private val job = Job()
    private var fetchDomainsJob: Job? = null
    override val coroutineContext: CoroutineContext
        get() = IO + job
    private var isStarted = false

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

    private val _clearBtnClicked = SingleLiveEvent<Void>()
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

    fun start(siteTitle: String?) {
        if (isStarted) {
            return
        }
        isStarted = true
        // isNullOrBlank not smart-casting for some reason..
        if (siteTitle == null || siteTitle.isBlank()) {
            resetUiState()
        } else {
            updateQueryInternal(TitleQuery(siteTitle))
        }
    }

    fun createSiteBtnClicked() {
        requireNotNull(selectedDomain) {
            "Create site button should not be visible if a domain is not selected"
        }
        _createSiteBtnClicked.value = selectedDomain
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
        updateUiStateToContent(null, ListState.Ready(emptyList()))
    }

    private fun fetchDomains(query: DomainSuggestionsQuery) {
        if (networkUtils.isNetworkAvailable()) {
            updateUiStateToContent(query, Loading(Ready(emptyList()), false))
            fetchDomainsJob = launch {
                delay(THROTTLE_DELAY)
                val payload = SuggestDomainsPayload(
                        query.value,
                        FETCH_DOMAINS_SHOULD_ONLY_FETCH_WORDPRESS_COM_DOMAINS,
                        FETCH_DOMAINS_SHOULD_INCLUDE_WORDPRESS_COM_DOMAINS,
                        FETCH_DOMAINS_SHOULD_INCLUDE_DOT_BLOG_SUB_DOMAINS,
                        FETCH_DOMAINS_SIZE
                )
                val onSuggestedDomains = fetchDomainsUseCase.fetchDomains(payload)
                withContext(MAIN) {
                    onDomainsFetched(query, onSuggestedDomains)
                }
            }
        } else {
            updateUiStateToContent(query, ListState.Error(listState, errorMessageResId = R.string.no_network_message))
        }
    }

    private fun onDomainsFetched(query: DomainSuggestionsQuery, event: OnSuggestedDomains) {
        if (event.isError) {
            updateUiStateToContent(
                    query,
                    ListState.Error(
                            listState,
                            errorMessageResId = R.string.site_creation_fetch_suggestions_error_unknown
                    )
            )
        } else {
            updateUiStateToContent(query, ListState.Success(event.suggestions.map { it.domain_name }))
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
                                showClearButton = isNonEmptyUserQuery
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
        val items = createSuggestionsUiStates(
                onRetry = { updateQueryInternal(query) },
                data = state.data,
                errorFetchingSuggestions = state is Error,
                errorResId = if (state is Error) state.errorMessageResId else null
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
            val lastItemIndex = data.size - 1
            data.forEachIndexed { index, domainName ->
                val itemUiState = DomainsModelUiState(
                        domainName,
                        showDivider = index != lastItemIndex,
                        checked = domainName == selectedDomain
                )
                itemUiState.onItemTapped = { selectedDomain = domainName }
                items.add(itemUiState)
            }
        }
        return items
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
        showClearButton: Boolean
    ): SiteCreationSearchInputUiState {
        return SiteCreationSearchInputUiState(
                hint = UiStringRes(R.string.new_site_creation_search_domain_input_hint),
                showProgress = showProgress,
                showClearButton = showClearButton
        )
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

        data class DomainsModelUiState(val name: String, val showDivider: Boolean, val checked: Boolean) :
                DomainsListItemUiState()

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

        /**
         * Automatic search initiated for the site title.
         */
        class TitleQuery(value: String) : DomainSuggestionsQuery(value)
    }
}
