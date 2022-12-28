package org.wordpress.android.ui.stats.refresh.lists.widget.minified

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel.DataType.VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsSiteSelectionViewModel
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class StatsMinifiedWidgetConfigureViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(mainDispatcher) {
    val settingsModel: LiveData<WidgetSettingsModel> by lazy {
        merge(
            siteSelectionViewModel.selectedSite,
            colorSelectionViewModel.viewMode,
            dataTypeSelectionViewModel.dataType
        ) { selectedSite, viewMode, dataType ->
            WidgetSettingsModel(
                selectedSite?.title,
                viewMode ?: LIGHT,
                dataType ?: VIEWS
            )
        }
    }
    private val mutableWidgetAdded = MutableLiveData<Event<WidgetAdded>>()
    val widgetAdded: LiveData<Event<WidgetAdded>> = mutableWidgetAdded

    private var appWidgetId: Int = -1
    private lateinit var siteSelectionViewModel: StatsSiteSelectionViewModel
    private lateinit var colorSelectionViewModel: StatsColorSelectionViewModel
    private lateinit var dataTypeSelectionViewModel: StatsDataTypeSelectionViewModel

    fun start(
        appWidgetId: Int,
        siteSelectionViewModel: StatsSiteSelectionViewModel,
        colorSelectionViewModel: StatsColorSelectionViewModel,
        dataTypeSelectionViewModel: StatsDataTypeSelectionViewModel
    ) {
        this.appWidgetId = appWidgetId
        this.siteSelectionViewModel = siteSelectionViewModel
        this.colorSelectionViewModel = colorSelectionViewModel
        this.dataTypeSelectionViewModel = dataTypeSelectionViewModel
        colorSelectionViewModel.start(appWidgetId)
        siteSelectionViewModel.start(appWidgetId)
        dataTypeSelectionViewModel.start(appWidgetId)
    }

    fun addWidget() {
        val selectedSite = siteSelectionViewModel.selectedSite.value
        if (appWidgetId != -1 && selectedSite != null) {
            appPrefsWrapper.setAppWidgetSiteId(selectedSite.siteId, appWidgetId)
            appPrefsWrapper.setAppWidgetColor(colorSelectionViewModel.viewMode.value ?: LIGHT, appWidgetId)
            appPrefsWrapper.setAppWidgetDataType(dataTypeSelectionViewModel.dataType.value ?: VIEWS, appWidgetId)
            mutableWidgetAdded.postValue(Event(WidgetAdded(appWidgetId)))
        }
    }

    data class WidgetSettingsModel(
        val siteTitle: String? = null,
        val color: Color,
        val dataType: DataType,
        val buttonEnabled: Boolean = siteTitle != null
    )

    data class WidgetAdded(val appWidgetId: Int)
}
