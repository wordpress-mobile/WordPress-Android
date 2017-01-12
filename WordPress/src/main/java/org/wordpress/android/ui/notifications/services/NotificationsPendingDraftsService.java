package org.wordpress.android.ui.notifications.services;

import android.app.AlarmManager;
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
import org.wordpress.android.ui.notifications.receivers.NotificationsPendingDraftsReceiver;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.Random;

import static org.wordpress.android.push.GCMMessageService.GENERIC_LOCAL_NOTIFICATION_ID;


public class NotificationsPendingDraftsService extends Service {

    private boolean running = false;
    public static final int PENDING_DRAFTS_NOTIFICATION_ID = GENERIC_LOCAL_NOTIFICATION_ID + 1;
    public static final String GROUPED_POST_ID_LIST_EXTRA = "groupedPostIdList";
    public static final String POST_ID_EXTRA = "postId";
    public static final String IS_PAGE_EXTRA = "isPage";
    private static final long ONE_DAY = 24 * 60 * 60 * 1000;
    private static final long ONE_WEEK = ONE_DAY * 7;
    private static final long ONE_MONTH = ONE_WEEK * 4;
    private static final long MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION = ONE_DAY; //a full 24 hours day
    private static final long MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE = 30; // 30 days


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

    // will start the pending drafts check every day and see where we are at.
    public static void scheduleNextStart(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, NotificationsPendingDraftsReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        // starting within the next 60 seconds, and then repeat each day
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, System.currentTimeMillis() + 60000,
                AlarmManager.INTERVAL_DAY, alarmIntent);

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
        1) check all “local” drafts, and check that they have been pending for more than ONE_DAY.
        2) make notification if ONE draft and if more than ONE make another text
        ONE: “You drafted 'Post title here' xxxx days ago but never published it.”
        (we also have a generic message if we can't determine for how long a draft has been sitting there or it's been
        more than 30 days)
        MORE: “You drafted %d posts but never published them. Tap to check.”

        UPDATE:
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

                    // now check those that have been sitting there for more than 1 day now.
                    long now = System.currentTimeMillis();
                    long one_day_ago = now - ONE_DAY;
                    long one_week_ago = now - ONE_WEEK;
                    long one_month_ago = now - ONE_MONTH;
                    ArrayList<Post> draftPostsNotInIgnoreLists =  getPostsNotInAnyPendingDraftsIgnorePostIdList(now, draftPosts);


                    // check the size and build the notification accordingly
                    int totalDraftsNotInIgnoreLists = draftPostsNotInIgnoreLists.size();
                    if (draftPostsNotInIgnoreLists.size() > 1) {
                        // only show draft count for drafts that were not in any ignore list
                        showPendingCountNotification(now, draftPostsNotInIgnoreLists);
                    }
                    else if (totalDraftsNotInIgnoreLists == 1) {

                        long daysInDraft = 0;

                        // if there is only one draft, we'e sure to have it in the draftPosts
                        Post post = draftPostsNotInIgnoreLists.get(0);

                        // at this point, this means we have one draft that has either never been marked
                        // for ignore, or it has been marked for ignore but we are now on a different timeframe
                        // (i.e. it was marked ignore for the day-old timeframe and now it's been a week).
                        // Because of that, we should be safe to eliminate it from the other ignore lists.
                        AppPrefs.deleteIdFromAllPendingDraftsIgnorePostIdLists(post.getLocalTablePostId());


                        // only notify the user if the last time they have been notified about this particular post was
                        // at least MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION ago, but let's
                        // not do it constantly each time the app comes to the foreground
                        if ((now - post.getDateLastNotified()) > MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION) {
                            daysInDraft = (now - post.getDateLastUpdated()) / ONE_DAY;
                            long postId = post.getLocalTablePostId();
                            boolean isPage = post.isPage();
                            post.setDateLastNotified(now);

                            // TODO: HERE CHECK THE daysInDraft and show the right message
                            // use the right message for 1 day, 1 week and 1 month old
                            if (daysInDraft < MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE) {
                                String formattedString = getString(R.string.pending_draft_one_generic);

                                long dateLastUpdated = post.getDateLastUpdated();
                                if (dateLastUpdated < one_month_ago) {
                                    formattedString = getString(R.string.pending_draft_one_month);
                                }
                                else
                                if (dateLastUpdated < one_week_ago) {
                                    // use any of the available 2 string formats, randomly
                                    Random randomNum = new Random();
                                    int result = randomNum.nextInt(2);
                                    if (result == 0)
                                        formattedString = getString(R.string.pending_draft_one_week_1);
                                    else
                                        formattedString = getString(R.string.pending_draft_one_week_2);
                                }
                                else
                                if (dateLastUpdated < one_day_ago) {
                                    // use any of the available 2 string formats, randomly
                                    Random randomNum = new Random();
                                    int result = randomNum.nextInt(2);
                                    if (result == 0)
                                        formattedString = getString(R.string.pending_draft_one_day_1);
                                    else
                                        formattedString = getString(R.string.pending_draft_one_day_2);
                                }

                                buildSinglePendingDraftNotification(post.getTitle(), formattedString, postId, isPage);

                            } else {
                                // if it's been more than MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE days, or if we don't know (i.e. value for lastUpdated
                                // is zero) then just show a generic message
                                buildSinglePendingDraftNotificationGeneric(post.getTitle(), postId, isPage);
                            }
                            WordPress.wpDB.updatePost(post);
                        }
                    }
                }
                completed();
            }
        }).start();
    }

    private void showPendingCountNotification(long now, ArrayList<Post> draftPostsNotInIgnoreList) {
        long longestLivingDraft = 0;
        boolean onlyPagesFound = true;
        boolean doShowNotification = false;

        for (Post post : draftPostsNotInIgnoreList) {

            // update each post dateLastNotified field to now
            if ((now - post.getDateLastNotified()) > MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION) {
                if (post.getDateLastNotified() > longestLivingDraft) {
                    doShowNotification = true;
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
        if (doShowNotification) {
            buildPendingDraftsNotification(draftPostsNotInIgnoreList, onlyPagesFound);
        }
    }

    private String getPostTitle(String postTitle) {
        String title = postTitle;
        if (TextUtils.isEmpty(postTitle)) {
            title = "(" + getResources().getText(R.string.untitled) + ")";
        }
        return title;
    }

    private void buildSinglePendingDraftNotification(String postTitle, String formattedMessage, long postId, boolean isPage){
        buildNotificationWithIntent(getResultIntentForOnePost(postId, isPage), String.format(formattedMessage, getPostTitle(postTitle)), postId, isPage);
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

    private ArrayList<Post> getPostsNotInAnyPendingDraftsIgnorePostIdList(long now, ArrayList<Post> postSet) {
        ArrayList<Post> postsNotInIgnorePostList = new ArrayList<>();
        if (postSet != null) {
            ArrayList<Long> idListDay = AppPrefs.getPendingDraftsIgnorePostIdList(AppPrefs.DeletablePrefKey.PENDING_DRAFTS_NOTIFICATION_IGNORE_LIST_DAY);
            ArrayList<Long> idListWeek = AppPrefs.getPendingDraftsIgnorePostIdList(AppPrefs.DeletablePrefKey.PENDING_DRAFTS_NOTIFICATION_IGNORE_LIST_WEEK);
            ArrayList<Long> idListMonth = AppPrefs.getPendingDraftsIgnorePostIdList(AppPrefs.DeletablePrefKey.PENDING_DRAFTS_NOTIFICATION_IGNORE_LIST_MONTH);

            for (Post onePost : postSet) {
                if (onePost != null) {
                    boolean foundId = false;
                    for (Long oneId : idListDay) {
                        if (onePost.getLocalTablePostId() == oneId
                                && isCurrentIgnoreTimeFrameDay(getDraftCurrentIgnoreTimeframeCandidate(now, onePost))) {
                            foundId = true;
                            break;
                        }
                    }

                    if (!foundId) {
                        for (Long oneId : idListWeek) {
                            if (onePost.getLocalTablePostId() == oneId
                                    && isCurrentIgnoreTimeFrameWeek(getDraftCurrentIgnoreTimeframeCandidate(now, onePost))) {
                                foundId = true;
                                break;
                            }
                        }
                    }

                    if (!foundId) {
                        for (Long oneId : idListMonth) {
                            if (onePost.getLocalTablePostId() == oneId
                                    && isCurrentIgnoreTimeFrameMonth(getDraftCurrentIgnoreTimeframeCandidate(now, onePost))) {
                                foundId = true;
                                break;
                            }
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

    public static boolean isCurrentIgnoreTimeFrameDay(AppPrefs.DeletablePrefKey timeFrameKey) {
        return (timeFrameKey == AppPrefs.DeletablePrefKey.PENDING_DRAFTS_NOTIFICATION_IGNORE_LIST_DAY);
    }

    public static boolean isCurrentIgnoreTimeFrameWeek(AppPrefs.DeletablePrefKey timeFrameKey) {
        return (timeFrameKey == AppPrefs.DeletablePrefKey.PENDING_DRAFTS_NOTIFICATION_IGNORE_LIST_WEEK);
    }

    public static boolean isCurrentIgnoreTimeFrameMonth(AppPrefs.DeletablePrefKey timeFrameKey) {
        return (timeFrameKey == AppPrefs.DeletablePrefKey.PENDING_DRAFTS_NOTIFICATION_IGNORE_LIST_MONTH);
    }

    public static AppPrefs.DeletablePrefKey getDraftCurrentIgnoreTimeframeCandidate(long now, Post post) {
        long one_day_ago = now - ONE_DAY;
        long one_week_ago = now - ONE_WEEK;
        long one_month_ago = now - ONE_MONTH;

        long dateLastUpdated = post.getDateLastUpdated();
        if (dateLastUpdated < one_month_ago) {
            return AppPrefs.DeletablePrefKey.PENDING_DRAFTS_NOTIFICATION_IGNORE_LIST_MONTH;
        }
        else
        if (dateLastUpdated < one_week_ago) {
            return AppPrefs.DeletablePrefKey.PENDING_DRAFTS_NOTIFICATION_IGNORE_LIST_WEEK;
        }
        else
        if (dateLastUpdated < one_day_ago) {
            return AppPrefs.DeletablePrefKey.PENDING_DRAFTS_NOTIFICATION_IGNORE_LIST_DAY;
        }

        //default: month as it's older
        return AppPrefs.DeletablePrefKey.PENDING_DRAFTS_NOTIFICATION_IGNORE_LIST_MONTH;
    }

    private ArrayList<Post> getPostsNotInPendingDraftsIgnorePostIdList(ArrayList<Post> postSet, AppPrefs.DeletablePrefKey listToCheck) {
        ArrayList<Post> postsNotInIgnorePostList = new ArrayList<>();
        if (postSet != null) {
            ArrayList<Long> idList = AppPrefs.getPendingDraftsIgnorePostIdList(listToCheck);

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
