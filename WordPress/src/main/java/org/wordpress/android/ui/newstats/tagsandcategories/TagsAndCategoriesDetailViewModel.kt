package org.wordpress.android.ui.newstats.tagsandcategories

import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsTagsUseCase
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class TagsAndCategoriesDetailViewModel @Inject constructor(
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
    override val maxItems: Int = DETAIL_MAX_ITEMS

    companion object {
        private const val DETAIL_MAX_ITEMS = 100
    }
}
