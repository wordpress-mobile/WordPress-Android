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
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackPoweredDialogViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val jetpackBrandingUtils: JetpackBrandingUtils
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _action = MutableLiveData<JetpackPoweredDialogAction>()
    val action: LiveData<JetpackPoweredDialogAction> = _action

    @Suppress("ForbiddenComment")
    fun start() {
        _uiState.value = UiState(
                listOf(
                        Illustration(R.raw.wp2jp),
                        Title(UiStringRes(string.wp_jetpack_powered)),
                        Caption(UiStringRes(string.wp_jetpack_powered_features))
                )
        )
    }

    @Suppress("ForbiddenComment")
    fun openJetpackAppDownloadLink() {
        jetpackBrandingUtils.trackGetJetpackAppTapped()
        _action.value = JetpackPoweredDialogAction.OpenPlayStore
    }

    @Suppress("ForbiddenComment")
    fun dismissBottomSheet() {
        jetpackBrandingUtils.trackDismissTapped()
        _action.value = JetpackPoweredDialogAction.DismissDialog
    }

    data class UiState(val uiItems: List<JetpackPoweredItem>)
}
