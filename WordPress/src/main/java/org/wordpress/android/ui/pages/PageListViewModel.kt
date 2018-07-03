package org.wordpress.android.ui.pages

import android.arch.lifecycle.ViewModel
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class PageListViewModel
@Inject constructor(val dispatcher: Dispatcher) : ViewModel() {
    private var isStarted: Boolean = false
    private var site: SiteModel? = null

    fun start(site: SiteModel) {
        this.site = site
        if (!isStarted) {
            isStarted = true
        }
    }

    fun stop() {
        this.site = null
    }
}
