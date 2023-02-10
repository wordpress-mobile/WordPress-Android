package org.wordpress.android.ui.sitecreation

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.networking.MShot
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleEmpty
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.experiments.SiteCreationDomainPurchasingExperiment
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.util.wizard.WizardState
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.SingleEventObservable
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.DialogHolder
import javax.inject.Inject

const val TAG_WARNING_DIALOG = "back_pressed_warning_dialog"
const val KEY_CURRENT_STEP = "key_current_step"
const val KEY_SITE_CREATION_COMPLETED = "key_site_creation_completed"
const val KEY_SITE_CREATION_STATE = "key_site_creation_state"

@Parcelize
@SuppressLint("ParcelCreator")
data class SiteCreationState(
    val siteIntent: String? = null,
    val siteName: String? = null,
    val segmentId: Long? = null,
    val siteDesign: String? = null,
    val domain: String? = null
) : WizardState, Parcelable

typealias NavigationTarget = WizardNavigationTarget<SiteCreationStep, SiteCreationState>

@HiltViewModel
class SiteCreationMainVM @Inject constructor(
    private val tracker: SiteCreationTracker,
    private val wizardManager: WizardManager<SiteCreationStep>,
    private val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchHomePageLayoutsUseCase: FetchHomePageLayoutsUseCase,
    private val imageManager: ImageManager,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil,
    private val domainPurchasingExperiment: SiteCreationDomainPurchasingExperiment,
) : ViewModel() {
    init {
        dispatcher.register(fetchHomePageLayoutsUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchHomePageLayoutsUseCase)
    }

    var siteCreationDisabled: Boolean = false
    private var isStarted = false
    private var siteCreationCompleted = false

    private lateinit var siteCreationState: SiteCreationState

    internal var preloadingJob: Job? = null

    val navigationTargetObservable: SingleEventObservable<NavigationTarget> by lazy {
        SingleEventObservable(
            Transformations.map(wizardManager.navigatorLiveData) {
                clearOldSiteCreationState(it)
                WizardNavigationTarget(it, siteCreationState)
            }
        )
    }

    private val _dialogAction = SingleLiveEvent<DialogHolder>()
    val dialogActionObservable: LiveData<DialogHolder> = _dialogAction

    private val _wizardFinishedObservable = SingleLiveEvent<CreateSiteState>()
    val wizardFinishedObservable: LiveData<CreateSiteState> = _wizardFinishedObservable

    private val _exitFlowObservable = SingleLiveEvent<Unit>()
    val exitFlowObservable: LiveData<Unit> = _exitFlowObservable

    private val _onBackPressedObservable = SingleLiveEvent<Unit>()
    val onBackPressedObservable: LiveData<Unit> = _onBackPressedObservable

    private val _showJetpackOverlay = MutableLiveData<Event<Boolean>>()
    val showJetpackOverlay: LiveData<Event<Boolean>> = _showJetpackOverlay

    fun start(savedInstanceState: Bundle?, siteCreationSource: SiteCreationSource) {
        if (isStarted) return
        if (savedInstanceState == null) {
            tracker.trackSiteCreationAccessed(siteCreationSource)
            tracker.trackSiteCreationDomainPurchasingExperimentVariation(domainPurchasingExperiment.getVariation())
            siteCreationState = SiteCreationState()
            if (jetpackFeatureRemovalOverlayUtil.shouldShowSiteCreationOverlay())
                showJetpackOverlay()
            if (jetpackFeatureRemovalOverlayUtil.shouldDisableSiteCreation())
                siteCreationDisabled = true
            else
                showSiteCreationNextStep()
        } else {
            siteCreationCompleted = savedInstanceState.getBoolean(KEY_SITE_CREATION_COMPLETED, false)
            siteCreationState = requireNotNull(savedInstanceState.getParcelable(KEY_SITE_CREATION_STATE))
            val currentStepIndex = savedInstanceState.getInt(KEY_CURRENT_STEP)
            wizardManager.setCurrentStepIndex(currentStepIndex)
        }
        isStarted = true
    }

    private fun showSiteCreationNextStep() {
        wizardManager.showNextStep()
    }

    private fun showJetpackOverlay() {
        _showJetpackOverlay.value = Event(true)
    }

    fun preloadThumbnails(context: Context) {
        if (preloadingJob == null) {
            preloadingJob = viewModelScope.launch(Dispatchers.IO) {
                if (networkUtils.isNetworkAvailable()) {
                    val response = fetchHomePageLayoutsUseCase.fetchStarterDesigns()

                    // Else clause added to better understand the reason that causes this crash:
                    // https://github.com/wordpress-mobile/WordPress-Android/issues/17020
                    if (response.isError) {
                        AppLog.e(T.THEMES, "Error preloading starter designs: ${response.error}")
                        return@launch
                    } else if (response.designs == null) {
                        AppLog.e(T.THEMES, "Null starter designs response: $response")
                        return@launch
                    }

                    for (design in response.designs) {
                        imageManager.preload(context, MShot(design.previewMobile))
                    }
                }
                preloadingJob = null
            }
        }
    }

    fun writeToBundle(outState: Bundle) {
        outState.putBoolean(KEY_SITE_CREATION_COMPLETED, siteCreationCompleted)
        outState.putInt(KEY_CURRENT_STEP, wizardManager.currentStep)
        outState.putParcelable(KEY_SITE_CREATION_STATE, siteCreationState)
    }

    fun onSiteIntentSelected(intent: String?) {
        siteCreationState = siteCreationState.copy(siteIntent = intent)
        wizardManager.showNextStep()
    }

    fun onSiteIntentSkipped() {
        siteCreationState = siteCreationState.copy(siteIntent = null)
        wizardManager.showNextStep()
    }

    fun onSiteNameSkipped() {
        siteCreationState = siteCreationState.copy(siteName = null)
        wizardManager.showNextStep()
    }

    fun onSiteNameEntered(siteName: String) {
        siteCreationState = siteCreationState.copy(siteName = siteName)
        wizardManager.showNextStep()
    }

    fun onSiteDesignSelected(siteDesign: String) {
        siteCreationState = siteCreationState.copy(siteDesign = siteDesign)
        wizardManager.showNextStep()
    }

    fun onBackPressed() {
        return if (wizardManager.isLastStep()) {
            if (siteCreationCompleted) {
                exitFlow(false)
            } else {
                _dialogAction.value = DialogHolder(
                    tag = TAG_WARNING_DIALOG,
                    title = null,
                    message = UiStringRes(R.string.new_site_creation_preview_back_pressed_warning),
                    positiveButton = UiStringRes(R.string.exit),
                    negativeButton = UiStringRes(R.string.cancel)
                )
            }
        } else {
            wizardManager.onBackPressed()
            _onBackPressedObservable.call()
        }
    }

    private fun clearOldSiteCreationState(wizardStep: SiteCreationStep) {
        when (wizardStep) {
            SiteCreationStep.SITE_DESIGNS -> Unit // Do nothing
            SiteCreationStep.DOMAINS -> siteCreationState.domain?.let {
                siteCreationState = siteCreationState.copy(domain = null)
            }
            SiteCreationStep.SITE_PREVIEW -> Unit // Do nothing
            SiteCreationStep.INTENTS -> Unit // Do nothing
            SiteCreationStep.SITE_NAME -> Unit // Do nothing
        }
    }

    fun onDomainsScreenFinished(domain: String) {
        siteCreationState = siteCreationState.copy(domain = domain)
        wizardManager.showNextStep()
    }

    fun screenTitleForWizardStep(step: SiteCreationStep): SiteCreationScreenTitle {
        val stepPosition = wizardManager.stepPosition(step)
        val stepCount = wizardManager.stepsCount
        val firstStep = stepPosition == 1
        val lastStep = stepPosition == stepCount
        val singleInBetweenStepDomains = step.name == SiteCreationStep.DOMAINS.name

        return when {
            firstStep -> ScreenTitleGeneral(R.string.new_site_creation_screen_title_general)
            lastStep -> ScreenTitleEmpty
            singleInBetweenStepDomains -> ScreenTitleGeneral(R.string.new_site_creation_domain_header_title)
            else -> ScreenTitleStepCount(
                R.string.new_site_creation_screen_title_step_count,
                stepCount - 2, // -2 -> first = general title (Create Site), last item = empty title
                stepPosition - 1 // -1 -> first item has general title - Create Site
            )
        }
    }

    fun onSiteCreationCompleted() {
        siteCreationCompleted = true
    }

    /**
     * Exits the flow and tracks an event when the user force-exits the "site creation in progress" before it completes.
     */
    private fun exitFlow(forceExit: Boolean) {
        if (forceExit) {
            tracker.trackFlowExited()
        }
        _exitFlowObservable.call()
    }

    fun onSitePreviewScreenFinished(createSiteState: CreateSiteState) {
        _wizardFinishedObservable.value = createSiteState
    }

    fun onPositiveDialogButtonClicked(instanceTag: String) {
        when (instanceTag) {
            TAG_WARNING_DIALOG -> {
                exitFlow(true)
            }
            else -> NotImplementedError("Unknown dialog tag: $instanceTag")
        }
    }

    fun onNegativeDialogButtonClicked(instanceTag: String) {
        when (instanceTag) {
            TAG_WARNING_DIALOG -> {
                // do nothing
            }
            else -> NotImplementedError("Unknown dialog tag: $instanceTag")
        }
    }

    sealed class SiteCreationScreenTitle {
        data class ScreenTitleStepCount(@StringRes val resId: Int, val stepsCount: Int, val stepPosition: Int) :
            SiteCreationScreenTitle()

        data class ScreenTitleGeneral(@StringRes val resId: Int) :
            SiteCreationScreenTitle()

        object ScreenTitleEmpty : SiteCreationScreenTitle() {
            const val screenTitle = ""
        }
    }
}
