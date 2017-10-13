package org.wordpress.android.ui.uploads;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.SparseArray;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.ui.notifications.ShareAndDismissNotificationReceiver;
import org.wordpress.android.ui.posts.PostsListActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.SystemServiceFactory;
import org.wordpress.android.util.WPMeShortlinks;

import java.util.List;
import java.util.Random;

class PostUploadNotifier {
    private final Context mContext;
    private final UploadService mService;

    private final NotificationManager mNotificationManager;
    private final Notification.Builder mNotificationBuilder;

    // error notifications will remain visible until the user discards or acts upon them,
    // so we need to be able to map them to the Post that failed
    private static final SparseArray<Integer> sPostIdToErrorNotificationId = new SparseArray<>();

    // used to hold notification data for everything (only one outstanding foreground notification
    // for the live UploadService instance
    private static NotificationData sNotificationData;

    private class NotificationData {
        int notificationId;
        int totalMediaItems;
        int currentMediaItem;
        int totalPostItems;
        int totalPageItemsIncludedInPostCount;
        int currentPostItem;
    }

    PostUploadNotifier(Context context, UploadService service) {
        // Add the uploader to the notification bar
        mContext = context;
        mService = service;
        sNotificationData = new NotificationData();
        mNotificationManager = (NotificationManager) SystemServiceFactory.get(mContext,
                Context.NOTIFICATION_SERVICE);
        mNotificationBuilder = new Notification.Builder(mContext.getApplicationContext());
        mNotificationBuilder.setSmallIcon(android.R.drawable.stat_sys_upload);
    }

    private void updateForegroundNotification(@Nullable PostModel post) {
        updateNotificationBuilder(post);
        doNotify(sNotificationData.notificationId, mNotificationBuilder.build());
    }

    private void updateNotificationBuilder(@Nullable PostModel post) {
        // set the Notification's title and prepare the Notifications message text, i.e. "1/3 Posts, 4/17 media items"
        if (sNotificationData.totalMediaItems > 0 && sNotificationData.totalPostItems == 0) {
            // only media items are being uploaded
            // check if special case for ONE media item
            if (sNotificationData.totalMediaItems == 1) {
                mNotificationBuilder.setContentTitle(buildNotificationTitleForMedia());
                mNotificationBuilder.setContentText(buildNotificationSubtitleForMedia());
            } else {
                mNotificationBuilder.setContentTitle(buildNotificationTitleForMixedContent());
                mNotificationBuilder.setContentText(buildNotificationSubtitleForMedia());
            }

        } else if (sNotificationData.totalMediaItems == 0 && sNotificationData.totalPostItems > 0) {
            // only Post / Pages are being uploaded
            // check if special case for ONE Post
            if (sNotificationData.totalPostItems == 1) {
                mNotificationBuilder.setContentTitle(buildNotificationTitleForPost(post));
                mNotificationBuilder.setContentText(buildNotificationSubtitleForPost(post));
            } else {
                mNotificationBuilder.setContentTitle(buildNotificationTitleForMixedContent());
                mNotificationBuilder.setContentText(buildNotificationSubtitleForPosts());
            }
        } else {
            // mixed content (Post/Pages and media) is being uploaded
            mNotificationBuilder.setContentTitle(buildNotificationTitleForMixedContent());
            mNotificationBuilder.setContentText(buildNotificationSubtitleForMixedContent());
        }
    }

    private synchronized void startOrUpdateForegroundNotification(@Nullable PostModel post) {
        updateNotificationBuilder(post);
        if (sNotificationData.notificationId == 0) {
            sNotificationData.notificationId = (new Random()).nextInt();
            mService.startForeground(sNotificationData.notificationId, mNotificationBuilder.build());
        } else {
            // service was already started, let's just modify the notification
            doNotify(sNotificationData.notificationId, mNotificationBuilder.build());
        }
    }

    // Post could have initial media, or not (nulable)
    void addPostInfoToForegroundNotification(@NonNull PostModel post, @Nullable List<MediaModel> media) {
        sNotificationData.totalPostItems++;
        if (post.isPage()) {
            sNotificationData.totalPageItemsIncludedInPostCount++;
        }
        if (media != null) {
            addMediaInfoToForegroundNotification(media);
        }
        startOrUpdateForegroundNotification(post);
    }

    void addMediaInfoToForegroundNotification(@NonNull List<MediaModel> media) {
        sNotificationData.totalMediaItems += media.size();
        startOrUpdateForegroundNotification(null);
    }

    void addMediaInfoToForegroundNotification(@NonNull MediaModel media) {
        sNotificationData.totalMediaItems++;
        startOrUpdateForegroundNotification(null);
    }

    void updateNotificationIcon(PostModel post, Bitmap icon) {
        // TODO MEDIA reimplement or remove completely

//        NotificationData notificationData = sPostIdToNotificationData.get(post.getId());
//
//        if (icon != null) {
//            notificationData.latestIcon = icon;
//            mNotificationBuilder.setLargeIcon(notificationData.latestIcon);
//        }
//        doNotify(sPostIdToNotificationData.get(post.getId()).notificationId, mNotificationBuilder.build());
    }

    void incrementUploadedPostCountFromForegroundNotification(PostModel post) {
        sNotificationData.currentPostItem++;
        if (post.isPage()) {
            sNotificationData.totalPageItemsIncludedInPostCount--;
        }

        // update Notification now
        if (!removeNotificationAndStopForegroundServiceIfNoItemsInQueue()) {
            updateForegroundNotification(post);
        }
    }

    void incrementUploadedMediaCountFromProgressNotificationOrFinish() {
        sNotificationData.currentMediaItem++;

        if (!removeNotificationAndStopForegroundServiceIfNoItemsInQueue()) {
            // update Notification now
            updateForegroundNotification(null);
        }
    }

    boolean removeNotificationAndStopForegroundServiceIfNoItemsInQueue() {
        if (sNotificationData.currentPostItem == sNotificationData.totalPostItems
                && sNotificationData.currentMediaItem == sNotificationData.totalMediaItems) {
            mNotificationManager.cancel(sNotificationData.notificationId);
            // reset the notification id so a new one is generated next time the service is started
            sNotificationData.notificationId = 0;
            mService.stopForeground(true);
            return true;
        }
        return false;
    }

    void cancelErrorNotification(PostModel post) {
        Integer errorNotificationId = sPostIdToErrorNotificationId.get(post.getId());
        if (errorNotificationId != null && errorNotificationId != 0) {
            mNotificationManager.cancel(errorNotificationId);
            sPostIdToErrorNotificationId.remove(post.getId());
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
        notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getApplicationContext()
                        .getResources(),
                R.mipmap.app_icon));

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

        Integer errorNotificationId = sPostIdToErrorNotificationId.get(post.getId());
        if (errorNotificationId == null || errorNotificationId == 0) {
            errorNotificationId = sNotificationData.notificationId + (new Random()).nextInt();
            sPostIdToErrorNotificationId.put(post.getId(), errorNotificationId);
        }

//        NotificationData notificationData = sPostIdToNotificationData.get(post.getId());
//        if (notificationData.notificationErrorId == 0) {
//            notificationData.notificationErrorId = notificationData.notificationId + (new Random()).nextInt();
//        }
        doNotify(errorNotificationId, notificationBuilder.build());
    }

    void updateNotificationProgress(PostModel post, float progress) {

        //TODO MEDIA reimplement although most probably will REMOVE THIS COMPLETELY
//        NotificationData notificationData = sPostIdToNotificationData.get(post.getId());
//        if (notificationData.totalMediaItems == 0) {
//            return;
//        }
//
//        // Simple way to show progress of entire post upload
//        // Would be better if we could get total bytes for all media items.
//        double currentChunkProgress = (notificationData.itemProgressSize * progress);
//
//        if (notificationData.currentMediaItem > 1) {
//            currentChunkProgress += notificationData.itemProgressSize * (notificationData.currentMediaItem - 1);
//        }
//
//        mNotificationBuilder.setProgress(100, (int) Math.ceil(currentChunkProgress), false);
//        doNotify(sPostIdToNotificationData.get(post.getId()).notificationId, mNotificationBuilder.build());
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
        if (totalMediaItems <= 0) {
            totalMediaItems = 1;
        }

        sNotificationData.totalMediaItems+=totalMediaItems;

        // TODO MEDIA REIMPLEMENT OR DELETE THIS
//        NotificationData notificationData = sPostIdToNotificationData.get(post.getId());
//        notificationData.totalMediaItems = totalMediaItems;
//        notificationData.itemProgressSize = 100.0f / notificationData.totalMediaItems;
    }

    void setCurrentMediaItem(PostModel post, int currentItem) {
        // TODO MEDIA REIMPLEMENT OR DELETE THIS
    }

    private String buildNotificationTitleForPost(PostModel post) {
        String postTitle = TextUtils.isEmpty(post.getTitle()) ? mContext.getString(R.string.untitled) : post.getTitle();
        return String.format(mContext.getString(R.string.uploading_post), postTitle);
    }

    private String buildNotificationTitleForMedia() {
        return mContext.getString(R.string.uploading_post_media);
    }

    private String buildNotificationTitleForMixedContent() {
        return mContext.getString(R.string.uploading_title);
    }

    private String buildNotificationSubtitleForPost(PostModel post){
        String uploadingMessage = String.format(
                mContext.getString(R.string.uploading_subtitle_posts_only),
                sNotificationData.currentPostItem + 1,
                sNotificationData.totalPostItems,
                post.isPage() ? mContext.getString(R.string.page).toLowerCase()
                        : mContext.getString(R.string.post).toLowerCase()
        );
        return uploadingMessage;
    }

    private String buildNotificationSubtitleForPosts(){
        String pagesAndOrPosts = getPagesAndOrPostsString();
        String uploadingMessage = String.format(
                mContext.getString(R.string.uploading_subtitle_posts_only),
                sNotificationData.currentPostItem + 1,
                sNotificationData.totalPostItems,
                pagesAndOrPosts
        );
        return uploadingMessage;
    }

    private String getPagesAndOrPostsString() {
        String pagesAndOrPosts = "";
        if (sNotificationData.totalPageItemsIncludedInPostCount > 0 && sNotificationData.totalPostItems > 0
                && sNotificationData.totalPostItems > sNotificationData.totalPageItemsIncludedInPostCount) {
            // we have both pages and posts
            pagesAndOrPosts = mContext.getString(R.string.post).toLowerCase() + "/" +
                    mContext.getString(R.string.page).toLowerCase();
        } else if (sNotificationData.totalPageItemsIncludedInPostCount > 0) {
            // we have only pages
            pagesAndOrPosts = mContext.getString(R.string.page).toLowerCase();
        } else {
            // we have only posts
            pagesAndOrPosts = mContext.getString(R.string.post).toLowerCase();
        }
        return pagesAndOrPosts;
    }

    private String buildNotificationSubtitleForMedia(){
        String uploadingMessage = String.format(
                mContext.getString(R.string.uploading_subtitle_media_only),
                sNotificationData.currentMediaItem + 1,
                sNotificationData.totalMediaItems
        );
        return uploadingMessage;
    }

    private String buildNotificationSubtitleForMixedContent(){
        String uploadingMessage = String.format(
                mContext.getString(R.string.uploading_subtitle_mixed),
                sNotificationData.currentPostItem + 1,
                sNotificationData.totalPostItems,
                getPagesAndOrPostsString(),
                sNotificationData.currentMediaItem + 1,
                sNotificationData.totalMediaItems
        );
        return uploadingMessage;
    }
}
