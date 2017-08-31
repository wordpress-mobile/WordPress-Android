package org.wordpress.android.ui.uploads;

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
import org.wordpress.android.util.SystemServiceFactory;
import org.wordpress.android.util.WPMeShortlinks;

import java.util.Random;

class PostUploadNotifier {
    private final Context mContext;
    private final UploadService mService;

    private final NotificationManager mNotificationManager;
    private final Notification.Builder mNotificationBuilder;

    private static final SparseArray<NotificationData> sPostIdToNotificationData = new SparseArray<>();

    private class NotificationData {
        int notificationId;
        int notificationErrorId;
        int totalMediaItems;
        int currentMediaItem;
        float itemProgressSize;
        Bitmap latestIcon;
    }

    PostUploadNotifier(Context context, UploadService service) {
        // Add the uploader to the notification bar
        mContext = context;
        mService = service;
        mNotificationManager = (NotificationManager) SystemServiceFactory.get(mContext,
                Context.NOTIFICATION_SERVICE);
        mNotificationBuilder = new Notification.Builder(mContext.getApplicationContext());
        mNotificationBuilder.setSmallIcon(android.R.drawable.stat_sys_upload);
    }

    void showForegroundNotificationForPost(@NonNull PostModel post, String message) {
        mNotificationBuilder.setContentTitle(buildNotificationTitleForPost(post));
        if (message != null) {
            mNotificationBuilder.setContentText(message);
        }
        int notificationId = (new Random()).nextInt() + post.getLocalSiteId();

        NotificationData notificationData = new NotificationData();
        notificationData.notificationId = notificationId;
        sPostIdToNotificationData.put(post.getId(), notificationData);
        mService.startForeground(notificationId, mNotificationBuilder.build());
    }

    void updateNotificationIcon(PostModel post, Bitmap icon) {
        NotificationData notificationData = sPostIdToNotificationData.get(post.getId());

        if (icon != null) {
            notificationData.latestIcon = icon;
            mNotificationBuilder.setLargeIcon(notificationData.latestIcon);
        }
        doNotify(sPostIdToNotificationData.get(post.getId()).notificationId, mNotificationBuilder.build());
    }

    void cancelNotification(PostModel post) {
        NotificationData notificationData = sPostIdToNotificationData.get(post.getId());
        if (notificationData != null) {
            mNotificationManager.cancel(notificationData.notificationId);
        }
        mService.stopForeground(true);
    }

    void cancelErrorNotification(PostModel post) {
        NotificationData notificationData = sPostIdToNotificationData.get(post.getId());
        if (notificationData != null) {
            mNotificationManager.cancel(notificationData.notificationErrorId);
        }
    }

    void updateNotificationSuccess(PostModel post, SiteModel site, boolean isFirstTimePublish) {
        AppLog.d(AppLog.T.POSTS, "updateNotificationSuccess");

        // Get the shareableUrl
        String shareableUrl = WPMeShortlinks.getPostShortlink(site, post);
        if (shareableUrl == null && !TextUtils.isEmpty(post.getLink())) {
            shareableUrl = post.getLink();
        }

        // Notification builder
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(mContext.getApplicationContext());
        String notificationTitle;
        String notificationMessage;

        String postTitle = TextUtils.isEmpty(post.getTitle()) ? mContext.getString(R.string.untitled) : post.getTitle();

        if (PostStatus.DRAFT.equals(PostStatus.fromPost(post))) {
            notificationTitle = mContext.getString(R.string.draft_uploaded);
            notificationMessage = String.format(mContext.getString(R.string.post_draft_param), postTitle);
        } else if (PostStatus.SCHEDULED.equals(PostStatus.fromPost(post))) {
            notificationTitle = mContext.getString(post.isPage() ? R.string.page_scheduled : R.string.post_scheduled);
            notificationMessage = String.format(mContext.getString(R.string.post_scheduled_param), postTitle);
        } else {
            if (post.isPage()) {
                notificationTitle = mContext.getString(
                        isFirstTimePublish ? R.string.page_published : R.string.page_updated);
            } else {
                notificationTitle = mContext.getString(
                        isFirstTimePublish ? R.string.post_published : R.string.post_updated);
            }
            notificationMessage = String.format(mContext.getString(
                    isFirstTimePublish ? R.string.post_published_param : R.string.post_updated_param), postTitle);
        }

        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_upload_done);

        NotificationData notificationData = sPostIdToNotificationData.get(post.getId());
        if (notificationData == null || notificationData.latestIcon == null) {
            notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getApplicationContext()
                    .getResources(),
                    R.mipmap.app_icon));
        } else {
            notificationBuilder.setLargeIcon(notificationData.latestIcon);
        }
        notificationBuilder.setContentTitle(notificationTitle);
        notificationBuilder.setContentText(notificationMessage);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(notificationMessage));
        notificationBuilder.setAutoCancel(true);

        long notificationId = getNotificationIdForPost(post);
        // Tap notification intent (open the post list)
        Intent notificationIntent = new Intent(mContext, PostsListActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(WordPress.SITE, site);
        notificationIntent.putExtra(PostsListActivity.EXTRA_VIEW_PAGES, post.isPage());
        PendingIntent pendingIntentPost = PendingIntent.getActivity(mContext,
                (int)notificationId,
                notificationIntent, PendingIntent.FLAG_ONE_SHOT);
        notificationBuilder.setContentIntent(pendingIntentPost);

        // Share intent - started if the user tap the share link button - only if the link exist
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

    void updateNotificationError(PostModel post, SiteModel site, String errorMessage) {
        AppLog.d(AppLog.T.POSTS, "updateNotificationError: " + errorMessage);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(mContext.getApplicationContext());

        long notificationId = getNotificationIdForPost(post);
        // Tap notification intent (open the post list)
        Intent notificationIntent = new Intent(mContext, PostsListActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(WordPress.SITE, site);
        notificationIntent.putExtra(PostsListActivity.EXTRA_VIEW_PAGES, post.isPage());
        notificationIntent.putExtra(PostsListActivity.EXTRA_TARGET_POST_LOCAL_ID, post.getId());
        notificationIntent.setAction(String.valueOf(notificationId));

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
                (int)notificationId,
                notificationIntent, PendingIntent.FLAG_ONE_SHOT);

        notificationBuilder.setSmallIcon(android.R.drawable.stat_notify_error);

        String postTitle = TextUtils.isEmpty(post.getTitle()) ? mContext.getString(R.string.untitled) : post.getTitle();
        String notificationTitle = String.format(mContext.getString(R.string.upload_failed_param), postTitle);
        notificationBuilder.setContentTitle(notificationTitle);

        notificationBuilder.setContentText(errorMessage);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(errorMessage));
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setAutoCancel(true);

        NotificationData notificationData = sPostIdToNotificationData.get(post.getId());
        if (notificationData.notificationErrorId == 0) {
            notificationData.notificationErrorId = notificationData.notificationId + (new Random()).nextInt();
        }
        doNotify(notificationData.notificationErrorId, notificationBuilder.build());
    }

    void updateNotificationProgress(PostModel post, float progress) {
        NotificationData notificationData = sPostIdToNotificationData.get(post.getId());
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
        doNotify(sPostIdToNotificationData.get(post.getId()).notificationId, mNotificationBuilder.build());
    }

    private synchronized void doNotify(long id, Notification notification) {
        try {
            mNotificationManager.notify((int) id, notification);
        } catch (RuntimeException runtimeException) {
            CrashlyticsUtils.logException(runtimeException, AppLog.T.UTILS, "See issue #2858 / #3966");
            AppLog.d(AppLog.T.POSTS, "See issue #2858 / #3966; notify failed with:" + runtimeException);
        }
    }

    void setTotalMediaItems(PostModel post, int totalMediaItems) {
        NotificationData notificationData = sPostIdToNotificationData.get(post.getId());
        if (totalMediaItems <= 0) {
            totalMediaItems = 1;
        }

        notificationData.totalMediaItems = totalMediaItems;
        notificationData.itemProgressSize = 100.0f / notificationData.totalMediaItems;
    }

    void setCurrentMediaItem(PostModel post, int currentItem) {
        NotificationData notificationData = sPostIdToNotificationData.get(post.getId());
        notificationData.currentMediaItem = currentItem;

        mNotificationBuilder.setContentText(String.format(mContext.getString(R.string.uploading_total),
                currentItem, notificationData.totalMediaItems));
    }

    private String buildNotificationTitleForPost(PostModel post) {
        String postTitle = TextUtils.isEmpty(post.getTitle()) ? mContext.getString(R.string.untitled) : post.getTitle();
        return String.format(mContext.getString(R.string.uploading_post), postTitle);
    }
}
