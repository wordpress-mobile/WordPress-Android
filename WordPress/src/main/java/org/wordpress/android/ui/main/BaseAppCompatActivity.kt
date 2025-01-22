package org.wordpress.android.ui.main

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

/**
 * Base class for all activities - initially created to support Android 15's edge-to-edge, but can be extended
 * in the future to handle other cases
 */
open class BaseAppCompatActivity : AppCompatActivity() {
    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)) {
            adjustContentForEdgeToEdge()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun adjustContentForEdgeToEdge() {
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            // do nothing if neither offset is being set
            if (shouldAdjustTopAndBottomOffsetForEdgeToEdge().not()) {
                return@setOnApplyWindowInsetsListener insets
            }

            // base the top offset on the system bar inset
            val topOffset = if (shouldAdjustTopOffsetForEdgeToEdge()) {
                insets.getInsets(WindowInsets.Type.statusBars()).top
            } else {
                0
            }

            // base the bottom offset on the navigation bar inset, but use a zero offset for the main activity
            // to accommodate the main BottomNavigationView
            val bottomOffset = if (shouldAdjustBottomOffsetForEdgeToEdge()) {
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

    private fun getActivityName() = this.localClassName.substringAfterLast(".")

    private fun shouldAdjustTopAndBottomOffsetForEdgeToEdge(): Boolean {
        val activityName = getActivityName()
        if (EXCLUDE_FROM_TOP_AND_BOTTOM_OFFSET_ACTIVITY_NAMES.contains(activityName)) {
            return false
        }
        return true
    }

    private fun shouldAdjustTopOffsetForEdgeToEdge(): Boolean {
        val activityName = getActivityName()
        if (EXCLUDE_FROM_TOP_AND_BOTTOM_OFFSET_ACTIVITY_NAMES.contains(activityName) or
            EXCLUDE_FROM_TOP_OFFSET_ACTIVITY_NAMES.contains(activityName)
        ) {
            return false
        }
        return true
    }

    private fun shouldAdjustBottomOffsetForEdgeToEdge(): Boolean {
        val activityName = getActivityName()
        if (EXCLUDE_FROM_TOP_AND_BOTTOM_OFFSET_ACTIVITY_NAMES.contains(activityName) or
            EXCLUDE_FROM_BOTTOM_OFFSET_ACTIVITY_NAMES.contains(activityName)
        ) {
            return false
        }
        return true
    }

    companion object {
        /**
         * List of activities to exclude from setting the top and bottom offset
         */
        private val EXCLUDE_FROM_TOP_AND_BOTTOM_OFFSET_ACTIVITY_NAMES = listOf(
            "BloggingPromptsListActivity",
            "DebugSharedPreferenceFlagsActivity",
            "DesignSystemActivity",
            "DomainManagementActivity",
            "EditJetpackSocialShareMessageActivity",
            "ExperimentalFeaturesActivity",
            "JetpackStaticPosterActivity",
            "JetpackFullPluginInstallActivity",
            "JetpackRemoteInstallActivity",
            "FeedbackFormActivity",
            "MediaPreviewActivity",
            "MenuActivity",
            "NewDomainSearchActivity",
            "PersonalizationActivity",
            "PurchaseDomainActivity",
            "SelfHostedUsersActivity",
        )

        /**
         * List of activities to exclude from setting just the top offset
         */
        private val EXCLUDE_FROM_TOP_OFFSET_ACTIVITY_NAMES = listOf(
            "PagesActivity",
            "PostsListActivity",
            "StatsActivity",
        )

        /**
         * List of activities to exclude from setting just the bottom offset
         */
        private val EXCLUDE_FROM_BOTTOM_OFFSET_ACTIVITY_NAMES = listOf(
            "WPMainActivity",
        )
    }
}
