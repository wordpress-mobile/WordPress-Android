package org.wordpress.android.ui.sitecreation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.coroutines.cancel
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.accounts.HelpActivity.Origin
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleEmpty
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM.SiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.ui.sitecreation.SiteCreationStep.DOMAINS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.INTENTS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_DESIGNS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_NAME
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_PREVIEW
import org.wordpress.android.ui.sitecreation.domains.DomainsScreenListener
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsFragment
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.ui.sitecreation.previews.SiteCreationPreviewFragment
import org.wordpress.android.ui.sitecreation.previews.SitePreviewScreenListener
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState.SiteCreationCompleted
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState.SiteNotCreated
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.CreateSiteState.SiteNotInLocalDb
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
import org.wordpress.android.util.wizard.WizardNavigationTarget
import javax.inject.Inject

@AndroidEntryPoint
class SiteCreationActivity : LocaleAwareActivity(),
        IntentsScreenListener,
        SiteNameScreenListener,
        DomainsScreenListener,
        SitePreviewScreenListener,
        OnHelpClickedListener,
        BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface {
    @Inject internal lateinit var uiHelpers: UiHelpers
    @Inject internal lateinit var siteNameFeatureConfig: SiteNameFeatureConfig
    private val mainViewModel: SiteCreationMainVM by viewModels()
    private val hppViewModel: HomePagePickerViewModel by viewModels()
    private val siteCreationIntentsViewModel: SiteCreationIntentsViewModel by viewModels()
    private val siteCreationSiteNameViewModel: SiteCreationSiteNameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.site_creation_activity)
        val siteCreationSource = intent.extras?.getString(ARG_CREATE_SITE_SOURCE)
        mainViewModel.start(savedInstanceState, SiteCreationSource.fromString(siteCreationSource))
        mainViewModel.preloadThumbnails(this)

        observeVMState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mainViewModel.writeToBundle(outState)
    }

    private fun observeVMState() {
        mainViewModel.navigationTargetObservable
                .observe(this, Observer { target -> target?.let { showStep(target) } })
        mainViewModel.wizardFinishedObservable.observe(this, Observer { createSiteState ->
            createSiteState?.let {
                val intent = Intent()
                val (siteCreated, localSiteId, titleTaskComplete) = when (createSiteState) {
                    // site creation flow was canceled
                    is SiteNotCreated -> Triple(false, null, false)
                    is SiteNotInLocalDb -> {
                        // Site was created, but we haven't been able to fetch it, let `SitePickerActivity` handle
                        // this with a Snackbar message.
                        intent.putExtra(SitePickerActivity.KEY_SITE_CREATED_BUT_NOT_FETCHED, true)
                        Triple(true, null, createSiteState.isSiteTitleTaskComplete)
                    }
                    is SiteCreationCompleted -> Triple(true, createSiteState.localSiteId,
                            createSiteState.isSiteTitleTaskComplete)
                }
                intent.putExtra(SitePickerActivity.KEY_SITE_LOCAL_ID, localSiteId)
                intent.putExtra(SitePickerActivity.KEY_SITE_TITLE_TASK_COMPLETED, titleTaskComplete)
                setResult(if (siteCreated) Activity.RESULT_OK else Activity.RESULT_CANCELED, intent)
                finish()
            }
        })
        mainViewModel.dialogActionObservable.observe(this, Observer { dialogHolder ->
            dialogHolder?.let {
                val supportFragmentManager = requireNotNull(supportFragmentManager) {
                    "FragmentManager can't be null at this point"
                }
                dialogHolder.show(this, supportFragmentManager, uiHelpers)
            }
        })
        mainViewModel.exitFlowObservable.observe(this, Observer {
            setResult(Activity.RESULT_CANCELED)
            finish()
        })
        mainViewModel.onBackPressedObservable.observe(this, Observer {
            ActivityUtils.hideKeyboard(this)
            super.onBackPressed()
        })
        siteCreationIntentsViewModel.onBackButtonPressed.observe(this, Observer {
            mainViewModel.onBackPressed()
        })
        siteCreationIntentsViewModel.onSkipButtonPressed.observe(this, Observer {
            mainViewModel.onSiteIntentSkipped()
        })
        siteCreationSiteNameViewModel.onBackButtonPressed.observe(this, Observer {
            mainViewModel.onBackPressed()
            ActivityUtils.hideKeyboard(this)
        })
        siteCreationSiteNameViewModel.onSkipButtonPressed.observe(this, Observer {
            ActivityUtils.hideKeyboard(this)
            mainViewModel.onSiteNameSkipped()
        })
        hppViewModel.onBackButtonPressed.observe(this, Observer {
            mainViewModel.onBackPressed()
        })
        hppViewModel.onDesignActionPressed.observe(this, Observer { design ->
            mainViewModel.onSiteDesignSelected(design.template)
        })
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

    override fun onDomainSelected(domain: String) {
        mainViewModel.onDomainsScreenFinished(domain)
    }

    override fun onSiteCreationCompleted() {
        mainViewModel.onSiteCreationCompleted()
    }

    override fun onSitePreviewScreenDismissed(createSiteState: CreateSiteState) {
        mainViewModel.onSitePreviewScreenFinished(createSiteState)
    }

    override fun onHelpClicked(origin: Origin) {
        ActivityLauncher.viewHelpAndSupport(this, origin, null, null)
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
            DOMAINS -> SiteCreationDomainsFragment.newInstance(
                    screenTitle
            )
            SITE_PREVIEW -> SiteCreationPreviewFragment.newInstance(screenTitle, target.wizardState)
        }
        slideInFragment(fragment, target.wizardStep.toString())
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

    private fun slideInFragment(fragment: Fragment, tag: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) != null) {
            // add to back stack and animate all screen except of the first one
            fragmentTransaction.addToBackStack(null).setCustomAnimations(
                    R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                    R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
            )
        }
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag)
        fragmentTransaction.commit()
    }

    override fun onPositiveClicked(instanceTag: String) {
        mainViewModel.onPositiveDialogButtonClicked(instanceTag)
    }

    override fun onNegativeClicked(instanceTag: String) {
        mainViewModel.onNegativeDialogButtonClicked(instanceTag)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }

    override fun onBackPressed() {
        mainViewModel.onBackPressed()
    }

    companion object {
        const val ARG_CREATE_SITE_SOURCE = "ARG_CREATE_SITE_SOURCE"
    }
}
