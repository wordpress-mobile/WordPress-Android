package org.wordpress.android.ui.stats.refresh.utils

import android.arch.lifecycle.LiveData
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsSiteProvider
@Inject constructor(private val siteStore: SiteStore, private val dispatcher: Dispatcher) {
    lateinit var siteModel: SiteModel
    private var initialized = false
    private val mutableSiteChanged = SingleLiveEvent<OnSiteChanged>()
    val siteChanged: LiveData<OnSiteChanged> = mutableSiteChanged

    fun start(site: SiteModel) {
        if (!initialized) {
            dispatcher.register(this)
            initialized = true
        }
        this.siteModel = site
    }

    fun clear() {
        if (mutableSiteChanged.value != null) {
            mutableSiteChanged.value = null
        }
    }

    fun stop() {
        if (initialized) {
            dispatcher.unregister(this)
            initialized = false
        }
    }

    fun hasLoadedSite(): Boolean = siteModel.siteId != 0L

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        val site = siteStore.getSiteByLocalId(siteModel.id)
        if (site != null && site.siteId != 0L) {
            siteModel = site
            mutableSiteChanged.value = event
        }
    }
}
