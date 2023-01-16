package org.wordpress.android.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.util.LayoutDirection
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.layoutDirection
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.UNKNOWN
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.quickstart.QuickStartReminderReceiver
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.ui.themes.ThemeBrowserUtils
import org.wordpress.android.util.extensions.getColorFromAttribute
import java.util.Locale

object QuickStartUtils {
    private val themeBrowserUtils = ThemeBrowserUtils()
    private const val QUICK_START_REMINDER_INTERVAL = (24 * 60 * 60 * 1000 * 2).toLong() // two days
    const val ICON_NOT_SET = -1

    /**
     * Formats the string, to highlight text between %1$s and %2$s with specified color, and add an icon
     * in front of it if necessary
     *
     * @param context Context used to access resources
     * @param messageId resources id of the message to display. If string contains basic HTML tags inside
     * <![CDATA[ ]]>, they will be converted to Spans.
     * @param iconId resource if of the icon that goes before the highlighted area
     */
    @JvmStatic
    @JvmOverloads
    @Suppress("SwallowedException")
    fun stylizeQuickStartPrompt(
        activityContext: Context,
        messageId: Int,
        iconId: Int = ICON_NOT_SET
    ): Spannable {
        val spanTagOpen = activityContext.getString(R.string.quick_start_span_start)
        val spanTagEnd = activityContext.getString(R.string.quick_start_span_end)
        var formattedMessage = activityContext.getString(messageId, spanTagOpen, spanTagEnd)

        val startOfHighlight = formattedMessage.indexOf(spanTagOpen)

        // remove the <span> tag
        formattedMessage = formattedMessage.replaceFirst(spanTagOpen, "")
        /**
         * Some string resources contain whitespaces before and after the placeholder tags.
         * For example: `Tap %1$s Customize %2$s to start` becomes `Tap <span> Customize </span> to start`.
         * However, when we remove "<span>" the string ends up having two whitespaces.
         */
        formattedMessage = formattedMessage.replaceFirst("  ", " ")

        val endOfHighlight = formattedMessage.indexOf(spanTagEnd)

        // remove the </span> tag
        formattedMessage = formattedMessage.replaceFirst(spanTagEnd, "")
        formattedMessage = formattedMessage.replaceFirst("  ", " ")

        val mutableSpannedMessage = SpannableStringBuilder(
            HtmlCompat.fromHtml(formattedMessage, HtmlCompat.FROM_HTML_MODE_COMPACT)
        )
        // nothing to highlight
        if (startOfHighlight != -1 && endOfHighlight != -1) {
            val highlightColor = activityContext.getColorFromAttribute(R.attr.colorSurface)
            mutableSpannedMessage.setSpan(
                ForegroundColorSpan(highlightColor),
                startOfHighlight, endOfHighlight, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val icon: Drawable? = try {
                // .mutate() allows us to avoid sharing the state of drawables
                activityContext.getDrawable(iconId)?.mutate()
            } catch (e: Resources.NotFoundException) {
                null
            }

            if (icon != null) {
                val iconSize = activityContext.resources
                    .getDimensionPixelOffset(R.dimen.dialog_snackbar_max_icons_size)
                icon.setBounds(0, 0, iconSize, iconSize)

                DrawableCompat.setTint(icon, highlightColor)
                if (startOfHighlight > 0) {
                    mutableSpannedMessage.insert(startOfHighlight - 1, "  ")
                } else {
                    mutableSpannedMessage.insert(startOfHighlight, "  ")
                }

                mutableSpannedMessage.setSpan(
                    ImageSpan(icon), startOfHighlight, startOfHighlight + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return mutableSpannedMessage
    }

    /**
     * Adds animated quick start focus point targetedView to the top level parent,
     * and places it in the top-right corner of the specified targetedView.
     *
     * @param topLevelParent Parent where quick start focus targetedView will be added.
     * Usually Relative or Frame layout
     * @param targetedView View in top-right corner of which the quick start focus view will be placed. Child of
     * topLevelParent
     * @param rightOffset specifies in px how much should we move view to the left from the right
     * @param topOffset specifies in px how much should we move view to the bottom from the top
     */
    @JvmStatic
    fun addQuickStartFocusPointAboveTheView(
        topLevelParent: ViewGroup,
        targetedView: View,
        rightOffset: Int,
        topOffset: Int
    ) {
        topLevelParent.post {
            val quickStartFocusPointView = LayoutInflater.from(topLevelParent.context)
                .inflate(R.layout.quick_start_focus_point_view, topLevelParent, false)
            val focusPointSize =
                topLevelParent.context.resources.getDimensionPixelOffset(R.dimen.quick_start_focus_point_size)

            val topLevelParentViewLocation = IntArray(2)
            topLevelParent.getLocationOnScreen(topLevelParentViewLocation)

            val topLevelParentsHorizontalOffset = topLevelParentViewLocation[0]
            val topLevelParentsVerticalOffset = topLevelParentViewLocation[1]

            val focusPointTargetViewLocation = IntArray(2)
            targetedView.getLocationOnScreen(focusPointTargetViewLocation)

            val realFocusPointContainerX = if (Locale.getDefault().layoutDirection == LayoutDirection.RTL) {
                (topLevelParentsHorizontalOffset + topLevelParent.width) -
                        (focusPointTargetViewLocation[0] + targetedView.width)
            } else {
                focusPointTargetViewLocation[0] - topLevelParentsHorizontalOffset
            }
            val realFocusPointOffsetFromTheLeft = targetedView.width - focusPointSize - rightOffset

            val focusPointContainerY = focusPointTargetViewLocation[1] - topLevelParentsVerticalOffset

            val x = realFocusPointContainerX + realFocusPointOffsetFromTheLeft
            val y = focusPointContainerY + topOffset

            val params = quickStartFocusPointView.layoutParams as MarginLayoutParams
            params.marginStart = x
            params.topMargin = y
            topLevelParent.addView(quickStartFocusPointView)

            quickStartFocusPointView.post {
                quickStartFocusPointView.layoutParams = params
            }
        }
    }

    @JvmStatic
    fun removeQuickStartFocusPoint(topLevelParent: ViewGroup) {
        val focusPointView = topLevelParent.findViewById<View>(R.id.quick_start_focus_point)
        if (focusPointView != null) {
            val directParent = focusPointView.parent
            if (directParent is ViewGroup) {
                directParent.removeView(focusPointView)
            }
        }
    }

    @JvmStatic
    fun isQuickStartAvailableForTheSite(siteModel: SiteModel): Boolean {
        return (siteModel.hasCapabilityManageOptions &&
                themeBrowserUtils.isAccessible(siteModel) &&
                SiteUtils.isAccessedViaWPComRest(siteModel))
    }

    @Suppress("ComplexMethod", "UseCheckOrError")
    @JvmStatic
    fun getQuickStartListTappedTracker(task: QuickStartTask): Stat {
        return when (task.string) {
            QuickStartStore.QUICK_START_CREATE_SITE_LABEL -> Stat.QUICK_START_LIST_CREATE_SITE_TAPPED
            QuickStartStore.QUICK_START_UPDATE_SITE_TITLE_LABEL -> Stat.QUICK_START_LIST_CREATE_SITE_TAPPED
            QuickStartStore.QUICK_START_VIEW_SITE_LABEL -> Stat.QUICK_START_LIST_VIEW_SITE_TAPPED
            QuickStartStore.QUICK_START_ENABLE_POST_SHARING_LABEL -> Stat.QUICK_START_LIST_ADD_SOCIAL_TAPPED
            QuickStartStore.QUICK_START_PUBLISH_POST_LABEL -> Stat.QUICK_START_LIST_PUBLISH_POST_TAPPED
            QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL -> Stat.QUICK_START_LIST_FOLLOW_SITE_TAPPED
            QuickStartStore.QUICK_START_UPLOAD_SITE_ICON_LABEL -> Stat.QUICK_START_LIST_UPLOAD_ICON_TAPPED
            QuickStartStore.QUICK_START_CHECK_STATS_LABEL ->
                Stat.QUICK_START_LIST_CHECK_STATS_TAPPED
            QuickStartStore.QUICK_START_REVIEW_PAGES_LABEL -> Stat.QUICK_START_LIST_REVIEW_PAGES_TAPPED
            QuickStartStore.QUICK_START_CHECK_NOTIFIATIONS_LABEL -> Stat.QUICK_START_LIST_CHECK_NOTIFICATIONS_TAPPED
            QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL -> Stat.QUICK_START_LIST_UPLOAD_MEDIA_TAPPED
            else -> throw IllegalStateException("The task '$task' is not valid")
        }
    }

    @Suppress("ComplexMethod", "UseCheckOrError")
    @JvmStatic
    fun getQuickStartListSkippedTracker(task: QuickStartTask): Stat {
        return when (task.string) {
            // Skipping create site task should never happen as of Quick Start v2.  The task is automatically set as
            // completed when Quick Start v2 begins since it is initiated when a new site is created.  The task case
            // is included here for completeness.
            QuickStartStore.QUICK_START_CREATE_SITE_LABEL -> Stat.QUICK_START_LIST_CREATE_SITE_SKIPPED
            QuickStartStore.QUICK_START_UPDATE_SITE_TITLE_LABEL -> Stat.QUICK_START_LIST_CREATE_SITE_SKIPPED
            QuickStartStore.QUICK_START_VIEW_SITE_LABEL -> Stat.QUICK_START_LIST_VIEW_SITE_SKIPPED
            QuickStartStore.QUICK_START_ENABLE_POST_SHARING_LABEL -> Stat.QUICK_START_LIST_ADD_SOCIAL_SKIPPED
            QuickStartStore.QUICK_START_PUBLISH_POST_LABEL -> Stat.QUICK_START_LIST_PUBLISH_POST_SKIPPED
            QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL -> Stat.QUICK_START_LIST_FOLLOW_SITE_SKIPPED
            QuickStartStore.QUICK_START_UPLOAD_SITE_ICON_LABEL -> Stat.QUICK_START_LIST_UPLOAD_ICON_SKIPPED
            QuickStartStore.QUICK_START_CHECK_STATS_LABEL -> Stat.QUICK_START_LIST_CHECK_STATS_SKIPPED
            QuickStartStore.QUICK_START_REVIEW_PAGES_LABEL -> Stat.QUICK_START_LIST_REVIEW_PAGES_SKIPPED
            QuickStartStore.QUICK_START_CHECK_NOTIFIATIONS_LABEL -> Stat.QUICK_START_LIST_CHECK_NOTIFICATIONS_SKIPPED
            QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL -> Stat.QUICK_START_LIST_UPLOAD_MEDIA_SKIPPED
            else -> throw IllegalStateException("The task '$task' is not valid")
        }
    }

    @Suppress("ComplexMethod", "UseCheckOrError")
    fun getTaskCompletedTracker(task: QuickStartTask): Stat {
        return when (task.string) {
            QuickStartStore.QUICK_START_CREATE_SITE_LABEL -> Stat.QUICK_START_CREATE_SITE_TASK_COMPLETED
            QuickStartStore.QUICK_START_UPDATE_SITE_TITLE_LABEL -> Stat.QUICK_START_UPDATE_SITE_TITLE_COMPLETED
            QuickStartStore.QUICK_START_VIEW_SITE_LABEL -> Stat.QUICK_START_VIEW_SITE_TASK_COMPLETED
            QuickStartStore.QUICK_START_ENABLE_POST_SHARING_LABEL -> Stat.QUICK_START_SHARE_SITE_TASK_COMPLETED
            QuickStartStore.QUICK_START_PUBLISH_POST_LABEL -> Stat.QUICK_START_PUBLISH_POST_TASK_COMPLETED
            QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL -> Stat.QUICK_START_FOLLOW_SITE_TASK_COMPLETED
            QuickStartStore.QUICK_START_UPLOAD_SITE_ICON_LABEL -> Stat.QUICK_START_UPLOAD_ICON_COMPLETED
            QuickStartStore.QUICK_START_CHECK_STATS_LABEL -> Stat.QUICK_START_CHECK_STATS_COMPLETED
            QuickStartStore.QUICK_START_REVIEW_PAGES_LABEL -> Stat.QUICK_START_REVIEW_PAGES_TASK_COMPLETED
            QuickStartStore.QUICK_START_CHECK_NOTIFIATIONS_LABEL ->
                Stat.QUICK_START_CHECK_NOTIFICATIONS_TASK_COMPLETED
            QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL -> Stat.QUICK_START_UPLOAD_MEDIA_TASK_COMPLETED
            else -> throw IllegalStateException("The task '$task' is not valid")
        }
    }

    fun startQuickStartReminderTimer(context: Context, quickStartTask: QuickStartTask) {
        val intent = Intent(context, QuickStartReminderReceiver::class.java)

        // for some reason we have to use a bundle to pass serializable to broadcast receiver
        val bundle = Bundle()
        bundle.putSerializable(QuickStartTaskDetails.KEY, QuickStartTaskDetails.getDetailsForTask(quickStartTask))
        intent.putExtra(QuickStartReminderReceiver.ARG_QUICK_START_TASK_BATCH, bundle)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RequestCodes.QUICK_START_REMINDER_RECEIVER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.set(
            AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + QUICK_START_REMINDER_INTERVAL,
            pendingIntent
        )
    }

    @JvmStatic
    fun cancelQuickStartReminder(context: Context) {
        val intent = Intent(context, QuickStartReminderReceiver::class.java)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RequestCodes.QUICK_START_REMINDER_RECEIVER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * This method tries to return the next uncompleted task of taskType
     * if no uncompleted task of taskType remain it tries to find and return uncompleted task of other task type
     */
    @JvmStatic
    fun getNextUncompletedQuickStartTaskForReminderNotification(
        quickStartStore: QuickStartStore,
        siteLocalId: Long,
        taskType: QuickStartTaskType,
        quickStartType: QuickStartType
    ): QuickStartTask? {
        val uncompletedTasksOfPreferredType = quickStartStore.getUncompletedTasksByType(siteLocalId, taskType)

        var nextTask: QuickStartTask? = null

        if (uncompletedTasksOfPreferredType.isEmpty()) {
            val otherQuickStartTaskTypes = quickStartType.taskTypes
                .filter { it != taskType && it != UNKNOWN }

            otherQuickStartTaskTypes.forEach {
                val otherUncompletedTasks = quickStartStore.getUncompletedTasksByType(siteLocalId, it)
                if (otherUncompletedTasks.isNotEmpty()) {
                    nextTask = quickStartStore.getUncompletedTasksByType(siteLocalId, it).first()
                    return@forEach
                }
            }
        } else {
            nextTask = uncompletedTasksOfPreferredType.first()
        }

        return nextTask
    }

    /**
     * This method tries to return the next uncompleted task from complete tasks pool
     */
    @JvmStatic
    @Suppress("ReturnCount")
    fun getNextUncompletedQuickStartTask(
        quickStartStore: QuickStartStore,
        quickStartType: QuickStartType,
        siteLocalId: Long
    ): QuickStartTask? {
        // get all the uncompleted tasks for all task types
        val uncompletedTasks = ArrayList<QuickStartTask>()
        quickStartType.taskTypes.forEach { type ->
            if (type != UNKNOWN) {
                uncompletedTasks.addAll(quickStartStore.getUncompletedTasksByType(siteLocalId, type))
            }
        }
        uncompletedTasks.sortBy { it.order }

        // Looks like we completed all the tasks. Nothing in the pipeline!
        if (uncompletedTasks.isEmpty()) {
            return null
        }

        // Only one task remaining, no need for extra logic.
        if (uncompletedTasks.size == 1) {
            return uncompletedTasks.first()
        }

        // if we have not skipped a task yet, return the first available task from the list
        val lastSkippedTask = AppPrefs.getLastSkippedQuickStartTask(quickStartType)
            ?: return uncompletedTasks.first()

        // look for a task that follows the one we skipped
        val taskThatFollowsSkippedOne = uncompletedTasks.firstOrNull {
            it.order > lastSkippedTask.order
        }

        // if we reached the end of the list (no tasks after skipped one) return task from the top of the list
        return taskThatFollowsSkippedOne ?: uncompletedTasks.first()
    }
}
