package org.wordpress.android.ui.jetpack.scan

import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class ScanViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    lateinit var site: SiteModel

    fun start(site: SiteModel) {
        if (isStarted) {
            return
        }
        this.site = site
        isStarted = true
    }
}
