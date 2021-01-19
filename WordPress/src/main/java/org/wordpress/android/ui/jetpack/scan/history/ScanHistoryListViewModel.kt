package org.wordpress.android.ui.jetpack.scan.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.scan.ScanListItemState
import org.wordpress.android.ui.jetpack.scan.builders.ThreatItemBuilder
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ScanHistoryListViewModel @Inject constructor(
    private val scanThreatItemBuilder: ThreatItemBuilder,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    lateinit var uiState: LiveData<List<ScanListItemState>>

    lateinit var site: SiteModel

    fun start(site: SiteModel, parentViewModel: ScanHistoryViewModel) {
        if (isStarted) return
        isStarted = true
        this.site = site
        uiState = parentViewModel.threats.map { threats ->
            // TODO malinjir filter by tab
            threats.map {
                scanThreatItemBuilder.buildThreatItem(it, this::onItemClicked)
            }
        }
    }

    private fun onItemClicked(threatId: Long) {
    }
}
