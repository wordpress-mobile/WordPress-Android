package org.wordpress.android.ui.newstats.tagsandcategories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsTagsUseCase
import org.wordpress.android.ui.newstats.repository.TagsResult
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

abstract class BaseTagsAndCategoriesViewModel(
    private val selectedSiteRepository:
        SelectedSiteRepository,
    private val statsTagsUseCase: StatsTagsUseCase,
    private val resourceProvider: ResourceProvider,
    private val mapper: TagsAndCategoriesMapper
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<TagsAndCategoriesCardUiState>(
            TagsAndCategoriesCardUiState.Loading
        )
    val uiState: StateFlow<TagsAndCategoriesCardUiState> =
        _uiState.asStateFlow()

    private val isLoaded = AtomicBoolean(false)
    private val isLoading = AtomicBoolean(false)
    // Main-thread-confined: only accessed from
    // viewModelScope (Dispatchers.Main).
    private var fetchJob: Job? = null

    protected abstract val maxItems: Int

    fun loadData() {
        if (isLoaded.get() ||
            !isLoading.compareAndSet(false, true)
        ) return
        fetchData()
    }

    @Suppress(
        "TooGenericExceptionCaught",
        "InstanceOfCheckForException"
    )
    protected fun fetchData(
        forceRefresh: Boolean = false
    ) {
        val site = selectedSiteRepository
            .getSelectedSite()
        if (site == null) {
            isLoading.set(false)
            _uiState.value =
                TagsAndCategoriesCardUiState.Error(
                    resourceProvider.getString(
                        R.string.stats_error_no_site
                    )
                )
            return
        }

        fetchJob = viewModelScope.launch {
            try {
                val result = statsTagsUseCase(
                    siteId = site.siteId,
                    max = maxItems,
                    forceRefresh = forceRefresh
                )
                isLoaded.set(
                    result is TagsResult.Success
                )
                handleResult(result)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLog.e(
                    AppLog.T.STATS,
                    "Error fetching tags: ${e.message}",
                    e
                )
                isLoaded.set(false)
                _uiState.value =
                    TagsAndCategoriesCardUiState.Error(
                        resourceProvider.getString(
                            R.string.stats_error_unknown
                        )
                    )
            } finally {
                isLoading.set(false)
            }
        }
    }

    protected fun resetForRefresh() {
        fetchJob?.cancel()
        isLoaded.set(false)
        isLoading.set(true)
        _uiState.value =
            TagsAndCategoriesCardUiState.Loading
    }

    private fun handleResult(result: TagsResult) {
        when (result) {
            is TagsResult.Success -> {
                val items = mapper.mapToUiItems(
                    result.data.tagGroups
                )
                _uiState.value = if (items.isEmpty()) {
                    TagsAndCategoriesCardUiState.NoData
                } else {
                    TagsAndCategoriesCardUiState.Loaded(
                        items = items,
                        maxViewsForBar =
                            items.first().views
                    )
                }
            }
            is TagsResult.Error -> {
                _uiState.value =
                    TagsAndCategoriesCardUiState.Error(
                        resourceProvider.getString(
                            R.string.stats_error_api
                        )
                    )
            }
        }
    }
}
