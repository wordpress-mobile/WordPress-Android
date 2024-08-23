package org.wordpress.android.widgets

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.android.play.core.review.ReviewManagerFactory
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.models.Note
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.extensions.logException
import java.util.Date
import java.util.concurrent.TimeUnit

object AppReviewManager {
    private const val PREF_NAME = "rate_wpandroid"

    private const val KEY_RATING_LAUNCH_TIMES = "rate_launch_times"
    private const val KEY_RATING_INTERACTIONS = "rate_interactions"

    private const val IN_APP_REVIEWS_SHOWN_DATE = "in_app_reviews_shown_date"

    private const val TARGET_COUNT_POST_PUBLISHED = 2
    private const val TARGET_COUNT_NOTIFICATIONS = 10

    // app must have been installed this long before the rating dialog will appear
    private const val CRITERIA_INSTALL_DAYS: Int = 7
    private val criteriaInstallMs = TimeUnit.DAYS.toMillis(CRITERIA_INSTALL_DAYS.toLong())

    private var launchTimes = 0
    private var inAppReviewsShownDate = Date(0)

    private lateinit var preferences: SharedPreferences

    /**
     * Call this when the launcher activity is launched.
     */
    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Increment launch times - note that like interactions, this was used by our
        // previous rating dialog but is no longer used. It is here in case we want
        // to include launch times in the future.
        launchTimes = preferences.getInt(KEY_RATING_LAUNCH_TIMES, 0)
        launchTimes++
        preferences.edit().apply {
            this.putInt(KEY_RATING_LAUNCH_TIMES, launchTimes)
            this.apply()
        }

        inAppReviewsShownDate = Date(preferences.getLong(IN_APP_REVIEWS_SHOWN_DATE, 0))
    }

    /**
     * Called when a post is published. We use this to determine which users will see the in-app review prompt.
     */
    fun onPostPublished() {
        if (shouldShowInAppReviewsPrompt()) return
        if (AppPrefs.getPublishedPostCount() < TARGET_COUNT_POST_PUBLISHED) {
            AppPrefs.incrementPublishedPostCount()
            AppLog.d(T.UTILS, "In-app reviews counter for published posts: ${AppPrefs.getPublishedPostCount()}")
        }
    }

    /**
     * Called when a notification is received. We use this to determine which users will see the in-app review prompt.
     */
    fun onNotificationReceived(note: Note) {
        if (shouldShowInAppReviewsPrompt()) return
        val shouldTrack = note.isUnread && (note.isLikeType || note.isCommentType || note.isFollowType)
        if (shouldTrack && AppPrefs.getInAppReviewsNotificationCount() < TARGET_COUNT_NOTIFICATIONS) {
            AppPrefs.incrementInAppReviewsNotificationCount()
            AppLog.d(T.UTILS, "In-app reviews counter for notification: ${AppPrefs.getInAppReviewsNotificationCount()}")
        }
    }

    /**
     * Check whether the in-app reviews prompt should be shown or not.
     * @return true if the prompt should be shown
     */
    private fun shouldShowInAppReviewsPrompt(): Boolean {
        val shouldWaitAfterLastShown = Date().time - inAppReviewsShownDate.time < criteriaInstallMs
        if (shouldWaitAfterLastShown) {
            return false
        }

        val publishedPostsGoal = AppPrefs.getPublishedPostCount() == TARGET_COUNT_POST_PUBLISHED
        val notificationsGoal = AppPrefs.getInAppReviewsNotificationCount() == TARGET_COUNT_NOTIFICATIONS
        return publishedPostsGoal || notificationsGoal
    }

    /**
     * Show the in-app reviews prompt if the necessary criteria are met
     */
    fun showInAppReviewsPromptIfNecessary(activity: Activity) {
        if (shouldShowInAppReviewsPrompt()) {
            launchInAppReviews(activity)
        }
    }

    private fun launchInAppReviews(activity: Activity) {
        AppLog.d(T.UTILS, "Launching in-app reviews prompt")
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnFailureListener { e ->
                    AppLog.e(T.UTILS, "Error launching google review API flow.", e)
                }
            } else {
                task.logException()
            }
        }

        resetInAppReviewsCounters()
    }

    /**
     * Store the date the in-app reviews prompt is attempted to launch.
     */
    private fun storeInAppReviewsShownDate() {
        inAppReviewsShownDate = Date(System.currentTimeMillis())
        preferences.edit().putLong(IN_APP_REVIEWS_SHOWN_DATE, inAppReviewsShownDate.time)?.apply()
    }

    private fun resetInAppReviewsCounters() {
        storeInAppReviewsShownDate()
        AppPrefs.resetPublishedPostCount()
        AppPrefs.resetInAppReviewsNotificationCount()
    }

    /**
     * Called from various places in the app where the user has performed a non-trivial action,
     * such as publishing a post or page. This was previously used to determine when to show our
     * custom rating dialog to involved users but is currently unused. It is left intact, and
     * interactions will continue to be counted, in case we want to include interactions in the
     * future when determining whether to show the Google review dialog.
     */
    fun incrementInteractions(incrementInteractionTracker: AnalyticsTracker.Stat) {
        var interactions = preferences.getInt(KEY_RATING_INTERACTIONS, 0)
        interactions++
        preferences.edit().putInt(KEY_RATING_INTERACTIONS, interactions)?.apply()
        AnalyticsTracker.track(incrementInteractionTracker)
    }
}
