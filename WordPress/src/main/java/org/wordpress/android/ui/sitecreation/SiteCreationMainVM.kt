package org.wordpress.android.ui.sitecreation

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.networking.MShot
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewActivity.OpenCheckout.CheckoutDetails
import org.wordpress.android.ui.domains.DomainRegistrationCompletedEvent
import org.wordpress.android.ui.domains.DomainsRegistrationTracker
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleEmpty
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Completed
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Created
import org.wordpress.android.ui.sitecreation.SiteCreationResult.CreatedButNotFetched
import org.wordpress.android.ui.sitecreation.SiteCreationResult.NotCreated
import org.wordpress.android.ui.sitecreation.domains.DomainModel
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.plans.PlanModel
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.extensions.getParcelableCompat
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
const val KEY_SITE_CREATION_STATE = "key_site_creation_state"

@Parcelize
data class SiteCreationState(
    val siteIntent: String? = null,
    val siteName: String? = null,
    val segmentId: Long? = null,
    val siteDesign: String? = null,
    val domain: DomainModel? = null,
    val plan: PlanModel? = null,
    val result: SiteCreationResult = NotCreated,
) : WizardState, Parcelable

typealias NavigationTarget = WizardNavigationTarget<SiteCreationStep, SiteCreationState>

sealed interface SiteCreationResult : Parcelable {
    @Parcelize
    object NotCreated : SiteCreationResult

    sealed interface Created: SiteCreationResult {
        val site: SiteModel
    }

    sealed interface CreatedButNotFetched : Created {
        @Parcelize
        data class NotInLocalDb(
            override val site: SiteModel,
        ) : CreatedButNotFetched

        @Parcelize
        data class InCart(
            override val site: SiteModel,
        ) : CreatedButNotFetched

        @Parcelize
        data class DomainRegistrationPurchased(
            val domainName: String,
            val email: String,
            override val site: SiteModel,
        ) : CreatedButNotFetched
    }

    @Parcelize
    data class Completed(
        override val site: SiteModel,
    ) : Created
}

typealias SiteCreationCompletionEvent = Pair<SiteCreationResult, Boolean>

@HiltViewModel
class SiteCreationMainVM @Inject constructor(
    private val tracker: SiteCreationTracker,
    private val wizardManager: WizardManager<SiteCreationStep>,
    private val networkUtils: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val fetchHomePageLayoutsUseCase: FetchHomePageLayoutsUseCase,
    private val imageManager: ImageManager,
    private val jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil,
    private val domainsRegistrationTracker: DomainsRegistrationTracker,
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

    private lateinit var siteCreationState: SiteCreationState

    internal var preloadingJob: Job? = null

    val navigationTargetObservable: SingleEventObservable<NavigationTarget> by lazy {
        SingleEventObservable(
            wizardManager.navigatorLiveData.map {
                clearOldSiteCreationState(it)
                WizardNavigationTarget(it, siteCreationState)
            }
        )
    }

    private val _dialogAction = SingleLiveEvent<DialogHolder>()
    val dialogActionObservable: LiveData<DialogHolder> = _dialogAction

    private val _onCompleted = SingleLiveEvent<SiteCreationCompletionEvent>()
    val onCompleted: LiveData<SiteCreationCompletionEvent> = _onCompleted

    private val _exitFlowObservable = SingleLiveEvent<Unit?>()
    val exitFlowObservable: LiveData<Unit?> = _exitFlowObservable

    private val _onBackPressedObservable = SingleLiveEvent<Unit?>()
    val onBackPressedObservable: LiveData<Unit?> = _onBackPressedObservable

    private val _showJetpackOverlay = MutableLiveData<Event<Boolean>>()
    val showJetpackOverlay: LiveData<Event<Boolean>> = _showJetpackOverlay

    private val _showDomainCheckout = SingleLiveEvent<CheckoutDetails>()
    val showDomainCheckout: LiveData<CheckoutDetails> = _showDomainCheckout

    fun start(savedInstanceState: Bundle?, siteCreationSource: SiteCreationSource) {
        if (isStarted) return
        if (savedInstanceState == null) {
            tracker.trackSiteCreationAccessed(siteCreationSource)
            siteCreationState = SiteCreationState()
            if (jetpackFeatureRemovalOverlayUtil.shouldShowSiteCreationOverlay())
                showJetpackOverlay()
            if (jetpackFeatureRemovalOverlayUtil.shouldDisableSiteCreation())
                siteCreationDisabled = true
            else
                showSiteCreationNextStep()
        } else {
            siteCreationState = requireNotNull(savedInstanceState.getParcelableCompat(KEY_SITE_CREATION_STATE))
            val currentStepIndex = savedInstanceState.getInt(KEY_CURRENT_STEP)
            try {
                wizardManager.setCurrentStepIndex(currentStepIndex)
            } catch (e: IllegalStateException) {
                // If the current step index is invalid, we reset the wizard
                wizardManager.setCurrentStepIndex(0)
                AppLog.e(T.THEMES, "Resetting site creation wizard: ${e.message}")
            }
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
        if (jetpackFeatureRemovalOverlayUtil.shouldDisableSiteCreation()) {
            return // no need to preload thumbnails if site creation is disabled
        }
        if (preloadingJob == null) {
            preloadingJob = viewModelScope.launch(Dispatchers.IO) {
                if (networkUtils.isNetworkAvailable()) {
                    val response = fetchHomePageLayoutsUseCase.fetchStarterDesigns()

                    // Else clause added to better understand the reason that causes this crash:
                    // https://github.com/wordpress-mobile/WordPress-Android/issues/17020
                    if (response.isError) {
                        AppLog.e(T.THEMES, "Error preloading starter designs: ${response.error}")
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

    fun onDesignSelected(siteDesign: String) {
        siteCreationState = siteCreationState.copy(siteDesign = siteDesign)
        wizardManager.showNextStep()
    }

    fun onBackPressed() {
        return if (wizardManager.isLastStep()) {
            if (siteCreationState.result is Completed) {
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
        if (wizardStep == SiteCreationStep.DOMAINS) {
            siteCreationState.domain?.let {
                siteCreationState = siteCreationState.copy(domain = null)
            }
        }
        if (wizardStep == SiteCreationStep.PLANS) {
            siteCreationState.plan?.let {
                siteCreationState = siteCreationState.copy(plan = null)
            }
        }
    }

    fun onDomainsScreenFinished(domain: DomainModel) {
        siteCreationState = siteCreationState.copy(domain = domain)
        wizardManager.showNextStep()
    }

    fun onPlanSelection(plan: PlanModel?, domainName: String?) {
        siteCreationState = siteCreationState.copy(plan = plan)
        domainName?.let {
            siteCreationState = siteCreationState.copy(domain = siteCreationState.domain?.copy(domainName = it))
        }
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

    fun onCartCreated(checkoutDetails: CheckoutDetails) {
        checkoutDetails.site?.let{
            siteCreationState = siteCreationState.copy(result = CreatedButNotFetched.InCart(it))
        }
        domainsRegistrationTracker.trackDomainsPurchaseWebviewViewed(checkoutDetails.site, isSiteCreation = true)
        _showDomainCheckout.value = checkoutDetails
    }

    fun onCheckoutResult(event: DomainRegistrationCompletedEvent?) {
        if (event == null) return
        if (event.canceled) {
            // Checkout canceled. A site with free domain will be created. Update the isFree parameter of the domain.
            siteCreationState = siteCreationState.copy(domain = siteCreationState.domain?.copy(isFree = true))
        } else {
            domainsRegistrationTracker.trackDomainsPurchaseDomainSuccess(isSiteCreation = true)
        }
        siteCreationState = siteCreationState.run {
            check(result is CreatedButNotFetched.InCart)
            copy(
                result = CreatedButNotFetched.DomainRegistrationPurchased(
                    event.domainName,
                    event.email,
                    result.site,
                )
            )
        }
        wizardManager.showNextStep()
    }

    fun onFreeSiteCreated(site: SiteModel) {
        siteCreationState = siteCreationState.copy(result = CreatedButNotFetched.NotInLocalDb(site))
        if (siteCreationState.plan == null || siteCreationState.plan?.productSlug == "free_plan") {
            wizardManager.showNextStep()
        }
    }

    fun onWizardCancelled() {
        _onCompleted.value = NotCreated to isSiteTitleTaskCompleted()
    }

    fun onWizardFinished(result: Created?) {
        val nullCheckedResult = result ?: NotCreated
        siteCreationState = siteCreationState.copy(result = nullCheckedResult)
        _onCompleted.value = nullCheckedResult to isSiteTitleTaskCompleted()
    }

    private fun isSiteTitleTaskCompleted() = !siteCreationState.siteName.isNullOrBlank()

    /**
     * Exits the flow and tracks an event when the user force-exits the "site creation in progress" before it completes.
     */
    private fun exitFlow(forceExit: Boolean) {
        if (forceExit) {
            tracker.trackFlowExited()
        }
        _exitFlowObservable.call()
    }

    fun onPositiveDialogButtonClicked(instanceTag: String) {
        check(instanceTag == TAG_WARNING_DIALOG) { "Unknown dialog tag: $instanceTag" }
        exitFlow(true)
    }

    fun onNegativeDialogButtonClicked(instanceTag: String) {
        check(instanceTag == TAG_WARNING_DIALOG) { "Unknown dialog tag: $instanceTag" }
        // do nothing
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
