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
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.CreateSiteState
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.util.wizard.WizardState
import org.wordpress.android.viewmodel.SingleEventObservable
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

private const val KEY_CURRENT_STEP = "key_current_step"
private const val KEY_SITE_CREATION_STATE = "key_site_creation_state"
private val SITE_CREATION_STEPS =
        // TODO we'll receive this from a server/Firebase config
        listOf(
                SiteCreationStep.fromString("site_creation_segments"),
                SiteCreationStep.fromString("site_creation_verticals"),
                SiteCreationStep.fromString("site_creation_site_info"),
                SiteCreationStep.fromString("site_creation_domains"),
                SiteCreationStep.fromString("site_creation_site_preview")
        )

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

class NewSiteCreationMainVM @Inject constructor() : ViewModel() {
    private var isStarted = false
    private lateinit var wizardManager: WizardManager<SiteCreationStep>
    private lateinit var siteCreationState: SiteCreationState

    val navigationTargetObservable: SingleEventObservable<NavigationTarget> by lazy {
        SingleEventObservable(
                Transformations.map(wizardManager.navigatorLiveData) {
                    WizardNavigationTarget(it, siteCreationState)
                }
        )
    }

    private val _wizardFinishedObservable = SingleLiveEvent<CreateSiteState>()
    val wizardFinishedObservable: LiveData<CreateSiteState> = _wizardFinishedObservable

    fun start(savedInstanceState: Bundle?) {
        if (isStarted) return
        if (savedInstanceState == null) {
            siteCreationState = SiteCreationState()
            wizardManager = WizardManager(SITE_CREATION_STEPS)
        } else {
            siteCreationState = savedInstanceState.getParcelable(KEY_SITE_CREATION_STATE)
            val currentStepIndex = savedInstanceState.getInt(KEY_CURRENT_STEP)
            wizardManager = WizardManager(SITE_CREATION_STEPS, currentStepIndex)
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

    fun shouldSuppressBackPress(): Boolean = wizardManager.isLastStep()

    fun onBackPressed() {
        wizardManager.onBackPressed()
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

    fun onSitePreviewScreenFinished(createSiteState: CreateSiteState) {
        _wizardFinishedObservable.value = createSiteState
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
