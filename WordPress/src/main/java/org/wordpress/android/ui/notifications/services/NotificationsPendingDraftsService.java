package org.wordpress.android.ui.notifications.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.Post;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.utils.NotificationsActions;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;


public class NotificationsPendingDraftsService extends Service {

    private boolean running = false;
    private Context mContext;

    public static void startService(Context context) {
        if (context == null) {
            return;
        }
        mContext = context;
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
        //TODO:
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
                ArrayList<Post> draftPostsOlderThan3Days = new ArrayList<Post>();
                if (draftPosts != null && draftPosts.size() > 0) {
                    //now check those that have been sitting there for more than 3 days now.
                    long one_day = 24 * 60 * 60 * 1000;
                    long three_days_ago = System.currentTimeMillis() - (one_day * 3);
                    for (Post post : draftPosts) {
                        if (post.getDateCreated() < three_days_ago) {
                            draftPostsOlderThan3Days.add(post);
                        }
                    }

                    //check the size and build the notification accordingly
                    if (draftPostsOlderThan3Days.size() == 1) {
                        buildSinglePendingDraftNotification();
                    } else if (draftPostsOlderThan3Days.size() > 1) {
                        buildPendingDraftsNotification();
                    }
                }
            }
        }).start();
    }

    private NotificationCompat.Builder getBuilder() {
        return new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.notification_icon)
                .setColor(getResources().getColor(R.color.blue_wordpress))
                .setContentTitle(getString(R.string.app_name))
                .setAutoCancel(true);
    }

    private void showMessageToUser(String message, boolean intermediateMessage, int pushId) {
        NotificationCompat.Builder builder = getBuilder().setContentText(message).setTicker(message);
        if (!intermediateMessage) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }
        builder.setProgress(0, 0, intermediateMessage);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
        notificationManager.notify(pushId, builder.build());
    }


    private void completed() {
        AppLog.i(AppLog.T.NOTIFS, "notifications pending drafts service > completed");
        running = false;
        stopSelf();
    }
}
