package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class StatsWidgetConfigureViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(mainDispatcher) {
    val settingsModel: LiveData<WidgetSettingsModel> by lazy {
        merge(
            siteSelectionViewModel.selectedSite,
            colorSelectionViewModel.viewMode
        ) { selectedSite, viewMode ->
            WidgetSettingsModel(
                selectedSite?.title,
                viewMode ?: LIGHT
            )
        }
    }
    private val mutableWidgetAdded = MutableLiveData<Event<WidgetAdded>>()
    val widgetAdded: LiveData<Event<WidgetAdded>> = mutableWidgetAdded

    private var appWidgetId: Int = -1
    private lateinit var widgetType: WidgetType
    private lateinit var siteSelectionViewModel: StatsSiteSelectionViewModel
    private lateinit var colorSelectionViewModel: StatsColorSelectionViewModel

    fun start(
        appWidgetId: Int,
        widgetType: WidgetType,
        siteSelectionViewModel: StatsSiteSelectionViewModel,
        colorSelectionViewModel: StatsColorSelectionViewModel
    ) {
        this.appWidgetId = appWidgetId
        this.widgetType = widgetType
        this.siteSelectionViewModel = siteSelectionViewModel
        this.colorSelectionViewModel = colorSelectionViewModel
        colorSelectionViewModel.start(appWidgetId)
        siteSelectionViewModel.start(appWidgetId)
    }

    fun addWidget() {
        val selectedSite = siteSelectionViewModel.selectedSite.value
        if (appWidgetId != -1 && selectedSite != null) {
            appPrefsWrapper.setAppWidgetSiteId(selectedSite.siteId, appWidgetId)
            appPrefsWrapper.setAppWidgetColor(colorSelectionViewModel.viewMode.value ?: LIGHT, appWidgetId)
            mutableWidgetAdded.postValue(Event(WidgetAdded(appWidgetId, widgetType)))
        }
    }

    data class WidgetSettingsModel(
        val siteTitle: String? = null,
        val color: Color,
        val buttonEnabled: Boolean = siteTitle != null
    )

    data class WidgetAdded(val appWidgetId: Int, val widgetType: WidgetType)
}
