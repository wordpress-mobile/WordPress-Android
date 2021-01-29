package org.wordpress.android.ui.mysite

import androidx.lifecycle.asFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.ShowSiteIconProgressBar

class MySiteStateProvider(
    private val mainDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    vararg sources: MySiteSource<*>
) {
    private val mySiteSources = mutableListOf<MySiteSource<*>>(
            selectedSiteSource(),
            siteIconProgressSource()
    ).apply {
        addAll(sources)
    }

    @FlowPreview
    val state: Flow<MySiteUiState> = selectedSiteRepository.siteSelected.asFlow().flatMapMerge { siteId ->
        if (siteId != null) {
            mySiteSources.map { it.buildSource(siteId).distinctUntilChanged() }
        } else {
            mySiteSources.filterIsInstance(SiteIndependentSource::class.java)
                    .map { it.buildSource().distinctUntilChanged() }
        }.asFlow().flattenMerge()
    }.let { partialStates ->
        flow {
            var accumulator = MySiteUiState()
            withContext(mainDispatcher) {
                partialStates.collect { partialState ->
                    if (partialState != null) {
                        accumulator = accumulator.update(partialState)
                        emit(accumulator)
                    }
                }
            }
        }
    }.distinctUntilChanged()

    private fun selectedSiteSource(): MySiteSource<SelectedSite> =
            object : MySiteSource<SelectedSite> {
                override fun buildSource(siteId: Int): Flow<SelectedSite?> {
                    val nullableFlow: Flow<SiteModel?> = selectedSiteRepository.selectedSiteChange.asFlow()
                    return nullableFlow
                            .filter { it == null || it.id == siteId }
                            .map { SelectedSite(it) }
                }
            }

    private fun siteIconProgressSource(): MySiteSource<ShowSiteIconProgressBar> =
            object : MySiteSource<ShowSiteIconProgressBar> {
                override fun buildSource(siteId: Int): Flow<ShowSiteIconProgressBar?> {
                    val nullableFlow: Flow<Boolean?> = selectedSiteRepository.showSiteIconProgressBar.asFlow()
                    return nullableFlow.map { ShowSiteIconProgressBar(it == true) }
                }
            }
}
