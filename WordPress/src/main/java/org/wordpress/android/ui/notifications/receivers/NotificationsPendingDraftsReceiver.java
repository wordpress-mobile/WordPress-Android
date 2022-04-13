package org.wordpress.android.ui.notifications.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.push.NativeNotificationsUtils;
import org.wordpress.android.push.NotificationType;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.mysite.SelectedSiteRepository;
import org.wordpress.android.ui.notifications.SystemNotificationsTracker;
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;

import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import static org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE;

public class NotificationsPendingDraftsReceiver extends BroadcastReceiver {
    public static final String POST_ID_EXTRA = "postId";
    public static final String IS_PAGE_EXTRA = "isPage";

    public static final long ONE_DAY = 24 * 60 * 60 * 1000;
    public static final long ONE_WEEK = ONE_DAY * 7;
    public static final long ONE_MONTH = ONE_WEEK * 4;

    private static final long MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE = 40; // just over a month

    private static final int BASE_REQUEST_CODE = 100;

    @Inject PostStore mPostStore;
    @Inject SiteStore mSiteStore;
    @Inject SystemNotificationsTracker mSystemNotificationsTracker;
    @Inject SelectedSiteRepository mSelectedSiteRepository;

    @Override
    public void onReceive(Context context, Intent intent) {
        ((WordPress) context.getApplicationContext()).component().inject(this);

        // for the case of being spanned after device restarts, get the latest drafts
        // and check the lastUpdated
        String action = intent.getAction();
        if (action != null && action.equals("android.intent.action.BOOT_COMPLETED")) {
            AppLog.i(AppLog.T.NOTIFS, "entering Pending Drafts Receiver from BOOT_COMPLETED");
            // build notifications for existing local drafts
            SiteModel site = mSiteStore.getSiteByLocalId(mSelectedSiteRepository.getSelectedSiteLocalId());
            if (site != null) {
                List<PostModel> draftPosts = mPostStore.getPostsForSite(site);
                for (PostModel post : draftPosts) {
                    // reschedule next notifications for each local draft post we have, as we have
                    // just been rebooted
                    PendingDraftsNotificationsUtils.scheduleNextNotifications(context, post.getId(),
                            post.getDateLocallyChanged());
                }
            }
        } else {
            AppLog.i(AppLog.T.NOTIFS, "entering Pending Drafts Receiver from alarm");
            // get extras from intent in order to build notification
            buildNotificationForPostId(intent.getIntExtra(POST_ID_EXTRA, 0), context);
        }
    }

    private void buildNotificationForPostId(int postId, Context context) {
        if (postId != 0) {
            PostModel post = mPostStore.getPostByLocalPostId(postId);
            if (post != null && PostStatus.DRAFT.toString().equals(post.getStatus())) {
                long now = System.currentTimeMillis();
                long dateLastUpdated = DateTimeUtils.timestampFromIso8601(post.getDateLocallyChanged());
                long daysInDraft = (now - dateLastUpdated) / ONE_DAY;
                boolean isPage = post.isPage();

                if (daysInDraft < MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE) {
                    String formattedString = context.getString(R.string.pending_draft_one_generic);

                    long oneDayAgo = now - ONE_DAY;
                    long oneWeekAgo = now - ONE_WEEK;
                    long oneMonthAgo = now - ONE_MONTH;

                    if (dateLastUpdated < oneMonthAgo) {
                        formattedString = context.getString(R.string.pending_draft_one_month);
                    } else if (dateLastUpdated < oneWeekAgo) {
                        // use any of the available 2 string formats, randomly
                        Random randomNum = new Random();
                        int result = randomNum.nextInt(2);
                        if (result == 0) {
                            formattedString = context.getString(R.string.pending_draft_one_week_1);
                        } else {
                            formattedString = context.getString(R.string.pending_draft_one_week_2);
                        }
                    } else if (dateLastUpdated < oneDayAgo) {
                        // use any of the available 2 string formats, randomly
                        Random randomNum = new Random();
                        int result = randomNum.nextInt(2);
                        if (result == 0) {
                            formattedString = context.getString(R.string.pending_draft_one_day_1);
                        } else {
                            formattedString = context.getString(R.string.pending_draft_one_day_2);
                        }
                    }

                    buildSinglePendingDraftNotification(context, post.getTitle(), formattedString, postId, isPage);
                } else {
                    // if it's been more than MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE days, or if we don't know
                    // (i.e. value for lastUpdated is zero) then just show a generic message
                    buildSinglePendingDraftNotificationGeneric(context, post.getTitle(), postId, isPage);
                }
            }
        }
    }

    private String getPostTitle(Context context, String postTitle) {
        String title = postTitle;
        if (TextUtils.isEmpty(postTitle)) {
            title = "(" + context.getResources().getText(R.string.untitled) + ")";
        }
        return title;
    }

    private void buildSinglePendingDraftNotification(Context context, String postTitle, String formattedMessage,
                                                     int postId, boolean isPage) {
        buildSinglePendingDraftNotification(context, getResultIntentForOnePost(context, postId, isPage),
                String.format(formattedMessage, getPostTitle(context, postTitle)), postId, isPage);
    }

    private void buildSinglePendingDraftNotificationGeneric(Context context, String postTitle, int postId,
                                                            boolean isPage) {
        buildSinglePendingDraftNotification(context, getResultIntentForOnePost(context, postId, isPage),
                String.format(context.getString(R.string.pending_draft_one_generic),
                        getPostTitle(context, postTitle)),
                postId, isPage);
    }

    private PendingIntent getResultIntentForOnePost(Context context, int postId, boolean isPage) {
        Intent resultIntent = new Intent(context, WPMainActivity.class);
        resultIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                              | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.putExtra(POST_ID_EXTRA, postId);
        resultIntent.putExtra(IS_PAGE_EXTRA, isPage);
        resultIntent.putExtra(ARG_NOTIFICATION_TYPE, NotificationType.PENDING_DRAFTS);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                BASE_REQUEST_CODE + PendingDraftsNotificationsUtils.makePendingDraftNotificationId(postId),
                resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return pendingIntent;
    }

    private void buildSinglePendingDraftNotification(Context context, PendingIntent intent, String message, int postId,
                                                     boolean isPage) {
        NotificationCompat.Builder builder = NativeNotificationsUtils.getBuilder(context,
                context.getString(R.string.notification_channel_important_id));
        builder.setContentText(message)
               .setPriority(NotificationCompat.PRIORITY_MAX)
               .setOnlyAlertOnce(true);
        builder.setContentIntent(intent);

        if (postId != 0) {
            addOpenDraftActionForNotification(context, builder, postId, isPage);
            addIgnoreActionForNotification(context, builder, postId, isPage);
        }
        addDismissActionForNotification(context, builder, postId, isPage);

        NativeNotificationsUtils.showMessageToUserWithBuilder(builder, message, false,
                PendingDraftsNotificationsUtils.makePendingDraftNotificationId(postId), context);
        mSystemNotificationsTracker.trackShownNotification(NotificationType.PENDING_DRAFTS);
    }

    private void addOpenDraftActionForNotification(Context context, NotificationCompat.Builder builder, int postId,
                                                   boolean isPage) {
        // adding open draft action
        Intent openDraftIntent = new Intent(context, WPMainActivity.class);
        openDraftIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
        openDraftIntent.putExtra(POST_ID_EXTRA, postId);
        openDraftIntent.putExtra(IS_PAGE_EXTRA, isPage);
        openDraftIntent.putExtra(ARG_NOTIFICATION_TYPE, NotificationType.PENDING_DRAFTS);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                // need to add + 1 so the request code is different, otherwise they overlap
                BASE_REQUEST_CODE + 1 + PendingDraftsNotificationsUtils.makePendingDraftNotificationId(postId),
                openDraftIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.addAction(R.drawable.ic_pencil_white_24dp, context.getText(R.string.edit), pendingIntent);
    }

    private void addIgnoreActionForNotification(Context context, NotificationCompat.Builder builder, int postId,
                                                boolean isPage) {
        // Call processing service when user taps on IGNORE - we should remember this decision for this post
        Intent ignoreIntent = new Intent(context, NotificationsProcessingService.class);
        ignoreIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE,
                NotificationsProcessingService.ARG_ACTION_DRAFT_PENDING_IGNORE);
        ignoreIntent.putExtra(POST_ID_EXTRA, postId);
        ignoreIntent.putExtra(IS_PAGE_EXTRA, isPage);
        ignoreIntent.putExtra(ARG_NOTIFICATION_TYPE, NotificationType.PENDING_DRAFTS);
        PendingIntent ignorePendingIntent = PendingIntent.getService(
                context,
                // need to add + 2 so the request code is different, otherwise they overlap
                BASE_REQUEST_CODE + 2 + PendingDraftsNotificationsUtils.makePendingDraftNotificationId(postId),
                ignoreIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.addAction(R.drawable.ic_close_white_24dp, context.getText(R.string.ignore), ignorePendingIntent);
    }

    private void addDismissActionForNotification(Context context, NotificationCompat.Builder builder, int postId,
                                                 boolean isPage) {
        // Call processing service when notification is dismissed
        Intent notificationDeletedIntent = new Intent(context, NotificationsProcessingService.class);
        notificationDeletedIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE,
                NotificationsProcessingService.ARG_ACTION_DRAFT_PENDING_DISMISS);
        notificationDeletedIntent.putExtra(POST_ID_EXTRA, postId);
        notificationDeletedIntent.putExtra(IS_PAGE_EXTRA, isPage);
        notificationDeletedIntent.putExtra(NotificationsProcessingService.ARG_NOTIFICATION_TYPE,
                NotificationType.PENDING_DRAFTS);
        PendingIntent dismissPendingIntent = PendingIntent.getService(
                context,
                // need to add + 3 so the request code is different, otherwise they overlap
                BASE_REQUEST_CODE + 3 + PendingDraftsNotificationsUtils.makePendingDraftNotificationId(postId),
                notificationDeletedIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setDeleteIntent(dismissPendingIntent);
    }
}
