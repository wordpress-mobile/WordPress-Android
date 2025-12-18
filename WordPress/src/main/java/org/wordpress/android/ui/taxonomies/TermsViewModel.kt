package org.wordpress.android.ui.taxonomies

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.model.TermsModel
import org.wordpress.android.fluxc.network.TrackNetworkRequestsInterceptor
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY
import org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_TAG
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.dataview.DataViewDropdownItem
import org.wordpress.android.ui.dataview.DataViewFieldType
import org.wordpress.android.ui.dataview.DataViewItem
import org.wordpress.android.ui.dataview.DataViewItemField
import org.wordpress.android.ui.dataview.DataViewViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyTermWithEditContext
import uniffi.wp_api.TermCreateParams
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.TermListParams
import uniffi.wp_api.TermUpdateParams
import uniffi.wp_api.WpApiParamOrder
import uniffi.wp_api.WpApiParamTermsOrderBy
import javax.inject.Inject
import javax.inject.Named

private const val INDENTATION_IN_DP = 10

enum class TermScreen {
    List,
    Detail,
    Create
}

data class TermDetailUiState(
    val termId: Long = 0L,
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val count: Long = 0L,
    val parentId: Long? = null,
    val availableParents: List<ParentOption>? = null,
)

data class ParentOption(
    val id: Long,
    val name: String
)

sealed class UiEvent {
    data class ShowError(val messageRes: Int) : UiEvent()
}

@HiltViewModel
class TermsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val taxonomyStore: TaxonomyStore,
    private val fluxCDispatcher: Dispatcher, // Used to include FluxC in the flow (local terms store)
    accountStore: AccountStore,
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    sharedPrefs: SharedPreferences,
    networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(IO_THREAD) ioDispatcher: CoroutineDispatcher,
    trackNetworkRequestsInterceptor: TrackNetworkRequestsInterceptor,
) : DataViewViewModel(
    mainDispatcher = mainDispatcher,
    appLogWrapper = appLogWrapper,
    sharedPrefs = sharedPrefs,
    networkUtilsWrapper = networkUtilsWrapper,
    selectedSiteRepository = selectedSiteRepository,
    accountStore = accountStore,
    ioDispatcher = ioDispatcher,
    trackNetworkRequestsInterceptor = trackNetworkRequestsInterceptor
) {
    private var taxonomySlug: String = ""
    private var isHierarchical: Boolean = false
    private var currentTerms = listOf<AnyTermWithEditContext>()
    private var navController: NavHostController? = null

    private val _termDetailState = MutableStateFlow<TermDetailUiState?>(null)
    val termDetailState: StateFlow<TermDetailUiState?> = _termDetailState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _uiEvent = MutableStateFlow<UiEvent?>(null)
    val uiEvent = _uiEvent.asStateFlow()

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun initialize(taxonomySlug: String, isHierarchical: Boolean) {
        this.taxonomySlug = taxonomySlug
        this.isHierarchical = isHierarchical
        taxonomyStore.onRegister()
        initialize()
    }

    fun navigateToTermDetail(termId: Long) {
        val term = currentTerms.firstOrNull { it.id == termId } ?: return

        val availableParents = if (isHierarchical) {
            val descendants = getDescendants(termId)
            currentTerms
                .filter { it.id != termId && it.id !in descendants }
                .map { ParentOption(id = it.id, name = it.name) }
        } else {
            null
        }

        _termDetailState.value = TermDetailUiState(
            termId = term.id,
            name = term.name,
            slug = term.slug,
            description = term.description,
            count = term.count,
            parentId = term.parent,
            availableParents = availableParents,
        )
        navController?.navigate(TermScreen.Detail.name)
    }

    fun navigateToCreateTerm() {
        val availableParents = if (isHierarchical) {
            currentTerms.map { ParentOption(id = it.id, name = it.name) }
        } else {
            null
        }

        _termDetailState.value = TermDetailUiState(
            termId = 0L, // 0 indicates a new term
            name = "",
            slug = "",
            description = "",
            count = 0L,
            parentId = 0L,
            availableParents = availableParents,
        )
        navController?.navigate(TermScreen.Create.name)
    }

    fun navigateBack() {
        clearTermDetail()
        navController?.navigateUp()
    }

    private fun getDescendants(termId: Long): Set<Long> {
        val descendants = mutableSetOf<Long>()

        fun addDescendantsRecursively(parentId: Long) {
            currentTerms.filter { it.parent == parentId }.forEach { child ->
                descendants.add(child.id)
                addDescendantsRecursively(child.id)
            }
        }

        addDescendantsRecursively(termId)
        return descendants
    }

    fun updateTermName(name: String) {
        _termDetailState.value = _termDetailState.value?.copy(name = name)
    }

    fun updateTermSlug(slug: String) {
        _termDetailState.value = _termDetailState.value?.copy(slug = slug)
    }

    fun updateTermDescription(description: String) {
        _termDetailState.value = _termDetailState.value?.copy(description = description)
    }

    fun updateTermParent(parentId: Long) {
        _termDetailState.value = _termDetailState.value?.copy(parentId = parentId)
    }

    fun clearTermDetail() {
        _termDetailState.value = null
    }

    override fun getLocalData(): List<DataViewItem> = when(val site = selectedSiteRepository.getSelectedSite()) {
        null -> emptyList()
        else -> {
            val terms = taxonomyStore.getTermsForSite(site, this.taxonomySlug)
            terms.map { term ->
                convertToDataViewItem(
                    terms,
                    term,
                    isHierarchical
                )
            }
        }
    }

    override fun getSupportedSorts(): List<DataViewDropdownItem> = if (isHierarchical) {
        listOf()
    } else {
        listOf(
            DataViewDropdownItem(id = SORT_BY_NAME_ID, titleRes = R.string.term_sort_by_name),
            DataViewDropdownItem(id = SORT_BY_COUNT_ID, titleRes = R.string.term_sort_by_count),
        )
    }

    override suspend fun performNetworkRequest(
        page: Int,
        searchQuery: String,
        filter: DataViewDropdownItem?,
        sortOrder: WpApiParamOrder,
        sortBy: DataViewDropdownItem?,
    ): List<DataViewItem> = withContext(ioDispatcher) {
        val selectedSite = selectedSiteRepository.getSelectedSite()

        if (selectedSite == null) {
            val error = "No selected site to get Terms"
            appLogWrapper.e(AppLog.T.API, error)
            onError(error)
            return@withContext emptyList()
        }

        val allTerms = getTermsList(selectedSite, page, searchQuery, sortOrder, sortBy)
        currentTerms = allTerms

        // Sort the results hierarchically if necessary
        val sortedTerms = if (isHierarchical) {
            sortByHierarchy(terms = allTerms)
        } else {
            allTerms
        }

        // Store terms when they are not filtered
        if (sortedTerms.isNotEmpty() && filter == null) {
            storeTerms(selectedSite, sortedTerms)
        }

        // Convert to DataViewItems and return
        sortedTerms.map { term ->
            // Do not use hierarchical indentation when the user is searching terms
            convertToDataViewItem(allTerms, term, isHierarchical && searchQuery.isEmpty())
        }
    }

    private fun sortByHierarchy(terms: List<AnyTermWithEditContext>): List<AnyTermWithEditContext> {
        val result = mutableListOf<AnyTermWithEditContext>()
        val termsById = terms.associateBy { it.id }
        val visited = mutableSetOf<Long>()

        fun addTermWithChildren(term: AnyTermWithEditContext) {
            if (term.id in visited) return
            visited.add(term.id)
            result.add(term)

            // Find and add all direct children
            terms.filter { it.parent == term.id }
                .sortedBy { it.name }
                .forEach { child ->
                    addTermWithChildren(child)
                }
        }

        // First, add all root terms (those with parent == 0 or no parent in the list)
        terms.filter { it.parent == 0L || termsById[it.parent] == null }
            .sortedBy { it.name }
            .forEach { rootTerm ->
                addTermWithChildren(rootTerm)
            }

        return result
    }

    private suspend fun storeTerms(site: SiteModel, terms: List<AnyTermWithEditContext>) = withContext(ioDispatcher) {
        val termsResponsePayload = FetchTermsResponsePayload(
            TermsModel(
                terms.map { term ->
                    TermModel(
                        term.id.toInt(),
                        site.id,
                        term.id,
                        taxonomySlug,
                        term.name,
                        term.slug,
                        term.description,
                        term.parent ?: 0,
                        term.parent != null,
                        term.count.toInt()
                    )
                },
            ),
            site,
            taxonomySlug
        )
        fluxCDispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(termsResponsePayload))
    }

    fun saveTerm() {
        viewModelScope.launch {
            val selectedSite = selectedSiteRepository.getSelectedSite()
            val currentTerm = _termDetailState.value
            if (selectedSite == null || currentTerm == null) {
                _uiEvent.value = UiEvent.ShowError(R.string.error_saving_term)
                return@launch
            }

            _isSaving.value = true

            val wpApiClient = wpApiClientProvider.getWpApiClient(selectedSite)

            val termsResponse = if (currentTerm.termId == 0L) {
                // Create new term
                wpApiClient.request { requestBuilder ->
                    requestBuilder.terms().create(
                        termEndpointType = getTermEndpointType(),
                        params = TermCreateParams(
                            name = currentTerm.name,
                            description = currentTerm.description,
                            slug = currentTerm.slug,
                            parent = if (isHierarchical) currentTerm.parentId else null
                        )
                    )
                }
            } else {
                // Update existing term
                wpApiClient.request { requestBuilder ->
                    requestBuilder.terms().update(
                        termEndpointType = getTermEndpointType(),
                        termId = currentTerm.termId,
                        params = TermUpdateParams(
                            name = currentTerm.name,
                            description = currentTerm.description,
                            slug = currentTerm.slug,
                            parent = currentTerm.parentId
                        )
                    )
                }
            }

            when (termsResponse) {
                is WpRequestResult.Success -> {
                    _isSaving.value = false
                    // Clear term detail to navigate back
                    clearTermDetail()
                    // Reload the list
                    initialize()
                }

                else -> {
                    _isSaving.value = false
                    _uiEvent.value = UiEvent.ShowError(R.string.error_saving_term)
                    appLogWrapper.e(AppLog.T.API, "Error saving term: $taxonomySlug")
                }
            }
        }
    }

    fun deleteTerm(termId: Long) {
        viewModelScope.launch {
            val selectedSite = selectedSiteRepository.getSelectedSite()
            if (selectedSite == null) {
                _uiEvent.value = UiEvent.ShowError(R.string.error_deleting_term)
                return@launch
            }

            _isDeleting.value = true

            val wpApiClient = wpApiClientProvider.getWpApiClient(selectedSite)

            val deleteResponse = wpApiClient.request { requestBuilder ->
                requestBuilder.terms().delete(
                    termEndpointType = getTermEndpointType(),
                    termId = termId
                )
            }

            when (deleteResponse) {
                is WpRequestResult.Success -> {
                    _isDeleting.value = false
                    if (deleteResponse.response.data.deleted) {
                        // Clear term detail to navigate back
                        clearTermDetail()
                        // Reload the list
                        initialize()
                    } else {
                        _uiEvent.value = UiEvent.ShowError(R.string.error_deleting_term)
                        appLogWrapper.e(AppLog.T.API, "Term was not deleted: $taxonomySlug")
                    }
                }

                else -> {
                    _isDeleting.value = false
                    _uiEvent.value = UiEvent.ShowError(R.string.error_deleting_term)
                    appLogWrapper.e(AppLog.T.API, "Error deleting term: $taxonomySlug")
                }
            }
        }
    }

    private fun convertToDataViewItem(
        allTerms: List<AnyTermWithEditContext>,
        term: AnyTermWithEditContext,
        useHierarchicalIndentation: Boolean
    ): DataViewItem {
        val indentation = if (useHierarchicalIndentation) {
            getHierarchicalIndentation(allTerms, term)
        } else {
            0
        }
        return DataViewItem(
            id = term.id,
            image = null,
            title = term.name,
            fields = listOf(
                DataViewItemField(
                    value = context.resources.getString(R.string.term_count, term.count),
                    valueType = DataViewFieldType.TEXT,
                )
            ),
            skipEndPositioning = true,
            data = term,
            indentation = (indentation * INDENTATION_IN_DP).dp
        )
    }

    private fun convertToDataViewItem(
        allTerms: List<TermModel>,
        term: TermModel,
        useHierarchicalIndentation: Boolean
    ): DataViewItem {
        val indentation = if (useHierarchicalIndentation) {
            getHierarchicalIndentation(allTerms, term)
        } else {
            0
        }
        return DataViewItem(
            id = term.remoteTermId,
            image = null,
            title = term.name,
            fields = listOf(
                DataViewItemField(
                    value = context.resources.getString(R.string.term_count, term.postCount),
                    valueType = DataViewFieldType.TEXT,
                )
            ),
            skipEndPositioning = true,
            data = term,
            indentation = (indentation * INDENTATION_IN_DP).dp
        )
    }


    /**
     * Returns an integer representation of the hierarchical indentation for the given term.
     */
    private fun getHierarchicalIndentation(
        allTerms: List<AnyTermWithEditContext>,
        term: AnyTermWithEditContext?
    ): Int {
        if (term == null) return 0

        val termsById = allTerms.associateBy { it.id }
        var indentation = 0
        var currentParentId = term.parent

        while (currentParentId != null && currentParentId > 0) {
            val parent = termsById[currentParentId]
            if (parent == null) break
            indentation++
            currentParentId = parent.parent
        }

        return indentation
    }

    /**
     * Returns an integer representation of the hierarchical indentation for the given term.
     */
    private fun getHierarchicalIndentation(
        allTerms: List<TermModel>,
        term: TermModel?
    ): Int {
        if (term == null) return 0

        val termsById = allTerms.associateBy { it.remoteTermId }
        var indentation = 0
        var currentParentId = term.parentRemoteId

        while (currentParentId > 0) {
            val parent = termsById[currentParentId]
            if (parent == null) break
            indentation++
            currentParentId = parent.parentRemoteId
        }

        return indentation
    }

    private suspend fun getTermsList(
        site: SiteModel,
        page: Int,
        searchQuery: String,
        sortOrder: WpApiParamOrder,
        sortBy: DataViewDropdownItem?
    ): List<AnyTermWithEditContext> {
        val wpApiClient = wpApiClientProvider.getWpApiClient(site)

        val termsResponse = wpApiClient.request { requestBuilder ->
            requestBuilder.terms().listWithEditContext(
                termEndpointType = getTermEndpointType(),
                params = TermListParams(
                    page = page.toUInt(),
                    search = searchQuery,
                    order = when (sortOrder) {
                        WpApiParamOrder.ASC -> WpApiParamOrder.ASC
                        WpApiParamOrder.DESC -> WpApiParamOrder.DESC
                    },
                    orderby = if (sortBy == null) {
                        null
                    } else {
                        if (sortBy.id == SORT_BY_COUNT_ID) {
                            WpApiParamTermsOrderBy.COUNT
                        } else {
                            WpApiParamTermsOrderBy.NAME // default
                        }
                    }
                )
            )
        }

        return when (termsResponse) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.API, "Fetched ${termsResponse.response.data.size} terms")
                termsResponse.response.data
            }

            else -> {
                val error = "Error getting Terms list for taxonomy: $taxonomySlug"
                appLogWrapper.e(AppLog.T.API, error)
                onError(error)
                emptyList()
            }
        }
    }

    private fun getTermEndpointType(): TermEndpointType = when (taxonomySlug) {
        DEFAULT_TAXONOMY_CATEGORY -> TermEndpointType.Categories
        DEFAULT_TAXONOMY_TAG -> TermEndpointType.Tags
        else -> TermEndpointType.Custom(taxonomySlug)
    }

    fun consumeUIEvent() {
        _uiEvent.value = null
    }

    companion object {
        private const val SORT_BY_NAME_ID = 1L
        private const val SORT_BY_COUNT_ID = 2L
    }
}
