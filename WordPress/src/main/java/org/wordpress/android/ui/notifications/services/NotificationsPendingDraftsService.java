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
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.push.NativeNotificationsUtils;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.android.volley.Request.Method.HEAD;

public class NotificationsPendingDraftsService extends Service {
    private boolean running = false;
    public static final int PENDING_DRAFTS_NOTIFICATION_ID = GCMMessageService.GENERIC_LOCAL_NOTIFICATION_ID + 1;
    public static final String GROUPED_POST_ID_LIST_EXTRA = "groupedPostIdList";
    public static final String POST_ID_EXTRA = "postId";
    public static final String IS_PAGE_EXTRA = "isPage";
    private static final long MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION = 24 * 60 * 60 * 1000; // 24 hours
    private static final long MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE = 30; // 30 days
    private static final long ONE_DAY = 24 * 60 * 60 * 1000;
    private SiteModel mSite;

    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;

    public static void checkPrefsAndStartService(Context context, SiteModel site) {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean shouldNotifyOfPendingDrafts = prefs.getBoolean(
                context.getString(R.string.pref_key_notification_pending_drafts), true);
        if (shouldNotifyOfPendingDrafts) {
            Intent intent = new Intent(context, NotificationsPendingDraftsService.class);
            intent.putExtra(WordPress.SITE, site);
            context.startService(intent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
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
            mSite = (SiteModel) intent.getSerializableExtra(WordPress.SITE);
            if (running) {
                return START_NOT_STICKY;
            }
            running = true;
            // TODO: we should use an IntentService and drop that Thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    performDraftsCheck();
                }
            }).start();
        }
        return START_NOT_STICKY;
    }

    // 1) check all “local” drafts, and check that they have been pending for more than 3 days.
    // 2) make notification if ONE draft and if more than ONE make another text
    // ONE: “You drafted 'Post title here' xxxx days ago but never published it.”
    // (we also have a generic message if we can't determine for how long a draft has been sitting there or it's been
    // more than 30 days)
    // MORE: “You drafted %d posts but never published them. Tap to check.”
    private void performDraftsCheck() {
        // TODO: Add a "getLocalDrafts" method to the Post Store
        List<PostModel> posts = mPostStore.getPostsForSite(mSite);
        ArrayList<PostModel> draftPosts = new ArrayList<>();
        for (PostModel post: posts) {
            if (post.isLocalDraft()) {
                draftPosts.add(post);
            }
        }
        if (draftPosts.size() > 0) {
            ArrayList<PostModel> draftPostsNotInIgnoreList;
            // now check those that have been sitting there for more than 3 days now.
            long now = System.currentTimeMillis();
            long three_days_ago = now - (ONE_DAY * 3);

            // only process posts that are not in the ignore list
            draftPostsNotInIgnoreList = getPostsNotInPendingDraftsIgnorePostIdList(draftPosts);

            if (draftPostsNotInIgnoreList.size() > 0) {
                ArrayList<PostModel> draftPostsOlderThan3Days = new ArrayList<>();

                for (PostModel post : draftPostsNotInIgnoreList) {
                    if (DateTimeUtils.timestampFromIso8601(post.getDateLocallyChanged()) < three_days_ago) {
                        draftPostsOlderThan3Days.add(post);
                    }
                }

                // check the size and build the notification accordingly
                long daysInDraft;
                if (draftPostsOlderThan3Days.size() == 1) {
                    PostModel post = draftPostsOlderThan3Days.get(0);

                    // only notify the user if the last time they have been notified about this particular
                    // post was at least MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION ago, but let's
                    // not do it constantly each time the app comes to the foreground
                    long dateLocallyChanged = DateTimeUtils.timestampFromIso8601Millis(post.getDateLocallyChanged());
                    long dateLastNotified = AppPrefs.getPendingDraftsLastNotificationDate(post);
                    if ((now - dateLastNotified) > MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION) {
                        daysInDraft = (now - dateLocallyChanged) / ONE_DAY;
                        boolean isPage = post.isPage();
                        AppPrefs.setPendingDraftsLastNotificationDate(post, now);
                        if (daysInDraft < MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE) {
                            buildSinglePendingDraftNotification(post.getTitle(), daysInDraft, post.getId(), isPage);
                        } else {
                            // if it's been more than MAX_DAYS_TO_SHOW_DAYS_IN_MESSAGE days, or if we don't
                            // know (i.e. value for lastUpdated is zero) then just show a generic message
                            buildSinglePendingDraftNotificationGeneric(post.getTitle(), post.getId(), isPage);
                        }
                    }
                } else if (draftPostsOlderThan3Days.size() > 1) {
                    long longestLivingDraft = 0;
                    boolean onlyPagesFound = true;
                    boolean doShowNotification = false;

                    for (PostModel post : draftPostsOlderThan3Days) {
                        long dateLastNotified = AppPrefs.getPendingDraftsLastNotificationDate(post);

                        // update each post dateLastNotified field to now
                        if ((now - dateLastNotified) > MINIMUM_ELAPSED_TIME_BEFORE_REPEATING_NOTIFICATION) {
                            if (dateLastNotified > longestLivingDraft) {
                                longestLivingDraft = dateLastNotified;
                                doShowNotification = true;
                                if (!post.isPage()) {
                                    onlyPagesFound = false;
                                }
                            }
                            AppPrefs.setPendingDraftsLastNotificationDate(post, now);

                        }
                    }

                    // if there was at least one notification that exceeded the minimum elapsed time to repeat the notif,
                    // then show the notification again
                    if (doShowNotification) {
                        buildPendingDraftsNotification(draftPostsOlderThan3Days, onlyPagesFound);
                    }
                }
            }
        }
        completed();
    }

    private String getPostTitle(String postTitle) {
        String title = postTitle;
        if (TextUtils.isEmpty(postTitle)) {
            title = "(" + getResources().getText(R.string.untitled) + ")";
        }
        return title;
    }

    private void buildSinglePendingDraftNotification(String postTitle, long daysInDraft, long postId, boolean isPage) {
        buildNotificationWithIntent(getResultIntentForOnePost(postId, isPage),
                String.format(getString(R.string.pending_draft_one), getPostTitle(postTitle), daysInDraft),
                postId, isPage);
    }

    private void buildSinglePendingDraftNotificationGeneric(String postTitle, long postId, boolean isPage) {
        buildNotificationWithIntent(getResultIntentForOnePost(postId, isPage),
                String.format(getString(R.string.pending_draft_one_generic), getPostTitle(postTitle)), postId, isPage);
    }

    private void buildPendingDraftsNotification(ArrayList<PostModel> posts, boolean showPages) {
        ArrayList<Long> postIdList = new ArrayList<>();
        for (PostModel onePost : posts) {
            postIdList.add((long) onePost.getId());
        }
        buildNotificationWithIntentForGroup(getResultIntentForMultiplePosts(posts, showPages),
                String.format(getString(R.string.pending_draft_more), posts.size()),
                postIdList, showPages);
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

    private PendingIntent getResultIntentForMultiplePosts(ArrayList<PostModel> posts, boolean isPage) {

        // convert posts list to csv id list
        ArrayList<Integer> postIdList = new ArrayList<>();
        for (PostModel post : posts) {
            postIdList.add(post.getId());
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

    private void buildNotificationWithIntentForGroup(PendingIntent intent, String message, ArrayList<Long> postIdList,
                                                     boolean isPage) {
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

    private void addOpenDraftActionForNotification(Context context, NotificationCompat.Builder builder,
                                                   ArrayList<Long> postIdList, boolean isPage) {
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

    private void addIgnoreActionForNotification(Context context, NotificationCompat.Builder builder,
                                                ArrayList<Long> postIdList, boolean isPage) {
        // Call processing service when user taps on IGNORE - we should remember this decision for this post
        Intent ignoreIntent = new Intent(context, NotificationsProcessingService.class);
        ignoreIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE,
                NotificationsProcessingService.ARG_ACTION_DRAFT_PENDING_IGNORE);
        setUniqueOrGroupedPostIdListExtraOnIntent(ignoreIntent, postIdList);
        ignoreIntent.putExtra(IS_PAGE_EXTRA, isPage);
        PendingIntent ignorePendingIntent =  PendingIntent.getService(context,
                2, ignoreIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.addAction(R.drawable.ic_close_white_24dp, getText(R.string.ignore),
                ignorePendingIntent);

    }

    private void addDismissActionForNotification(Context context, NotificationCompat.Builder builder,
                                                 ArrayList<Long> postIdList, boolean isPage) {
        // Call processing service when notification is dismissed
        Intent notificationDeletedIntent = new Intent(context, NotificationsProcessingService.class);
        notificationDeletedIntent.putExtra(NotificationsProcessingService.ARG_ACTION_TYPE,
                NotificationsProcessingService.ARG_ACTION_DRAFT_PENDING_DISMISS);
        setUniqueOrGroupedPostIdListExtraOnIntent(notificationDeletedIntent, postIdList);
        notificationDeletedIntent.putExtra(IS_PAGE_EXTRA, isPage);
        PendingIntent dismissPendingIntent =  PendingIntent.getService(context,
                3, notificationDeletedIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setDeleteIntent(dismissPendingIntent);
    }

    private ArrayList<PostModel> getPostsNotInPendingDraftsIgnorePostIdList(ArrayList<PostModel> postSet) {
        ArrayList<PostModel> postsNotInIgnorePostList = new ArrayList<>();
        if (postSet != null) {
            ArrayList<Long> idList = AppPrefs.getPendingDraftsIgnorePostIdList();

            for (PostModel onePost : postSet) {
                if (onePost != null) {
                    boolean foundId = false;
                    for (Long oneId : idList) {
                        if (onePost.getId() == oneId) {
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
