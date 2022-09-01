package org.wordpress.android.ui.stats.refresh.lists.widget.configuration

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class StatsColorSelectionViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val mutableViewMode = MutableLiveData<Color>()
    val viewMode: LiveData<Color> = mutableViewMode

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
        val colorMode = appPrefsWrapper.getAppWidgetColor(appWidgetId)
        if (colorMode != null) {
            mutableViewMode.postValue(colorMode)
        }
    }

    fun selectColor(color: Color) {
        mutableViewMode.postValue(color)
    }

    fun openColorDialog() {
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

    enum class Color(@StringRes val title: Int) {
        LIGHT(R.string.stats_widget_color_light), DARK(R.string.stats_widget_color_dark)
    }
}
