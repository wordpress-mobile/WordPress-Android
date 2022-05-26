package org.wordpress.android.ui.stats.refresh.utils

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider.SiteUpdateResult.NotConnectedJetpackSite
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider.SiteUpdateResult.SiteConnected
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsSiteProvider
@Inject constructor(
    private val siteStore: SiteStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val dispatcher: Dispatcher
) {
    var siteModel = SiteModel()
        private set

    private val _siteChanged = MutableLiveData<Event<SiteUpdateResult>>()
    val siteChanged: LiveData<Event<SiteUpdateResult>> = _siteChanged
    private val maxAttempts = 3
    private var counter = 0

    init {
        reset()
        dispatcher.register(this)
    }

    fun start(localSiteId: Int): Boolean {
        if (localSiteId != 0) {
            val siteChanged = localSiteId != siteModel.id
            siteStore.getSiteByLocalId(localSiteId)?.let { site ->
                siteModel = site
            }
            return siteChanged
        }
        return false
    }

    fun reset() {
        start(selectedSiteRepository.getSelectedSiteLocalId())
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun clear() {
        if (_siteChanged.value != null) {
            _siteChanged.value = null
        }
    }

    fun hasLoadedSite(): Boolean = siteModel.siteId != 0L

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.isError) {
            return
        }
        siteStore.getSiteByLocalId(siteModel.id)?.let { site ->
            if (site.siteId != 0L) {
                counter = 0
                siteModel = site
                _siteChanged.value = Event(SiteConnected(site.siteId))
            } else {
                if (counter < maxAttempts) {
                    counter++
                    dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site))
                } else {
                    counter = 0
                    _siteChanged.value = Event(NotConnectedJetpackSite)
                }
            }
        }
    }

    sealed class SiteUpdateResult {
        object NotConnectedJetpackSite : SiteUpdateResult()
        data class SiteConnected(val siteId: Long) : SiteUpdateResult()
    }
}
