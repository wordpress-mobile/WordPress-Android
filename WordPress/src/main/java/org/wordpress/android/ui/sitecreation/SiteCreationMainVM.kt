package org.wordpress.android.ui.sitecreation

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.R
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleEmpty
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.ui.sitecreation.SiteCreationStep.DOMAINS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SEGMENTS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_PREVIEW
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState
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
    val domain: String? = null
) : WizardState, Parcelable

typealias NavigationTarget = WizardNavigationTarget<SiteCreationStep, SiteCreationState>

class SiteCreationMainVM @Inject constructor(
    private val tracker: SiteCreationTracker,
    private val wizardManager: WizardManager<SiteCreationStep>
) : ViewModel() {
    private var isStarted = false
    private var siteCreationCompleted = false

    private lateinit var siteCreationState: SiteCreationState

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

    private fun clearOldSiteCreationState(wizardStep: SiteCreationStep) {
        when (wizardStep) {
            SEGMENTS -> siteCreationState.segmentId?.let {
                siteCreationState = siteCreationState.copy(segmentId = null) }
            DOMAINS -> siteCreationState.domain?.let {
                siteCreationState = siteCreationState.copy(domain = null) }
            SITE_PREVIEW -> {} // intentionally left empty
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
        val singleInBetweenStepDomains = wizardManager.stepsCount == 3 && step.name == DOMAINS.name

        return when {
            firstStep -> ScreenTitleGeneral(R.string.new_site_creation_screen_title_general)
            lastStep -> ScreenTitleEmpty
            singleInBetweenStepDomains -> ScreenTitleGeneral(R.string.my_site_select_domains_page_title)
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
