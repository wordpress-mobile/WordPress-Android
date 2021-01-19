package org.wordpress.android.ui.jetpack.scan.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FetchScanHistoryPayload
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ScanHistoryViewModel @Inject constructor(
    private val scanStore: ScanStore,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _threats = MutableLiveData<List<ThreatModel>>()
    val threats: LiveData<List<ThreatModel>> = _threats

    lateinit var site: SiteModel

    fun start(site: SiteModel) {
        if (isStarted) return
        isStarted = true
        this.site = site
        fetchScanHistory()
    }

    private fun fetchScanHistory() {
        launch {
            val result = scanStore.fetchScanHistory(FetchScanHistoryPayload(site))
            if (result.isError) {
                // TODO malinjir handle error
            } else {
                _threats.value = scanStore.getScanHistoryForSite(site)
            }
        }
    }
}
