package org.wordpress.android.ui.stats.refresh.utils

import android.arch.lifecycle.LiveData
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsSiteProvider
@Inject constructor(private val siteStore: SiteStore, dispatcher: Dispatcher) {
    var siteModel = SiteModel()
        private set

    private val mutableSiteChanged = SingleLiveEvent<OnSiteChanged>()
    val siteChanged: LiveData<OnSiteChanged> = mutableSiteChanged

    init {
        siteStore.getSiteByLocalId(AppPrefs.getSelectedSite())?.let { site ->
            siteModel = site
        }
        dispatcher.register(this)
    }

    fun clear() {
        if (mutableSiteChanged.value != null) {
            mutableSiteChanged.value = null
        }
    }

    fun hasLoadedSite(): Boolean = siteModel.siteId != 0L

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        siteStore.getSiteByLocalId(AppPrefs.getSelectedSite())?.let { site ->
            siteModel = site
            mutableSiteChanged.value = event
        }
    }
}
