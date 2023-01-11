package org.wordpress.android.ui.jetpackoverlay

import androidx.lifecycle.LiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureOverlayActions.OpenMigrationInfoLink
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackOverlayDismissalType.CLOSE_BUTTON
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackOverlayDismissalType.CONTINUE_BUTTON
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource.UNSPECIFIED
import org.wordpress.android.util.config.JPDeadlineConfig
import org.wordpress.android.util.config.PhaseThreeBlogPostLinkConfig
import org.wordpress.android.util.config.PhaseTwoBlogPostLinkConfig
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
@Suppress("LongParameterList")
class JetpackFeatureFullScreenOverlayViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val jetpackFeatureOverlayContentBuilder: JetpackFeatureOverlayContentBuilder,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil,
    private val jpDeadlineConfig: JPDeadlineConfig,
    private val phaseTwoBlogPostLinkConfig: PhaseTwoBlogPostLinkConfig,
    private val phaseThreeBlogPostLinkConfig: PhaseThreeBlogPostLinkConfig
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = SingleLiveEvent<JetpackFeatureOverlayUIState>()
    val uiState: LiveData<JetpackFeatureOverlayUIState> = _uiState

    private val _action = SingleLiveEvent<JetpackFeatureOverlayActions>()
    val action: LiveData<JetpackFeatureOverlayActions> = _action

    private lateinit var screenType: JetpackFeatureOverlayScreenType
    private var isSiteCreationOverlayScreen: Boolean = false
    private var siteCreationOrigin: SiteCreationSource = UNSPECIFIED
    private var isDeepLinkOverlayScreen: Boolean = false
    private var isFeatureCollectionOverlayScreen: Boolean = false
    private var featureCollectionOverlayOrigin: JetpackFeatureCollectionOverlaySource =
        JetpackFeatureCollectionOverlaySource.UNSPECIFIED

    fun openJetpackAppDownloadLink() {
        if (isSiteCreationOverlayScreen) {
            _action.value = JetpackFeatureOverlayActions.OpenPlayStore
            jetpackFeatureRemovalOverlayUtil.trackInstallJetpackTappedInSiteCreationOverlay(siteCreationOrigin)
        } else if (isDeepLinkOverlayScreen) {
            _action.value = JetpackFeatureOverlayActions.ForwardToJetpack
            jetpackFeatureRemovalOverlayUtil.trackInstallJetpackTappedInDeepLinkOverlay()
        } else if (isFeatureCollectionOverlayScreen) {
            _action.value = JetpackFeatureOverlayActions.OpenPlayStore
            jetpackFeatureRemovalOverlayUtil.trackInstallJetpackTappedInFeatureCollectionOverlay(
                featureCollectionOverlayOrigin
            )
        } else {
            _action.value = JetpackFeatureOverlayActions.OpenPlayStore
            jetpackFeatureRemovalOverlayUtil.trackInstallJetpackTapped(screenType)
        }
    }

    fun continueToFeature() {
        _action.value = JetpackFeatureOverlayActions.DismissDialog
        if (isSiteCreationOverlayScreen)
            jetpackFeatureRemovalOverlayUtil.trackBottomSheetDismissedInSiteCreationOverlay(
                siteCreationOrigin,
                CONTINUE_BUTTON
            )
        else if (isDeepLinkOverlayScreen)
            jetpackFeatureRemovalOverlayUtil.trackBottomSheetDismissedInDeepLinkOverlay(CONTINUE_BUTTON)
        else if (isFeatureCollectionOverlayScreen)
            jetpackFeatureRemovalOverlayUtil.trackBottomSheetDismissedInFeatureCollectionOverlay(
                featureCollectionOverlayOrigin,
                CONTINUE_BUTTON
            )
        else jetpackFeatureRemovalOverlayUtil.trackBottomSheetDismissed(screenType, CONTINUE_BUTTON)
    }

    fun closeBottomSheet() {
        _action.value = JetpackFeatureOverlayActions.DismissDialog
        if (isSiteCreationOverlayScreen)
            jetpackFeatureRemovalOverlayUtil.trackBottomSheetDismissedInSiteCreationOverlay(
                siteCreationOrigin,
                CLOSE_BUTTON
            )
        else if (isDeepLinkOverlayScreen)
            jetpackFeatureRemovalOverlayUtil.trackBottomSheetDismissedInDeepLinkOverlay(CLOSE_BUTTON)
        else if (isFeatureCollectionOverlayScreen)
            jetpackFeatureRemovalOverlayUtil.trackBottomSheetDismissedInFeatureCollectionOverlay(
                featureCollectionOverlayOrigin,
                CLOSE_BUTTON
            )
        else jetpackFeatureRemovalOverlayUtil.trackBottomSheetDismissed(screenType, CLOSE_BUTTON)
    }

    @Suppress("ReturnCount", "LongParameterList")
    fun init(
        overlayScreenType: JetpackFeatureOverlayScreenType?,
        isSiteCreationOverlay: Boolean,
        isDeepLinkOverlay: Boolean,
        siteCreationSource: SiteCreationSource,
        isFeatureCollectionOverlay: Boolean,
        featureCollectionOverlaySource: JetpackFeatureCollectionOverlaySource,
        rtlLayout: Boolean
    ) {
        if (isSiteCreationOverlay) {
            isSiteCreationOverlayScreen = true
            siteCreationOrigin = siteCreationSource
            _uiState.postValue(
                jetpackFeatureOverlayContentBuilder.buildSiteCreationOverlayState(
                    getSiteCreationPhase()!!,
                    rtlLayout
                )
            )
            jetpackFeatureRemovalOverlayUtil.trackSiteCreationOverlayShown(siteCreationOrigin)
            return
        }

        if (isDeepLinkOverlay) {
            isDeepLinkOverlayScreen = true
            _uiState.postValue(jetpackFeatureOverlayContentBuilder.buildDeepLinkOverlayState(rtlLayout))
            jetpackFeatureRemovalOverlayUtil.trackDeepLinkOverlayShown()
            return
        }

        if (isFeatureCollectionOverlay) {
            isFeatureCollectionOverlayScreen = true
            featureCollectionOverlayOrigin = featureCollectionOverlaySource
            _uiState.postValue(
                jetpackFeatureOverlayContentBuilder.buildFeatureCollectionOverlayState(
                    rtlLayout,
                    getCurrentPhase()!!,
                    phaseThreeBlogPostLinkConfig.getValue()
                )
            )
            jetpackFeatureRemovalOverlayUtil.onFeatureCollectionOverlayShown(featureCollectionOverlaySource)
            return
        }

        screenType = overlayScreenType ?: return
        val params = JetpackFeatureOverlayContentBuilderParams(
            currentPhase = getCurrentPhase()!!,
            isRtl = rtlLayout,
            feature = overlayScreenType,
            jpDeadlineDate = jpDeadlineConfig.getValue(),
            phaseTwoBlogPostLink = phaseTwoBlogPostLinkConfig.getValue(),
            phaseThreeBlogPostLink = phaseThreeBlogPostLinkConfig.getValue()
        )
        _uiState.postValue(jetpackFeatureOverlayContentBuilder.build(params = params))
        jetpackFeatureRemovalOverlayUtil.onOverlayShown(overlayScreenType)
    }

    private fun getCurrentPhase() = jetpackFeatureRemovalPhaseHelper.getCurrentPhase()

    private fun getSiteCreationPhase() = jetpackFeatureRemovalPhaseHelper.getSiteCreationPhase()

    fun openJetpackMigrationInfoLink(migrationInfoRedirectUrl: String) {
        if (isFeatureCollectionOverlayScreen) {
            jetpackFeatureRemovalOverlayUtil.trackLearnMoreAboutMigrationClickedInFeatureCollectionOverlay(
                featureCollectionOverlayOrigin
            )
        } else {
            jetpackFeatureRemovalOverlayUtil.trackLearnMoreAboutMigrationClicked(screenType)
        }
        _action.value = OpenMigrationInfoLink(migrationInfoRedirectUrl)
    }
}

