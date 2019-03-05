package org.wordpress.android.ui.sitecreation

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.StringRes
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.R
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleEmpty
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.CreateSiteState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.util.wizard.WizardState
import org.wordpress.android.viewmodel.SingleEventObservable
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.DialogHolder
import javax.inject.Inject

const val TAG_WARNING_DIALOG = "back_pressed_warning_dialog"
const val KEY_CURRENT_STEP = "key_current_step"
const val KEY_SITE_CREATION_STATE = "key_site_creation_state"

@Parcelize
@SuppressLint("ParcelCreator")
data class SiteCreationState(
    val segmentId: Long? = null,
    val verticalId: String? = null,
    val siteTitle: String? = null,
    val siteTagLine: String? = null,
    val domain: String? = null
) : WizardState, Parcelable

typealias NavigationTarget = WizardNavigationTarget<SiteCreationStep, SiteCreationState>

class NewSiteCreationMainVM @Inject constructor(
    private val tracker: NewSiteCreationTracker,
    private val wizardManager: WizardManager<SiteCreationStep>
) : ViewModel() {
    private var isStarted = false
    private var siteCreationCompleted = false

    private lateinit var siteCreationState: SiteCreationState

    val navigationTargetObservable: SingleEventObservable<NavigationTarget> by lazy {
        SingleEventObservable(
                Transformations.map(wizardManager.navigatorLiveData) {
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

    fun start(savedInstanceState: Bundle?) {
        if (isStarted) return
        if (savedInstanceState == null) {
            tracker.trackSiteCreationAccessed()
            siteCreationState = SiteCreationState()
        } else {
            siteCreationState = savedInstanceState.getParcelable(KEY_SITE_CREATION_STATE)
            val currentStepIndex = savedInstanceState.getInt(KEY_CURRENT_STEP)
            wizardManager.setCurrentStepIndex(currentStepIndex)
        }
        isStarted = true
        if (savedInstanceState == null) {
            // Show the next step only if it's a fresh activity so we can handle the navigation
            wizardManager.showNextStep()
        }
    }

    fun writeToBundle(outState: Bundle) {
        outState.putInt(KEY_CURRENT_STEP, wizardManager.currentStep)
        outState.putParcelable(KEY_SITE_CREATION_STATE, siteCreationState)
    }

    fun onSegmentSelected(segmentId: Long) {
        siteCreationState = siteCreationState.copy(segmentId = segmentId)
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

    fun onVerticalsScreenFinished(verticalId: String) {
        siteCreationState = siteCreationState.copy(verticalId = verticalId)
        wizardManager.showNextStep()
    }

    fun onDomainsScreenFinished(domain: String) {
        siteCreationState = siteCreationState.copy(domain = domain)
        wizardManager.showNextStep()
    }

    fun onInfoScreenFinished(siteTitle: String, tagLine: String?) {
        siteCreationState = siteCreationState.copy(siteTitle = siteTitle, siteTagLine = tagLine)
        wizardManager.showNextStep()
    }

    fun onSkipClicked() {
        wizardManager.showNextStep()
    }

    fun screenTitleForWizardStep(step: SiteCreationStep): NewSiteCreationScreenTitle {
        val stepPosition = wizardManager.stepPosition(step)
        val stepCount = wizardManager.stepsCount
        val firstStep = stepPosition == 1
        val lastStep = stepPosition == stepCount

        return when {
            firstStep -> ScreenTitleGeneral(R.string.new_site_creation_screen_title_general)
            lastStep -> ScreenTitleEmpty
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

    sealed class NewSiteCreationScreenTitle {
        data class ScreenTitleStepCount(@StringRes val resId: Int, val stepsCount: Int, val stepPosition: Int) :
                NewSiteCreationScreenTitle()

        data class ScreenTitleGeneral(@StringRes val resId: Int) :
                NewSiteCreationScreenTitle()

        object ScreenTitleEmpty : NewSiteCreationScreenTitle() {
            const val screenTitle = ""
        }
    }
}
