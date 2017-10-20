package org.wordpress.android.ui.uploads;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

class PostUploadNotifier {
    private final Context mContext;
    private final UploadService mService;

    private final NotificationManager mNotificationManager;
    private final Notification.Builder mNotificationBuilder;

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
        final Map<Integer, Float> mediaItemToProgressMap = new HashMap<>();
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
        updateNotificationProgress();
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

    // Post could have initial media, or not (nullable)
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

    void addMediaInfoToForegroundNotification(@NonNull List<MediaModel> mediaList) {
        sNotificationData.totalMediaItems += mediaList.size();
        // setup progresses for each media item
        for (MediaModel media : mediaList) {
            setProgressForMediaItem(media.getId(), 0.0f);
        }
        startOrUpdateForegroundNotification(null);
    }

    void addMediaInfoToForegroundNotification(@NonNull MediaModel media) {
        sNotificationData.totalMediaItems++;
        // setup progress for media item
        setProgressForMediaItem(media.getId(), 0.0f);
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

    void incrementUploadedPostCountFromForegroundNotification(@NonNull PostModel post) {
        sNotificationData.currentPostItem++;
        if (post.isPage()) {
            sNotificationData.totalPageItemsIncludedInPostCount--;
        }

        // update Notification now
        if (!removeNotificationAndStopForegroundServiceIfNoItemsInQueue()) {
            updateForegroundNotification(post);
        }
    }

    void incrementUploadedMediaCountFromProgressNotificationOrFinish(int mediaId) {
        sNotificationData.currentMediaItem++;
        if (!removeNotificationAndStopForegroundServiceIfNoItemsInQueue()) {
            // update Notification now
            updateForegroundNotification(null);
        }
    }

    private boolean removeNotificationAndStopForegroundServiceIfNoItemsInQueue() {
        if (sNotificationData.currentPostItem == sNotificationData.totalPostItems
                && sNotificationData.currentMediaItem == sNotificationData.totalMediaItems) {
            mNotificationManager.cancel(sNotificationData.notificationId);
            // reset the notification id so a new one is generated next time the service is started
            sNotificationData.notificationId = 0;
            resetNotificationCounters();
            mService.stopForeground(true);
            return true;
        }
        return false;
    }

    private void resetNotificationCounters() {
        sNotificationData.currentPostItem = 0;
        sNotificationData.currentMediaItem = 0;
        sNotificationData.totalMediaItems = 0;
        sNotificationData.totalPostItems = 0;
        sNotificationData.totalPageItemsIncludedInPostCount = 0;
        sNotificationData.mediaItemToProgressMap.clear();
    }

    // cancels the error or success notification (only one of these exist per Post at any given
    // time
    void cancelFinalNotification(@NonNull PostModel post) {
        mNotificationManager.cancel((int)getNotificationIdForPost(post));
    }

    void updateNotificationSuccess(@NonNull PostModel post, @NonNull SiteModel site, boolean isFirstTimePublish) {
        if (!WordPress.sAppIsInTheBackground) {
            // only produce success notifications for the user if the app is in the background
            return;
        }
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
        notificationTitle = "\"" + postTitle + "\" ";
        notificationMessage = site.getName();

        if (PostStatus.DRAFT.equals(PostStatus.fromPost(post))) {
            notificationTitle += mContext.getString(R.string.draft_uploaded);
        } else if (PostStatus.SCHEDULED.equals(PostStatus.fromPost(post))) {
            notificationTitle += mContext.getString(post.isPage() ? R.string.page_scheduled : R.string.post_scheduled);
        } else {
            if (post.isPage()) {
                notificationTitle += mContext.getString(
                        isFirstTimePublish ? R.string.page_published : R.string.page_updated);
            } else {
                notificationTitle += mContext.getString(
                        isFirstTimePublish ? R.string.post_published : R.string.post_updated);
            }
        }

        notificationBuilder.setSmallIcon(R.drawable.ic_my_sites_24dp);
        notificationBuilder.setColor(mContext.getResources().getColor(R.color.blue_wordpress));

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

        // add draft Publish action for drafts
        if (PostStatus.fromPost(post) == PostStatus.DRAFT) {
            Intent publishIntent = UploadService.getUploadPostServiceIntent(mContext, post, isFirstTimePublish, notificationId, true);
            PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, publishIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuilder.addAction(R.drawable.ic_posts_grey_24dp, mContext.getString(R.string.post_publish_q_action),
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

    void updateNotificationError(@NonNull PostModel post, @NonNull SiteModel site, String errorMessage) {
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

        notificationBuilder.setSmallIcon(R.drawable.ic_my_sites_24dp);
        notificationBuilder.setColor(mContext.getResources().getColor(R.color.blue_wordpress));

        String postTitle = TextUtils.isEmpty(post.getTitle()) ? mContext.getString(R.string.untitled) : post.getTitle();
        String notificationTitle = String.format(mContext.getString(R.string.upload_failed_param), postTitle);

        // first we build a summary of what failed and what went OK, like this:
        // i.e. "1 post, 3 media files not uploaded (9 successfully uploaded)"
        String newErrorMessage = "";
        int postItemsNotUploaded = sNotificationData.totalPostItems > 0 ? sNotificationData.totalPostItems - (sNotificationData.currentPostItem-1) : 0;
        int mediaItemsNotUploaded = sNotificationData.totalMediaItems - sNotificationData.currentMediaItem;
        if (postItemsNotUploaded > 0) {
            newErrorMessage += postItemsNotUploaded + " " + getPagesAndOrPostsString();
            if (mediaItemsNotUploaded > 0) {
                newErrorMessage += ", ";
            }
        }

        if (mediaItemsNotUploaded > 0) {
            newErrorMessage += String.format(mContext.getString(R.string.media_files_not_uploaded), mediaItemsNotUploaded);
            if (mediaItemsNotUploaded < sNotificationData.currentMediaItem) {
                // some media items were uploaded successfully
                newErrorMessage += " " + String.format(mContext.getString(R.string.media_files_uploaded_succcessfully),
                        (sNotificationData.currentMediaItem - mediaItemsNotUploaded));
            }
        }

        // now append the detailed error message below
        if (newErrorMessage.length() > 0) {
            newErrorMessage += "\n" + errorMessage;
        } else {
            newErrorMessage = errorMessage;
        }

        notificationBuilder.setContentTitle(notificationTitle);
        notificationBuilder.setContentText(newErrorMessage);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(newErrorMessage));
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setAutoCancel(true);

        doNotify(notificationId, notificationBuilder.build());
    }

    void updateNotificationProgressForMedia(MediaModel media, float progress) {
        if (sNotificationData.totalMediaItems == 0 && sNotificationData.totalPostItems == 0) {
            return;
        }

        // only update if media item is in our map - this check is performed because
        // it could happen that a media item is already done uploading but we receive an upload
        // progress event from FluxC after that. We just need to avoid re-adding the item to the map.
        Float currentProgress = sNotificationData.mediaItemToProgressMap.get(media.getId());
        // also, only set updates in increments of 5% per media item to avoid lots of notification updates
        if (currentProgress != null && progress > (currentProgress + 0.05f)) {
            setProgressForMediaItem(media.getId(), progress);
            updateNotificationProgress();
        }
    }

    private void updateNotificationProgress() {
        if (sNotificationData.totalMediaItems == 0 && sNotificationData.totalPostItems == 0) {
            return;
        }

        mNotificationBuilder.setProgress(100, (int) Math.ceil(getCurrentOverallProgress() * 100), false);
        doNotify(sNotificationData.notificationId, mNotificationBuilder.build());
    }

    private void setProgressForMediaItem(int mediaId, float progress) {
        sNotificationData.mediaItemToProgressMap.put(mediaId, progress);
    }

    private float getCurrentOverallProgress() {
        int totalItemCount = sNotificationData.totalPostItems + sNotificationData.totalMediaItems;
        float currentMediaProgress = getCurrentMediaProgress();
        float overAllProgress;
        overAllProgress = sNotificationData.totalPostItems > 0 ?
                (sNotificationData.currentPostItem/sNotificationData.totalPostItems) * totalItemCount : 0;
        overAllProgress += sNotificationData.totalMediaItems > 0 ?
                (sNotificationData.currentMediaItem/sNotificationData.totalMediaItems) * totalItemCount : 0;
        overAllProgress += currentMediaProgress;
        return overAllProgress;
    }

    private float getCurrentMediaProgress() {
        float currentMediaProgress = 0.0f;
        int size = sNotificationData.mediaItemToProgressMap.values().size();
        for (Float itemProgress : sNotificationData.mediaItemToProgressMap.values()) {
            currentMediaProgress += (itemProgress / size);
        }
        return currentMediaProgress;
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
        sNotificationData.totalMediaItems+=totalMediaItems;
    }

    private String buildNotificationTitleForPost(PostModel post) {
        String postTitle = (post == null || TextUtils.isEmpty(post.getTitle())) ? mContext.getString(R.string.untitled) : post.getTitle();
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
                getCurrentPostItem() + 1,
                sNotificationData.totalPostItems,
                (post != null && post.isPage()) ? mContext.getString(R.string.page).toLowerCase()
                        : mContext.getString(R.string.post).toLowerCase()
        );
        return uploadingMessage;
    }

    private String buildNotificationSubtitleForPosts(){
        String pagesAndOrPosts = getPagesAndOrPostsString();
        String uploadingMessage = String.format(
                mContext.getString(R.string.uploading_subtitle_posts_only),
                getCurrentPostItem() + 1,
                sNotificationData.totalPostItems,
                pagesAndOrPosts
        );
        return uploadingMessage;
    }

    private String getPagesAndOrPostsString() {
        String pagesAndOrPosts;
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
                getCurrentMediaItem() + 1,
                sNotificationData.totalMediaItems
        );
        return uploadingMessage;
    }

    private String buildNotificationSubtitleForMixedContent(){
        String uploadingMessage = String.format(
                mContext.getString(R.string.uploading_subtitle_mixed),
                getCurrentPostItem() + 1,
                sNotificationData.totalPostItems,
                getPagesAndOrPostsString(),
                getCurrentMediaItem() + 1,
                sNotificationData.totalMediaItems
        );
        return uploadingMessage;
    }

    private int getCurrentPostItem() {
        int currentPostItem = sNotificationData.currentPostItem >= sNotificationData.totalPostItems ?
                sNotificationData.totalPostItems-1 : sNotificationData.currentPostItem;
        return currentPostItem;
    }

    private int getCurrentMediaItem() {
        int currentMediaItem = sNotificationData.currentMediaItem >= sNotificationData.totalMediaItems ?
                sNotificationData.totalMediaItems-1 : sNotificationData.currentMediaItem;
        return currentMediaItem;
    }
}
