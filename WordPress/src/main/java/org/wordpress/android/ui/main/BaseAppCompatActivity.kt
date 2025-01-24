package org.wordpress.android.ui.main

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import org.wordpress.android.designsystem.DesignSystemActivity
import org.wordpress.android.ui.blaze.blazecampaigns.BlazeCampaignParentActivity
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListActivity
import org.wordpress.android.ui.debug.preferences.DebugSharedPreferenceFlagsActivity
import org.wordpress.android.ui.domains.management.DomainManagementActivity
import org.wordpress.android.ui.domains.management.newdomainsearch.NewDomainSearchActivity
import org.wordpress.android.ui.domains.management.purchasedomain.PurchaseDomainActivity
import org.wordpress.android.ui.jetpackoverlay.JetpackStaticPosterActivity
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.install.JetpackFullPluginInstallActivity
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallActivity
import org.wordpress.android.ui.main.feedbackform.FeedbackFormActivity
import org.wordpress.android.ui.media.MediaPreviewActivity
import org.wordpress.android.ui.media.MediaSettingsActivity
import org.wordpress.android.ui.mysite.menu.MenuActivity
import org.wordpress.android.ui.mysite.personalization.PersonalizationActivity
import org.wordpress.android.ui.pages.PagesActivity
import org.wordpress.android.ui.posts.PostsListActivity
import org.wordpress.android.ui.posts.sharemessage.EditJetpackSocialShareMessageActivity
import org.wordpress.android.ui.prefs.ExperimentalFeaturesActivity
import org.wordpress.android.ui.selfhostedusers.SelfHostedUsersActivity
import org.wordpress.android.ui.sitemonitor.SiteMonitorParentActivity
import org.wordpress.android.ui.stats.refresh.StatsActivity

/**
 * Base class for all activities - initially created to handle insets for Android 15's edge-to-edge support,
 * but can be extended in the future to handle other situations
 */
open class BaseAppCompatActivity : AppCompatActivity() {
    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // When both compileSdkVersion and targetSdkVersion are 35+, the OS defaults to
        // using edge-to-edge. We need to adjust for this by applying insets as needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            applyInsetOffsets()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun applyInsetOffsets() {
        // val activityName = this.localClassName.substringAfterLast(".")
        val excludedActivity = excludedActivities[this.localClassName]
        val applyTopOffset = excludedActivity?.applyTopOffset ?: true
        val applyBottomOffset = excludedActivity?.applyBottomOffset ?: true

        if (applyTopOffset || applyBottomOffset) {
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                // Notice we're using systemBars rather than statusBar and accounting for the display cutouts
                val innerPadding = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout()
                )

                // Adjust system bars padding to avoid overlap
                view.setPadding(
                    innerPadding.left,
                    if (applyTopOffset) innerPadding.top else 0,
                    innerPadding.right,
                    if (applyBottomOffset) innerPadding.bottom else 0
                )

                insets
            }
        }
    }

    private class ActivityOffsets(
        var applyTopOffset: Boolean,
        var applyBottomOffset: Boolean,
    )

    /**
     * Activities that are excluded from the edge-to-edge top offset, bottom offset, or both. Activities not listed
     * here will have both offsets applied. Note that many of these excluded activities are Compose-based because
     * Compose automatically adjusts for edge-to-edge insets. We may want to revisit this approach as we add more
     * Compose-based activities to the project.
     */
    private val excludedActivities: HashMap<String, ActivityOffsets> = hashMapOf(
        // apply neither top nor bottom offset
        BlazeCampaignParentActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        BloggingPromptsListActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        DebugSharedPreferenceFlagsActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        DesignSystemActivity::class.java.name to ActivityOffsets
            (applyTopOffset = false,
            applyBottomOffset = false
        ),
        DomainManagementActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        EditJetpackSocialShareMessageActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        ExperimentalFeaturesActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        FeedbackFormActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        JetpackFullPluginInstallActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        JetpackRemoteInstallActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        JetpackStaticPosterActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        MediaPreviewActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        MenuActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        NewDomainSearchActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        PersonalizationActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        PurchaseDomainActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        SelfHostedUsersActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),
        SiteMonitorParentActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = false
        ),

        // apply bottom offset only
        MediaSettingsActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = true
        ),
        PagesActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = true
        ),
        PostsListActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = true
        ),
        StatsActivity::class.java.name to ActivityOffsets(
            applyTopOffset = false,
            applyBottomOffset = true
        ),

        // apply top offset only
        WPMainActivity::class.java.name to ActivityOffsets(
            applyTopOffset = true,
            applyBottomOffset = false
        ),
    )
}
