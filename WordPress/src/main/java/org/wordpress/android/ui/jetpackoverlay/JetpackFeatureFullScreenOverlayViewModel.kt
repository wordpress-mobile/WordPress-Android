package org.wordpress.android.ui.jetpackoverlay

import androidx.lifecycle.LiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackOverlayDismissalType.CLOSE_BUTTON
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackOverlayDismissalType.CONTINUE_BUTTON
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class JetpackFeatureFullScreenOverlayViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val jetpackFeatureOverlayContentBuilder: JetpackFeatureOverlayContentBuilder,
    private val jetpackFeatureRemovalHelper: JetpackFeatureRemovalHelper,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = SingleLiveEvent<JetpackFeatureOverlayUIState>()
    val uiState: LiveData<JetpackFeatureOverlayUIState> = _uiState

    private val _action = SingleLiveEvent<JetpackFeatureOverlayActions>()
    val action: LiveData<JetpackFeatureOverlayActions> = _action

    private var isDeepLinkOverlayScreen: Boolean = false
    private var featureCollectionOverlayOrigin: JetpackFeatureCollectionOverlaySource =
        JetpackFeatureCollectionOverlaySource.UNSPECIFIED

    fun openJetpackAppDownloadLink() {
        if (isDeepLinkOverlayScreen) {
            _action.value = JetpackFeatureOverlayActions.ForwardToJetpack
            jetpackFeatureRemovalOverlayUtil.trackInstallJetpackTappedInDeepLinkOverlay()
        } else {
            _action.value = JetpackFeatureOverlayActions.OpenPlayStore
            jetpackFeatureRemovalOverlayUtil.trackInstallJetpackTappedInFeatureCollectionOverlay(
                featureCollectionOverlayOrigin
            )
        }
    }

    fun continueToFeature() = dismiss(CONTINUE_BUTTON)

    fun closeBottomSheet() = dismiss(CLOSE_BUTTON)

    private fun dismiss(dismissalType: JetpackFeatureRemovalOverlayUtil.JetpackOverlayDismissalType) {
        _action.value = JetpackFeatureOverlayActions.DismissDialog
        if (isDeepLinkOverlayScreen) {
            jetpackFeatureRemovalOverlayUtil.trackBottomSheetDismissedInDeepLinkOverlay(dismissalType)
        } else {
            jetpackFeatureRemovalOverlayUtil.trackBottomSheetDismissedInFeatureCollectionOverlay(
                featureCollectionOverlayOrigin,
                dismissalType
            )
        }
    }

    fun init(
        isDeepLinkOverlay: Boolean,
        featureCollectionOverlaySource: JetpackFeatureCollectionOverlaySource,
        rtlLayout: Boolean
    ) {
        if (isDeepLinkOverlay) {
            isDeepLinkOverlayScreen = true
            _uiState.postValue(jetpackFeatureOverlayContentBuilder.buildDeepLinkOverlayState(rtlLayout))
            jetpackFeatureRemovalOverlayUtil.trackDeepLinkOverlayShown()
            return
        }

        if (!jetpackFeatureRemovalHelper.shouldRemoveJetpackFeatures()) {
            return _action.postValue(JetpackFeatureOverlayActions.DismissDialog)
        }
        featureCollectionOverlayOrigin = featureCollectionOverlaySource
        _uiState.postValue(jetpackFeatureOverlayContentBuilder.buildFeatureCollectionOverlayState(rtlLayout))
        jetpackFeatureRemovalOverlayUtil.onFeatureCollectionOverlayShown(featureCollectionOverlaySource)
    }
}

