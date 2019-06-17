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

class StatsDataTypeSelectionViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val mutableDataType = MutableLiveData<DataType>()
    val dataType: LiveData<DataType> = mutableDataType

    private var appWidgetId: Int = -1

    fun start(
        appWidgetId: Int
    ) {
        this.appWidgetId = appWidgetId
        val dataType = appPrefsWrapper.getAppWidgetDataType(appWidgetId)
        if (dataType != null) {
            mutableDataType.postValue(dataType)
        }
    }

    fun dataTypeClicked(color: DataType) {
        mutableDataType.postValue(color)
    }

    enum class DataType(@StringRes val title: Int) {
        VIEWS(R.string.stats_views),
        VISITORS(R.string.stats_visitors),
        COMMENTS(R.string.stats_comments),
        LIKES(R.string.stats_likes)
    }
}
