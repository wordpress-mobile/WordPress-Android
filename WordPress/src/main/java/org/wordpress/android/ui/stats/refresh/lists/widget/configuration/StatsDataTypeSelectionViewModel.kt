package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class StatsDataTypeSelectionViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val mutableDataType = MutableLiveData<DataType>()
    val dataType: LiveData<DataType> = mutableDataType

    private val mutableNotification = MutableLiveData<Event<Int>>()
    val notification: LiveData<Event<Int>> = mutableNotification

    private val mutableDialogOpened = MutableLiveData<Event<Unit>>()
    val dialogOpened: LiveData<Event<Unit>> = mutableDialogOpened

    private var appWidgetId: Int = -1

    @SuppressLint("NullSafeMutableLiveData")
    fun start(
        appWidgetId: Int
    ) {
        this.appWidgetId = appWidgetId
        val dataType = appPrefsWrapper.getAppWidgetDataType(appWidgetId)
        if (dataType != null) {
            mutableDataType.postValue(dataType)
        }
    }

    fun selectDataType(dataType: DataType) {
        mutableDataType.postValue(dataType)
    }

    fun openDataTypeDialog() {
        if (accountStore.hasAccessToken()) {
            mutableDialogOpened.postValue(Event(Unit))
        } else {
            val message = if (BuildConfig.IS_JETPACK_APP) {
                string.stats_widget_log_in_to_add_message
            } else {
                string.stats_widget_log_in_message
            }
            mutableNotification.postValue(Event(message))
        }
    }

    enum class DataType(@StringRes val title: Int) {
        VIEWS(R.string.stats_views),
        VISITORS(R.string.stats_visitors),
        COMMENTS(R.string.stats_comments),
        LIKES(R.string.stats_likes)
    }
}
