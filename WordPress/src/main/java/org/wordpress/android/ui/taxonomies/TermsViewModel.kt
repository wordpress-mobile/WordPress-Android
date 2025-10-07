package org.wordpress.android.ui.taxonomies

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.unit.dp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY
import org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_TAG
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
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.TermListParams
import uniffi.wp_api.AnyTermWithEditContext
import uniffi.wp_api.WpApiParamOrder
import uniffi.wp_api.WpApiParamTermsOrderBy
import javax.inject.Inject
import javax.inject.Named

private const val INDENTATION_IN_DP = 10

@HiltViewModel
class TermsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    sharedPrefs: SharedPreferences,
    networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(IO_THREAD) ioDispatcher: CoroutineDispatcher,
) : DataViewViewModel(
    mainDispatcher = mainDispatcher,
    appLogWrapper = appLogWrapper,
    sharedPrefs = sharedPrefs,
    networkUtilsWrapper = networkUtilsWrapper,
    selectedSiteRepository = selectedSiteRepository,
    accountStore = accountStore,
    ioDispatcher = ioDispatcher
) {
    private var taxonomySlug: String = ""
    private var isHierarchical: Boolean = false
    private var currentTerms = listOf<AnyTermWithEditContext>()

    fun initialize(taxonomySlug: String, isHierarchical: Boolean) {
        this.taxonomySlug = taxonomySlug
        this.isHierarchical = isHierarchical
        initialize()
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

    fun getTerm(termId: Long): AnyTermWithEditContext? {
        val item = uiState.value.items.firstOrNull {
            (it.data as? AnyTermWithEditContext)?.id == termId
        }
        return item?.data as? AnyTermWithEditContext
    }

    fun getAllTerms(): List<AnyTermWithEditContext> = currentTerms

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

    private suspend fun getTermsList(
        site: SiteModel,
        page: Int,
        searchQuery: String,
        sortOrder: WpApiParamOrder,
        sortBy: DataViewDropdownItem?
    ): List<AnyTermWithEditContext> {
        val wpApiClient = wpApiClientProvider.getWpApiClient(site)

        val termEndpointType = when (taxonomySlug) {
            DEFAULT_TAXONOMY_CATEGORY -> TermEndpointType.Categories
            DEFAULT_TAXONOMY_TAG -> TermEndpointType.Tags
            else -> TermEndpointType.Custom(taxonomySlug)
        }

        val termsResponse = wpApiClient.request { requestBuilder ->
            requestBuilder.terms().listWithEditContext(
                termEndpointType = termEndpointType,
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

    companion object {
        private const val SORT_BY_NAME_ID = 1L
        private const val SORT_BY_COUNT_ID = 2L
    }
}
