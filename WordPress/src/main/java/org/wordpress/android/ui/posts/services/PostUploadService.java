package org.wordpress.android.ui.posts.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.SparseArray;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaModel.UploadState;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaError;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.posts.services.PostEvents.PostUploadStarted;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.helpers.MediaFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class PostUploadService extends Service {
    private static final ArrayList<PostModel> mPostsList = new ArrayList<>();
    private static PostModel mCurrentUploadingPost = null;
    private static Map<String, Object> mCurrentUploadingPostAnalyticsProperties;
    private static boolean mUseLegacyMode;
    private UploadPostTask mCurrentTask = null;

    private static final Set<Integer> mFirstPublishPosts = new HashSet<>();

    private Context mContext;
    private PostUploadNotifier mPostUploadNotifier;

    private SparseArray<CountDownLatch> mMediaLatchMap = new SparseArray<>();

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject MediaStore mMediaStore;

    /**
     * Adds a post to the queue.
     */
    public static void addPostToUpload(PostModel post) {
        synchronized (mPostsList) {
            mPostsList.add(post);
        }
    }

    /**
     * Adds a post to the queue and tracks post analytics.
     * To be used only the first time a post is uploaded, i.e. when its status changes from local draft or remote draft
     * to published.
     */
    public static void addPostToUploadAndTrackAnalytics(PostModel post) {
        synchronized (mFirstPublishPosts) {
            mFirstPublishPosts.add(post.getId());
        }
        synchronized (mPostsList) {
            mPostsList.add(post);
        }
    }

    public static void setLegacyMode(boolean enabled) {
        mUseLegacyMode = enabled;
    }

    /**
     * Returns true if the passed post is either uploading or waiting to be uploaded.
     */
    public static boolean isPostUploading(PostModel post) {
        // first check the currently uploading post
        if (mCurrentUploadingPost != null && mCurrentUploadingPost.getId() == post.getId()) {
            return true;
        }
        // then check the list of posts waiting to be uploaded
        if (mPostsList.size() > 0) {
            synchronized (mPostsList) {
                for (PostModel queuedPost : mPostsList) {
                    if (queuedPost.getId() == post.getId()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);
        mContext = this.getApplicationContext();
        mPostUploadNotifier = new PostUploadNotifier(mContext, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cancel current task, it will reset post from "uploading" to "local draft"
        if (mCurrentTask != null) {
            AppLog.d(T.POSTS, "cancelling current upload task");
            mCurrentTask.cancel(true);
        }
        mDispatcher.unregister(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (mPostsList) {
            if (mPostsList.size() == 0 || mContext == null) {
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        uploadNextPost();
        // We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

    private void uploadNextPost() {
        synchronized (mPostsList) {
            if (mCurrentTask == null) { //make sure nothing is running
                mCurrentUploadingPost = null;
                mCurrentUploadingPostAnalyticsProperties = null;
                if (mPostsList.size() > 0) {
                    mCurrentUploadingPost = mPostsList.remove(0);
                    mCurrentTask = new UploadPostTask();
                    mCurrentTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mCurrentUploadingPost);
                } else {
                    stopSelf();
                }
            }
        }
    }

    private void finishUpload() {
        synchronized (mPostsList) {
            mCurrentTask = null;
            mCurrentUploadingPost = null;
            mCurrentUploadingPostAnalyticsProperties = null;
        }
        uploadNextPost();
    }

    private class UploadPostTask extends AsyncTask<PostModel, Boolean, Boolean> {
        private PostModel mPost;
        private SiteModel mSite;

        private String mErrorMessage = "";
        private boolean mIsMediaError = false;
        private long featuredImageID = -1;

        // Used for analytics
        private boolean mHasImage, mHasVideo, mHasCategory;

        @Override
        protected void onPostExecute(Boolean pushActionWasDispatched) {
            if (!pushActionWasDispatched) {
                // This block only runs if the PUSH_POST action was never dispatched - if it was dispatched, any error
                // will be handled in OnPostChanged instead of here
                mPostUploadNotifier.updateNotificationError(mPost, mSite, mErrorMessage, mIsMediaError);
                finishUpload();
            }
        }

        @Override
        protected Boolean doInBackground(PostModel... posts) {
            mPost = posts[0];

            String postTitle = TextUtils.isEmpty(mPost.getTitle()) ? getString(R.string.untitled) : mPost.getTitle();
            String uploadingPostTitle = String.format(getString(R.string.posting_post), postTitle);
            String uploadingPostMessage = String.format(
                    getString(R.string.sending_content),
                    mPost.isPage() ? getString(R.string.page).toLowerCase() : getString(R.string.post).toLowerCase()
            );

            mPostUploadNotifier.updateNotificationNewPost(mPost, uploadingPostTitle, uploadingPostMessage);

            mSite = mSiteStore.getSiteByLocalId(mPost.getLocalSiteId());
            if (mSite == null) {
                mErrorMessage = mContext.getString(R.string.blog_not_found);
                return false;
            }

            if (TextUtils.isEmpty(mPost.getStatus())) {
                mPost.setStatus(PostStatus.PUBLISHED.toString());
            }

            String content = mPost.getContent();
            // Get rid of ZERO WIDTH SPACE character that the Visual editor can insert
            // at the beginning of the content.
            // http://www.fileformat.info/info/unicode/char/200b/index.htm
            // See: https://github.com/wordpress-mobile/WordPress-Android/issues/5009
            if (content.length() > 0 && content.charAt(0) == '\u200B') {
                content = content.substring(1, content.length());
            }
            content = processPostMedia(content);
            mPost.setContent(content);

            // If media file upload failed, let's stop here and prompt the user
            if (mIsMediaError) {
                return false;
            }

            if (mPost.getCategoryIdList().size() > 0) {
                mHasCategory = true;
            }

            // Support for legacy editor - images are identified as featured as they're being uploaded with the post
            if (mUseLegacyMode && featuredImageID != -1) {
                mPost.setFeaturedImageId(featuredImageID);
            }

            EventBus.getDefault().post(new PostUploadStarted(mPost.getLocalSiteId()));

            RemotePostPayload payload = new RemotePostPayload(mPost, mSite);
            mDispatcher.dispatch(PostActionBuilder.newPushPostAction(payload));

            // Track analytics only if the post is newly published
            if (mFirstPublishPosts.contains(mPost.getId())) {
                prepareUploadAnalytics();
            }

            return true;
        }

        private boolean hasGallery() {
            Pattern galleryTester = Pattern.compile("\\[.*?gallery.*?\\]");
            Matcher matcher = galleryTester.matcher(mPost.getContent());
            return matcher.find();
        }

        private void prepareUploadAnalytics() {
            // Calculate the words count
            mCurrentUploadingPostAnalyticsProperties = new HashMap<>();
            mCurrentUploadingPostAnalyticsProperties.put("word_count", AnalyticsUtils.getWordCount(mPost.getContent()));

            if (hasGallery()) {
                mCurrentUploadingPostAnalyticsProperties.put("with_galleries", true);
            }
            if (mHasImage) {
                mCurrentUploadingPostAnalyticsProperties.put("with_photos", true);
            }
            if (mHasVideo) {
                mCurrentUploadingPostAnalyticsProperties.put("with_videos", true);
            }
            if (mHasCategory) {
                mCurrentUploadingPostAnalyticsProperties.put("with_categories", true);
            }
            if (!mPost.getTagNameList().isEmpty()) {
                mCurrentUploadingPostAnalyticsProperties.put("with_tags", true);
            }
            mCurrentUploadingPostAnalyticsProperties.put("via_new_editor", AppPrefs.isVisualEditorEnabled());
        }

        /**
         * Finds media in post content, uploads them, and returns the HTML to insert in the post
         */
        private String processPostMedia(String postContent) {
            String imageTagsPattern = "<img[^>]+android-uri\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
            Pattern pattern = Pattern.compile(imageTagsPattern);
            Matcher matcher = pattern.matcher(postContent);

            int totalMediaItems = 0;
            List<String> imageTags = new ArrayList<>();
            while (matcher.find()) {
                imageTags.add(matcher.group());
                totalMediaItems++;
            }

            mPostUploadNotifier.setTotalMediaItems(mPost, totalMediaItems);

            int mediaItemCount = 0;
            for (String tag : imageTags) {
                Pattern p = Pattern.compile("android-uri=\"([^\"]+)\"");
                Matcher m = p.matcher(tag);
                if (m.find()) {
                    String imageUri = m.group(1);
                    if (!imageUri.equals("")) {
                        MediaModel mediaModel = mMediaStore.getPostMediaWithPath(mPost.getId(), imageUri);
                        if (mediaModel == null) {
                            mIsMediaError = true;
                            continue;
                        }
                        MediaFile mediaFile = FluxCUtils.mediaFileFromMediaModel(mediaModel);
                        if (mediaFile != null) {
                            // Get image thumbnail for notification icon
                            Bitmap imageIcon = ImageUtils.getWPImageSpanThumbnailFromFilePath(
                                    mContext,
                                    imageUri,
                                    DisplayUtils.dpToPx(mContext, 128)
                            );

                            // Crop the thumbnail to be squared in the center
                            if (imageIcon != null) {
                                int squaredSize = DisplayUtils.dpToPx(mContext, 64);
                                imageIcon = ThumbnailUtils.extractThumbnail(imageIcon, squaredSize, squaredSize);
                            }

                            mediaItemCount++;
                            mPostUploadNotifier.setCurrentMediaItem(mPost, mediaItemCount);
                            mPostUploadNotifier.updateNotificationIcon(mPost, imageIcon);

                            String mediaUploadOutput;
                            if (mediaFile.isVideo()) {
                                mHasVideo = true;
                                mediaUploadOutput = uploadVideo(mediaFile);
                            } else {
                                mHasImage = true;
                                mediaUploadOutput = uploadImage(mediaFile);
                            }

                            if (mediaUploadOutput != null) {
                                postContent = postContent.replace(tag, mediaUploadOutput);
                            } else {
                                postContent = postContent.replace(tag, "");
                                mIsMediaError = true;
                            }
                        }
                    }
                }
            }

            return postContent;
        }

        private String uploadImage(MediaFile mediaFile) {
            AppLog.d(T.POSTS, "uploadImage: " + mediaFile.getFilePath());

            if (mediaFile.getFilePath() == null) {
                return null;
            }

            Uri imageUri = Uri.parse(mediaFile.getFilePath());
            File imageFile = null;

            if (imageUri.toString().contains("content:")) {
                String[] projection = new String[]{Images.Media._ID, Images.Media.DATA, Images.Media.MIME_TYPE};

                Cursor cur = mContext.getContentResolver().query(imageUri, projection, null, null, null);
                if (cur != null && cur.moveToFirst()) {
                    int dataColumn = cur.getColumnIndex(Images.Media.DATA);

                    String thumbData = cur.getString(dataColumn);
                    imageFile = new File(thumbData);
                    mediaFile.setFilePath(imageFile.getPath());
                }
                SqlUtils.closeCursor(cur);
            } else { // file is not in media library
                String path = imageUri.toString().replace("file://", "");
                imageFile = new File(path);
                mediaFile.setFilePath(path);
            }

            // check if the file exists
            if (imageFile == null) {
                mErrorMessage = mContext.getString(R.string.file_not_found);
                return null;
            }

            String fullSizeUrl = uploadImageFile(mediaFile, mSite);
            if (fullSizeUrl == null) {
                mErrorMessage = mContext.getString(R.string.error_media_upload);
                return null;
            }

            return mediaFile.getImageHtmlForUrls(fullSizeUrl, null, false);
        }

        private String uploadVideo(MediaFile mediaFile) {
            // create temp file for media upload
            String tempFileName = "wp-" + System.currentTimeMillis();
            try {
                mContext.openFileOutput(tempFileName, Context.MODE_PRIVATE);
            } catch (FileNotFoundException e) {
                mErrorMessage = getResources().getString(R.string.file_error_create);
                return null;
            }

            if (mediaFile.getFilePath() == null) {
                mErrorMessage = mContext.getString(R.string.error_media_upload);
                return null;
            }

            Uri videoUri = Uri.parse(mediaFile.getFilePath());
            File videoFile = null;
            String mimeType = "", xRes = "", yRes = "";

            if (videoUri.toString().contains("content:")) {
                String[] projection = new String[]{Video.Media._ID, Video.Media.DATA, Video.Media.MIME_TYPE,
                        Video.Media.RESOLUTION};
                Cursor cur = mContext.getContentResolver().query(videoUri, projection, null, null, null);

                if (cur != null && cur.moveToFirst()) {
                    int dataColumn = cur.getColumnIndex(Video.Media.DATA);
                    int mimeTypeColumn = cur.getColumnIndex(Video.Media.MIME_TYPE);
                    int resolutionColumn = cur.getColumnIndex(Video.Media.RESOLUTION);

                    mediaFile = new MediaFile();

                    String thumbData = cur.getString(dataColumn);
                    mimeType = cur.getString(mimeTypeColumn);

                    videoFile = new File(thumbData);
                    mediaFile.setFilePath(videoFile.getPath());
                    String resolution = cur.getString(resolutionColumn);
                    if (resolution != null) {
                        String[] resolutions = resolution.split("x");
                        if (resolutions.length >= 2) {
                            xRes = resolutions[0];
                            yRes = resolutions[1];
                        }
                    } else {
                        // Default resolution
                        xRes = "640";
                        yRes = "480";
                    }
                }
                SqlUtils.closeCursor(cur);
            } else { // file is not in media library
                String filePath = videoUri.toString().replace("file://", "");
                mediaFile.setFilePath(filePath);
                videoFile = new File(filePath);
            }

            if (videoFile == null) {
                mErrorMessage = mContext.getResources().getString(R.string.error_media_upload);
                return null;
            }

            if (TextUtils.isEmpty(mimeType)) {
                mimeType = MediaUtils.getMediaFileMimeType(videoFile);
            }

            CountDownLatch countDownLatch = new CountDownLatch(1);
            MediaPayload payload = new MediaPayload(mSite, FluxCUtils.mediaModelFromMediaFile(mediaFile));
            mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

            try {
                mMediaLatchMap.put(mediaFile.getId(), countDownLatch);
                countDownLatch.await();
            } catch (InterruptedException e) {
                AppLog.e(T.POSTS, "CountDownLatch await interrupted for media file: " + mediaFile.getId() + " - " + e);
                mIsMediaError = true;
            }

            MediaModel finishedMedia = mMediaStore.getMediaWithLocalId(mediaFile.getId());

            if (finishedMedia == null || finishedMedia.getUploadState() == null || !finishedMedia.getUploadState().equals(UploadState.UPLOADED.name())) {
                mIsMediaError = true;
                return null;
            }

            if (!TextUtils.isEmpty(finishedMedia.getVideoPressGuid())) {
                return "[wpvideo " + finishedMedia.getVideoPressGuid() + "]\n";
            } else {
                return String.format(
                        "<video width=\"%s\" height=\"%s\" controls=\"controls\"><source src=\"%s\" type=\"%s\" /><a href=\"%s\">Click to view video</a>.</video>",
                        xRes, yRes, finishedMedia.getUrl(), mimeType, finishedMedia.getUrl());
            }
        }

        private String uploadImageFile(MediaFile mediaFile, SiteModel site) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            MediaPayload payload = new MediaPayload(site, FluxCUtils.mediaModelFromMediaFile(mediaFile));
            mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));

            try {
                mMediaLatchMap.put(mediaFile.getId(), countDownLatch);
                countDownLatch.await();
            } catch (InterruptedException e) {
                AppLog.e(T.POSTS, "CountDownLatch await interrupted for media file: " + mediaFile.getId() + " - " + e);
                mIsMediaError = true;
            }

            MediaModel finishedMedia = mMediaStore.getMediaWithLocalId(mediaFile.getId());

            if (finishedMedia == null || finishedMedia.getUploadState() == null || !finishedMedia.getUploadState().equals(UploadState.UPLOADED.name())) {
                mIsMediaError = true;
                return null;
            }

            String pictureURL = finishedMedia.getUrl();

            if (mediaFile.isFeatured()) {
                featuredImageID = finishedMedia.getMediaId();
                if (!mediaFile.isFeaturedInPost()) {
                    return "";
                }
            }

            return pictureURL;
        }
    }

    /**
     * Returns an error message string for a failed post upload.
     */
    private @NonNull String getErrorMessageFromPostError(PostModel post, PostError error) {
        switch (error.type) {
            case UNKNOWN_POST:
                return getString(R.string.error_unknown_post);
            case UNKNOWN_POST_TYPE:
                return getString(R.string.error_unknown_post_type);
            case UNAUTHORIZED:
                return post.isPage() ? getString(R.string.error_refresh_unauthorized_pages) :
                        getString(R.string.error_refresh_unauthorized_posts);
        }
        // In case of a generic or uncaught error, return the message from the API response or the error type
        return TextUtils.isEmpty(error.message) ? error.type.toString() : error.message;
    }

    private @NonNull String getErrorMessageFromMediaError(MediaError error) {
         switch (error.type) {
            case FS_READ_PERMISSION_DENIED:
                return getString(R.string.error_media_insufficient_fs_permissions);
            case NOT_FOUND:
                return getString(R.string.error_media_not_found);
            case AUTHORIZATION_REQUIRED:
                return getString(R.string.error_media_unauthorized);
             case PARSE_ERROR:
                 return getString(R.string.error_media_parse_error);
            case REQUEST_TOO_LARGE:
                return getString(R.string.error_media_request_too_large);
        }
        // In case of a generic or uncaught error, return the message from the API response or the error type
        return TextUtils.isEmpty(error.message) ? error.type.toString() : error.message;
    }

    private @NonNull String getErrorMessage(PostModel post, String specificMessage) {
        String postType = getString(post.isPage() ? R.string.page : R.string.post).toLowerCase();
        return String.format(mContext.getResources().getText(R.string.error_upload_params).toString(), postType,
                specificMessage);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
        SiteModel site = mSiteStore.getSiteByLocalId(event.post.getLocalSiteId());

        if (event.isError()) {
            AppLog.e(T.POSTS, "Post upload failed. " + event.error.type + ": " + event.error.message);
            String message = getErrorMessage(event.post, getErrorMessageFromPostError(event.post, event.error));
            mPostUploadNotifier.updateNotificationError(event.post, site, message, false);
            mFirstPublishPosts.remove(event.post.getId());
        } else {
            mPostUploadNotifier.cancelNotification(event.post);
            boolean isFirstTimePublish = mFirstPublishPosts.remove(event.post.getId());
            mPostUploadNotifier.updateNotificationSuccess(event.post, site, isFirstTimePublish);
            if (isFirstTimePublish) {
                if (mCurrentUploadingPostAnalyticsProperties != null){
                    mCurrentUploadingPostAnalyticsProperties.put("post_id", event.post.getRemotePostId());
                }
                AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_PUBLISHED_POST,
                        mSiteStore.getSiteByLocalId(event.post.getLocalSiteId()),
                        mCurrentUploadingPostAnalyticsProperties);
            }
        }

        finishUpload();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        // Event for unknown media, ignoring
        if (event.media == null || mCurrentUploadingPost == null || mMediaLatchMap.get(event.media.getId()) == null) {
            AppLog.w(T.MEDIA, "Media event not recognized: " + event.media);
            return;
        }

        if (event.isError()) {
            AppLog.e(T.MEDIA, "Media upload failed. " + event.error.type + ": " + event.error.message);
            SiteModel site = mSiteStore.getSiteByLocalId(mCurrentUploadingPost.getLocalSiteId());
            String message = getErrorMessage(mCurrentUploadingPost, getErrorMessageFromMediaError(event.error));
            mPostUploadNotifier.updateNotificationError(mCurrentUploadingPost, site, message, true);
            mFirstPublishPosts.remove(mCurrentUploadingPost.getId());
            finishUpload();
            return;
        }

        if (event.canceled) {
            // Not implemented
            return;
        }

        if (event.completed) {
            AppLog.i(T.MEDIA, "Media upload completed for post. Media id: " + event.media.getId()
                    + ", post id: " + mCurrentUploadingPost.getId());
            mMediaLatchMap.get(event.media.getId()).countDown();
            mMediaLatchMap.remove(event.media.getId());
        } else {
            // Progress update
            mPostUploadNotifier.updateNotificationProgress(mCurrentUploadingPost, event.progress);
        }
    }
}
