package org.wordpress.android.ui.postsrs.terms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import uniffi.wp_api.AnyTermWithViewContext
import uniffi.wp_api.TermEndpointType
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class TermSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSiteRepository: SelectedSiteRepository,
    private val restClient: PostRsRestClient,
    private val resourceProvider: ResourceProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
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

    private var allTerms = listOf<AnyTermWithViewContext>()
    private val selectedIds =
        initialSelectedIds.toMutableSet()

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
            loadTerms()
        }
    }

    fun retry() {
        loadTerms()
    }

    fun onTermToggled(id: Long) {
        if (!selectedIds.remove(id)) {
            selectedIds.add(id)
        }
        rebuildUi()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        rebuildUi()
    }

    fun onAddTermClicked() {
        val parentOptions = if (isCategories) {
            allTerms.map { ParentOption(it.id, it.name) }
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
                val newId = withContext(Dispatchers.IO) {
                    restClient.createTerm(
                        currentSite,
                        endpointType,
                        name,
                        parentId
                    )
                }
                if (newId != null) {
                    selectedIds.add(newId)
                    reloadTerms()
                } else {
                    _events.trySend(
                        TermSelectionEvent.ShowSnackbar(
                            resourceProvider.getString(
                                R.string
                                    .post_rs_settings_term_create_error
                            )
                        )
                    )
                    _uiState.update {
                        it.copy(isCreating = false)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to create term",
                    e
                )
                _events.trySend(
                    TermSelectionEvent.ShowSnackbar(
                        resourceProvider.getString(
                            R.string
                                .post_rs_settings_term_create_error
                        )
                    )
                )
                _uiState.update {
                    it.copy(isCreating = false)
                }
            }
        }
    }

    fun onSaveClicked() {
        _events.trySend(
            TermSelectionEvent.FinishWithSelection(
                selectedIds.toLongArray()
            )
        )
    }

    fun onBackClicked() {
        _events.trySend(TermSelectionEvent.Finish)
    }

    private fun loadTerms() {
        val currentSite = site ?: return
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
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            fetchAndApplyTerms(currentSite)
        }
    }

    private fun reloadTerms() {
        val currentSite = site ?: return
        viewModelScope.launch {
            fetchAndApplyTerms(currentSite)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchAndApplyTerms(
        site: org.wordpress.android.fluxc.model.SiteModel,
    ) {
        try {
            val terms = withContext(Dispatchers.IO) {
                restClient.fetchAllTerms(site, endpointType)
            }
            allTerms = terms
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isCreating = false,
                    error = null
                )
            }
            rebuildUi()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.POSTS,
                "Failed to load terms",
                e
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isCreating = false,
                    error = resourceProvider.getString(
                        R.string.request_failed_message
                    )
                )
            }
        }
    }

    private fun rebuildUi() {
        val query = _uiState.value.searchQuery
            .trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allTerms
        } else {
            allTerms.filter {
                it.name.lowercase().contains(query)
            }
        }
        val sorted = sortAndIndent(filtered)
        _uiState.update { it.copy(terms = sorted) }
    }

    private fun sortAndIndent(
        terms: List<AnyTermWithViewContext>,
    ): List<SelectableTerm> {
        if (!isCategories) {
            return terms.sortedBy { it.name.lowercase() }
                .map {
                    SelectableTerm(
                        id = it.id,
                        name = it.name,
                        level = 0,
                        isSelected = it.id in selectedIds
                    )
                }
        }
        val hierarchical = sortByHierarchy(terms)
        val termsById = allTerms.associateBy { it.id }
        return hierarchical.map { term ->
            SelectableTerm(
                id = term.id,
                name = term.name,
                level = getIndentation(termsById, term),
                isSelected = term.id in selectedIds
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
                .sortedBy { it.name.lowercase() }
                .forEach { addWithChildren(it) }
        }

        terms.filter {
            it.parent == 0L ||
                it.parent == null ||
                termsById[it.parent] == null
        }
            .sortedBy { it.name.lowercase() }
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

    companion object {
        const val EXTRA_IS_CATEGORIES =
            "extra_is_categories"
        const val EXTRA_SELECTED_IDS =
            "extra_selected_ids"
        const val RESULT_SELECTED_IDS =
            "result_selected_ids"
    }
}
