package org.wordpress.android.ui.posts.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.SparseArray;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.ui.notifications.ShareAndDismissNotificationReceiver;
import org.wordpress.android.ui.posts.PostsListActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.SystemServiceFactory;
import org.wordpress.android.util.WPMeShortlinks;

import java.util.Random;

public class PostUploadNotifier {
    private final Context mContext;
    private final PostUploadService mService;

    private final NotificationManager mNotificationManager;
    private final Notification.Builder mNotificationBuilder;

    private final SparseArray<NotificationData> mPostIdToNotificationData = new SparseArray<>();

    private class NotificationData {
        int notificationId;
        int notificationErrorId;
        int totalMediaItems;
        int currentMediaItem;
        float itemProgressSize;
        Bitmap latestIcon;
    }

    public PostUploadNotifier(Context context, PostUploadService service) {
        // Add the uploader to the notification bar
        mContext = context;
        mService = service;
        mNotificationManager = (NotificationManager) SystemServiceFactory.get(mContext,
                Context.NOTIFICATION_SERVICE);
        mNotificationBuilder = new Notification.Builder(mContext.getApplicationContext());
        mNotificationBuilder.setSmallIcon(android.R.drawable.stat_sys_upload);
    }

    public void updateNotificationNewPost(PostModel post, String title, String message) {
        if (title != null) {
            mNotificationBuilder.setContentTitle(title);
        }
        if (message != null) {
            mNotificationBuilder.setContentText(message);
        }
        int notificationId = (new Random()).nextInt() + post.getLocalSiteId();

        NotificationData notificationData = new NotificationData();
        notificationData.notificationId = notificationId;
        mPostIdToNotificationData.put(post.getId(), notificationData);
        mService.startForeground(notificationId, mNotificationBuilder.build());
    }

    public void updateNotificationIcon(PostModel post, Bitmap icon) {
        NotificationData notificationData = mPostIdToNotificationData.get(post.getId());

        if (icon != null) {
            notificationData.latestIcon = icon;
            mNotificationBuilder.setLargeIcon(notificationData.latestIcon);
        }
        doNotify(mPostIdToNotificationData.get(post.getId()).notificationId, mNotificationBuilder.build());
    }

    public void cancelNotification(PostModel post) {
        mNotificationManager.cancel(mPostIdToNotificationData.get(post.getId()).notificationId);
    }

    public void updateNotificationSuccess(PostModel post, SiteModel site, boolean firstTimePublishEh) {
        AppLog.d(AppLog.T.POSTS, "updateNotificationSuccess");

        NotificationData notificationData = mPostIdToNotificationData.get(post.getId());

        // Get the shareableUrl
        String shareableUrl = WPMeShortlinks.getPostShortlink(site, post);
        if (shareableUrl == null && !TextUtils.isEmpty(post.getLink())) {
            shareableUrl = post.getLink();
        }

        // Notification builder
        Notification.Builder notificationBuilder = new Notification.Builder(mContext.getApplicationContext());
        String notificationTitle = (String) (post.pageEh() ? mContext.getResources().getText(R.string
                .page_published) : mContext.getResources().getText(R.string.post_published));
        if (!firstTimePublishEh) {
            notificationTitle = (String) (post.pageEh() ? mContext.getResources().getText(R.string
                    .page_updated) : mContext.getResources().getText(R.string.post_updated));
        }
        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_upload_done);
        if (notificationData.latestIcon == null) {
            notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getApplicationContext()
                    .getResources(),
                    R.mipmap.app_icon));
        } else {
            notificationBuilder.setLargeIcon(notificationData.latestIcon);
        }
        notificationBuilder.setContentTitle(notificationTitle);
        notificationBuilder.setContentText(post.getTitle());
        notificationBuilder.setAutoCancel(true);

        // Tap notification intent (open the post list)
        Intent notificationIntent = new Intent(mContext, PostsListActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(WordPress.SITE, site);
        notificationIntent.putExtra(PostsListActivity.EXTRA_VIEW_PAGES, post.pageEh());
        PendingIntent pendingIntentPost = PendingIntent.getActivity(mContext, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntentPost);

        // Share intent - started if the user tap the share link button - only if the link exist
        long notificationId = getNotificationIdForPost(post);
        if (shareableUrl != null && PostStatus.fromPost(post) == PostStatus.PUBLISHED) {
            Intent shareIntent = new Intent(mContext, ShareAndDismissNotificationReceiver.class);
            shareIntent.putExtra(ShareAndDismissNotificationReceiver.NOTIFICATION_ID_KEY, notificationId);
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareableUrl);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, shareIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuilder.addAction(R.drawable.ic_share_white_24dp, mContext.getString(R.string.share_action),
                    pendingIntent);
        }
        doNotify(notificationId, notificationBuilder.build());
    }

    private long getNotificationIdForPost(PostModel post) {
        long remotePostId = post.getRemotePostId();
        // We can't use the local table post id here because it can change between first post (local draft) to
        // first edit (post pulled from the server)
        return post.getLocalSiteId() + remotePostId;
    }

    public void updateNotificationError(PostModel post, SiteModel site, String errorMessage, boolean mediaErrorEh) {
        AppLog.d(AppLog.T.POSTS, "updateNotificationError: " + errorMessage);

        Notification.Builder notificationBuilder = new Notification.Builder(mContext.getApplicationContext());
        String postOrPage = (String) (post.pageEh() ? mContext.getResources().getText(R.string.page_id)
                : mContext.getResources().getText(R.string.post_id));
        Intent notificationIntent = new Intent(mContext, PostsListActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra(WordPress.SITE, site);
        notificationIntent.putExtra(PostsListActivity.EXTRA_VIEW_PAGES, post.pageEh());
        notificationIntent.putExtra(PostsListActivity.EXTRA_ERROR_MSG, errorMessage);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String errorText = mContext.getResources().getText(R.string.upload_failed).toString();
        if (mediaErrorEh) {
            errorText = mContext.getResources().getText(R.string.media) + " "
                    + mContext.getResources().getText(R.string.error);
        }

        notificationBuilder.setSmallIcon(android.R.drawable.stat_notify_error);
        notificationBuilder.setContentTitle((mediaErrorEh) ? errorText :
                mContext.getResources().getText(R.string.upload_failed));
        notificationBuilder.setContentText((mediaErrorEh) ? errorMessage : postOrPage + " " + errorText
                + ": " + errorMessage);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setAutoCancel(true);

        NotificationData notificationData = mPostIdToNotificationData.get(post.getId());
        if (notificationData.notificationErrorId == 0) {
            notificationData.notificationErrorId = notificationData.notificationId + (new Random()).nextInt();
        }
        doNotify(notificationData.notificationErrorId, notificationBuilder.build());
    }

    public void updateNotificationProgress(PostModel post, float progress) {
        NotificationData notificationData = mPostIdToNotificationData.get(post.getId());
        if (notificationData.totalMediaItems == 0) {
            return;
        }

        // Simple way to show progress of entire post upload
        // Would be better if we could get total bytes for all media items.
        double currentChunkProgress = (notificationData.itemProgressSize * progress);

        if (notificationData.currentMediaItem > 1) {
            currentChunkProgress += notificationData.itemProgressSize * (notificationData.currentMediaItem - 1);
        }

        mNotificationBuilder.setProgress(100, (int) Math.ceil(currentChunkProgress), false);
        doNotify(mPostIdToNotificationData.get(post.getId()).notificationId, mNotificationBuilder.build());
    }

    private synchronized void doNotify(long id, Notification notification) {
        try {
            mNotificationManager.notify((int) id, notification);
        } catch (RuntimeException runtimeException) {
            CrashlyticsUtils.logException(runtimeException, AppLog.T.UTILS, "See issue #2858 / #3966");
            AppLog.d(AppLog.T.POSTS, "See issue #2858 / #3966; notify failed with:" + runtimeException);
        }
    }

    public void setTotalMediaItems(PostModel post, int totalMediaItems) {
        NotificationData notificationData = mPostIdToNotificationData.get(post.getId());
        if (totalMediaItems <= 0) {
            totalMediaItems = 1;
        }

        notificationData.totalMediaItems = totalMediaItems;
        notificationData.itemProgressSize = 100.0f / notificationData.totalMediaItems;
    }

    public void setCurrentMediaItem(PostModel post, int currentItem) {
        NotificationData notificationData = mPostIdToNotificationData.get(post.getId());
        notificationData.currentMediaItem = currentItem;

        mNotificationBuilder.setContentText(String.format(mContext.getString(R.string.uploading_total),
                currentItem, notificationData.totalMediaItems));
    }
}
