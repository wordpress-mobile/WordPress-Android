package org.wordpress.android.ui.sitecreation

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.os.Parcelable
import android.support.annotation.StringRes
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.R
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleEmpty
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.util.wizard.WizardState
import org.wordpress.android.viewmodel.SingleEventObservable
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

@Parcelize
@SuppressLint("ParcelCreator")
data class SiteCreationState(
    val segmentId: Long? = null,
    val verticalId: String? = null,
    val siteTitle: String? = null,
    val siteTagLine: String? = null
) : WizardState, Parcelable

typealias NavigationTarget = WizardNavigationTarget<SiteCreationStep, SiteCreationState>

class NewSiteCreationMainVM @Inject constructor() : ViewModel() {
    private val wizardManager: WizardManager<SiteCreationStep> = WizardManager(
            // TODO we'll receive this from a server/Firebase config
            listOf(
                    SiteCreationStep.fromString("site_creation_segments"),
                    SiteCreationStep.fromString("site_creation_verticals"),
                    SiteCreationStep.fromString("site_creation_site_info"),
                    SiteCreationStep.fromString("site_creation_domains"),
                    SiteCreationStep.fromString("site_creation_site_preview")
            )
    )
    private var isStarted = false
    private var siteCreationState = SiteCreationState()

    val navigationTargetObservable: SingleEventObservable<NavigationTarget> = SingleEventObservable(
            Transformations.map(wizardManager.navigatorLiveData) {
                WizardNavigationTarget(it, siteCreationState)
            }
    )

    private val _wizardFinishedObservable = SingleLiveEvent<Int?>()
    val wizardFinishedObservable: LiveData<Int?> = _wizardFinishedObservable

    fun start() {
        if (isStarted) return
        isStarted = true
        wizardManager.showNextStep()
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

    fun onSitePreviewScreenFinished(newSiteLocalId: Int?) {
        _wizardFinishedObservable.value = newSiteLocalId
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
