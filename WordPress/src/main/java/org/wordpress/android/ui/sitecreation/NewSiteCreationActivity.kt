package org.wordpress.android.ui.sitecreation

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.accounts.HelpActivity.Origin
import org.wordpress.android.ui.main.SitePickerActivity
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleEmpty
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.ui.sitecreation.SiteCreationStep.DOMAINS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SEGMENTS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_INFO
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SITE_PREVIEW
import org.wordpress.android.ui.sitecreation.SiteCreationStep.VERTICALS
import org.wordpress.android.ui.sitecreation.creation.SitePreviewScreenListener
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsFragment
import org.wordpress.android.ui.sitecreation.segments.SegmentsScreenListener
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsFragment
import org.wordpress.android.ui.sitecreation.verticals.VerticalsScreenListener
import org.wordpress.android.util.wizard.WizardNavigationTarget
import javax.inject.Inject

class NewSiteCreationActivity : AppCompatActivity(),
        SegmentsScreenListener,
        VerticalsScreenListener,
        SiteInfoScreenListener,
        SitePreviewScreenListener,
        OnSkipClickedListener,
        OnHelpClickedListener {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var mainViewModel: NewSiteCreationMainVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        setContentView(R.layout.new_site_creation_activity)
        mainViewModel = ViewModelProviders.of(this, viewModelFactory).get(NewSiteCreationMainVM::class.java)
        mainViewModel.start()

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_ACCESSED)
        }
        observeVMState()
    }

    private fun observeVMState() {
        mainViewModel.navigationTargetObservable
                .observe(this, Observer { target -> target?.let { showStep(target) } })
        mainViewModel.wizardFinishedObservable.observe(this, Observer { newSiteLocalId ->
            val intent = Intent()
            intent.putExtra(SitePickerActivity.KEY_LOCAL_ID, newSiteLocalId)
            setResult(Activity.RESULT_OK, intent)
            finish()
        })
    }

    override fun onSegmentSelected(segmentId: Long) {
        mainViewModel.onSegmentSelected(segmentId)
    }

    override fun onVerticalSelected(verticalId: String) {
        mainViewModel.onVerticalsScreenFinished(verticalId)
    }

    override fun onSitePreviewScreenDismissed(newSiteLocalId: Int?) {
        mainViewModel.onSitePreviewScreenFinished(newSiteLocalId)
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
            DOMAINS -> NewSiteCreationDomainFragment.newInstance(screenTitle, "Test site")
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

    private fun slideInFragment(fragment: Fragment?, tag: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag)
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) != null) {
            fragmentTransaction.addToBackStack(null)
        } else {
            fragmentTransaction.setCustomAnimations(
                    R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                    R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
            )
        }
        fragmentTransaction.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }

    override fun onBackPressed() {
        if (!mainViewModel.shouldSuppressBackPress()) {
            mainViewModel.onBackPressed()
            super.onBackPressed()
        }
    }
}
