package org.wordpress.android.ui.sitecreation

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.sitecreation.SiteCreationStep.DOMAINS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.SEGMENTS
import org.wordpress.android.ui.sitecreation.SiteCreationStep.VERTICALS
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsFragment
import org.wordpress.android.ui.sitecreation.segments.SegmentsScreenListener
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsFragment
import org.wordpress.android.ui.sitecreation.verticals.VerticalsScreenListener
import org.wordpress.android.util.wizard.WizardNavigationTarget
import javax.inject.Inject

class NewSiteCreationActivity : AppCompatActivity(),
        SegmentsScreenListener,
        VerticalsScreenListener,
        OnSkipClickedListener {
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
    }

    override fun onSegmentSelected(segmentId: Long) {
        mainViewModel.onSegmentSelected(segmentId)
    }

    override fun onVerticalSelected(verticalId: String) {
        mainViewModel.onVerticalsScreenFinished(verticalId)
    }

    override fun onSkipClicked() {
        mainViewModel.onSkipClicked()
    }

    private fun showStep(target: WizardNavigationTarget<SiteCreationStep, SiteCreationState>) {
        val fragment = when (target.wizardStep) {
            SEGMENTS -> NewSiteCreationSegmentsFragment.newInstance()
            VERTICALS ->
                NewSiteCreationVerticalsFragment.newInstance(target.wizardState.segmentId!!)
            DOMAINS -> NewSiteCreationDomainFragment.newInstance("Test title")
        }
        slideInFragment(fragment, target.wizardStep.toString())
    }

    private fun slideInFragment(fragment: Fragment?, tag: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
                R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
        )
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag)
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) != null) {
            fragmentTransaction.addToBackStack(null)
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
        mainViewModel.onBackPressed()
        super.onBackPressed()
    }
}
