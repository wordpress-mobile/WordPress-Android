package org.wordpress.android.widgets

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.review.ReviewManagerFactory
import org.wordpress.android.R
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
    private const val KEY_INSTALL_DATE = "rate_install_date"
    private const val KEY_LAUNCH_TIMES = "rate_launch_times"
    private const val KEY_OPT_OUT = "rate_opt_out"
    private const val KEY_ASK_LATER_DATE = "rate_ask_later_date"
    private const val KEY_INTERACTIONS = "rate_interactions"
    private const val IN_APP_REVIEWS_SHOWN_DATE = "in_app_reviews_shown_date"
    private const val DO_NOT_SHOW_IN_APP_REVIEWS_PROMPT = "do_not_show_in_app_reviews_prompt"
    private const val TARGET_COUNT_POST_PUBLISHED = 2
    private const val TARGET_COUNT_NOTIFICATIONS = 10

    // app must have been installed this long before the rating dialog will appear
    private const val CRITERIA_INSTALL_DAYS: Int = 7
    private val criteriaInstallMs = TimeUnit.DAYS.toMillis(CRITERIA_INSTALL_DAYS.toLong())

    // app must have been launched this many times before the rating dialog will appear
    private const val CRITERIA_LAUNCH_TIMES: Int = 10

    // user must have performed this many interactions before the rating dialog will appear
    private const val CRITERIA_INTERACTIONS: Int = 10

    private var installDate = Date()
    private var askLaterDate = Date()
    private var launchTimes = 0
    private var interactions = 0
    private var optOut = false
    private var inAppReviewsShownDate = Date(0)
    private var doNotShowInAppReviewsPrompt = false

    private lateinit var preferences: SharedPreferences

    /**
     * Call this when the launcher activity is launched.
     */
    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()

        // If it is the first launch, save the date in shared preference.
        if (preferences.getLong(KEY_INSTALL_DATE, 0) == 0L) {
            storeInstallDate(context)
        }

        // Increment launch times
        launchTimes = preferences.getInt(KEY_LAUNCH_TIMES, 0)
        launchTimes++
        editor.putInt(KEY_LAUNCH_TIMES, launchTimes)
        editor.apply()

        interactions = preferences.getInt(KEY_INTERACTIONS, 0)
        optOut = preferences.getBoolean(KEY_OPT_OUT, false)
        installDate = Date(preferences.getLong(KEY_INSTALL_DATE, 0))
        askLaterDate = Date(preferences.getLong(KEY_ASK_LATER_DATE, 0))

        inAppReviewsShownDate = Date(preferences.getLong(IN_APP_REVIEWS_SHOWN_DATE, 0))
        doNotShowInAppReviewsPrompt = preferences.getBoolean(DO_NOT_SHOW_IN_APP_REVIEWS_PROMPT, false)
    }

    fun launchInAppReviews(activity: Activity) {
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
     * Show the rate dialog if the criteria is satisfied.
     * @return true if shown, false otherwise.
     */
    fun showRateDialogIfNeeded(fragmentManger: FragmentManager): Boolean {
        return if (shouldShowRateDialog()) {
            showRateDialog(fragmentManger)
            true
        } else {
            false
        }
    }

    /**
     * Called from various places in the app where the user has performed a non-trivial action, such as publishing post
     * or page. We use this to avoid showing the rating dialog to uninvolved users
     */
    fun incrementInteractions(incrementInteractionTracker: AnalyticsTracker.Stat) {
        if (!optOut) {
            interactions++
            preferences.edit().putInt(KEY_INTERACTIONS, interactions)?.apply()
            AnalyticsTracker.track(incrementInteractionTracker)
        }
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
    fun shouldShowInAppReviewsPrompt(): Boolean {
        val shouldWaitAfterLastShown = Date().time - inAppReviewsShownDate.time < criteriaInstallMs
        val shouldWaitAfterAskLaterTapped = Date().time - askLaterDate.time < criteriaInstallMs
        val publishedPostsGoal = AppPrefs.getPublishedPostCount() == TARGET_COUNT_POST_PUBLISHED
        val notificationsGoal = AppPrefs.getInAppReviewsNotificationCount() == TARGET_COUNT_NOTIFICATIONS
        return !doNotShowInAppReviewsPrompt && !shouldWaitAfterAskLaterTapped && !shouldWaitAfterLastShown &&
            (publishedPostsGoal || notificationsGoal)
    }

    /**
     * Check whether the rate dialog should be shown or not.
     * @return true if the dialog should be shown
     */
    private fun shouldShowRateDialog(): Boolean {
        return if (optOut or (launchTimes < CRITERIA_LAUNCH_TIMES) or (interactions < CRITERIA_INTERACTIONS)) {
            false
        } else {
            Date().time - installDate.time >= criteriaInstallMs && Date().time - askLaterDate.time >= criteriaInstallMs
        }
    }

    private fun showRateDialog(fragmentManger: FragmentManager) {
        var dialog = fragmentManger.findFragmentByTag(AppRatingDialog.TAG_APP_RATING_PROMPT_DIALOG)
        if (dialog == null) {
            dialog = AppRatingDialog()
            dialog.show(fragmentManger, AppRatingDialog.TAG_APP_RATING_PROMPT_DIALOG)
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_REVIEWS_SAW_PROMPT)

            resetInAppReviewsCounters()
        }
    }

    class AppRatingDialog : DialogFragment() {
        companion object {
            internal const val TAG_APP_RATING_PROMPT_DIALOG = "TAG_APP_RATING_PROMPT_DIALOG"
        }

        @Suppress("SwallowedException")
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            val appName = getString(R.string.app_name)
            val title = getString(R.string.app_rating_title, appName)
            builder.setTitle(title)
                .setMessage(R.string.app_rating_message)
                .setCancelable(true)
                .setPositiveButton(R.string.app_rating_rate_now) { _, _ ->
                    val appPackage = requireActivity().packageName
                    val url = "market://details?id=$appPackage"
                    try {
                        requireActivity().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: ActivityNotFoundException) {
                        // play store app isn't on this device so open app's page in browser instead
                        requireActivity().startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    "http://play.google.com/store/apps/details?id=" +
                                        requireActivity().packageName
                                )
                            )
                        )
                    }

                    setOptOut()
                    AnalyticsTracker.track(AnalyticsTracker.Stat.APP_REVIEWS_RATED_APP)

                    // Reset the published post counter of in-app reviews prompt flow.
                    AppPrefs.resetPublishedPostCount()
                }
                .setNeutralButton(R.string.app_rating_rate_later) { _, _ ->
                    clearSharedPreferences()
                    storeAskLaterDate()
                    AnalyticsTracker.track(AnalyticsTracker.Stat.APP_REVIEWS_DECIDED_TO_RATE_LATER)
                }
                .setNegativeButton(R.string.app_rating_rate_never) { _, _ ->
                    setOptOut()
                    AnalyticsTracker.track(AnalyticsTracker.Stat.APP_REVIEWS_DECLINED_TO_RATE_APP)

                    doNotShowInAppReviewsPromptAgain()
                }
            return builder.create()
        }

        override fun onCancel(dialog: DialogInterface) {
            super.onCancel(dialog)
            clearSharedPreferences()
            storeAskLaterDate()
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_REVIEWS_CANCELLED_PROMPT)
        }
    }

    /**
     * Clear data other than opt-out in shared preferences - called when the "Later" is pressed or dialog is canceled.
     */
    private fun clearSharedPreferences() {
        preferences.edit().remove(KEY_INSTALL_DATE)?.remove(KEY_LAUNCH_TIMES)?.remove(KEY_INTERACTIONS)?.apply()
    }

    /**
     * Set opt out flag - the rate dialog will never be shown unless app data is cleared.
     */
    private fun setOptOut() {
        preferences.edit().putBoolean(KEY_OPT_OUT, optOut)?.apply()
        this.optOut = true
    }

    /**
     * Set do not show in-app reviews prompt flag - the in-app reviews prompt will never be shown unless app data is
     * cleared.
     */
    private fun doNotShowInAppReviewsPromptAgain() =
        preferences.edit().putBoolean(DO_NOT_SHOW_IN_APP_REVIEWS_PROMPT, optOut)?.apply()

    /**
     * Store install date - retrieved from package manager if possible.
     */
    private fun storeInstallDate(context: Context) {
        var installDate = Date()
        val packMan = context.packageManager
        try {
            val pkgInfo = packMan.getPackageInfo(context.packageName, 0)
            installDate = Date(pkgInfo.firstInstallTime)
        } catch (e: PackageManager.NameNotFoundException) {
            AppLog.e(T.UTILS, e)
        }
        preferences.edit().putLong(KEY_INSTALL_DATE, installDate.time)?.apply()
    }

    /**
     * Store the date the user asked for being asked again later.
     */
    private fun storeAskLaterDate() {
        val nextAskDate = System.currentTimeMillis()
        preferences.edit().putLong(KEY_ASK_LATER_DATE, nextAskDate)?.apply()
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
}
