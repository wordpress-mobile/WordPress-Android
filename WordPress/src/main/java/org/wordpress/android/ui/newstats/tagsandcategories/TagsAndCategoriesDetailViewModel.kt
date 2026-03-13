package org.wordpress.android.ui.newstats.tagsandcategories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.TagsResult
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class TagsAndCategoriesDetailViewModel @Inject constructor(
    private val selectedSiteRepository:
        SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<TagsAndCategoriesCardUiState>(
            TagsAndCategoriesCardUiState.Loading
        )
    val uiState: StateFlow<TagsAndCategoriesCardUiState> =
        _uiState.asStateFlow()

    fun loadData() {
        _uiState.value =
            TagsAndCategoriesCardUiState.Loading
        fetchData()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun fetchData() {
        val site = selectedSiteRepository
            .getSelectedSite()
        if (site == null) {
            _uiState.value =
                TagsAndCategoriesCardUiState.Error(
                    resourceProvider.getString(
                        R.string.stats_error_no_site
                    )
                )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            _uiState.value =
                TagsAndCategoriesCardUiState.Error(
                    resourceProvider.getString(
                        R.string.stats_error_api
                    )
                )
            return
        }

        statsRepository.init(accessToken)

        viewModelScope.launch {
            try {
                val result = statsRepository.fetchTags(
                    siteId = site.siteId,
                    max = DETAIL_MAX_ITEMS
                )
                handleResult(result)
            } catch (e: Exception) {
                _uiState.value =
                    TagsAndCategoriesCardUiState.Error(
                        e.message ?: resourceProvider
                            .getString(
                                R.string.stats_error_unknown
                            )
                    )
            }
        }
    }

    private fun handleResult(result: TagsResult) {
        when (result) {
            is TagsResult.Success -> {
                val items = result.data.tagGroups
                    .map { group ->
                        val tagUiItems = group.tags
                            .map { tag ->
                                TagUiItem(
                                    name = tag.name,
                                    tagType = tag.tagType
                                )
                            }
                        TagGroupUiItem(
                            name = tagUiItems.joinToString(
                                TAGS_SEPARATOR
                            ) { it.name },
                            tags = tagUiItems,
                            views = group.views,
                            displayType =
                                TagGroupDisplayType
                                    .fromTags(tagUiItems)
                        )
                    }
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
        private const val TAGS_SEPARATOR = " / "
    }
}
