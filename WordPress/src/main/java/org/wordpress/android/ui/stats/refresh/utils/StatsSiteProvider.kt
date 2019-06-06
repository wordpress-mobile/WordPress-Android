package org.wordpress.android.ui.stats.refresh.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.viewmodel.Event
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

    private val mutableSiteChanged = MutableLiveData<Event<Int>>()
    val siteChanged: LiveData<Event<Int>> = mutableSiteChanged

    init {
        reset()
        dispatcher.register(this)
    }

    fun start(localSiteId: Int) {
        if (localSiteId != 0) {
            val siteChanged = localSiteId != siteModel.id
            siteStore.getSiteByLocalId(localSiteId)?.let { site ->
                siteModel = site
            }
            if (siteChanged) {
                mutableSiteChanged.postValue(Event(localSiteId))
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
        if (event.isError) {
            return
        }
        siteStore.getSiteByLocalId(siteModel.id)?.let { site ->
            siteModel = site
            mutableSiteChanged.value = Event(siteModel.id)
        }
    }

    class SelectedSiteStorage {
        val currentLocalSiteId
            get() = AppPrefs.getSelectedSite()
    }
}
