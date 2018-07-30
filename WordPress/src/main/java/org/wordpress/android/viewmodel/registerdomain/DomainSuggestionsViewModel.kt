package org.wordpress.android.viewmodel.registerdomain

import android.arch.lifecycle.ViewModel
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class DomainSuggestionsViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore
) : ViewModel() {
    lateinit var site: SiteModel

    fun start(site: SiteModel) {
        this.site = site
    }
}
