package org.wordpress.android.ui.mysite.jetpackbadge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Caption
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Illustration
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Title
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackPoweredDialogViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _action = MutableLiveData<JetpackPoweredDialogAction>()
    val action: LiveData<JetpackPoweredDialogAction> = _action

    fun start() {
        // TODO: Tracks
        _uiState.value = UiState(
                listOf(
                        Illustration(R.raw.wp2jp),
                        Title(UiStringRes(string.wp_jetpack_powered)),
                        Caption(UiStringRes(string.wp_jetpack_powered_features))
                )
        )
    }

    fun openJetpackAppDownloadLink() {
        // TODO: Tracks
        _action.value = JetpackPoweredDialogAction.OpenPlayStore
    }

    fun dismissBottomSheet() {
        // TODO: Tracks
        _action.value = JetpackPoweredDialogAction.DismissDialog
    }

    data class UiState(val uiItems: List<JetpackPoweredItem>)

    companion object {
        const val JETPACK_PACKAGE_NAME = "com.jetpack.android"
    }
}
