package org.wordpress.android.ui.sitecreation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.ActivityLauncherWrapper.Companion.CAMPAIGN_SITE_CREATION
import org.wordpress.android.ui.ActivityLauncherWrapper.Companion.JETPACK_PACKAGE_NAME
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.accounts.HelpActivity.Origin
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewActivity.OpenCheckout
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayFragment
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureFullScreenOverlayViewModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureOverlayActions.DismissDialog
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureOverlayActions.OpenPlayStore
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.main.ChooseSiteActivity
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleEmpty
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Completed
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Created
import org.wordpress.android.ui.sitecreation.SiteCreationResult.CreatedButNotFetched
import org.wordpress.android.ui.sitecreation.SiteCreationStep.DOMAINS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.INTENTS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.PLANS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.PROGRESS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_DESIGNS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_NAME
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_PREVIEW
import org.wordpress.android.ui.sitecreation.domains.DomainModel
import org.wordpress.android.ui.sitecreation.domains.DomainsScreenListener
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsFragment
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.ui.sitecreation.plans.PlanModel
import org.wordpress.android.ui.sitecreation.plans.PlansScreenListener
import org.wordpress.android.ui.sitecreation.plans.SiteCreationPlansFragment
import org.wordpress.android.ui.sitecreation.previews.SiteCreationPreviewFragment
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressFragment
import org.wordpress.android.ui.sitecreation.progress.SiteCreationProgressViewModel
import org.wordpress.android.ui.sitecreation.sitename.SiteCreationSiteNameFragment
import org.wordpress.android.ui.sitecreation.sitename.SiteCreationSiteNameViewModel
import org.wordpress.android.ui.sitecreation.sitename.SiteNameScreenListener
import org.wordpress.android.ui.sitecreation.theme.HomePagePickerFragment
import org.wordpress.android.ui.sitecreation.theme.HomePagePickerViewModel
import org.wordpress.android.ui.sitecreation.verticals.IntentsScreenListener
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsFragment
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationIntentsViewModel
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.config.SiteNameFeatureConfig
import org.wordpress.android.util.extensions.exhaustive
import org.wordpress.android.util.extensions.onBackPressedCompat
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject
import android.R as AndroidR

@AndroidEntryPoint
class SiteCreationActivity : LocaleAwareActivity(),
    IntentsScreenListener,
    SiteNameScreenListener,
    DomainsScreenListener,
    PlansScreenListener,
    OnHelpClickedListener,
    BasicDialogPositiveClickInterface,
    BasicDialogNegativeClickInterface {
    @Inject
    internal lateinit var uiHelpers: UiHelpers

    @Inject
    internal lateinit var siteNameFeatureConfig: SiteNameFeatureConfig
    private val mainViewModel: SiteCreationMainVM by viewModels()
    private val hppViewModel: HomePagePickerViewModel by viewModels()
    private val siteCreationIntentsViewModel: SiteCreationIntentsViewModel by viewModels()
    private val siteCreationSiteNameViewModel: SiteCreationSiteNameViewModel by viewModels()
    private val jetpackFullScreenViewModel: JetpackFeatureFullScreenOverlayViewModel by viewModels()
    private val progressViewModel: SiteCreationProgressViewModel by viewModels()
    private val previewViewModel: SitePreviewViewModel by viewModels()

    @Inject
    internal lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

    @Inject
    internal lateinit var activityLauncherWrapper: ActivityLauncherWrapper

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            mainViewModel.onBackPressed()
        }
    }

    private val domainCheckoutActivityLauncher = registerForActivityResult(OpenCheckout()) {
        mainViewModel.onCheckoutResult(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.site_creation_activity)

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        mainViewModel.start(savedInstanceState, getSiteCreationSource())
        mainViewModel.preloadThumbnails(this)

        observeVMState()
        observeOverlayEvents()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mainViewModel.writeToBundle(outState)
    }

    @Suppress("LongMethod")
    private fun observeVMState() {
        mainViewModel.navigationTargetObservable.observe(this, ::showStep)
        mainViewModel.onCompleted.observe(this) { (result, isTitleTaskComplete) ->
            val intent = Intent().apply {
                putExtra(ChooseSiteActivity.KEY_SITE_LOCAL_ID, (result as? Created)?.site?.id)
                putExtra(ChooseSiteActivity.KEY_SITE_TITLE_TASK_COMPLETED, isTitleTaskComplete)
                // Let `ChooseSiteActivity` handle this with a SnackBar message
                putExtra(ChooseSiteActivity.KEY_SITE_CREATED_BUT_NOT_FETCHED, result is CreatedButNotFetched)
            }
            setResult(if (result is Completed) Activity.RESULT_OK else Activity.RESULT_CANCELED, intent)
            finish()
        }
        mainViewModel.dialogActionObservable.observe(this) {
            it?.show(this, supportFragmentManager, uiHelpers)
        }
        mainViewModel.exitFlowObservable.observe(this) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        mainViewModel.onBackPressedObservable.observe(this) {
            ActivityUtils.hideKeyboard(this)
            onBackPressedDispatcher.onBackPressedCompat(backPressedCallback)
        }
        mainViewModel.showDomainCheckout.observe(this, domainCheckoutActivityLauncher::launch)
        siteCreationIntentsViewModel.onBackButtonPressed.observe(this) { mainViewModel.onBackPressed() }
        siteCreationIntentsViewModel.onSkipButtonPressed.observe(this) { mainViewModel.onSiteIntentSkipped() }
        siteCreationSiteNameViewModel.onBackButtonPressed.observe(this) {
            mainViewModel.onBackPressed()
            ActivityUtils.hideKeyboard(this)
        }
        siteCreationSiteNameViewModel.onSkipButtonPressed.observe(this) {
            ActivityUtils.hideKeyboard(this)
            mainViewModel.onSiteNameSkipped()
        }
        hppViewModel.onBackButtonPressed.observe(this) { mainViewModel.onBackPressed() }
        hppViewModel.onDesignActionPressed.observe(this) { mainViewModel.onDesignSelected(it.template) }
        progressViewModel.onCancelWizardClicked.observe(this) { mainViewModel.onWizardCancelled() }
        progressViewModel.onFreeSiteCreated.observe(this, mainViewModel::onFreeSiteCreated)
        progressViewModel.onCartCreated.observe(this, mainViewModel::onCartCreated)
        previewViewModel.onOkButtonClicked.observe(this, mainViewModel::onWizardFinished)
    }

    private fun observeOverlayEvents() {
        if(BuildConfig.IS_JETPACK_APP)
            return

        val fragment = supportFragmentManager.findFragmentByTag(JetpackFeatureFullScreenOverlayFragment.TAG)
                    as? JetpackFeatureFullScreenOverlayFragment ?: JetpackFeatureFullScreenOverlayFragment
                        .newInstance(isSiteCreationOverlay = true, siteCreationSource = getSiteCreationSource())

        jetpackFullScreenViewModel.action.observe(this) { action ->
            if (mainViewModel.siteCreationDisabled) finish()
            when (action) {
                is OpenPlayStore -> {
                    fragment.dismiss()
                    activityLauncherWrapper.openPlayStoreLink(this, JETPACK_PACKAGE_NAME, CAMPAIGN_SITE_CREATION)
                }
                is DismissDialog -> {
                    fragment.dismiss()
                }
                else -> fragment.dismiss()
            }.exhaustive
        }

        mainViewModel.showJetpackOverlay.observeEvent(this) {
            if (mainViewModel.siteCreationDisabled)
                showFragment(fragment, JetpackFeatureFullScreenOverlayFragment.TAG)
            else fragment.show(supportFragmentManager, JetpackFeatureFullScreenOverlayFragment.TAG)
        }
    }

    private fun getSiteCreationSource(): SiteCreationSource {
        val siteCreationSource = intent.extras?.getString(ARG_CREATE_SITE_SOURCE)
        return SiteCreationSource.fromString(siteCreationSource)
    }

    override fun onIntentSelected(intent: String?) {
        mainViewModel.onSiteIntentSelected(intent)
        if (!siteNameFeatureConfig.isEnabled()) {
            ActivityUtils.hideKeyboard(this)
        }
    }

    override fun onSiteNameEntered(siteName: String) {
        mainViewModel.onSiteNameEntered(siteName)
        ActivityUtils.hideKeyboard(this)
    }

    override fun onDomainSelected(domain: DomainModel) {
        mainViewModel.onDomainsScreenFinished(domain)
    }

    override fun onPlanSelected(plan: PlanModel?, domainName: String?) {
        mainViewModel.onPlanSelection(plan, domainName)
    }

    override fun onHelpClicked(origin: Origin) {
        ActivityLauncher.viewHelp(this, origin, null, null)
    }

    private fun showStep(target: WizardNavigationTarget<SiteCreationStep, SiteCreationState>) {
        val screenTitle = getScreenTitle(target.wizardStep)
        val fragment = when (target.wizardStep) {
            INTENTS -> SiteCreationIntentsFragment()
            SITE_NAME -> SiteCreationSiteNameFragment.newInstance(target.wizardState.siteIntent)
            SITE_DESIGNS -> {
                // Cancel preload job before displaying the theme picker.
                mainViewModel.preloadingJob?.cancel("Preload did not complete before theme picker was shown.")
                HomePagePickerFragment.newInstance(target.wizardState.siteIntent)
            }
            DOMAINS -> SiteCreationDomainsFragment.newInstance(screenTitle)
            PLANS -> SiteCreationPlansFragment.newInstance(target.wizardState)
            PROGRESS -> SiteCreationProgressFragment.newInstance(target.wizardState)
            SITE_PREVIEW -> SiteCreationPreviewFragment.newInstance(screenTitle, target.wizardState)
        }
        showFragment(fragment, target.wizardStep.toString(), target.wizardStep != SITE_PREVIEW)
    }

    private fun getScreenTitle(step: SiteCreationStep): String {
        return when (val screenTitleData = mainViewModel.screenTitleForWizardStep(step)) {
            is ScreenTitleStepCount -> getString(
                screenTitleData.resId,
                screenTitleData.stepPosition,
                screenTitleData.stepsCount
            )
            is ScreenTitleGeneral -> getString(screenTitleData.resId)
            is ScreenTitleEmpty -> screenTitleData.screenTitle
        }
    }

    private fun showFragment(fragment: Fragment, tag: String, slideIn: Boolean = true) {
        supportFragmentManager.commit {
            if (supportFragmentManager.findFragmentById(R.id.fragment_container) != null) {
                // add to back stack and animate all screen except of the first one
                addToBackStack(null)
                if (slideIn) {
                    setCustomAnimations(
                        R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                        R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
                    )
                } else {
                    setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                }
            }
            replace(R.id.fragment_container, fragment, tag)
        }
    }

    override fun onPositiveClicked(instanceTag: String) {
        mainViewModel.onPositiveDialogButtonClicked(instanceTag)
    }

    override fun onNegativeClicked(instanceTag: String) {
        mainViewModel.onNegativeDialogButtonClicked(instanceTag)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }

    companion object {
        const val ARG_CREATE_SITE_SOURCE = "ARG_CREATE_SITE_SOURCE"
        const val ARG_STATE = "ARG_SITE_CREATION_STATE"
    }
}
