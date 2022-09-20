package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class StatsSiteSelectionViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val mutableSelectedSite = MutableLiveData<SiteUiModel>()
    val selectedSite: LiveData<SiteUiModel> = mutableSelectedSite

    private val mutableSites = MutableLiveData<List<SiteUiModel>>()
    val sites: LiveData<List<SiteUiModel>> = mutableSites
    private val mutableHideSiteDialog = MutableLiveData<Event<Unit>>()
    val hideSiteDialog: LiveData<Event<Unit>> = mutableHideSiteDialog

    private val mutableNotification = MutableLiveData<Event<Int>>()
    val notification: LiveData<Event<Int>> = mutableNotification

    private val mutableDialogOpened = MutableLiveData<Event<Unit>>()
    val dialogOpened: LiveData<Event<Unit>> = mutableDialogOpened

    fun start(appWidgetId: Int) {
        val siteId = appPrefsWrapper.getAppWidgetSiteId(appWidgetId)
        if (siteId > -1) {
            mutableSelectedSite.postValue(siteStore.getSiteBySiteId(siteId)?.let { toUiModel(it) })
        }
    }

    fun loadSites() {
        val sites = siteStore.sites.filter { it.isWPCom || it.isJetpackConnected }.map { toUiModel(it) }
        mutableSites.postValue(sites)
    }

    private fun toUiModel(site: SiteModel): SiteUiModel {
        val blogName = SiteUtils.getSiteNameOrHomeURL(site)
        val homeUrl = SiteUtils.getHomeURLOrHostName(site)
        val title = when {
            !blogName.isNullOrEmpty() -> blogName
            !homeUrl.isNullOrEmpty() -> homeUrl
            else -> null
        }
        val description = when {
            !homeUrl.isNullOrEmpty() -> homeUrl
            else -> null
        }
        return SiteUiModel(site.siteId, site.iconUrl, title, description, this::selectSite)
    }

    private fun selectSite(site: SiteUiModel) {
        mutableHideSiteDialog.postValue(Event(Unit))
        mutableSelectedSite.postValue(site)
    }

    fun openSiteDialog() {
        if (accountStore.hasAccessToken()) {
            mutableDialogOpened.postValue(Event(Unit))
        } else {
            val message = if (BuildConfig.IS_JETPACK_APP) {
                R.string.stats_widget_log_in_to_add_message
            } else {
                R.string.stats_widget_log_in_message
            }
            mutableNotification.postValue(Event(message))
        }
    }

    data class SiteUiModel(
        val siteId: Long,
        val iconUrl: String?,
        val title: String?,
        val url: String?,
        private val onClick: (site: SiteUiModel) -> Unit
    ) {
        fun click() {
            onClick(this)
        }
    }
}
