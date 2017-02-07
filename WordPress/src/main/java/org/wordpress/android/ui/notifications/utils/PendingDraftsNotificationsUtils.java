package org.wordpress.android.ui.notifications.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.wordpress.android.models.Post;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.ui.notifications.receivers.NotificationsPendingDraftsReceiver;


public class PendingDraftsNotificationsUtils {

    // Pending draft notification base request code for alarms
    private static final int BROADCAST_BASE_REQUEST_CODE = 181;
    private static final int PENDING_DRAFTS_NOTIFICATION_ID = GCMMessageService.GENERIC_LOCAL_NOTIFICATION_ID + 1;

    /*
     * Schedules alarms for draft posts to remind the user they have pending drafts
     * Starts since the last time the post was updated for one day, one week, and one month
     * only if these periods have not passed yet.
     */
    public static void scheduleNextNotifications(Context context, Post post) {
        if (post == null || context == null) return;
            /*
            Have +1 day, +1 week, +1 month reminders, with different messages randomly chosen, that could even be fun:

            1 day:
                You drafted “post title” yesterday. Don’t forget to publish it!
                Did you know that “post title” is still a draft? Publish away!
            1 week:
                Your draft, “post title” awaits you — be sure to publish it!
                “Post title” remains a draft. Remember to publish it!
            1 month
                Don’t leave it hanging! “Post title” is waiting to be published.
            * */

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = getNotificationsPendingDraftReceiverIntent(context, post.getLocalTablePostId());

        long now = System.currentTimeMillis();
        long one_day_ago = now - NotificationsPendingDraftsReceiver.ONE_DAY;
        long one_week_ago = now - NotificationsPendingDraftsReceiver.ONE_WEEK;
        long one_month_ago = now - NotificationsPendingDraftsReceiver.ONE_MONTH;

        long dateLastUpdated = post.getDateLastUpdated();

        // set alarms for one day + one week + one month if just over a day but less than a week,
        // set alarms for a week and another for a month, if over a week but less than a month
        // set alarm for a month for anything further than a month
        long postId = post.getLocalTablePostId();
        if (dateLastUpdated > one_day_ago) {
            // last updated is within a 24 hour timeframe
            PendingIntent alarmIntentOneDay = getOneDayAlarmIntent(context, intent, postId);
            alarmManager.set(AlarmManager.RTC_WAKEUP, dateLastUpdated + NotificationsPendingDraftsReceiver.ONE_DAY, alarmIntentOneDay);

            PendingIntent alarmIntentOneWeek = getOneWeekAlarmIntent(context, intent, postId);
            alarmManager.set(AlarmManager.RTC_WAKEUP, dateLastUpdated + NotificationsPendingDraftsReceiver.ONE_WEEK, alarmIntentOneWeek);

            PendingIntent alarmIntentOneMonth = getOneMonthAlarmIntent(context, intent, postId);
            alarmManager.set(AlarmManager.RTC_WAKEUP, dateLastUpdated + NotificationsPendingDraftsReceiver.ONE_MONTH, alarmIntentOneMonth);
        }
        else
        if (dateLastUpdated > one_week_ago) {
            // last updated is within a 1 week timeframe (between 1 day and 7 days)
            PendingIntent alarmIntentOneWeek = getOneWeekAlarmIntent(context, intent, postId);
            alarmManager.set(AlarmManager.RTC_WAKEUP, dateLastUpdated + NotificationsPendingDraftsReceiver.ONE_WEEK, alarmIntentOneWeek);

            PendingIntent alarmIntentOneMonth = getOneMonthAlarmIntent(context, intent, postId);
            alarmManager.set(AlarmManager.RTC_WAKEUP, dateLastUpdated + NotificationsPendingDraftsReceiver.ONE_MONTH, alarmIntentOneMonth);
        }
        else
        if (dateLastUpdated > one_month_ago) {
            // last updated is within a 1 month timeframe (between 7 days and 30 days)
            PendingIntent alarmIntentOneMonth = getOneMonthAlarmIntent(context, intent, postId);
            alarmManager.set(AlarmManager.RTC_WAKEUP, dateLastUpdated + NotificationsPendingDraftsReceiver.ONE_MONTH, alarmIntentOneMonth);
        }

    }

    public static void cancelPendingDraftAlarms(Context context, long localPostId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = getNotificationsPendingDraftReceiverIntent(context, localPostId);

        PendingIntent alarmIntentOneDay = getOneDayAlarmIntent(context, intent, localPostId);
        PendingIntent alarmIntentOneWeek = getOneWeekAlarmIntent(context, intent, localPostId);
        PendingIntent alarmIntentOneMonth = getOneMonthAlarmIntent(context, intent, localPostId);

        alarmManager.cancel(alarmIntentOneDay);
        alarmManager.cancel(alarmIntentOneWeek);
        alarmManager.cancel(alarmIntentOneMonth);
    }

    public static int makePendingDraftNotificationId(long localPostId) {
        // constructs a notification ID (int) based on a localPostId (long) which should be low numbers
        // by casting explicitely
        // Integer.MAX_VALUE should be enough notifications
        return PENDING_DRAFTS_NOTIFICATION_ID + (int)localPostId;
    }

    private static Intent getNotificationsPendingDraftReceiverIntent(Context context, long localPostId) {
        Intent intent = new Intent(context, NotificationsPendingDraftsReceiver.class);
        intent.putExtra(NotificationsPendingDraftsReceiver.POST_ID_EXTRA, localPostId);
        return intent;
    }

    private static PendingIntent getOneDayAlarmIntent(Context context, Intent notifPendingDraftReceiverIntent, long postId) {
        PendingIntent alarmIntentOneDay = PendingIntent.getBroadcast(context,
                BROADCAST_BASE_REQUEST_CODE + makePendingDraftNotificationId(postId),
                notifPendingDraftReceiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return alarmIntentOneDay;
    }

    private static PendingIntent getOneWeekAlarmIntent(Context context, Intent notifPendingDraftReceiverIntent, long postId) {
        PendingIntent alarmIntentOneWeek = PendingIntent.getBroadcast(context,
                BROADCAST_BASE_REQUEST_CODE + 1 + makePendingDraftNotificationId(postId),
                // need to add + 1 so the request code is different from oneDay and oneMonth pendingIntents, otherwise they overlap
                notifPendingDraftReceiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return alarmIntentOneWeek;
    }

    private static PendingIntent getOneMonthAlarmIntent(Context context, Intent notifPendingDraftReceiverIntent, long postId) {
        PendingIntent alarmIntentOneMonth = PendingIntent.getBroadcast(context,
                BROADCAST_BASE_REQUEST_CODE + 2 + makePendingDraftNotificationId(postId),
                // need to add + 2 so the request code is different from oneDay and oneWeek pendingIntents, otherwise they overlap
                notifPendingDraftReceiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return alarmIntentOneMonth;
    }

}
