package org.wordpress.android.ui.mysite

import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.util.filter
import org.wordpress.android.util.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedSiteSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository
) : MySiteSource<SelectedSite> {
    override fun buildSource(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ) = selectedSiteRepository.selectedSiteChange
            .filter { it == null || it.id == siteLocalId }
            .map { SelectedSite(it) }
}
