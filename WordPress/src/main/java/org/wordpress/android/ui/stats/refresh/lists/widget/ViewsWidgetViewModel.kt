package org.wordpress.android.ui.stats.refresh.lists.widget

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ViewsWidgetViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val siteStore: SiteStore,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val mutableSelectedSite = MutableLiveData<SiteModel>()
    private val mutableViewMode = MutableLiveData<ViewMode>()
    val uiModel: LiveData<UiModel> = merge(mutableSelectedSite, mutableViewMode) { selectedSite, viewMode ->
        UiModel(
                selectedSite?.displayName,
                viewMode
        )
    }
    private val mutableWidgetAdded = MutableLiveData<Event<WidgetAdded>>()
    val widgetAdded: LiveData<Event<WidgetAdded>> = mutableWidgetAdded
    private var appWidgetId: Int = -1

    fun start(appWidgetId: Int) {
        this.appWidgetId = appWidgetId
        val colorModeId = appPrefsWrapper.getAppWidgetColorModeId(appWidgetId)
        if (colorModeId >= 0) {
            mutableViewMode.postValue(ViewMode.values()[colorModeId])
        }
        val siteId = appPrefsWrapper.getAppWidgetSiteId(appWidgetId)
        if (siteId > -1) {
            mutableSelectedSite.postValue(siteStore.getSiteBySiteId(siteId))
        }
    }

    fun siteClicked() {
        TODO("not implemented")
    }

    fun colorClicked() {
        TODO("not implemented")
    }

    fun addWidget() {
        val selectedSite = mutableSelectedSite.value
        val selectedViewMode = mutableViewMode.value
        if (appWidgetId != -1 && selectedSite != null && selectedViewMode != null) {
            appPrefsWrapper.setAppWidgetSiteId(selectedSite.siteId, appWidgetId)
            appPrefsWrapper.setAppWidgetColorModeId(selectedViewMode.ordinal, appWidgetId)
            mutableWidgetAdded.postValue(Event(WidgetAdded(appWidgetId)))
        }
    }

    enum class ViewMode(@StringRes val title: Int) {
        LIGHT(R.string.stats_widget_color_light), DARK(R.string.stats_widget_color_dark)
    }

    data class UiModel(
        val siteTitle: String? = null,
        val viewMode: ViewMode? = null,
        val buttonEnabled: Boolean = siteTitle != null && viewMode != null
    )

    data class WidgetAdded(val appWidgetId: Int)
}
