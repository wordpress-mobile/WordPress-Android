package org.wordpress.android.ui.newstats.tagsandcategories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsTagsUseCase
import org.wordpress.android.ui.newstats.repository.TagsResult
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class TagsAndCategoriesDetailViewModel @Inject constructor(
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
    private var fetchJob: Job? = null

    fun loadData() {
        if (isLoaded.get() ||
            !isLoading.compareAndSet(false, true)
        ) return
        _uiState.value =
            TagsAndCategoriesCardUiState.Loading
        fetchData()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun fetchData() {
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
                    max = DETAIL_MAX_ITEMS
                )
                isLoaded.set(
                    result is TagsResult.Success
                )
                handleResult(result)
            } catch (e: Exception) {
                _uiState.value =
                    TagsAndCategoriesCardUiState.Error(
                        e.message ?: resourceProvider
                            .getString(
                                R.string
                                    .stats_error_unknown
                            )
                    )
            } finally {
                isLoading.set(false)
            }
        }
    }

    private fun handleResult(result: TagsResult) {
        when (result) {
            is TagsResult.Success -> {
                val items = mapper.mapToUiItems(
                    result.data.tagGroups
                )
                _uiState.value =
                    TagsAndCategoriesCardUiState.Loaded(
                        items = items,
                        maxViewsForBar =
                            items.firstOrNull()
                                ?.views ?: 1L
                    )
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

    companion object {
        private const val DETAIL_MAX_ITEMS = 100
    }
}
