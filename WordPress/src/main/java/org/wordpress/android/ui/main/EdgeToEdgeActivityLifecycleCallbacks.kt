package org.wordpress.android.ui.main

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Applies edge-to-edge inset padding to third-party activities (e.g. Zendesk)
 * that don't extend [BaseAppCompatActivity] and therefore don't get
 * inset handling from [BaseAppCompatActivity.applyInsetOffsets].
 */
class EdgeToEdgeActivityLifecycleCallbacks :
    Application.ActivityLifecycleCallbacks {
    // Zendesk Support SDK 5.5.3 activity class names
    private val targetActivities = setOf(
        "zendesk.support.guide.HelpCenterActivity",
        "zendesk.support.guide.ViewArticleActivity",
        "zendesk.support.request.RequestActivity",
        "zendesk.support.requestlist.RequestListActivity"
    )

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
        if (activity::class.java.name in targetActivities) {
            applyInsetOffsets(activity)
        }
    }

    private fun applyInsetOffsets(activity: Activity) {
        ViewCompat.setOnApplyWindowInsetsListener(
            activity.window.decorView
        ) { view, insets ->
            val padding = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
                    or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(
                padding.left,
                padding.top,
                padding.right,
                padding.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
