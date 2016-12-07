package org.wordpress.android.ui.notifications.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.push.NativeNotificationsUtils;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;

import static org.wordpress.android.push.GCMMessageService.GENERIC_LOCAL_NOTIFICATION_ID;


public class NotificationsPendingDraftsService extends Service {

    private boolean running = false;
    public static final int PENDING_DRAFTS_NOTIFICATION_ID = GENERIC_LOCAL_NOTIFICATION_ID + 1;
    public static final String POST_ID_EXTRA = "postId";
    public static final String IS_PAGE_EXTRA = "isPage";
    //FIXME change this below line
    //private static final long MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION = 24 * 60 * 60 * 1000; //a full 24 hours day
    private static final long MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION = 60 * 1000; //a full 24 hours day
    private static final long MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE = 30; //30 days

    private static final long ONE_DAY = 24 * 60 * 60 * 1000;

    public static void startService(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, NotificationsPendingDraftsService.class);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.NOTIFS, "notifications pending drafts service > created");
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.NOTIFS, "notifications pending drafts service > destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            performDraftsCheck();
        }
        return START_NOT_STICKY;
    }

    private void performDraftsCheck() {
        if (running) {
            return;
        }
        running = true;
        /*
        1) check all “local” drafts, and check that they have been pending for more than 3 days.
        2) make notification if ONE draft and if more than ONE make another text
        ONE: “You’ve got this draft pending for publishing for xxx days. Would you like to check it?”
        (we also have a generic message if we can't determine for how long a draft has been sitting there or it's been
        more than 30 days)
        MORE: “You’ve got 2 posts in drafts. Want to check them?”
        * */
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Post> draftPosts =  WordPress.wpDB.getDraftPostList(WordPress.getCurrentBlog().getLocalTableBlogId());
                ArrayList<Post> draftPostsOlderThan3Days = new ArrayList<>();
                if (draftPosts != null && draftPosts.size() > 0) {
                    //now check those that have been sitting there for more than 3 days now.
                    long now = System.currentTimeMillis();
                    //FIXME change this below line
                    //long three_days_ago = now - (ONE_DAY * 3);
                    long three_days_ago = now - (30000);
                    for (Post post : draftPosts) {
                        if (post.getDateLastUpdated() < three_days_ago) {
                            draftPostsOlderThan3Days.add(post);
                        }
                    }

                    //check the size and build the notification accordingly
                    long daysInDraft = 0;
                    if (draftPostsOlderThan3Days.size() == 1) {
                        Post post = draftPostsOlderThan3Days.get(0);
                        daysInDraft = (now - post.getDateLastUpdated()) / ONE_DAY;
                        long postId = post.getLocalTablePostId();
                        boolean isPage = post.isPage();

                        //only notify the user if the last time they have been notified about this particular post was at least
                        //MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION ago, but let's not do it constantly each time the app comes to the foreground
                        if ((now - post.getDateLastNotified()) > MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION) {
                            post.setDateLastNotified(now);
                            if (daysInDraft < MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE) {
                                buildSinglePendingDraftNotification(post.getTitle(), daysInDraft, postId, isPage);
                            } else {
                                //if it's been more than MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE days, or if we don't know (i.e. value for lastUpdated
                                //is zero) then just show a generic message
                                buildSinglePendingDraftNotification(post.getTitle(), postId, isPage);
                            }
                            WordPress.wpDB.updatePost(post);
                        }

                    } else if (draftPostsOlderThan3Days.size() > 1) {
                        long longestLivingDraft = 0;
                        boolean onlyPagesFound = true;
                        for (Post post : draftPostsOlderThan3Days) {
                            //update each post dateLastNotified field to now
                            if ((now - post.getDateLastNotified()) > MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION) {
                                if (post.getDateLastNotified() > longestLivingDraft) {
                                    longestLivingDraft = post.getDateLastNotified();
                                    if (!post.isPage()) {
                                        onlyPagesFound = false;
                                    }
                                }
                                post.setDateLastNotified(now);
                                WordPress.wpDB.updatePost(post);
                            }
                        }

                        //if there was at least one notification that exceeded the minimum elapsed time to repeat the notif,
                        //then show the notification again
                        if (longestLivingDraft > 0) {
                            buildPendingDraftsNotification(draftPostsOlderThan3Days.size(), onlyPagesFound);
                        }
                    }

                    completed();
                }
            }
        }).start();
    }

    private void buildSinglePendingDraftNotification(String postTitle, long daysInDraft, long postId, boolean isPage){
        buildNotificationWithIntent(String.format(getString(R.string.pending_draft_one), postTitle, daysInDraft), postId, isPage);
    }

    private void buildSinglePendingDraftNotification(String postTitle, long postId, boolean isPage){
        buildNotificationWithIntent(String.format(getString(R.string.pending_draft_one_generic), postTitle), postId, isPage);
    }

    private void buildPendingDraftsNotification(int count, boolean showPages) {
        buildNotificationWithIntent(String.format(getString(R.string.pending_draft_more), count), 0, showPages);
    }

    private void buildNotificationWithIntent(String message, long postId, boolean isPage) {
        NotificationCompat.Builder builder = NativeNotificationsUtils.getBuilder(this);
        builder.setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        Intent resultIntent = new Intent(this, WPMainActivity.class);
        resultIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.putExtra(POST_ID_EXTRA, postId);
        resultIntent.putExtra(IS_PAGE_EXTRA, isPage);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        if (postId != 0) {
            addOpenDraftActionForNotification(this, builder, postId, isPage);
            addIgnoreActionForNotification(this, builder, postId, isPage);
        }
        addDismissActionForNotification(this,builder, postId, isPage);

        NativeNotificationsUtils.showMessageToUserWithBuilder(builder, message, false,
                PENDING_DRAFTS_NOTIFICATION_ID, this);
    }

    private void addOpenDraftActionForNotification(Context context, NotificationCompat.Builder builder, long postId, boolean isPage) {
        // adding open draft action
        Intent openDraftIntent = new Intent(context, WPMainActivity.class);
        openDraftIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
        openDraftIntent.putExtra(POST_ID_EXTRA, postId);
        openDraftIntent.putExtra(IS_PAGE_EXTRA, isPage);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, openDraftIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.ab_icon_edit, getText(R.string.edit),
                pendingIntent);
    }

    private void addIgnoreActionForNotification(Context context, NotificationCompat.Builder builder, long postId, boolean isPage) {
        // Call processing service when user taps on IGNORE - we should remember this decision for this post
        Intent ignoreIntent = new Intent(context, NotificationsProcessingService.class);
        ignoreIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE, NotificationsProcessingService.ARG_ACTION_DRAFT_PENDING_IGNORE);
        ignoreIntent.putExtra(POST_ID_EXTRA, postId);
        ignoreIntent.putExtra(IS_PAGE_EXTRA, isPage);
        PendingIntent dismissPendingIntent =  PendingIntent.getService(context,
                2, ignoreIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.addAction(R.drawable.ic_close_white_24dp, getText(R.string.ignore),
                dismissPendingIntent);

    }

    private void addDismissActionForNotification(Context context, NotificationCompat.Builder builder, long postId, boolean isPage) {
        // Call processing service when notification is dismissed
        Intent notificationDeletedIntent = new Intent(context, NotificationsProcessingService.class);
        notificationDeletedIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE, NotificationsProcessingService.ARG_ACTION_DRAFT_PENDING_DISMISS);
        if (postId != 0 ) {
            notificationDeletedIntent.putExtra(POST_ID_EXTRA, postId);
            notificationDeletedIntent.putExtra(IS_PAGE_EXTRA, isPage);
        }
        PendingIntent pendingDeleteIntent =
                PendingIntent.getBroadcast(context, PENDING_DRAFTS_NOTIFICATION_ID, notificationDeletedIntent, 0);
        builder.setDeleteIntent(pendingDeleteIntent);
    }

    private void completed() {
        AppLog.i(AppLog.T.NOTIFS, "notifications pending drafts service > completed");
        running = false;
        stopSelf();
    }
}
