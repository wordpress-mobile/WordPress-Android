package org.wordpress.android.ui.newstats.tagsandcategories

import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsTagsUseCase
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class TagsAndCategoriesViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    statsTagsUseCase: StatsTagsUseCase,
    resourceProvider: ResourceProvider,
    mapper: TagsAndCategoriesMapper
) : BaseTagsAndCategoriesViewModel(
    selectedSiteRepository,
    statsTagsUseCase,
    resourceProvider,
    mapper
) {
    override val maxItems: Int = CARD_MAX_ITEMS

    fun refresh() {
        resetForRefresh()
        fetchData(forceRefresh = true)
    }

    companion object {
        private const val CARD_MAX_ITEMS = 7
    }
}
