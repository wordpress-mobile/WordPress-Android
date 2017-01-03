package org.wordpress.android.ui.notifications.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.push.NativeNotificationsUtils;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;

import static org.wordpress.android.push.GCMMessageService.GENERIC_LOCAL_NOTIFICATION_ID;


public class NotificationsPendingDraftsService extends Service {

    private boolean running = false;
    public static final int PENDING_DRAFTS_NOTIFICATION_ID = GENERIC_LOCAL_NOTIFICATION_ID + 1;
    public static final String GROUPED_POST_ID_LIST_EXTRA = "groupedPostIdList";
    public static final String POST_ID_EXTRA = "postId";
    public static final String IS_PAGE_EXTRA = "isPage";
    private static final long MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION = 24 * 60 * 60 * 1000; //a full 24 hours day
    private static final long MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE = 30; // 30 days

    private static final long ONE_DAY = 24 * 60 * 60 * 1000;

    private static void startService(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, NotificationsPendingDraftsService.class);
        context.startService(intent);
    }

    public static void checkPrefsAndStartService(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean shouldNotifyOfPendingDrafts = prefs.getBoolean(context.getString(R.string.pref_key_notification_pending_drafts), true);
        if (shouldNotifyOfPendingDrafts && WordPress.getCurrentBlog() != null) {
            NotificationsPendingDraftsService.startService(context);
        }

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
        if (running || WordPress.getCurrentBlog() == null) {
            return;
        }
        running = true;
        /*
        1) check all “local” drafts, and check that they have been pending for more than 3 days.
        2) make notification if ONE draft and if more than ONE make another text
        ONE: “You drafted 'Post title here' xxxx days ago but never published it.”
        (we also have a generic message if we can't determine for how long a draft has been sitting there or it's been
        more than 30 days)
        MORE: “You drafted %d posts but never published them. Tap to check.”
        * */
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (WordPress.getCurrentBlog() == null) {
                    AppLog.w(AppLog.T.NOTIFS, "Current blog is null. No drafts checking.");
                    completed();
                    return;
                }

                ArrayList<Post> draftPosts =  WordPress.wpDB.getDraftPostList(WordPress.getCurrentBlog().getLocalTableBlogId());
                if (draftPosts != null && draftPosts.size() > 0) {
                    ArrayList<Post> draftPostsNotInIgnoreList;
                    // now check those that have been sitting there for more than 3 days now.
                    long now = System.currentTimeMillis();
                    long three_days_ago = now - (ONE_DAY * 3);

                    // only process posts that are not in the ignore list
                    draftPostsNotInIgnoreList = getPostsNotInPendingDraftsIgnorePostIdList(draftPosts);

                    if (draftPostsNotInIgnoreList.size() > 0) {
                        ArrayList<Post> draftPostsOlderThan3Days = new ArrayList<>();

                        for (Post post : draftPostsNotInIgnoreList) {
                            if (post.getDateLastUpdated() < three_days_ago) {
                                draftPostsOlderThan3Days.add(post);
                            }
                        }

                        // check the size and build the notification accordingly
                        long daysInDraft = 0;
                        if (draftPostsOlderThan3Days.size() == 1) {
                            Post post = draftPostsOlderThan3Days.get(0);

                            // only notify the user if the last time they have been notified about this particular post was at least
                            // MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION ago, but let's not do it constantly each time the app comes to the foreground
                            if ((now - post.getDateLastNotified()) > MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION) {
                                daysInDraft = (now - post.getDateLastUpdated()) / ONE_DAY;
                                long postId = post.getLocalTablePostId();
                                boolean isPage = post.isPage();

                                post.setDateLastNotified(now);
                                if (daysInDraft < MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE) {
                                    buildSinglePendingDraftNotification(post.getTitle(), daysInDraft, postId, isPage);
                                } else {
                                    // if it's been more than MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE days, or if we don't know (i.e. value for lastUpdated
                                    // is zero) then just show a generic message
                                    buildSinglePendingDraftNotificationGeneric(post.getTitle(), postId, isPage);
                                }
                                WordPress.wpDB.updatePost(post);
                            }
                        } else if (draftPostsOlderThan3Days.size() > 1) {
                            long longestLivingDraft = 0;
                            boolean onlyPagesFound = true;
                            for (Post post : draftPostsOlderThan3Days) {

                                // update each post dateLastNotified field to now
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

                            // if there was at least one notification that exceeded the minimum elapsed time to repeat the notif,
                            // then show the notification again
                            if (longestLivingDraft > 0) {
                                buildPendingDraftsNotification(draftPostsOlderThan3Days, onlyPagesFound);
                            }
                        }
                    }

                }
                completed();
            }
        }).start();
    }

    private String getPostTitle(String postTitle) {
        String title = postTitle;
        if (TextUtils.isEmpty(postTitle)) {
            title = "(" + getResources().getText(R.string.untitled) + ")";
        }
        return title;
    }

    private void buildSinglePendingDraftNotification(String postTitle, long daysInDraft, long postId, boolean isPage){
        buildNotificationWithIntent(getResultIntentForOnePost(postId, isPage), String.format(getString(R.string.pending_draft_one), getPostTitle(postTitle), daysInDraft), postId, isPage);
    }

    private void buildSinglePendingDraftNotificationGeneric(String postTitle, long postId, boolean isPage){
        buildNotificationWithIntent(getResultIntentForOnePost(postId, isPage), String.format(getString(R.string.pending_draft_one_generic), getPostTitle(postTitle)), postId, isPage);
    }

    private void buildPendingDraftsNotification(ArrayList<Post> posts, boolean showPages) {
        ArrayList<Long> postIdList = new ArrayList<>();
        for (Post onePost : posts) {
            postIdList.add(new Long(onePost.getLocalTablePostId()));
        }
        buildNotificationWithIntentForGroup(getResultIntentForMultiplePosts(posts, showPages), String.format(getString(R.string.pending_draft_more), posts.size()), postIdList, showPages);
    }

    private PendingIntent getResultIntentForOnePost(long postId, boolean isPage) {

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

        return pendingIntent;
    }

    private PendingIntent getResultIntentForMultiplePosts(ArrayList<Post> posts, boolean isPage) {

        // convert posts list to csv id list
        ArrayList<Long> postIdList = new ArrayList<>();
        for (Post post : posts) {
            postIdList.add(post.getLocalTablePostId());
        }

        Intent resultIntent = new Intent(this, WPMainActivity.class);
        resultIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.putExtra(GROUPED_POST_ID_LIST_EXTRA, postIdList);
        resultIntent.putExtra(IS_PAGE_EXTRA, isPage);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);

        return pendingIntent;
    }

    private void buildNotificationWithIntent(PendingIntent intent, String message, long postId, boolean isPage) {
        NotificationCompat.Builder builder = NativeNotificationsUtils.getBuilder(this);
        builder.setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setContentIntent(intent);

        ArrayList<Long> postIdList = new ArrayList<>();
        postIdList.add(new Long(postId));
        if (postId != 0) {
            addOpenDraftActionForNotification(this, builder, postIdList, isPage);
            addIgnoreActionForNotification(this, builder, postIdList, isPage);
        }
        addDismissActionForNotification(this,builder, postIdList, isPage);

        NativeNotificationsUtils.showMessageToUserWithBuilder(builder, message, false,
                PENDING_DRAFTS_NOTIFICATION_ID, this);
    }

    private void buildNotificationWithIntentForGroup(PendingIntent intent, String message, ArrayList<Long> postIdList, boolean isPage) {
        NotificationCompat.Builder builder = NativeNotificationsUtils.getBuilder(this);
        builder.setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setContentIntent(intent);

        addOpenDraftActionForNotification(this, builder, postIdList, isPage);
        addIgnoreActionForNotification(this, builder, postIdList, isPage);
        addDismissActionForNotification(this,builder, postIdList, isPage);

        NativeNotificationsUtils.showMessageToUserWithBuilder(builder, message, false,
                PENDING_DRAFTS_NOTIFICATION_ID, this);
    }

    private static void setUniqueOrGroupedPostIdListExtraOnIntent(Intent intent, ArrayList<Long> postIdList){
        if (postIdList != null) {
            if (postIdList.size() == 1) {
                intent.putExtra(POST_ID_EXTRA, postIdList.get(0));
            } else {
                intent.putExtra(GROUPED_POST_ID_LIST_EXTRA, postIdList);
            }
        }
    }

    private void addOpenDraftActionForNotification(Context context, NotificationCompat.Builder builder, ArrayList<Long> postIdList, boolean isPage) {
        // adding open draft action
        Intent openDraftIntent = new Intent(context, WPMainActivity.class);
        openDraftIntent.putExtra(WPMainActivity.ARG_OPENED_FROM_PUSH, true);
        setUniqueOrGroupedPostIdListExtraOnIntent(openDraftIntent, postIdList);
        openDraftIntent.putExtra(IS_PAGE_EXTRA, isPage);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, openDraftIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.ic_edit_icon, getText(R.string.edit),
                pendingIntent);
    }

    private void addIgnoreActionForNotification(Context context, NotificationCompat.Builder builder, ArrayList<Long> postIdList, boolean isPage) {
        // Call processing service when user taps on IGNORE - we should remember this decision for this post
        Intent ignoreIntent = new Intent(context, NotificationsProcessingService.class);
        ignoreIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE, NotificationsProcessingService.ARG_ACTION_DRAFT_PENDING_IGNORE);
        setUniqueOrGroupedPostIdListExtraOnIntent(ignoreIntent, postIdList);
        ignoreIntent.putExtra(IS_PAGE_EXTRA, isPage);
        PendingIntent ignorePendingIntent =  PendingIntent.getService(context,
                2, ignoreIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.addAction(R.drawable.ic_close_white_24dp, getText(R.string.ignore),
                ignorePendingIntent);

    }

    private void addDismissActionForNotification(Context context, NotificationCompat.Builder builder, ArrayList<Long> postIdList, boolean isPage) {
        // Call processing service when notification is dismissed
        Intent notificationDeletedIntent = new Intent(context, NotificationsProcessingService.class);
        notificationDeletedIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE, NotificationsProcessingService.ARG_ACTION_DRAFT_PENDING_DISMISS);
        setUniqueOrGroupedPostIdListExtraOnIntent(notificationDeletedIntent, postIdList);
        notificationDeletedIntent.putExtra(IS_PAGE_EXTRA, isPage);
        PendingIntent dismissPendingIntent =  PendingIntent.getService(context,
                3, notificationDeletedIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setDeleteIntent(dismissPendingIntent);
    }

    private ArrayList<Post> getPostsNotInPendingDraftsIgnorePostIdList(ArrayList<Post> postSet) {
        ArrayList<Post> postsNotInIgnorePostList = new ArrayList<>();
        if (postSet != null) {
            ArrayList<Long> idList = AppPrefs.getPendingDraftsIgnorePostIdList();

            for (Post onePost : postSet) {
                if (onePost != null) {
                    boolean foundId = false;
                    for (Long oneId : idList) {
                        if (onePost.getLocalTablePostId() == oneId) {
                            foundId = true;
                            break;
                        }
                    }

                    if (!foundId) {
                        postsNotInIgnorePostList.add(onePost);
                    }
                }
            }
        }
        return postsNotInIgnorePostList;
    }

    private void completed() {
        AppLog.i(AppLog.T.NOTIFS, "notifications pending drafts service > completed");
        running = false;
        stopSelf();
    }
}
