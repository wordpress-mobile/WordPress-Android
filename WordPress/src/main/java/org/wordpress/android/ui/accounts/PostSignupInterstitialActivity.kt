package org.wordpress.android.ui.accounts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.post_signup_interstitial_default.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_SIGNUP_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_SIGNUP_INTERSTITIAL_CREATE_NEW_SITE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_SIGNUP_INTERSTITIAL_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.POST_SIGNUP_INTERSTITIAL_SHOWN
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class PostSignupInterstitialActivity : AppCompatActivity() {
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        setContentView(R.layout.post_signup_interstitial_activity)

        trackInterstitialShown()

        create_new_site_button.setOnClickListener { startSiteCreationFlow() }
        add_self_hosted_site_button.setOnClickListener { startSiteConnectionFlow() }
        dismiss_button.setOnClickListener { dismiss() }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        dismiss()
    }

    private fun trackInterstitialShown() {
        analyticsTrackerWrapper.track(POST_SIGNUP_INTERSTITIAL_SHOWN)
        appPrefsWrapper.shouldShowPostSignupInterstitial = false
    }

    private fun startSiteCreationFlow() {
        analyticsTrackerWrapper.track(POST_SIGNUP_INTERSTITIAL_CREATE_NEW_SITE_TAPPED)

        ActivityLauncher.newBlogForResult(this)
        finish()
    }

    private fun startSiteConnectionFlow() {
        analyticsTrackerWrapper.track(POST_SIGNUP_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED)

        ActivityLauncher.addSelfHostedSiteForResult(this)
        finish()
    }

    private fun dismiss() {
        analyticsTrackerWrapper.track(POST_SIGNUP_INTERSTITIAL_DISMISSED)

        ActivityLauncher.viewReader(this)
        finish()
    }
}
