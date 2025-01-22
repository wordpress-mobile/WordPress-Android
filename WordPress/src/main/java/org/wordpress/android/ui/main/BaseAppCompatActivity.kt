package org.wordpress.android.ui.main

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

/**
 * Base class for all activities - initially created to support Android 15's edge-to-edge, but can be extended
 * in the future to handle other situations
 */
open class BaseAppCompatActivity : AppCompatActivity() {
    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)) {
            applyInsetOffsets()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun applyInsetOffsets() {
        val activityName = this.localClassName.substringAfterLast(".")
        val excludedActivity = excludedActivities.find { it.activityName == activityName }
        val applyTopOffset = excludedActivity?.applyTopOffset ?: true
        val applyBottomOffset = excludedActivity?.applyBottomOffset ?: true

        if (applyTopOffset || applyBottomOffset) {
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                // base the top offset on the system bar inset
                val topOffset = if (applyTopOffset) {
                    insets.getInsets(WindowInsets.Type.statusBars()).top
                } else {
                    0
                }

                // base the bottom offset on the navigation bar inset, but use a zero offset for the main activity
                // to accommodate the main BottomNavigationView
                val bottomOffset = if (applyBottomOffset) {
                    insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                } else {
                    0
                }

                // Adjust system bars padding to avoid overlap
                view.setPadding(
                    0,
                    topOffset,
                    0,
                    bottomOffset
                )

                insets
            }
        }
    }

    private class ExcludedActivity(
        var activityName: String,
        var applyTopOffset: Boolean,
        var applyBottomOffset: Boolean,
    )

    /**
     * Activities that are excluded from the edge-to-edge top offset, bottom offset, or both - activities not listed
     * here will have both offsets applied
     */
    private val excludedActivities = hashSetOf(
        // apply neither top nor bottom offset
        ExcludedActivity("BloggingPromptsListActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("DebugSharedPreferenceFlagsActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("DesignSystemActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("DomainManagementActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("EditJetpackSocialShareMessageActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("ExperimentalFeaturesActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("JetpackStaticPosterActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("JetpackFullPluginInstallActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("JetpackRemoteInstallActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("FeedbackFormActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("MediaPreviewActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("MenuActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("NewDomainSearchActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("PersonalizationActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("PurchaseDomainActivity", applyTopOffset = false, applyBottomOffset = false),
        ExcludedActivity("SelfHostedUsersActivity", applyTopOffset = false, applyBottomOffset = false),
        // apply bottom offset only
        ExcludedActivity("MediaSettingsActivity", applyTopOffset = false, applyBottomOffset = true),
        ExcludedActivity("PagesActivity", applyTopOffset = false, applyBottomOffset = true),
        ExcludedActivity("PostsListActivity", applyTopOffset = false, applyBottomOffset = true),
        ExcludedActivity("StatsActivity", applyTopOffset = false, applyBottomOffset = true),
        // apply top offset only
        ExcludedActivity("WPMainActivity", applyTopOffset = true, applyBottomOffset = false),
    )
}
