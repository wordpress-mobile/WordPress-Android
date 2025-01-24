package org.wordpress.android.ui.main

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat

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
        val activityName = this.localClassName.substringAfterLast(".")
        val excludedActivity = excludedActivities[activityName]
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
     * Compose automatically adjusts for edge-to-edge insets. We may want to revisit this approach as more
     * Compose-based activities are added to the project.
     */
    private val excludedActivities: HashMap<String, ActivityOffsets> = hashMapOf(
        // apply neither top nor bottom offset
        "BlazeCampaignParentActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "BloggingPromptsListActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "DebugSharedPreferenceFlagsActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "DesignSystemActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "DomainManagementActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "EditJetpackSocialShareMessageActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "ExperimentalFeaturesActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "FeedbackFormActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "JetpackFullPluginInstallActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "JetpackRemoteInstallActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "JetpackStaticPosterActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "MediaPreviewActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "MenuActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "NewDomainSearchActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "PersonalizationActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "PurchaseDomainActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "SelfHostedUsersActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "SiteMonitorParentActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),

        // apply bottom offset only
        "MediaSettingsActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = true),
        "PagesActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = true),
        "PostsListActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = true),
        "StatsActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = true),

        // apply top offset only
        "WPMainActivity" to ActivityOffsets(applyTopOffset = true, applyBottomOffset = false),
    )
}
