package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class StatsColorSelectionViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val mutableViewMode = MutableLiveData<Color>()
    val viewMode: LiveData<Color> = mutableViewMode

    private var appWidgetId: Int = -1

    fun start(
        appWidgetId: Int
    ) {
        this.appWidgetId = appWidgetId
        val colorMode = appPrefsWrapper.getAppWidgetColor(appWidgetId)
        if (colorMode != null) {
            mutableViewMode.postValue(colorMode)
        }
    }

    fun colorClicked(color: Color) {
        mutableViewMode.postValue(color)
    }

    enum class Color(@StringRes val title: Int) {
        LIGHT(R.string.stats_widget_color_light), DARK(R.string.stats_widget_color_dark)
    }
}
