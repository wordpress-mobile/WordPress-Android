package org.wordpress.android.ui.jetpackoverlay

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackFeatureFullScreenOverlayViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val jetpackFeatureOverlayContentBuilder: JetpackFeatureOverlayContentBuilder,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<JetpackFeatureOverlayUIState>()
    val uiState: LiveData<JetpackFeatureOverlayUIState> = _uiState

    private val _action = MutableLiveData<JetpackFeatureOverlayActions>()
    val action: LiveData<JetpackFeatureOverlayActions> = _action

    fun openJetpackAppDownloadLink() {
        _action.value = JetpackFeatureOverlayActions.OpenPlayStore
    }

    fun dismissBottomSheet() {
        _action.value = JetpackFeatureOverlayActions.DismissDialog
    }

    fun closeBottomSheetDialog() {
        _action.value = JetpackFeatureOverlayActions.DismissDialog
    }

    fun init(overlayScreenType: JetpackFeatureOverlayScreenType?, rtlLayout: Boolean) {
        val params = JetpackFeatureOverlayContentBuilderParams(
                currentPhase = getCurrentPhase()!!,
                isRtl = rtlLayout,
                feature = overlayScreenType
        )
        _uiState.postValue(jetpackFeatureOverlayContentBuilder.build(params = params))
        jetpackFeatureRemovalOverlayUtil.onOverlayShown(overlayScreenType)
    }

    private fun getCurrentPhase() = jetpackFeatureRemovalPhaseHelper.getCurrentPhase()
}

