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
import org.apache.commons.lang3.StringUtils
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
import org.wordpress.android.ui.sitecreation.domain.NewSiteCreationDomainsViewModel.DomainsListItemUiState.DomainsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.domain.NewSiteCreationDomainsViewModel.DomainsListItemUiState.DomainsModelUiState
import org.wordpress.android.ui.sitecreation.domain.NewSiteCreationDomainsViewModel.DomainsUiState.DomainsUiContentState
import org.wordpress.android.ui.sitecreation.usecases.FetchDomainsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext

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

    private var listState: ListState<String> = ListState.Init()

    private val _clearBtnClicked = SingleLiveEvent<Void>()
    val clearBtnClicked = _clearBtnClicked

    private val _domainSelected = SingleLiveEvent<String>()
    val domainSelected: LiveData<String> = _domainSelected

    init {
        dispatcher.register(fetchDomainsUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchDomainsUseCase)
    }

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
        resetUiState()
    }

    fun onClearTextBtnClicked() {
        _clearBtnClicked.call()
    }

    fun updateQuery(query: String) {
        fetchDomainsJob?.cancel() // cancel any previous requests
        if (query.isNotEmpty()) {
            fetchDomains(query)
        } else {
            resetUiState()
        }
    }

    private fun resetUiState() {
        updateUiStateToContent("", ListState.Ready(emptyList()))
    }

    private fun fetchDomains(query: String) {
        if (networkUtils.isNetworkAvailable()) {
            updateUiStateToContent(query, Loading(Ready(emptyList()), false))
            fetchDomainsJob = launch {
                delay(THROTTLE_DELAY)
                val payload = SuggestDomainsPayload(
                        query,
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
            // TODO: What do we do if the network is not available
        }
    }

    private fun onDomainsFetched(query: String, event: OnSuggestedDomains) {
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

    private fun updateUiStateToContent(query: String, state: ListState<String>) {
        listState = state
        updateUiState(
                DomainsUiState(
                        headerUiState = createHeaderUiState(
                                shouldShowHeader(query)
                        ),
                        searchInputUiState = createSearchInputUiState(
                                query,
                                showProgress = state is Loading
                        ),
                        contentState = createDomainsUiContentState(query, state)
                )
        )
    }

    private fun updateUiState(uiState: DomainsUiState) {
        _uiState.value = uiState
    }

    private fun createDomainsUiContentState(query: String, state: ListState<String>): DomainsUiContentState {
        val items = createSuggestionsUiStates(
                onRetry = { updateQuery(query) },
                data = state.data,
                errorFetchingSuggestions = state is Error,
                errorResId = if (state is Error) state.errorMessageResId else null
        )
        return if (items.isEmpty()) {
            if (query.isNotBlank() && (state is Success || state is Ready)) {
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
                            showDivider = index != lastItemIndex
                    )
                    itemUiState.onItemTapped = { _domainSelected.value = itemUiState.name }
                    items.add(itemUiState)
            }
        }
        return items
    }

    private fun shouldShowHeader(query: String): Boolean {
        return StringUtils.isEmpty(query)
    }

    private fun createHeaderUiState(
        isVisible: Boolean
    ): SiteCreationHeaderUiState? {
        // TODO: We need to pass in a resource for the title and subtitle
        return if (isVisible) SiteCreationHeaderUiState("title", "subtitle") else null
    }

    private fun createSearchInputUiState(
        query: String,
        showProgress: Boolean
    ): SiteCreationSearchInputUiState {
        return SiteCreationSearchInputUiState(
                "hint", // TODO: We need to pass in resource
                showProgress,
                showClearButton = !StringUtils.isEmpty(query)
        )
    }

    data class DomainsUiState(
        val headerUiState: SiteCreationHeaderUiState?,
        val searchInputUiState: SiteCreationSearchInputUiState,
        val contentState: DomainsUiContentState = DomainsUiContentState.Initial
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

        data class DomainsModelUiState(val name: String, val showDivider: Boolean) :
                DomainsListItemUiState()

        data class DomainsFetchSuggestionsErrorUiState(
            @StringRes val messageResId: Int,
            @StringRes val retryButtonResId: Int
        ) : DomainsListItemUiState()
    }
}
