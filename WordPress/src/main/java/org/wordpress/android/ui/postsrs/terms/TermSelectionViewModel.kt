package org.wordpress.android.ui.postsrs.terms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import uniffi.wp_api.AnyTermWithViewContext
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.TermListParams
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
@Suppress("TooManyFunctions")
class TermSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSiteRepository: SelectedSiteRepository,
    private val restClient: PostRsRestClient,
    private val resourceProvider: ResourceProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val isCategories: Boolean =
        savedStateHandle[EXTRA_IS_CATEGORIES] ?: true

    private val initialSelectedIds: LongArray =
        savedStateHandle[EXTRA_SELECTED_IDS] ?: longArrayOf()

    val endpointType: TermEndpointType =
        if (isCategories) {
            TermEndpointType.Categories
        } else {
            TermEndpointType.Tags
        }

    private val site =
        selectedSiteRepository.getSelectedSite()

    private val _uiState =
        MutableStateFlow(
            TermSelectionUiState(
                isHierarchical = isCategories
            )
        )
    val uiState: StateFlow<TermSelectionUiState> =
        _uiState.asStateFlow()

    private val _events =
        Channel<TermSelectionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadedTerms =
        mutableListOf<AnyTermWithViewContext>()
    private val selectedIds =
        initialSelectedIds.toMutableSet()
    private var searchJob: Job? = null

    init {
        if (site == null) {
            _events.trySend(
                TermSelectionEvent.ShowSnackbar(
                    resourceProvider.getString(
                        R.string.blog_not_found
                    )
                )
            )
            _events.trySend(TermSelectionEvent.Finish)
        } else {
            loadFirstPage()
        }
    }

    fun retry() {
        loadFirstPage()
    }

    fun onTermToggled(id: Long) {
        if (!selectedIds.remove(id)) {
            selectedIds.add(id)
        }
        rebuildUi()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isNotEmpty() &&
            trimmed.length < MIN_SEARCH_QUERY_LENGTH
        ) {
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update {
                it.copy(isSearching = true)
            }
            loadFirstPage(showLoading = false)
        }
    }

    fun onAddTermClicked() {
        val parentOptions = if (isCategories) {
            loadedTerms.map {
                ParentOption(it.id, displayName(it))
            }
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(
                showAddDialog = true,
                parentOptions = parentOptions
            )
        }
    }

    fun onAddDialogDismissed() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    @Suppress("TooGenericExceptionCaught")
    fun onAddTermConfirmed(
        name: String,
        parentId: Long?,
    ) {
        val currentSite = site ?: return
        _uiState.update {
            it.copy(
                showAddDialog = false,
                isCreating = true
            )
        }
        viewModelScope.launch {
            try {
                val newId = withContext(ioDispatcher) {
                    restClient.createTerm(
                        currentSite,
                        endpointType,
                        name,
                        parentId
                    )
                }
                if (newId != null) {
                    selectedIds.add(newId)
                    loadFirstPage(showLoading = false)
                } else {
                    handleCreateTermError()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to create term",
                    e
                )
                handleCreateTermError()
            }
        }
    }

    fun onSaveClicked() {
        _events.trySend(
            TermSelectionEvent.FinishWithSelection(
                selectedIds.toList()
            )
        )
    }

    fun onBackClicked() {
        _events.trySend(TermSelectionEvent.Finish)
    }

    private fun loadFirstPage(
        showLoading: Boolean = true,
    ) {
        val currentSite = site ?: return
        if (showLoading) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                _uiState.value = TermSelectionUiState(
                    isLoading = false,
                    isHierarchical = isCategories,
                    error = resourceProvider.getString(
                        R.string.error_generic_network
                    )
                )
                return
            }
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }
        }
        viewModelScope.launch {
            loadedTerms.clear()
            val search = _uiState.value.searchQuery
                .trim().ifEmpty { null }
            fetchAllPages(currentSite, search)
        }
    }

    @Suppress("TooGenericExceptionCaught", "LongMethod")
    private suspend fun fetchAllPages(
        site: SiteModel,
        search: String? = null,
    ) {
        var nextPageParams: TermListParams? = null
        do {
            try {
                val result = withContext(ioDispatcher) {
                    restClient.fetchTermsPage(
                        site,
                        endpointType,
                        search = search,
                        nextPageParams = nextPageParams,
                    )
                }
                loadedTerms.addAll(result.terms)
                nextPageParams = result.nextPageParams
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to load terms",
                    e
                )
                if (loadedTerms.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSearching = false,
                            isCreating = false,
                            error = resourceProvider.getString(
                                R.string.request_failed_message
                            )
                        )
                    }
                    return
                }
                nextPageParams = null
            }
        } while (nextPageParams != null)

        _uiState.update {
            it.copy(
                isLoading = false,
                isSearching = false,
                isCreating = false,
                error = null,
            )
        }
        rebuildUi()
    }

    private fun rebuildUi() {
        val hasSearchQuery = _uiState.value.searchQuery
            .trim().isNotEmpty()
        val terms = if (
            isCategories && !hasSearchQuery
        ) {
            sortByHierarchy(loadedTerms)
        } else {
            loadedTerms.toList()
        }
        val termsById = loadedTerms.associateBy { it.id }
        val selectable = terms.map { term ->
            SelectableTerm(
                id = term.id,
                name = displayName(term),
                level = if (
                    isCategories && !hasSearchQuery
                ) {
                    getIndentation(termsById, term)
                } else {
                    0
                },
                isSelected = term.id in selectedIds,
            )
        }
        val hasChanges =
            selectedIds != initialSelectedIds.toSet()
        _uiState.update {
            it.copy(
                terms = selectable,
                hasChanges = hasChanges,
            )
        }
    }

    private fun sortByHierarchy(
        terms: List<AnyTermWithViewContext>,
    ): List<AnyTermWithViewContext> {
        val result =
            mutableListOf<AnyTermWithViewContext>()
        val termsById = terms.associateBy { it.id }
        val visited = mutableSetOf<Long>()

        fun addWithChildren(
            term: AnyTermWithViewContext,
        ) {
            if (term.id in visited) return
            visited.add(term.id)
            result.add(term)
            terms.filter { it.parent == term.id }
                .sortedBy { displayName(it).lowercase() }
                .forEach { addWithChildren(it) }
        }

        terms.filter {
            it.parent == 0L ||
                it.parent == null ||
                termsById[it.parent] == null
        }
            .sortedBy { displayName(it).lowercase() }
            .forEach { addWithChildren(it) }

        return result
    }

    private fun getIndentation(
        termsById: Map<Long, AnyTermWithViewContext>,
        term: AnyTermWithViewContext,
    ): Int {
        var level = 0
        var parentId = term.parent
        while (parentId != null && parentId > 0) {
            val parent = termsById[parentId] ?: break
            level++
            parentId = parent.parent
        }
        return level
    }

    private fun displayName(
        term: AnyTermWithViewContext,
    ) = term.name.ifBlank { term.slug }

    private fun handleCreateTermError() {
        _events.trySend(
            TermSelectionEvent.ShowSnackbar(
                resourceProvider.getString(
                    R.string
                        .post_rs_settings_term_create_error
                )
            )
        )
        _uiState.update { it.copy(isCreating = false) }
    }

    companion object {
        const val EXTRA_IS_CATEGORIES =
            "extra_is_categories"
        const val EXTRA_SELECTED_IDS =
            "extra_selected_ids"
        const val RESULT_SELECTED_IDS =
            "result_selected_ids"
        private const val SEARCH_DEBOUNCE_MS = 500L
        private const val MIN_SEARCH_QUERY_LENGTH = 3
    }
}
