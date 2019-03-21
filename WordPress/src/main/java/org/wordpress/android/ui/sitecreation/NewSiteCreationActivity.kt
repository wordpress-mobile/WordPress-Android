package org.wordpress.android.ui.sitecreation

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.accounts.HelpActivity.Origin
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleEmpty
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.ui.sitecreation.SiteCreationStep.DOMAINS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SEGMENTS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_INFO
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_PREVIEW
import org.wordpress.android.ui.sitecreation.SiteCreationStep.VERTICALS
import org.wordpress.android.ui.sitecreation.domains.DomainsScreenListener
import org.wordpress.android.ui.sitecreation.domains.NewSiteCreationDomainsFragment
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.misc.OnSkipClickedListener
import org.wordpress.android.ui.sitecreation.previews.NewSiteCreationPreviewFragment
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.CreateSiteState
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.CreateSiteState.SiteCreationCompleted
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.CreateSiteState.SiteNotCreated
import org.wordpress.android.ui.sitecreation.previews.NewSitePreviewViewModel.CreateSiteState.SiteNotInLocalDb
import org.wordpress.android.ui.sitecreation.previews.SitePreviewScreenListener
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsFragment
import org.wordpress.android.ui.sitecreation.segments.SegmentsScreenListener
import org.wordpress.android.ui.sitecreation.siteinfo.NewSiteCreationSiteInfoFragment
import org.wordpress.android.ui.sitecreation.siteinfo.SiteInfoScreenListener
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsFragment
import org.wordpress.android.ui.sitecreation.verticals.VerticalsScreenListener
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import javax.inject.Inject

class NewSiteCreationActivity : AppCompatActivity(),
        SegmentsScreenListener,
        VerticalsScreenListener,
        DomainsScreenListener,
        SiteInfoScreenListener,
        SitePreviewScreenListener,
        OnSkipClickedListener,
        OnHelpClickedListener,
        BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers
    private lateinit var mainViewModel: NewSiteCreationMainVM

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.new_site_creation_activity)
        mainViewModel = ViewModelProviders.of(this, viewModelFactory).get(NewSiteCreationMainVM::class.java)
        mainViewModel.start(savedInstanceState)

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
                val (siteCreated, localSiteId) = when (createSiteState) {
                    // site creation flow was canceled
                    is SiteNotCreated -> Pair(false, null)
                    is SiteNotInLocalDb -> {
                        // Site was created, but we haven't been able to fetch it, let `SitePickerActivity` handle
                        // this with a Snackbar message.
                        intent.putExtra(SitePickerActivity.KEY_SITE_CREATED_BUT_NOT_FETCHED, true)
                        Pair(true, null)
                    }
                    is SiteCreationCompleted -> Pair(true, createSiteState.localSiteId)
                }
                intent.putExtra(SitePickerActivity.KEY_LOCAL_ID, localSiteId)
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
            super.onBackPressed()
        })
    }

    override fun onSegmentSelected(segmentId: Long) {
        mainViewModel.onSegmentSelected(segmentId)
    }

    override fun onVerticalSelected(verticalId: String) {
        mainViewModel.onVerticalsScreenFinished(verticalId)
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

    override fun onSkipClicked() {
        mainViewModel.onSkipClicked()
    }

    override fun onHelpClicked(origin: Origin) {
        ActivityLauncher.viewHelpAndSupport(this, origin, null, null)
    }

    override fun onSiteInfoFinished(siteTitle: String, tagLine: String?) {
        mainViewModel.onInfoScreenFinished(siteTitle, tagLine)
    }

    private fun showStep(target: WizardNavigationTarget<SiteCreationStep, SiteCreationState>) {
        val screenTitle = getScreenTitle(target.wizardStep)
        val fragment = when (target.wizardStep) {
            SEGMENTS -> NewSiteCreationSegmentsFragment.newInstance(screenTitle)
            VERTICALS ->
                NewSiteCreationVerticalsFragment.newInstance(
                        screenTitle,
                        target.wizardState.segmentId!!
                )
            DOMAINS -> NewSiteCreationDomainsFragment.newInstance(screenTitle, target.wizardState.siteTitle)
            SITE_INFO -> NewSiteCreationSiteInfoFragment.newInstance(screenTitle)
            SITE_PREVIEW -> NewSiteCreationPreviewFragment.newInstance(screenTitle, target.wizardState)
        }
        slideInFragment(fragment, target.wizardStep.toString())
    }

    private fun getScreenTitle(step: SiteCreationStep): String {
        val screenTitleData = mainViewModel.screenTitleForWizardStep(step)
        return when (screenTitleData) {
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
}
