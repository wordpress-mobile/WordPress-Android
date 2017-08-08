package org.wordpress.android.ui.uploads;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
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
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.uploads.PostEvents.PostUploadStarted;
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

public class PostUploadHandler implements UploadHandler<PostModel> {
    private static final ArrayList<PostModel> sQueuedPostsList = new ArrayList<>();
    private static final Set<Integer> sFirstPublishPosts = new HashSet<>();
    private static PostModel sCurrentUploadingPost = null;
    private static Map<String, Object> sCurrentUploadingPostAnalyticsProperties;

    private static boolean sUseLegacyMode;

    private PostUploadNotifier mPostUploadNotifier;
    private UploadPostTask mCurrentTask = null;

    private SparseArray<CountDownLatch> mMediaLatchMap = new SparseArray<>();

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject MediaStore mMediaStore;

    PostUploadHandler(PostUploadNotifier postUploadNotifier) {
        ((WordPress) WordPress.getContext()).component().inject(this);
        AppLog.i(T.POSTS, "PostUploadHandler > Created");
        mDispatcher.register(this);
        mPostUploadNotifier = postUploadNotifier;
    }

    void unregister() {
        mDispatcher.unregister(this);
    }

    @Override
    public boolean hasInProgressUploads() {
        return mCurrentTask != null || !sQueuedPostsList.isEmpty();
    }

    @Override
    public void cancelInProgressUploads() {
        if (mCurrentTask != null) {
            AppLog.i(T.POSTS, "PostUploadHandler > Cancelling current upload task");
            mCurrentTask.cancel(true);
        }
    }

    @Override
    public void upload(@NonNull PostModel post) {
        synchronized (sQueuedPostsList) {
            sQueuedPostsList.add(post);
        }
        uploadNextPost();
    }

    void registerPostForAnalyticsTracking(@NonNull PostModel post) {
        synchronized (sFirstPublishPosts) {
            sFirstPublishPosts.add(post.getId());
        }
    }

    void unregisterPostForAnalyticsTracking(@NonNull PostModel post) {
        synchronized (sFirstPublishPosts) {
            sFirstPublishPosts.remove(post.getId());
        }
    }

    static void setLegacyMode(boolean enabled) {
        sUseLegacyMode = enabled;
    }

    static boolean isPostUploadingOrQueued(PostModel post) {
        return post != null && (isPostUploading(post) || isPostQueued(post));
    }

    static boolean isPostQueued(PostModel post) {
        if (post == null) {
            return false;
        }

        // Check the list of posts waiting to be uploaded
        if (sQueuedPostsList.size() > 0) {
            synchronized (sQueuedPostsList) {
                for (PostModel queuedPost : sQueuedPostsList) {
                    if (queuedPost.getId() == post.getId()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static boolean isPostUploading(PostModel post) {
        return post != null && sCurrentUploadingPost != null && sCurrentUploadingPost.getId() == post.getId();
    }

    private void uploadNextPost() {
        synchronized (sQueuedPostsList) {
            if (mCurrentTask == null) { //make sure nothing is running
                sCurrentUploadingPost = null;
                sCurrentUploadingPostAnalyticsProperties = null;
                if (sQueuedPostsList.size() > 0) {
                    sCurrentUploadingPost = sQueuedPostsList.remove(0);
                    mCurrentTask = new UploadPostTask();
                    mCurrentTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, sCurrentUploadingPost);
                } else {
                    AppLog.i(T.POSTS, "PostUploadHandler > Completed");
                }
            }
        }
    }

    private void finishUpload() {
        synchronized (sQueuedPostsList) {
            mCurrentTask = null;
            sCurrentUploadingPost = null;
            sCurrentUploadingPostAnalyticsProperties = null;
        }
        uploadNextPost();
    }

    private class UploadPostTask extends AsyncTask<PostModel, Boolean, Boolean> {
        private Context mContext;

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
                mPostUploadNotifier.cancelNotification(mPost);
                mPostUploadNotifier.updateNotificationError(mPost, mSite, mErrorMessage);
                finishUpload();
            }
        }

        @Override
        protected Boolean doInBackground(PostModel... posts) {
            mContext = WordPress.getContext();
            mPost = posts[0];

            String uploadingPostMessage = String.format(
                    mContext.getString(R.string.sending_content),
                    mPost.isPage() ? mContext.getString(R.string.page).toLowerCase()
                            : mContext.getString(R.string.post).toLowerCase()
            );

            mPostUploadNotifier.showForegroundNotificationForPost(mPost, uploadingPostMessage);

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
            if (sUseLegacyMode && featuredImageID != -1) {
                mPost.setFeaturedImageId(featuredImageID);
            }

            // Track analytics only if the post is newly published
            if (sFirstPublishPosts.contains(mPost.getId())) {
                prepareUploadAnalytics(mPost.getContent());
            }

            EventBus.getDefault().post(new PostUploadStarted(mPost.getLocalSiteId()));

            RemotePostPayload payload = new RemotePostPayload(mPost, mSite);
            mDispatcher.dispatch(PostActionBuilder.newPushPostAction(payload));

            return true;
        }

        private boolean hasGallery() {
            Pattern galleryTester = Pattern.compile("\\[.*?gallery.*?\\]");
            Matcher matcher = galleryTester.matcher(mPost.getContent());
            return matcher.find();
        }

        private void prepareUploadAnalytics(String postContent) {
            // Calculate the words count
            sCurrentUploadingPostAnalyticsProperties = new HashMap<>();
            sCurrentUploadingPostAnalyticsProperties.put("word_count", AnalyticsUtils.getWordCount(mPost.getContent()));
            sCurrentUploadingPostAnalyticsProperties.put("editor_source", AppPrefs.isAztecEditorEnabled() ? "aztec" :
                    AppPrefs.isVisualEditorEnabled() ? "hybrid" : "legacy");

            if (hasGallery()) {
                sCurrentUploadingPostAnalyticsProperties.put("with_galleries", true);
            }
            if (!mHasImage) {
                // Check if there is a img tag in the post. Media added in any editor other than legacy.
                String imageTagsPattern = "<img[^>]+src\\s*=\\s*[\"]([^\"]+)[\"][^>]*>";
                Pattern pattern = Pattern.compile(imageTagsPattern);
                Matcher matcher = pattern.matcher(postContent);
                mHasImage = matcher.find();
            }
            if (mHasImage) {
                sCurrentUploadingPostAnalyticsProperties.put("with_photos", true);
            }
            if (!mHasVideo) {
                // Check if there is a video tag in the post. Media added in any editor other than legacy.
                String videoTagsPattern = "<video[^>]+src\\s*=\\s*[\"]([^\"]+)[\"][^>]*>|\\[wpvideo\\s+([^\\]]+)\\]";
                Pattern pattern = Pattern.compile(videoTagsPattern);
                Matcher matcher = pattern.matcher(postContent);
                mHasVideo = matcher.find();
            }
            if (mHasVideo) {
                sCurrentUploadingPostAnalyticsProperties.put("with_videos", true);
            }
            if (mHasCategory) {
                sCurrentUploadingPostAnalyticsProperties.put("with_categories", true);
            }
            if (!mPost.getTagNameList().isEmpty()) {
                sCurrentUploadingPostAnalyticsProperties.put("with_tags", true);
            }
            sCurrentUploadingPostAnalyticsProperties.put("via_new_editor", AppPrefs.isVisualEditorEnabled());
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
                        MediaModel mediaModel = mMediaStore.getMediaForPostWithPath(mPost, imageUri);
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
            AppLog.i(T.POSTS, "PostUploadHandler > UploadImage: " + mediaFile.getFilePath());

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
                mErrorMessage = mContext.getResources().getString(R.string.file_error_create);
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
                AppLog.e(T.POSTS, "PostUploadHandler > CountDownLatch await interrupted for media file: "
                        + mediaFile.getId() + " - " + e);
                mIsMediaError = true;
            }

            MediaModel finishedMedia = mMediaStore.getMediaWithLocalId(mediaFile.getId());

            if (finishedMedia == null || finishedMedia.getUploadState() == null ||
                    !finishedMedia.getUploadState().equals(MediaUploadState.UPLOADED.toString())) {
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
                AppLog.e(T.POSTS, "PostUploadHandler > CountDownLatch await interrupted for media file: "
                        + mediaFile.getId() + " - " + e);
                mIsMediaError = true;
            }

            MediaModel finishedMedia = mMediaStore.getMediaWithLocalId(mediaFile.getId());

            if (finishedMedia == null || finishedMedia.getUploadState() == null ||
                    !finishedMedia.getUploadState().equals(MediaUploadState.UPLOADED.toString())) {
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
     * Has priority 9 on OnPostUploaded events, which ensures that PostUploadHandler is the first to receive
     * and process OnPostUploaded events, before they trickle down to other subscribers.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 9)
    public void onPostUploaded(OnPostUploaded event) {
        SiteModel site = mSiteStore.getSiteByLocalId(event.post.getLocalSiteId());

        if (event.isError()) {
            AppLog.w(T.POSTS, "PostUploadHandler > Post upload failed. " + event.error.type + ": "
                    + event.error.message);
            Context context = WordPress.getContext();
            String errorMessage = UploadUtils.getErrorMessageFromPostError(context, event.post, event.error);
            String notificationMessage = UploadUtils.getErrorMessage(context, event.post, errorMessage, false);
            mPostUploadNotifier.updateNotificationError(event.post, site, notificationMessage);
            mPostUploadNotifier.cancelNotification(event.post);
            sFirstPublishPosts.remove(event.post.getId());
        } else {
            mPostUploadNotifier.cancelNotification(event.post);
            boolean isFirstTimePublish = sFirstPublishPosts.remove(event.post.getId());
            mPostUploadNotifier.updateNotificationSuccess(event.post, site, isFirstTimePublish);
            if (isFirstTimePublish) {
                if (sCurrentUploadingPostAnalyticsProperties != null){
                    sCurrentUploadingPostAnalyticsProperties.put("post_id", event.post.getRemotePostId());
                }
                AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_PUBLISHED_POST,
                        mSiteStore.getSiteByLocalId(event.post.getLocalSiteId()),
                        sCurrentUploadingPostAnalyticsProperties);
            }
        }

        finishUpload();
    }

    /**
     * Has priority 8 on OnMediaUploaded events, which is the second-highest (after the MediaUploadHandler).
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 8)
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.media == null) {
            AppLog.w(T.POSTS, "PostUploadHandler > Received media event for null media, ignoring");
            return;
        }
        if (sUseLegacyMode) {
            handleMediaUploadCompletedLegacy(event);
        }
    }

    private void handleMediaUploadCompletedLegacy(OnMediaUploaded event) {
        // Event for unknown media, ignoring
        if (sCurrentUploadingPost == null || mMediaLatchMap.get(event.media.getId()) == null) {
            AppLog.i(T.POSTS, "PostUploadHandler > Media event not recognized: " + event.media.getId() + ", ignoring");
            return;
        }

        if (event.isError()) {
            AppLog.w(T.POSTS, "PostUploadHandler > Media upload failed. " + event.error.type + ": "
                    + event.error.message);
            SiteModel site = mSiteStore.getSiteByLocalId(sCurrentUploadingPost.getLocalSiteId());
            Context context = WordPress.getContext();
            String errorMessage = UploadUtils.getErrorMessageFromMediaError(context, event.media, event.error);
            String notificationMessage =
                    UploadUtils.getErrorMessage(context, sCurrentUploadingPost, errorMessage, true);
            mPostUploadNotifier.cancelNotification(sCurrentUploadingPost);
            mPostUploadNotifier.updateNotificationError(sCurrentUploadingPost, site, notificationMessage);
            sFirstPublishPosts.remove(sCurrentUploadingPost.getId());
            finishUpload();
            return;
        }

        if (event.canceled) {
            // Not implemented
            return;
        }

        if (event.completed) {
            AppLog.i(T.POSTS, "PostUploadHandler > Media upload completed for post. Media id: " + event.media.getId()
                    + ", post id: " + sCurrentUploadingPost.getId());
            mMediaLatchMap.get(event.media.getId()).countDown();
            mMediaLatchMap.remove(event.media.getId());
        } else {
            // Progress update
            mPostUploadNotifier.updateNotificationProgress(sCurrentUploadingPost, event.progress);
        }
    }
}
