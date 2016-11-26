package org.wordpress.android.ui.notifications.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.push.NativeNotificationsUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;

import static org.wordpress.android.push.GCMMessageService.GENERIC_LOCAL_NOTIFICATION_ID;


public class NotificationsPendingDraftsService extends Service {

    private boolean running = false;
    private static final int PENDING_DRAFTS_NOTIFICATION_ID = GENERIC_LOCAL_NOTIFICATION_ID + 1;

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
        MORE: “You’ve got 2 posts in drafts. Want to check them?”
        * */
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Post> draftPosts =  WordPress.wpDB.getDraftPostList(WordPress.getCurrentBlog().getLocalTableBlogId());
                ArrayList<Post> draftPostsOlderThan3Days = new ArrayList<>();
                if (draftPosts != null && draftPosts.size() > 0) {
                    //now check those that have been sitting there for more than 3 days now.
                    long one_day = 24 * 60 * 60 * 1000;
                    long now = System.currentTimeMillis();
                    long three_days_ago = now - (one_day * 3);
                    long daysInDraft = 0;
                    for (Post post : draftPosts) {
                        //FIXME: change LESS THAN for GREATER THAN !!!!!
                        if (post.getDateCreated() < three_days_ago) {
                            //daysInDraft = (now - post.getDateCreated()) / one_day;
                            daysInDraft = post.getDateCreated();
                            daysInDraft = now - post.getDateCreated();
                            daysInDraft = daysInDraft / one_day;
                            draftPostsOlderThan3Days.add(post);
                        }
                    }

                    //check the size and build the notification accordingly
                    if (draftPostsOlderThan3Days.size() == 1) {
                        buildSinglePendingDraftNotification(daysInDraft);
                    } else if (draftPostsOlderThan3Days.size() > 1) {
                        buildPendingDraftsNotification(draftPostsOlderThan3Days.size());
                    }
                }
            }
        }).start();
    }

    private void buildSinglePendingDraftNotification(long daysInDraft){
        NativeNotificationsUtils.showFinalMessageToUser(String.format(getString(R.string.pending_draft_one), daysInDraft),
                PENDING_DRAFTS_NOTIFICATION_ID, this);
        completed();
    }

    private void buildPendingDraftsNotification(int count) {
        NativeNotificationsUtils.showFinalMessageToUser(String.format(getString(R.string.pending_draft_more), count),
                PENDING_DRAFTS_NOTIFICATION_ID, this);
        completed();
    }

    private void completed() {
        AppLog.i(AppLog.T.NOTIFS, "notifications pending drafts service > completed");
        running = false;
        stopSelf();
    }
}
