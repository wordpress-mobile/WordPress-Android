package org.wordpress.android.ui.taxonomies

import android.content.Context
import android.content.SharedPreferences
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
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.TermListParams
import uniffi.wp_api.AnyTermWithEditContext
import uniffi.wp_api.WpApiParamOrder
import javax.inject.Inject
import javax.inject.Named

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

    fun initialize(slug: String, isHierarchical: Boolean) {
        taxonomySlug = slug
        initialize()
    }

    override fun getSupportedSorts(): List<DataViewDropdownItem> = if (true) {
        // TODO
        // Don't support sorting in hierarchical taxonomies
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

        val allTerms = getTermsList(selectedSite)

        // Filter by search query
        val filteredTerms = if (searchQuery.isBlank()) {
            allTerms
        } else {
            allTerms.filter { term ->
                term.name.contains(searchQuery, ignoreCase = true) ||
                term.slug.contains(searchQuery, ignoreCase = true)
            }
        }

        // Sort the results
        val sortedTerms = when (sortBy?.id) {
            SORT_BY_NAME_ID -> {
                if (sortOrder == WpApiParamOrder.ASC) {
                    filteredTerms.sortedBy { it.name }
                } else {
                    filteredTerms.sortedByDescending { it.name }
                }
            }
            SORT_BY_COUNT_ID -> {
                if (sortOrder == WpApiParamOrder.ASC) {
                    filteredTerms.sortedBy { it.count }
                } else {
                    filteredTerms.sortedByDescending { it.count }
                }
            }
            else -> filteredTerms
        }

        // Convert to DataViewItems and return
        sortedTerms.map { term ->
            convertToDataViewItem(term)
        }
    }

    fun getTerm(termId: Long): AnyTermWithEditContext? {
        val item = uiState.value.items.firstOrNull {
            (it.data as? AnyTermWithEditContext)?.id == termId
        }
        return item?.data as? AnyTermWithEditContext
    }

    private fun convertToDataViewItem(term: AnyTermWithEditContext): DataViewItem {
        val parent = term.parent ?: 0
        val indentation = if (isHierarchical && parent > 0) {
            " - "
        } else {
            ""
        }
        return DataViewItem(
            id = term.id,
            image = null,
            title = "${indentation}${term.name}",
            fields = listOf(
                DataViewItemField(
                    value = context.resources.getString(R.string.term_count, term.count),
                    valueType = DataViewFieldType.TEXT,
                )
            ),
            skipEndPositioning = true,
            data = term
        )
    }

    private suspend fun getTermsList(site: SiteModel): List<AnyTermWithEditContext> {
        val wpApiClient = wpApiClientProvider.getWpApiClient(site)

        val termEndpointType = when (taxonomySlug) {
            DEFAULT_TAXONOMY_CATEGORY -> TermEndpointType.Categories
            DEFAULT_TAXONOMY_TAG -> TermEndpointType.Tags
            else -> TermEndpointType.Custom(taxonomySlug)
        }

        val termsResponse = wpApiClient.request { requestBuilder ->
            requestBuilder.terms().listWithEditContext(
                termEndpointType = termEndpointType,
                params = TermListParams()
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
