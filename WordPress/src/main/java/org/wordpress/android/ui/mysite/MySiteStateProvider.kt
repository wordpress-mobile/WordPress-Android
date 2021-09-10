package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.ShowSiteIconProgressBar
import org.wordpress.android.util.filter
import org.wordpress.android.util.map

class MySiteStateProvider(
    private val coroutineScope: CoroutineScope,
    private val selectedSiteRepository: SelectedSiteRepository,
    vararg sources: MySiteSource<*>
) {
    private val mySiteSources = mutableListOf<MySiteSource<*>>(
            selectedSiteSource(),
            siteIconProgressSource()
    ).apply {
        addAll(sources)
    }

    val state: LiveData<MySiteUiState> = selectedSiteRepository.siteSelected.switchMap { siteLocalId ->
        val result = MediatorLiveData<SiteIdToState>()
        val currentSources = if (siteLocalId != null) {
            mySiteSources.map { source -> source.buildSource(coroutineScope, siteLocalId).distinctUntilChanged() }
        } else {
            mySiteSources.filterIsInstance(SiteIndependentSource::class.java)
                    .map { source -> source.buildSource(coroutineScope).distinctUntilChanged() }
        }
        for (newSource in currentSources) {
            result.addSource(newSource) { partialState ->
                if (partialState != null) {
                    result.value = (result.value ?: SiteIdToState(siteLocalId)).update(partialState)
                }
            }
        }
        // We want to filter out the empty state where we have a site ID but site object is missing.
        // Without this check there is an emission of a NoSites state even if we have the site
        result.filter { it.siteId == null || it.state.site != null }.map { it.state }
    }.distinctUntilChanged()

    private data class SiteIdToState(val siteId: Int?, val state: MySiteUiState = MySiteUiState()) {
        fun update(partialState: PartialState): SiteIdToState {
            return this.copy(state = state.update(partialState))
        }
    }

    private fun selectedSiteSource(): MySiteSource<SelectedSite> =
            object : MySiteSource<SelectedSite> {
                override fun buildSource(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<SelectedSite> {
                    return selectedSiteRepository.selectedSiteChange
                            .filter { it == null || it.id == siteLocalId }
                            .map { SelectedSite(it) }
                }
            }

    private fun siteIconProgressSource(): MySiteSource<ShowSiteIconProgressBar> =
            object : MySiteSource<ShowSiteIconProgressBar> {
                override fun buildSource(
                    coroutineScope: CoroutineScope,
                    siteLocalId: Int
                ): LiveData<ShowSiteIconProgressBar> {
                    return selectedSiteRepository.showSiteIconProgressBar
                            .map { ShowSiteIconProgressBar(it == true) }
                            .distinctUntilChanged()
                }
            }
}
