package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.SiteModel
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
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val mutableSelectedSite = MutableLiveData<SiteUiModel>()
    val selectedSite: LiveData<SiteUiModel> = mutableSelectedSite

    private val mutableSites = MutableLiveData<List<SiteUiModel>>()
    val sites: LiveData<List<SiteUiModel>> = mutableSites
    private val mutableHideSiteDialog = MutableLiveData<Event<Unit>>()
    val hideSiteDialog: LiveData<Event<Unit>> = mutableHideSiteDialog

    fun start(appWidgetId: Int) {
        val siteId = appPrefsWrapper.getAppWidgetSiteId(appWidgetId)
        if (siteId > -1) {
            mutableSelectedSite.postValue(siteStore.getSiteBySiteId(siteId)?.let { toUiModel(it) })
        }
    }

    fun loadSites() {
        mutableSites.postValue(siteStore.sites.map { toUiModel(it) })
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
