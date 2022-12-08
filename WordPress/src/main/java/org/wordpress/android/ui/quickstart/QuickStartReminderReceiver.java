package org.wordpress.android.ui.quickstart;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.store.QuickStartStore;
import org.wordpress.android.push.NotificationPushIds;
import org.wordpress.android.push.NotificationType;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.mysite.MySiteViewModel;
import org.wordpress.android.ui.mysite.SelectedSiteRepository;
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository;
import org.wordpress.android.ui.notifications.SystemNotificationsTracker;

import javax.inject.Inject;

import static org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QuickStartReminderReceiver extends BroadcastReceiver {
    public static final String ARG_QUICK_START_TASK_BATCH = "ARG_QUICK_START_TASK_BATCH";

    @Inject SharedPreferences mSharedPreferences;
    @Inject QuickStartStore mQuickStartStore;
    @Inject SystemNotificationsTracker mSystemNotificationsTracker;
    @Inject SelectedSiteRepository mSelectedSiteRepository;
    @Inject QuickStartRepository mQuickStartRepository;
    @Inject QuickStartTracker mQuickStartTracker;

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String notificationSettingsPrefKey = context.getString(R.string.wp_pref_notifications_main);
        boolean isNotificationSettingsEnabled = mSharedPreferences.getBoolean(notificationSettingsPrefKey, true);
        if (!isNotificationSettingsEnabled) {
            return;
        }

        Bundle bundleWithQuickStartTaskDetails = intent.getBundleExtra(ARG_QUICK_START_TASK_BATCH);

        if (bundleWithQuickStartTaskDetails == null) {
            return;
        }

        int selectedSiteLocalId = mSelectedSiteRepository.getSelectedSiteLocalId();

        QuickStartTaskDetails quickStartTaskDetails = (QuickStartTaskDetails) bundleWithQuickStartTaskDetails
                .getSerializable(QuickStartTaskDetails.KEY);

        QuickStartType quickStartType = mQuickStartRepository.getQuickStartType();

        // Failsafes
        if (
                quickStartTaskDetails == null
                || selectedSiteLocalId == SelectedSiteRepository.UNAVAILABLE
                || !quickStartType.isQuickStartInProgress(mQuickStartStore, selectedSiteLocalId)
                || mQuickStartStore.hasDoneTask(
                        selectedSiteLocalId,
                        quickStartType.getTaskFromString(quickStartTaskDetails.getTaskString())
                )
        ) {
            return;
        }

        Intent resultIntent = new Intent(context, WPMainActivity.class);
        resultIntent.putExtra(MySiteViewModel.ARG_QUICK_START_TASK, true);
        NotificationType notificationType = NotificationType.QUICK_START_REMINDER;
        resultIntent.putExtra(ARG_NOTIFICATION_TYPE, notificationType);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                              | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent notificationContentIntent =
                PendingIntent.getActivity(context, NotificationPushIds.QUICK_START_REMINDER_NOTIFICATION_ID,
                        resultIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Notification notification = new NotificationCompat.Builder(context,
                context.getString(R.string.notification_channel_reminder_id))
                .setSmallIcon(R.drawable.ic_app_white_24dp)
                .setContentTitle(context.getString(quickStartTaskDetails.getTitleResId()))
                .setContentText(context.getString(quickStartTaskDetails.getSubtitleResId()))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setContentIntent(notificationContentIntent)
                .setDeleteIntent(NotificationsProcessingService
                        .getPendingIntentForNotificationDismiss(context,
                                NotificationPushIds.QUICK_START_REMINDER_NOTIFICATION_ID,
                                notificationType))
                .build();

        notificationManager.notify(NotificationPushIds.QUICK_START_REMINDER_NOTIFICATION_ID, notification);
        mQuickStartTracker.track(Stat.QUICK_START_NOTIFICATION_SENT);
        mSystemNotificationsTracker.trackShownNotification(notificationType);
    }
}
