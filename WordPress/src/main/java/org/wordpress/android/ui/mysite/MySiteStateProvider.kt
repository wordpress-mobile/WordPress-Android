package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.ShowSiteIconProgressBar
import org.wordpress.android.util.filter
import org.wordpress.android.util.map
import javax.inject.Named

class MySiteStateProvider(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    vararg sources: MySiteSource<*>
) {
    private val mySiteSources = mutableListOf<MySiteSource<*>>(
            selectedSiteSource(),
            siteIconProgressSource()
    ).apply {
        addAll(sources)
    }

    val state: LiveData<MySiteUiState> = selectedSiteRepository.siteSelected.switchMap { siteId ->
        data class SiteIdToState(val siteId: Int?, val state: MySiteUiState = MySiteUiState()) {
            fun update(partialState: PartialState): SiteIdToState {
                return this.copy(state = state.update(partialState))
            }
        }
        val result = MediatorLiveData<SiteIdToState>()
        val currentSources = if (siteId != null) {
            mySiteSources.map { source -> source.buildSource(siteId).distinctUntilChanged().asLiveData(bgDispatcher) }
        } else {
            mySiteSources.filterIsInstance(SiteIndependentSource::class.java)
                    .map { source -> source.buildSource().distinctUntilChanged().asLiveData(bgDispatcher) }
        }
        for (newSource in currentSources) {
            result.addSource(newSource) { partialState ->
                if (partialState != null) {
                    result.value = (result.value ?: SiteIdToState(siteId)).update(partialState)
                }
            }
        }
        result.filter { it.siteId == null || it.state.site != null }.map { it.state }
    }.distinctUntilChanged()

    private fun selectedSiteSource(): MySiteSource<SelectedSite> =
            object : MySiteSource<SelectedSite> {
                override fun buildSource(siteId: Int): Flow<SelectedSite?> {
                    return selectedSiteRepository.selectedSiteChange
                            .filter { it == null || it.id == siteId }
                            .map { SelectedSite(it) }.asFlow()
                }
            }

    private fun siteIconProgressSource(): MySiteSource<ShowSiteIconProgressBar> =
            object : MySiteSource<ShowSiteIconProgressBar> {
                override fun buildSource(siteId: Int): Flow<ShowSiteIconProgressBar> {
                    return selectedSiteRepository.showSiteIconProgressBar.map { ShowSiteIconProgressBar(it == true) }
                            .asFlow()
                            .distinctUntilChanged()
                }
            }
}
