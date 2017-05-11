package org.wordpress.android.ui.posts.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
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
import org.wordpress.android.util.StringUtils;
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

    public void createNotificationForPost(@NonNull PostModel post, String message) {
        mNotificationBuilder.setContentTitle(buildNotificationTitleForPost(post));
        if (message != null) {
            mNotificationBuilder.setContentText(message);
        }
        int notificationId = (new Random()).nextInt() + post.getLocalSiteId();

        NotificationData notificationData = new NotificationData();
        notificationData.notificationId = notificationId;
        mPostIdToNotificationData.put(post.getId(), notificationData);
        mService.startForeground(notificationId, mNotificationBuilder.build());
    }

    public boolean isDisplayingNotificationForPost(@NonNull PostModel post) {
        return mPostIdToNotificationData.get(post.getId()) != null;
    }

    public void updateNotificationMessage(@NonNull PostModel post, String message) {
        NotificationData notificationData = mPostIdToNotificationData.get(post.getId());
        mNotificationBuilder.setContentText(StringUtils.notNullStr(message));
        doNotify(notificationData.notificationId, mNotificationBuilder.build());
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
        NotificationData notificationData = mPostIdToNotificationData.get(post.getId());
        if (notificationData != null) {
            mNotificationManager.cancel(notificationData.notificationId);
        }
    }

    public void updateNotificationSuccess(PostModel post, SiteModel site, boolean isFirstTimePublish) {
        AppLog.d(AppLog.T.POSTS, "updateNotificationSuccess");

        // Get the shareableUrl
        String shareableUrl = WPMeShortlinks.getPostShortlink(site, post);
        if (shareableUrl == null && !TextUtils.isEmpty(post.getLink())) {
            shareableUrl = post.getLink();
        }

        // Notification builder
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext.getApplicationContext());
        String notificationTitle = (String) (post.isPage() ? mContext.getResources().getText(R.string
                .page_published) : mContext.getResources().getText(R.string.post_published));
        if (!isFirstTimePublish) {
            notificationTitle = (String) (post.isPage() ? mContext.getResources().getText(R.string
                    .page_updated) : mContext.getResources().getText(R.string.post_updated));
        }
        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_upload_done);

        NotificationData notificationData = mPostIdToNotificationData.get(post.getId());
        if (notificationData == null || notificationData.latestIcon == null) {
            notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getApplicationContext()
                    .getResources(),
                    R.mipmap.app_icon));
        } else {
            notificationBuilder.setLargeIcon(notificationData.latestIcon);
        }
        String message = post.getTitle();
        notificationBuilder.setContentTitle(notificationTitle);
        notificationBuilder.setContentText(message);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        notificationBuilder.setAutoCancel(true);

        // Tap notification intent (open the post list)
        Intent notificationIntent = new Intent(mContext, PostsListActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(WordPress.SITE, site);
        notificationIntent.putExtra(PostsListActivity.EXTRA_VIEW_PAGES, post.isPage());
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
            notificationBuilder.addAction(R.drawable.ic_share_24dp, mContext.getString(R.string.share_action),
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

    public void updateNotificationError(PostModel post, SiteModel site, String errorMessage, boolean isMediaError) {
        AppLog.d(AppLog.T.POSTS, "updateNotificationError: " + errorMessage);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext.getApplicationContext());
        String postOrPage = (String) (post.isPage() ? mContext.getResources().getText(R.string.page_id)
                : mContext.getResources().getText(R.string.post_id));
        Intent notificationIntent = new Intent(mContext, PostsListActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra(WordPress.SITE, site);
        notificationIntent.putExtra(PostsListActivity.EXTRA_VIEW_PAGES, post.isPage());
        notificationIntent.putExtra(PostsListActivity.EXTRA_ERROR_MSG, errorMessage);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String errorText = mContext.getResources().getText(R.string.upload_failed).toString();
        if (isMediaError) {
            errorText = mContext.getResources().getText(R.string.media) + " "
                    + mContext.getResources().getText(R.string.error);
        }

        String message = (isMediaError) ? errorMessage : postOrPage + " " + errorText + ": " + errorMessage;
        notificationBuilder.setSmallIcon(android.R.drawable.stat_notify_error);
        notificationBuilder.setContentTitle((isMediaError) ? errorText :
                mContext.getResources().getText(R.string.upload_failed));
        notificationBuilder.setContentText(message);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
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

    private String buildNotificationTitleForPost(PostModel post) {
        String postTitle = TextUtils.isEmpty(post.getTitle()) ? mContext.getString(R.string.untitled) : post.getTitle();
        return String.format(mContext.getString(R.string.posting_post), postTitle);
    }
}
