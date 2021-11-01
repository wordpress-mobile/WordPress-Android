package org.wordpress.android.ui.mysite

import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.ShowSiteIconProgressBar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteIconProgressSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository
) : MySiteSource<ShowSiteIconProgressBar> {
    override fun buildSource(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ) = selectedSiteRepository.showSiteIconProgressBar
            .map { ShowSiteIconProgressBar(it) }
            .distinctUntilChanged()
}
