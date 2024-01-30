package org.wordpress.android.ui.sitemonitor

import android.util.Log
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SiteMonitorParentViewModel @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private val _webViewStates = MutableLiveData<Map<SiteMonitorType, SiteMonitorWebViewState?>>(emptyMap())
    // val webViewStates: LiveData<Map<SiteMonitorType, SiteMonitorWebViewState?>> = _webViewStates

    fun start() {
        // todo: understand when the webViewState should be reset
        Log.i(javaClass.simpleName, "***=> start: ")
    }

    fun saveWebViewState(type: SiteMonitorType, state: SiteMonitorWebViewState) {
        Log.i(javaClass.simpleName, "***=> saveWebViewState: $type, $state")
            _webViewStates.value = _webViewStates.value?.toMutableMap()?.apply {
                put(type, state)
        }
    }

    fun getWebViewState(type: SiteMonitorType): SiteMonitorWebViewState? {
        Log.i(javaClass.simpleName, "***=> getWebViewState: $type")
        return _webViewStates.value?.get(type)
    }
}
