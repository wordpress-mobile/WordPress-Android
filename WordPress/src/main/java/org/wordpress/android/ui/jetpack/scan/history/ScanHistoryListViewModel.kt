package org.wordpress.android.ui.jetpack.scan.history

import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class ScanHistoryListViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    lateinit var site: SiteModel

    fun start(site: SiteModel) {
        if (isStarted) return
        isStarted = true
        this.site = site
    }
}
