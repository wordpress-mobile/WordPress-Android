package org.wordpress.android.ui.stats.refresh.lists.widget

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureFragment.ViewType
import org.wordpress.android.ui.stats.refresh.lists.widget.StatsWidgetConfigureViewModel.Color.LIGHT
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class StatsWidgetConfigureViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val siteStore: SiteStore,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val mutableSelectedSite = MutableLiveData<SiteUiModel>()
    private val mutableViewMode = MutableLiveData<Color>()
    val settingsModel: LiveData<WidgetSettingsModel> = merge(
            mutableSelectedSite,
            mutableViewMode
    ) { selectedSite, viewMode ->
        WidgetSettingsModel(
                selectedSite?.title,
                viewMode ?: LIGHT
        )
    }
    private val mutableWidgetAdded = MutableLiveData<Event<WidgetAdded>>()
    val widgetAdded: LiveData<Event<WidgetAdded>> = mutableWidgetAdded

    private val mutableSites = MutableLiveData<List<SiteUiModel>>()
    val sites: LiveData<List<SiteUiModel>> = mutableSites
    private val mutableHideSiteDialog = MutableLiveData<Event<Unit>>()
    val hideSiteDialog: LiveData<Event<Unit>> = mutableHideSiteDialog

    private var appWidgetId: Int = -1
    private lateinit var viewType: ViewType

    fun start(appWidgetId: Int, viewType: ViewType) {
        this.appWidgetId = appWidgetId
        this.viewType = viewType
        val colorMode = appPrefsWrapper.getAppWidgetColor(appWidgetId)
        if (colorMode != null) {
            mutableViewMode.postValue(colorMode)
        }
        val siteId = appPrefsWrapper.getAppWidgetSiteId(appWidgetId)
        if (siteId > -1) {
            mutableSelectedSite.postValue(siteStore.getSiteBySiteId(siteId)?.let { toUiModel(it) })
        }
    }

    fun colorClicked(color: Color) {
        mutableViewMode.postValue(color)
    }

    fun addWidget() {
        val selectedSite = mutableSelectedSite.value
        if (appWidgetId != -1 && selectedSite != null) {
            appPrefsWrapper.setAppWidgetSiteId(selectedSite.siteId, appWidgetId)
            appPrefsWrapper.setAppWidgetColor(mutableViewMode.value ?: LIGHT, appWidgetId)
            mutableWidgetAdded.postValue(Event(WidgetAdded(appWidgetId, viewType)))
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

    enum class Color(@StringRes val title: Int) {
        LIGHT(R.string.stats_widget_color_light), DARK(R.string.stats_widget_color_dark)
    }

    data class WidgetSettingsModel(
        val siteTitle: String? = null,
        val color: Color,
        val buttonEnabled: Boolean = siteTitle != null
    )

    data class WidgetAdded(val appWidgetId: Int, val viewType: ViewType)

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
