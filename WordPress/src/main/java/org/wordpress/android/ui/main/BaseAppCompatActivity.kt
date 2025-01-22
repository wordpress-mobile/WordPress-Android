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
        val excludedActivity = excludedActivities[activityName]
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

                // base the bottom offset on the navigation bar inset
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

    private class ActivityOffsets(
        var applyTopOffset: Boolean,
        var applyBottomOffset: Boolean,
    )

    /**
     * Activities that are excluded from the edge-to-edge top offset, bottom offset, or both. Activities not listed
     * here will have both offsets applied.
     */
    private val excludedActivities: HashMap<String, ActivityOffsets> = hashMapOf(
        // apply neither top nor bottom offset
        "BloggingPromptsListActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "DebugSharedPreferenceFlagsActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "DesignSystemActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "DomainManagementActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "EditJetpackSocialShareMessageActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "ExperimentalFeaturesActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "JetpackStaticPosterActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "JetpackFullPluginInstallActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "JetpackRemoteInstallActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "FeedbackFormActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "MediaPreviewActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "MenuActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "NewDomainSearchActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "PersonalizationActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "PurchaseDomainActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        "SelfHostedUsersActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = false),
        // apply bottom offset only
        "MediaSettingsActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = true),
        "PagesActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = true),
        "PostsListActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = true),
        "StatsActivity" to ActivityOffsets(applyTopOffset = false, applyBottomOffset = true),
        // apply top offset only
        "WPMainActivity" to ActivityOffsets(applyTopOffset = true, applyBottomOffset = false),
    )
}
