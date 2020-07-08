package org.wordpress.android.ui.uploads;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;
import androidx.core.app.NotificationCompat;

import org.greenrobot.eventbus.EventBus;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.push.NotificationType;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.notifications.ShareAndDismissNotificationReceiver;
import org.wordpress.android.ui.notifications.SystemNotificationsTracker;
import org.wordpress.android.ui.pages.PagesActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PostUtils;
import org.wordpress.android.ui.posts.PostsListActivity;
import org.wordpress.android.ui.posts.PostsListActivityKt;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.SystemServiceFactory;
import org.wordpress.android.util.WPMeShortlinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.wordpress.android.push.NotificationsProcessingService.ARG_NOTIFICATION_TYPE;
import static org.wordpress.android.ui.pages.PagesActivityKt.EXTRA_PAGE_REMOTE_ID_KEY;

class PostUploadNotifier {
    private final Context mContext;
    private final UploadService mService;

    private final NotificationManager mNotificationManager;
    private final SystemNotificationsTracker mSystemNotificationsTracker;
    private final NotificationCompat.Builder mNotificationBuilder;

    private static final int BASE_MEDIA_ERROR_NOTIFICATION_ID = 72000;

    private enum PagesOrPostsType {
        POST,
        PAGE,
        POSTS,
        PAGES,
        PAGES_OR_POSTS
    }

    // used to hold notification data for everything (only one outstanding foreground notification
    // for the live UploadService instance
    private static NotificationData sNotificationData;

    private class NotificationData {
        int mNotificationId;
        int mTotalMediaItems;
        int mCurrentMediaItem;
        int mTotalPostItems;
        int mTotalPageItemsIncludedInPostCount;
        int mCurrentPostItem;
        final SparseArrayCompat<Float> mediaItemToProgressMap = new SparseArrayCompat<>();
        final List<PostImmutableModel> mUploadedPostsCounted = new ArrayList<>();
    }

    PostUploadNotifier(Context context, UploadService service, SystemNotificationsTracker systemNotificationsTracker) {
        // Add the uploader to the notification bar
        mContext = context;
        mService = service;
        mSystemNotificationsTracker = systemNotificationsTracker;
        sNotificationData = new NotificationData();
        mNotificationManager = (NotificationManager) SystemServiceFactory.get(mContext,
                                                                              Context.NOTIFICATION_SERVICE);
        mNotificationBuilder = new NotificationCompat.Builder(mContext.getApplicationContext(),
                context.getString(R.string.notification_channel_transient_id));
        mNotificationBuilder.setSmallIcon(android.R.drawable.stat_sys_upload)
                            .setColor(context.getResources().getColor(R.color.primary_50))
                            .setOnlyAlertOnce(true);
    }

    private void updateForegroundNotification(@Nullable PostImmutableModel post) {
        updateNotificationBuilder(post);
        updateNotificationProgress();
    }

    private void updateNotificationBuilder(@Nullable PostImmutableModel post) {
        // set the Notification's title and prepare the Notifications message text, i.e. "1/3 Posts, 4/17 media items"
        if (sNotificationData.mTotalMediaItems > 0 && sNotificationData.mTotalPostItems == 0) {
            // only media items are being uploaded
            // check if special case for ONE media item
            if (sNotificationData.mTotalMediaItems == 1) {
                mNotificationBuilder.setContentTitle(buildNotificationTitleForMedia());
                mNotificationBuilder.setContentText(buildNotificationSubtitleForMedia());
            } else {
                mNotificationBuilder.setContentTitle(buildNotificationTitleForMixedContent());
                mNotificationBuilder.setContentText(buildNotificationSubtitleForMedia());
            }
        } else if (sNotificationData.mTotalMediaItems == 0 && sNotificationData.mTotalPostItems > 0) {
            // only Post / Pages are being uploaded
            // check if special case for ONE Post
            if (sNotificationData.mTotalPostItems == 1) {
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

    private synchronized void startOrUpdateForegroundNotification(@Nullable PostImmutableModel post) {
        updateNotificationBuilder(post);
        if (sNotificationData.mNotificationId == 0) {
            sNotificationData.mNotificationId = (new Random()).nextInt();
            mService.startForeground(sNotificationData.mNotificationId, mNotificationBuilder.build());
        } else {
            // service was already started, let's just modify the notification
            doNotify(sNotificationData.mNotificationId, mNotificationBuilder.build(), null);
        }
    }

    void removePostInfoFromForegroundNotificationData(@NonNull PostImmutableModel post,
                                                      @Nullable List<MediaModel> media) {
        if (sNotificationData.mTotalPostItems > 0) {
            sNotificationData.mTotalPostItems--;
            if (post.isPage()) {
                sNotificationData.mTotalPageItemsIncludedInPostCount--;
            }
        }
        if (media != null) {
            removeMediaInfoFromForegroundNotification(media);
        }
    }

    // Post could have initial media, or not (nullable)
    void addPostInfoToForegroundNotification(@NonNull PostImmutableModel post, @Nullable List<MediaModel> media) {
        sNotificationData.mTotalPostItems++;
        if (post.isPage()) {
            sNotificationData.mTotalPageItemsIncludedInPostCount++;
        }
        if (media != null) {
            addMediaInfoToForegroundNotification(media);
        }
        startOrUpdateForegroundNotification(post);
    }

    void removePostInfoFromForegroundNotification(@NonNull PostImmutableModel post, @Nullable List<MediaModel> media) {
        removePostInfoFromForegroundNotificationData(post, media);
        startOrUpdateForegroundNotification(post);
    }

    void removeMediaInfoFromForegroundNotification(@NonNull List<MediaModel> mediaList) {
        if (sNotificationData.mTotalMediaItems >= mediaList.size()) {
            sNotificationData.mTotalMediaItems -= mediaList.size();
            // update Notification now
            updateForegroundNotification(null);
        }
    }

    void removeOneMediaItemInfoFromForegroundNotification() {
        if (sNotificationData.mTotalMediaItems >= 1) {
            sNotificationData.mTotalMediaItems--;
            // update Notification now
            updateForegroundNotification(null);
        }
    }

    void addMediaInfoToForegroundNotification(@NonNull List<MediaModel> mediaList) {
        sNotificationData.mTotalMediaItems += mediaList.size();
        // setup progresses for each media item
        for (MediaModel media : mediaList) {
            setProgressForMediaItem(media.getId(), 0.0f);
        }
        startOrUpdateForegroundNotification(null);
    }

    void addMediaInfoToForegroundNotification(@NonNull MediaModel media) {
        sNotificationData.mTotalMediaItems++;
        // setup progress for media item
        setProgressForMediaItem(media.getId(), 0.0f);
        startOrUpdateForegroundNotification(null);
    }

    void incrementUploadedPostCountFromForegroundNotification(@NonNull PostImmutableModel post) {
        incrementUploadedPostCountFromForegroundNotification(post, false);
    }

    void incrementUploadedPostCountFromForegroundNotification(@NonNull PostImmutableModel post, boolean force) {
        // first we need to check that we only count this post once as "ended" (either successfully or with error)
        // for every error we get. We'll then try to increment the Post count as it's been cancelled/failed because the
        // related media was cancelled or has failed too (i.e. we can't upload a Post with failed media, therefore
        // it needs to be cancelled).
        if (!force && isPostAlreadyInPostCount(post)) {
            return;
        } else {
            addPostToPostCount(post);
        }
        sNotificationData.mCurrentPostItem++;

        // update Notification now
        if (!removeNotificationAndStopForegroundServiceIfNoItemsInQueue()) {
            updateForegroundNotification(post);
        }
    }

    void incrementUploadedMediaCountFromProgressNotification(int mediaId) {
        sNotificationData.mCurrentMediaItem++;
        if (!removeNotificationAndStopForegroundServiceIfNoItemsInQueue()) {
            // update Notification now
            updateForegroundNotification(null);
        }
    }

    private boolean removeNotificationAndStopForegroundServiceIfNoItemsInQueue() {
        if (sNotificationData.mCurrentPostItem == sNotificationData.mTotalPostItems
            && sNotificationData.mCurrentMediaItem == sNotificationData.mTotalMediaItems) {
            mNotificationManager.cancel(sNotificationData.mNotificationId);
            // reset the notification id so a new one is generated next time the service is started
            sNotificationData.mNotificationId = 0;
            resetNotificationCounters();
            mService.stopForeground(true);
            return true;
        }
        return false;
    }

    private void resetNotificationCounters() {
        sNotificationData.mCurrentPostItem = 0;
        sNotificationData.mCurrentMediaItem = 0;
        sNotificationData.mTotalMediaItems = 0;
        sNotificationData.mTotalPostItems = 0;
        sNotificationData.mTotalPageItemsIncludedInPostCount = 0;
        sNotificationData.mediaItemToProgressMap.clear();
        sNotificationData.mUploadedPostsCounted.clear();
    }

    private boolean isPostAlreadyInPostCount(@NonNull PostImmutableModel post) {
        for (PostImmutableModel onePost : sNotificationData.mUploadedPostsCounted) {
            if (onePost.getId() == post.getId()) {
                return true;
            }
        }
        return false;
    }

    private void addPostToPostCount(@NonNull PostImmutableModel post) {
        sNotificationData.mUploadedPostsCounted.add(post);
    }

    // cancels the error or success notification (only one of these exist per Post at any given
    // time
    static void cancelFinalNotification(Context context, @NonNull PostImmutableModel post) {
        if (context != null) {
            NotificationManager notificationManager =
                    (NotificationManager) SystemServiceFactory.get(context, Context.NOTIFICATION_SERVICE);
            notificationManager.cancel((int) getNotificationIdForPost(post));
        }
    }

    static void cancelFinalNotificationForMedia(Context context, @NonNull SiteModel site) {
        if (context != null) {
            NotificationManager notificationManager =
                    (NotificationManager) SystemServiceFactory.get(context, Context.NOTIFICATION_SERVICE);
            notificationManager.cancel((int) getNotificationIdForMedia(site));
        }
    }

    void updateNotificationSuccessForPost(@NonNull PostImmutableModel post, @NonNull SiteModel site,
                                          boolean isFirstTimePublish) {
        if (!WordPress.sAppIsInTheBackground) {
            // only produce success notifications for the user if the app is in the background
            return;
        }
        AppLog.d(AppLog.T.POSTS, "updateNotificationSuccessForPost");

        // Get the shareableUrl
        String shareableUrl = WPMeShortlinks.getPostShortlink(site, post);
        if (shareableUrl == null && !TextUtils.isEmpty(post.getLink())) {
            shareableUrl = post.getLink();
        }

        // Notification builder
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(mContext.getApplicationContext(),
                        mContext.getString(R.string.notification_channel_normal_id));
        String notificationTitle;
        String notificationMessage;

        String postTitle = TextUtils.isEmpty(post.getTitle()) ? mContext.getString(R.string.untitled) : post.getTitle();
        notificationTitle = "\"" + postTitle + "\" ";
        notificationMessage = site.getName();

        PostStatus status = PostStatus.fromPost(post);
        switch (status) {
            case DRAFT:
                notificationTitle += mContext.getString(R.string.draft_uploaded);
                break;
            case SCHEDULED:
                notificationTitle += mContext.getString(
                        post.isPage() ? R.string.page_scheduled : R.string.post_scheduled);
                break;
            case PUBLISHED:
                if (post.isPage()) {
                    notificationTitle += mContext.getString(
                            isFirstTimePublish ? R.string.page_published : R.string.page_updated);
                } else {
                    notificationTitle += mContext.getString(
                            isFirstTimePublish ? R.string.post_published : R.string.post_updated);
                }
                break;
            default:
                notificationTitle += mContext.getString(post.isPage() ? R.string.page_updated : R.string.post_updated);
                break;
        }

        notificationBuilder.setSmallIcon(R.drawable.ic_my_sites_white_24dp);
        notificationBuilder.setColor(mContext.getResources().getColor(R.color.primary_50));

        notificationBuilder.setContentTitle(notificationTitle);
        notificationBuilder.setContentText(notificationMessage);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(notificationMessage));
        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.setAutoCancel(true);
        long notificationId = getNotificationIdForPost(post);

        NotificationType notificationType = NotificationType.POST_UPLOAD_SUCCESS;
        notificationBuilder.setDeleteIntent(NotificationsProcessingService
                .getPendingIntentForNotificationDismiss(mContext, (int) notificationId,
                        notificationType));

        Intent notificationIntent = getNotificationIntent(post, site);
        notificationIntent.putExtra(ARG_NOTIFICATION_TYPE, notificationType);

        PendingIntent pendingIntentPost = PendingIntent.getActivity(mContext,
                                                                    (int) notificationId,
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
            notificationBuilder.addAction(R.drawable.ic_share_white_24dp, mContext.getString(R.string.share_action),
                                          pendingIntent);
        }

        // add draft Publish action for drafts
        if (PostStatus.fromPost(post) == PostStatus.DRAFT || PostStatus.fromPost(post) == PostStatus.PENDING) {
            Intent publishIntent = UploadService.getPublishPostServiceIntent(mContext, post, isFirstTimePublish);
            PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, publishIntent,
                                                                   PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuilder.addAction(R.drawable.ic_posts_white_24dp, mContext.getString(R.string.button_publish),
                                          pendingIntent);
        }

        doNotify(notificationId, notificationBuilder.build(), notificationType);
    }

    void updateNotificationSuccessForMedia(@NonNull List<MediaModel> mediaList, @NonNull SiteModel site) {
        // show the snackbar
        if (mediaList != null && !mediaList.isEmpty()) {
            String snackbarMessage = buildSnackbarSuccessMessageForMedia(mediaList.size());
            EventBus.getDefault().postSticky(new UploadService.UploadMediaSuccessEvent(mediaList, snackbarMessage));
        }

        if (!WordPress.sAppIsInTheBackground) {
            // only produce success notifications for the user if the app is in the background
            return;
        }
        AppLog.d(AppLog.T.MEDIA, "updateNotificationSuccessForMedia");

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(mContext.getApplicationContext(),
                        mContext.getString(R.string.notification_channel_normal_id));

        long notificationId = getNotificationIdForMedia(site);
        // Tap notification intent (open the media browser)
        Intent notificationIntent = new Intent(mContext, MediaBrowserActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(WordPress.SITE, site);
        notificationIntent.setAction(String.valueOf(notificationId));
        NotificationType notificationType = NotificationType.MEDIA_UPLOAD_SUCCESS;
        notificationIntent.putExtra(ARG_NOTIFICATION_TYPE, notificationType);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
                                                                (int) notificationId,
                                                                notificationIntent, PendingIntent.FLAG_ONE_SHOT);

        notificationBuilder.setSmallIcon(R.drawable.ic_my_sites_white_24dp);
        notificationBuilder.setColor(mContext.getResources().getColor(R.color.primary_50));

        String notificationTitle = buildSuccessMessageForMedia(mediaList.size());
        String notificationMessage =
                TextUtils.isEmpty(site.getName()) ? mContext.getString(R.string.untitled) : site.getName();

        notificationBuilder.setContentTitle(notificationTitle);
        notificationBuilder.setContentText(notificationMessage);
        // notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(newSuccessMessage));
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setDeleteIntent(NotificationsProcessingService
                .getPendingIntentForNotificationDismiss(mContext, (int) notificationId,
                        notificationType));

        // Add WRITE POST action - only if there is media we can insert in the Post
        if (mediaList != null && !mediaList.isEmpty()) {
            ArrayList<MediaModel> mediaToIncludeInPost = new ArrayList<>(mediaList);

            Intent writePostIntent = new Intent(mContext, EditPostActivity.class);
            writePostIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            writePostIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            writePostIntent.putExtra(WordPress.SITE, site);
            writePostIntent.putExtra(EditPostActivity.EXTRA_IS_PAGE, false);
            writePostIntent.putExtra(EditPostActivity.EXTRA_INSERT_MEDIA, mediaToIncludeInPost);
            writePostIntent.setAction(String.valueOf(notificationId));

            PendingIntent actionPendingIntent =
                    PendingIntent.getActivity(mContext, RequestCodes.EDIT_POST, writePostIntent,
                                              PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuilder.addAction(0, mContext.getString(R.string.media_files_uploaded_write_post),
                                          actionPendingIntent);
        }

        doNotify(notificationId, notificationBuilder.build(), notificationType);
    }

    public static long getNotificationIdForPost(PostImmutableModel post) {
        long postIdToUse = post.getRemotePostId();
        if (post.isLocalDraft()) {
            postIdToUse = post.getId();
            // Note: local drafts just don't have a remote post Id set yet, so they are all 0. Because of this, we only
            // use the local id for local drafts.
            // there could be the case where a local draft on the device, and a remote post, can get the same ID,
            // since the ID is returned from 2 different sources. In theory this is true, but however the chances are
            // too low so it seems it should work in practically 100% of times.
        }
        // We can't use the local table post id here because it can change between first post (local draft) to
        // first edit (post pulled from the server)
        // but, if this is a local draft, we don't have a choice but to use the local id.
        // otherwise, remote ID is always 0 for any local draft, and this means we'd be providing the same
        // notificationId for 2 different local drafts for the same site, with this ending in the latest notification
        // "stepping" on the first one (the user would always get to see the latest failed local draft, but wouldn't get
        // a notice about the previous ones).
        return post.getLocalSiteId() + postIdToUse;
    }

    public static long getNotificationIdForMedia(SiteModel site) {
        if (site != null) {
            return BASE_MEDIA_ERROR_NOTIFICATION_ID + site.getId();
        } else {
            return BASE_MEDIA_ERROR_NOTIFICATION_ID;
        }
    }

    /*
     * This method will create an error notification with the description of the *final state* of the queue
     * for this Post (i.e. how many media items have been uploaded successfully and how many failed, as well
     * as the information for the Post itself if we couldn't upload it).
     *
     * In order to give the user a description of the *current state* of failed media items, you can pass a value
     * other than zero (0) in overrideMediaNotUploadedCount and this value will be shown instead.
     */
    void updateNotificationErrorForPost(@NonNull PostModel post, @NonNull SiteModel site, String errorMessage,
                                        int overrideMediaNotUploadedCount) {
        AppLog.d(AppLog.T.POSTS, "updateNotificationErrorForPost: " + errorMessage);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(mContext.getApplicationContext(),
                        mContext.getString(R.string.notification_channel_normal_id));

        long notificationId = getNotificationIdForPost(post);
        Intent notificationIntent = getNotificationIntent(post, site);
        notificationIntent.setAction(String.valueOf(notificationId));
        NotificationType notificationType = NotificationType.POST_UPLOAD_ERROR;
        notificationIntent.putExtra(ARG_NOTIFICATION_TYPE, notificationType);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
                                                                (int) notificationId,
                                                                notificationIntent, PendingIntent.FLAG_ONE_SHOT);

        notificationBuilder.setSmallIcon(android.R.drawable.stat_notify_error);

        String postTitle = TextUtils.isEmpty(post.getTitle()) ? mContext.getString(R.string.untitled) : post.getTitle();
        String notificationTitle = String.format(mContext.getString(R.string.upload_failed_param), postTitle);

        String newErrorMessage = buildErrorMessageMixed(overrideMediaNotUploadedCount);
        String snackbarMessage = buildSnackbarErrorMessage(newErrorMessage, errorMessage);

        notificationBuilder.setContentTitle(notificationTitle);
        notificationBuilder.setContentText(newErrorMessage);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(newErrorMessage));
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.setDeleteIntent(NotificationsProcessingService
                .getPendingIntentForNotificationDismiss(mContext, (int) notificationId,
                        notificationType));

        // Add RETRY action - only available on Aztec
        if (AppPrefs.isAztecEditorEnabled()) {
            Intent publishIntent = UploadService.getRetryUploadServiceIntent(mContext, post,
                                                                            PostUtils.isFirstTimePublish(post));
            PendingIntent actionPendingIntent = PendingIntent.getService(mContext, 0, publishIntent,
                                                                         PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuilder.addAction(0, mContext.getString(R.string.retry),
                                          actionPendingIntent)
                               .setColor(mContext.getResources().getColor(R.color.accent));
        }

        EventBus.getDefault().postSticky(new UploadService.UploadErrorEvent(post, snackbarMessage));

        doNotify(notificationId, notificationBuilder.build(), notificationType);
    }

    @NonNull
    private Intent getNotificationIntent(@NonNull PostImmutableModel post, @NonNull SiteModel site) {
        // Tap notification intent (open the post/page list)
        Intent notificationIntent;
        if (post.isPage()) {
            notificationIntent = new Intent(mContext, PagesActivity.class);
            notificationIntent.putExtra(EXTRA_PAGE_REMOTE_ID_KEY, post.getRemotePostId());
        } else {
            notificationIntent = new Intent(mContext, PostsListActivity.class);
            notificationIntent.putExtra(PostsListActivityKt.EXTRA_TARGET_POST_LOCAL_ID, post.getId());
        }

        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(WordPress.SITE, site);
        return notificationIntent;
    }

    void updateNotificationErrorForMedia(@NonNull List<MediaModel> mediaList, @NonNull SiteModel site,
                                         String errorMessage) {
        AppLog.d(AppLog.T.MEDIA, "updateNotificationErrorForMedia: " + errorMessage);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(mContext.getApplicationContext(),
                        mContext.getString(R.string.notification_channel_normal_id));

        long notificationId = getNotificationIdForMedia(site);
        // Tap notification intent (open the media browser)
        Intent notificationIntent = new Intent(mContext, MediaBrowserActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(WordPress.SITE, site);
        notificationIntent.setAction(String.valueOf(notificationId));
        NotificationType notificationType = NotificationType.MEDIA_UPLOAD_ERROR;
        notificationIntent.putExtra(ARG_NOTIFICATION_TYPE, notificationType);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
                                                                (int) notificationId,
                                                                notificationIntent, PendingIntent.FLAG_ONE_SHOT);

        notificationBuilder.setSmallIcon(android.R.drawable.stat_notify_error);

        String siteName = TextUtils.isEmpty(site.getName()) ? mContext.getString(R.string.untitled) : site.getName();
        String notificationTitle = String.format(mContext.getString(R.string.upload_failed_param), siteName);

        String newErrorMessage = buildErrorMessageForMedia(mediaList.size());
        String snackbarMessage = buildSnackbarErrorMessage(newErrorMessage, errorMessage);

        notificationBuilder.setContentTitle(notificationTitle);
        notificationBuilder.setContentText(newErrorMessage);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(newErrorMessage));
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.setDeleteIntent(NotificationsProcessingService
                .getPendingIntentForNotificationDismiss(mContext, (int) notificationId,
                        notificationType));

        // Add RETRY action - only if there is media to retry
        if (mediaList != null && !mediaList.isEmpty()) {
            ArrayList<MediaModel> mediaListToRetry = new ArrayList<>();
            mediaListToRetry.addAll(mediaList);
            Intent publishIntent = UploadService.getUploadMediaServiceIntent(mContext, mediaListToRetry, true);
            PendingIntent actionPendingIntent = PendingIntent.getService(mContext, 1, publishIntent,
                                                                         PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuilder.addAction(0, mContext.getString(R.string.retry),
                                          actionPendingIntent)
                               .setColor(mContext.getResources().getColor(R.color.accent));
        }

        EventBus.getDefault().postSticky(new UploadService.UploadErrorEvent(mediaList, snackbarMessage));
        doNotify(notificationId, notificationBuilder.build(), notificationType);
    }

    private String buildErrorMessageMixed(int overrideMediaNotUploadedCount) {
        // first we build a summary of what failed and what went OK, like this:
        // i.e. "1 post, with 3 media files not uploaded (9 successfully uploaded)"
        String newErrorMessage = "";
        int postItemsNotUploaded = sNotificationData.mTotalPostItems > 0
                ? sNotificationData.mTotalPostItems - getCurrentPostItem() : 0;
        int mediaItemsNotUploaded = overrideMediaNotUploadedCount > 0
                ? overrideMediaNotUploadedCount : sNotificationData.mTotalMediaItems - getCurrentMediaItem();

        if (postItemsNotUploaded > 0 && mediaItemsNotUploaded > 0) {
            switch (getPagesAndOrPostsType(postItemsNotUploaded)) {
                case POST:
                    newErrorMessage = (mediaItemsNotUploaded == 1
                        ? mContext.getString(R.string.media_file_post_singular_mixed_not_uploaded_one_file)
                        : String.format(
                                mContext.getString(R.string.media_file_post_singular_mixed_not_uploaded_files_plural),
                        mediaItemsNotUploaded));
                    break;
                case PAGE:
                    newErrorMessage = (mediaItemsNotUploaded == 1
                        ? mContext.getString(R.string.media_file_page_singular_mixed_not_uploaded_one_file)
                        : String.format(
                                mContext.getString(R.string.media_file_page_singular_mixed_not_uploaded_files_plural),
                        mediaItemsNotUploaded));
                    break;
                case PAGES:
                    newErrorMessage = (mediaItemsNotUploaded == 1
                            ? String.format(
                                mContext.getString(R.string.media_file_pages_plural_mixed_not_uploaded_one_file),
                                postItemsNotUploaded)
                            : String.format(
                                mContext.getString(R.string.media_file_pages_plural_mixed_not_uploaded_files_plural),
                                postItemsNotUploaded,
                                mediaItemsNotUploaded));
                    break;
                case PAGES_OR_POSTS:
                    newErrorMessage = (mediaItemsNotUploaded == 1
                    ? String.format(
                            mContext.getString(R.string.media_file_pages_and_posts_mixed_not_uploaded_one_file),
                            postItemsNotUploaded)
                    : String.format(
                            mContext.getString(R.string.media_file_pages_and_posts_mixed_not_uploaded_files_plural),
                            postItemsNotUploaded,
                            mediaItemsNotUploaded));
                    break;
                case POSTS:
                default:
                    newErrorMessage = (mediaItemsNotUploaded == 1
                    ? String.format(
                            mContext.getString(R.string.media_file_posts_plural_mixed_not_uploaded_one_file),
                            postItemsNotUploaded)
                    : String.format(
                            mContext.getString(R.string.media_file_posts_plural_mixed_not_uploaded_files_plural),
                            postItemsNotUploaded,
                            mediaItemsNotUploaded));
                    break;
            }
        } else if (postItemsNotUploaded > 0) {
            switch (getPagesAndOrPostsType(postItemsNotUploaded)) {
                case POST:
                    newErrorMessage = mContext.getString(R.string.media_file_post_singular_only_not_uploaded);
                    break;
                case PAGE:
                    newErrorMessage = mContext.getString(R.string.media_file_page_singular_only_not_uploaded);
                    break;
                case PAGES:
                    newErrorMessage = String.format(
                            mContext.getString(R.string.media_file_pages_plural_only_not_uploaded),
                            postItemsNotUploaded);
                    break;
                case PAGES_OR_POSTS:
                    newErrorMessage = String.format(
                            mContext.getString(R.string.media_file_pages_and_posts_only_not_uploaded),
                            postItemsNotUploaded);
                    break;
                case POSTS:
                default:
                    newErrorMessage = String.format(
                            mContext.getString(R.string.media_file_posts_plural_only_not_uploaded),
                            postItemsNotUploaded);
                    break;
            }
        } else if (mediaItemsNotUploaded > 0) {
            if (mediaItemsNotUploaded == 1) {
                newErrorMessage = mContext.getString(R.string.media_file_not_uploaded);
            } else {
                newErrorMessage =
                        String.format(mContext.getString(R.string.media_files_not_uploaded), mediaItemsNotUploaded);
            }
        }

        if (mediaItemsNotUploaded > 0
            && (getCurrentMediaItem()) > 0) {
            // some media items were uploaded successfully
            newErrorMessage += String.format(mContext.getString(R.string.media_files_uploaded_successfully),
                                             sNotificationData.mCurrentMediaItem);
        }

        return newErrorMessage;
    }

    private String buildErrorMessageForMedia(int mediaItemsNotUploaded) {
        String newErrorMessage = "";
        if (mediaItemsNotUploaded > 0) {
            if (mediaItemsNotUploaded == 1) {
                newErrorMessage += mContext.getString(R.string.media_file_not_uploaded);
            } else {
                newErrorMessage +=
                        String.format(mContext.getString(R.string.media_files_not_uploaded), mediaItemsNotUploaded);
            }

            if (mediaItemsNotUploaded <= sNotificationData.mCurrentMediaItem) {
                // some media items were uploaded successfully
                newErrorMessage += " " + String.format(mContext.getString(R.string.media_files_uploaded_successfully),
                                                       sNotificationData.mCurrentMediaItem);
            }
        }

        return newErrorMessage;
    }

    private String buildSuccessMessageForMedia(int mediaItemsUploaded) {
        // all media items were uploaded successfully
        String successMessage = mediaItemsUploaded == 1 ? mContext.getString(R.string.media_file_uploaded)
                : String.format(mContext.getString(R.string.media_all_files_uploaded_successfully),
                                mediaItemsUploaded);
        return successMessage;
    }

    private String buildSnackbarSuccessMessageForMedia(int mediaItemsUploaded) {
        String successMessage = "";
        if (mediaItemsUploaded > 0) {
            if (mediaItemsUploaded == 1) {
                successMessage += mContext.getString(R.string.media_file_uploaded);
            } else {
                successMessage += String.format(mContext.getString(R.string.media_files_uploaded), mediaItemsUploaded);
            }
        }
        return successMessage;
    }

    private String buildSnackbarErrorMessage(String newErrorMessage, String detailErrorMessage) {
        // now append the detailed error message below
        String snackbarMessage = new String(newErrorMessage);
        if (newErrorMessage.length() > 0) {
            snackbarMessage += "\n" + detailErrorMessage;
        } else {
            snackbarMessage = detailErrorMessage;
        }

        return snackbarMessage;
    }


    void updateNotificationProgressForMedia(MediaModel media, float progress) {
        if (sNotificationData.mTotalMediaItems == 0 && sNotificationData.mTotalPostItems == 0) {
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
        if (sNotificationData.mTotalMediaItems == 0 && sNotificationData.mTotalPostItems == 0) {
            return;
        }

        mNotificationBuilder.setProgress(100, (int) Math.ceil(getCurrentOverallProgress() * 100), false);
        doNotify(sNotificationData.mNotificationId, mNotificationBuilder.build(), null);
    }

    private void setProgressForMediaItem(int mediaId, float progress) {
        sNotificationData.mediaItemToProgressMap.put(mediaId, progress);
    }

    private float getCurrentOverallProgress() {
        int totalItemCount = sNotificationData.mTotalPostItems + sNotificationData.mTotalMediaItems;
        float currentMediaProgress = getCurrentMediaProgress();
        float overAllProgress;
        overAllProgress = sNotificationData.mTotalPostItems > 0
                ? (sNotificationData.mCurrentPostItem / sNotificationData.mTotalPostItems) * totalItemCount : 0;
        overAllProgress += sNotificationData.mTotalMediaItems > 0
                ? (sNotificationData.mCurrentMediaItem / sNotificationData.mTotalMediaItems) * totalItemCount : 0;
        overAllProgress += currentMediaProgress;
        return overAllProgress;
    }

    private float getCurrentMediaProgress() {
        float currentMediaProgress = 0.0f;
        int size = sNotificationData.mediaItemToProgressMap.size();
        for (int i = 0; i < size; i++) {
            int key = sNotificationData.mediaItemToProgressMap.keyAt(i);
            float itemProgress = sNotificationData.mediaItemToProgressMap.get(key);
            currentMediaProgress += (itemProgress / size);
        }
        return currentMediaProgress;
    }

    private synchronized void doNotify(long id, Notification notification, NotificationType notificationType) {
        try {
            mNotificationManager.notify((int) id, notification);
            if (notificationType != null) {
                mSystemNotificationsTracker.trackShownNotification(notificationType);
            }
        } catch (RuntimeException runtimeException) {
            AppLog.e(T.POSTS, "doNotify failed; See issue #2858 / #3966", runtimeException);
        }
    }

    void setTotalMediaItems(PostImmutableModel post, int totalMediaItems) {
        if (post != null) {
            sNotificationData.mTotalPostItems = 1;
            if (post.isPage()) {
                sNotificationData.mTotalPageItemsIncludedInPostCount = 1;
            }
        }
        sNotificationData.mTotalMediaItems = totalMediaItems;
    }

    private String buildNotificationTitleForPost(PostImmutableModel post) {
        String postTitle =
                (post == null || TextUtils.isEmpty(post.getTitle())) ? mContext.getString(R.string.untitled)
                        : post.getTitle();
        return String.format(mContext.getString(R.string.uploading_post), postTitle);
    }

    private String buildNotificationTitleForMedia() {
        return mContext.getString(R.string.uploading_media);
    }

    private String buildNotificationTitleForMixedContent() {
        return mContext.getString(R.string.uploading_title);
    }

    private String buildNotificationSubtitleForPost(PostImmutableModel post) {
        String uploadingMessage =
                (post != null && post.isPage()) ? mContext.getString(R.string.uploading_subtitle_pages_only_one)
                : mContext.getString(R.string.uploading_subtitle_posts_only_one);
        return uploadingMessage;
    }

    private String buildNotificationSubtitleForPosts() {
        int remaining = sNotificationData.mTotalPostItems - getCurrentPostItem();
        PagesOrPostsType pagesAndOrPosts = getPagesAndOrPostsType(remaining);
        String strToUse;
        switch (pagesAndOrPosts) {
            case PAGES:
                strToUse = mContext.getString(R.string.uploading_subtitle_pages_only_plural);
                break;
            case PAGES_OR_POSTS:
                strToUse = mContext.getString(R.string.uploading_subtitle_pages_posts);
                break;
            case POSTS:
            default:
                strToUse = mContext.getString(R.string.uploading_subtitle_posts_only_plural);
                break;
        }

        return String.format(strToUse, remaining);
    }

    private PagesOrPostsType getPagesAndOrPostsType(int remaining) {
        PagesOrPostsType pagesAndOrPosts;
        if (sNotificationData.mTotalPageItemsIncludedInPostCount > 0 && sNotificationData.mTotalPostItems > 0
            && sNotificationData.mTotalPostItems > sNotificationData.mTotalPageItemsIncludedInPostCount) {
            // we have both pages and posts
            pagesAndOrPosts = PagesOrPostsType.PAGES_OR_POSTS;
        } else if (sNotificationData.mTotalPageItemsIncludedInPostCount > 0) {
            // we have only pages
            if (remaining == 1) {
                // only one page
                pagesAndOrPosts = PagesOrPostsType.PAGE;
            } else {
                pagesAndOrPosts = PagesOrPostsType.PAGES;
            }
        } else {
            // we have only posts
            if (remaining == 1) {
                // only one post
                pagesAndOrPosts = PagesOrPostsType.POST;
            } else {
                pagesAndOrPosts = PagesOrPostsType.POSTS;
            }
        }
        return pagesAndOrPosts;
    }

    private String buildNotificationSubtitleForMedia() {
        String uploadingMessage;
        if (sNotificationData.mTotalMediaItems == 1) {
            uploadingMessage = mContext.getString(R.string.uploading_subtitle_media_only_one);
        } else {
            uploadingMessage = String.format(
                    mContext.getString(R.string.uploading_subtitle_media_only),
                    sNotificationData.mTotalMediaItems - getCurrentMediaItem(),
                    sNotificationData.mTotalMediaItems
            );
        }
        return uploadingMessage;
    }

    private String buildNotificationSubtitleForMixedContent() {
        int remaining = sNotificationData.mTotalPostItems - getCurrentPostItem();
        String uploadingMessage;

        if (sNotificationData.mTotalMediaItems == 1) {
            switch (getPagesAndOrPostsType(remaining)) {
                case PAGES:
                    uploadingMessage = String.format(
                            mContext.getString(R.string.uploading_subtitle_mixed_pages_plural_media_one),
                            remaining);
                    break;
                case PAGE:
                    uploadingMessage = mContext.getString(R.string.uploading_subtitle_mixed_page_singular_media_one);
                    break;
                case POST:
                    uploadingMessage = mContext.getString(R.string.uploading_subtitle_mixed_post_singular_media_one);
                    break;
                case PAGES_OR_POSTS:
                    uploadingMessage = String.format(
                            mContext.getString(R.string.uploading_subtitle_mixed_pages_and_posts_plural_media_one),
                            remaining);
                    break;
                case POSTS:
                default:
                    uploadingMessage = String.format(
                            mContext.getString(R.string.uploading_subtitle_mixed_posts_plural_media_one),
                            remaining);
                    break;
            }
        } else {
            switch (getPagesAndOrPostsType(remaining)) {
                case PAGES:
                    uploadingMessage = String.format(
                            mContext.getString(R.string.uploading_subtitle_mixed_pages_plural_media_plural),
                            remaining,
                            sNotificationData.mTotalMediaItems - getCurrentMediaItem(),
                            sNotificationData.mTotalMediaItems);
                    break;
                case PAGE:
                    uploadingMessage = String.format(
                            mContext.getString(R.string.uploading_subtitle_mixed_page_singular_media_plural),
                            sNotificationData.mTotalMediaItems - getCurrentMediaItem(),
                            sNotificationData.mTotalMediaItems);
                    break;
                case POST:
                    uploadingMessage = String.format(
                            mContext.getString(R.string.uploading_subtitle_mixed_post_singular_media_plural),
                            sNotificationData.mTotalMediaItems - getCurrentMediaItem(),
                            sNotificationData.mTotalMediaItems);
                    break;
                case PAGES_OR_POSTS:
                    uploadingMessage = String.format(
                            mContext.getString(R.string.uploading_subtitle_mixed_pages_and_posts_plural_media_plural),
                            remaining,
                            sNotificationData.mTotalMediaItems - getCurrentMediaItem(),
                            sNotificationData.mTotalMediaItems);
                    break;
                case POSTS:
                default:
                    uploadingMessage = String.format(
                            mContext.getString(R.string.uploading_subtitle_mixed_posts_plural_media_plural),
                            remaining,
                            sNotificationData.mTotalMediaItems - getCurrentMediaItem(),
                            sNotificationData.mTotalMediaItems);
                    break;
            }
        }
        return uploadingMessage;
    }

    private int getCurrentPostItem() {
        int currentPostItem = sNotificationData.mCurrentPostItem >= sNotificationData.mTotalPostItems
                ? sNotificationData.mTotalPostItems - 1 : sNotificationData.mCurrentPostItem;
        return currentPostItem;
    }

    private int getCurrentMediaItem() {
        int currentMediaItem = sNotificationData.mCurrentMediaItem >= sNotificationData.mTotalMediaItems
                ? sNotificationData.mTotalMediaItems - 1 : sNotificationData.mCurrentMediaItem;
        return currentMediaItem;
    }
}
