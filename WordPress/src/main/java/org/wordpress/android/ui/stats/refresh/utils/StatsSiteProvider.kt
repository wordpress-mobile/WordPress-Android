package org.wordpress.android.ui.stats.refresh.utils

import androidx.lifecycle.LiveData
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
@Inject constructor(
    private val siteStore: SiteStore,
    private val selectedSite: SelectedSiteStorage,
    dispatcher: Dispatcher
) {
    var siteModel = SiteModel()
        private set

    private val mutableSiteChanged = SingleLiveEvent<OnSiteChanged>()
    val siteChanged: LiveData<OnSiteChanged> = mutableSiteChanged

    init {
        reset()
        dispatcher.register(this)
    }

    fun start(localSiteId: Int) {
        if (localSiteId != 0) {
            siteStore.getSiteByLocalId(localSiteId)?.let { site ->
                siteModel = site
            }
        }
    }

    fun reset() {
        start(selectedSite.currentLocalSiteId)
    }

    fun clear() {
        if (mutableSiteChanged.value != null) {
            mutableSiteChanged.value = null
        }
    }

    fun hasLoadedSite(): Boolean = siteModel.siteId != 0L

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        siteStore.getSiteByLocalId(selectedSite.currentLocalSiteId)?.let { site ->
            siteModel = site
            mutableSiteChanged.value = event
        }
    }

    class SelectedSiteStorage {
        val currentLocalSiteId
            get() = AppPrefs.getSelectedSite()
    }
}
