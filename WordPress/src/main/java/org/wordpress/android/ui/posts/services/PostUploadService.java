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
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
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
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.helpers.MediaFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static com.android.volley.Request.Method.HEAD;

public class PostUploadService extends Service {
    private static final ArrayList<PostModel> mPostsList = new ArrayList<>();
    private static PostModel mCurrentUploadingPost = null;
    private static boolean mUseLegacyMode;
    private UploadPostTask mCurrentTask = null;

    private static final Set<Integer> mFirstPublishPosts = new HashSet<>();

    private Context mContext;
    private PostUploadNotifier mPostUploadNotifier;

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;

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
        }
        uploadNextPost();
    }

    private class UploadPostTask extends AsyncTask<PostModel, Boolean, Boolean> {
        private PostModel mPost;
        private SiteModel mSite;

        private String mErrorMessage = "";
        private boolean mIsMediaError = false;
        private int featuredImageID = -1;

        // Used for analytics
        private boolean mHasImage, mHasVideo, mHasCategory;

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
                trackUploadAnalytics();
            }

            return true;
        }

        private boolean hasGallery() {
            Pattern galleryTester = Pattern.compile("\\[.*?gallery.*?\\]");
            Matcher matcher = galleryTester.matcher(mPost.getContent());
            return matcher.find();
        }

        private void trackUploadAnalytics() {
            // Calculate the words count
            Map<String, Object> properties = new HashMap<>();
            properties.put("word_count", AnalyticsUtils.getWordCount(mPost.getContent()));

            if (hasGallery()) {
                properties.put("with_galleries", true);
            }
            if (mHasImage) {
                properties.put("with_photos", true);
            }
            if (mHasVideo) {
                properties.put("with_videos", true);
            }
            if (mHasCategory) {
                properties.put("with_categories", true);
            }
            if (!mPost.getTagNameList().isEmpty()) {
                properties.put("with_tags", true);
            }
            properties.put("via_new_editor", AppPrefs.isVisualEditorEnabled());
            AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_PUBLISHED_POST, mSite, properties);
        }

        /**
         * Finds media in post content, uploads them, and returns the HTML to insert in the post
         */
        private String processPostMedia(String postContent) {
            String imageTagsPattern = "<img[^>]+android-uri\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
            Pattern pattern = Pattern.compile(imageTagsPattern);
            Matcher matcher = pattern.matcher(postContent);

            int totalMediaItems = 0;
            List<String> imageTags = new ArrayList<String>();
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
                        // TODO: MediaStore
                        // MediaFile mediaFile = WordPress.wpDB.getMediaFile(imageUri, mPost);
                        MediaFile mediaFile = new MediaFile();
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
            String mimeType = "", path = "";

            if (imageUri.toString().contains("content:")) {
                String[] projection = new String[]{Images.Media._ID, Images.Media.DATA, Images.Media.MIME_TYPE};

                Cursor cur = mContext.getContentResolver().query(imageUri, projection, null, null, null);
                if (cur != null && cur.moveToFirst()) {
                    int dataColumn = cur.getColumnIndex(Images.Media.DATA);
                    int mimeTypeColumn = cur.getColumnIndex(Images.Media.MIME_TYPE);

                    String thumbData = cur.getString(dataColumn);
                    mimeType = cur.getString(mimeTypeColumn);
                    imageFile = new File(thumbData);
                    path = thumbData;
                    mediaFile.setFilePath(imageFile.getPath());
                }
                SqlUtils.closeCursor(cur);
            } else { // file is not in media library
                path = imageUri.toString().replace("file://", "");
                imageFile = new File(path);
                mediaFile.setFilePath(path);
            }

            // check if the file exists
            if (imageFile == null) {
                mErrorMessage = mContext.getString(R.string.file_not_found);
                return null;
            }

            if (TextUtils.isEmpty(mimeType)) {
                mimeType = MediaUtils.getMediaFileMimeType(imageFile);
            }
            String fileName = MediaUtils.getMediaFileName(imageFile, mimeType);

            // Upload the full size picture if "Original Size" is selected in settings

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", fileName);
            parameters.put("type", mimeType);
            parameters.put("bits", mediaFile);
            parameters.put("overwrite", true);

            String fullSizeUrl = uploadImageFile(parameters, mediaFile, mSite);
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
            String videoName = MediaUtils.getMediaFileName(videoFile, mimeType);

            // try to upload the video
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("name", videoName);
            m.put("type", mimeType);
            m.put("bits", mediaFile);
            m.put("overwrite", true);

            Object[] params = {1, mSite.getUsername(), mSite.getPassword(), m};

            File tempFile;
            try {
                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(videoName);
                tempFile = createTempUploadFile(fileExtension);
            } catch (IOException e) {
                mErrorMessage = getResources().getString(R.string.file_error_create);
                return null;
            }

            // TODO: MediaStore
            Object result = null;
            // Object result = uploadFileHelper(params, tempFile);
            Map<?, ?> resultMap = (HashMap<?, ?>) result;
            if (resultMap != null && resultMap.containsKey("url")) {
                String resultURL = resultMap.get("url").toString();
                if (resultMap.containsKey(MediaFile.VIDEOPRESS_SHORTCODE_ID)) {
                    resultURL = resultMap.get(MediaFile.VIDEOPRESS_SHORTCODE_ID).toString() + "\n";
                } else {
                    resultURL = String.format(
                            "<video width=\"%s\" height=\"%s\" controls=\"controls\"><source src=\"%s\" type=\"%s\" /><a href=\"%s\">Click to view video</a>.</video>",
                            xRes, yRes, resultURL, mimeType, resultURL);
                }

                return resultURL;
            } else {
                mErrorMessage = mContext.getResources().getString(R.string.error_media_upload);
                return null;
            }
        }


        private void setUploadPostErrorMessage(Exception e) {
            mErrorMessage = String.format(mContext.getResources().getText(R.string.error_upload).toString(),
                    mPost.isPage() ? mContext.getResources().getText(R.string.page).toString() :
                            mContext.getResources().getText(R.string.post).toString()) + " - " + e.getMessage();
            mIsMediaError = false;
            AppLog.e(T.EDITOR, mErrorMessage, e);
        }

        private String uploadImageFile(Map<String, Object> pictureParams, MediaFile mf, SiteModel site) {
            // create temporary upload file
            File tempFile;
            try {
                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(mf.getFileName());
                tempFile = createTempUploadFile(fileExtension);
            } catch (IOException e) {
                mIsMediaError = true;
                mErrorMessage = mContext.getString(R.string.file_not_found);
                return null;
            }

            Object[] params = {1,
                    StringUtils.notNullStr(mSite.getUsername()),
                    StringUtils.notNullStr(mSite.getPassword()),
                    pictureParams};
            // Object result = uploadFileHelper(params, tempFile);
            // TODO: MediaStore
            Object result = null;
            if (result == null) {
                mIsMediaError = true;
                return null;
            }

            Map<?, ?> contentHash = (HashMap<?, ?>) result;
            String pictureURL = contentHash.get("url").toString();

            if (mf.isFeatured()) {
                try {
                    if (contentHash.get("id") != null) {
                        featuredImageID = Integer.parseInt(contentHash.get("id").toString());
                        if (!mf.isFeaturedInPost())
                            return "";
                    }
                } catch (NumberFormatException e) {
                    AppLog.e(T.POSTS, e);
                }
            }

            return pictureURL;
        }
    }

    private File createTempUploadFile(String fileExtension) throws IOException {
        return File.createTempFile("wp-", fileExtension, mContext.getCacheDir());
    }

    /**
     * Returns an error message string for a failed post upload.
     */
    private String buildErrorMessage(PostModel post, PostError error) {
        // TODO: We should interpret event.error.type and pass our own string rather than use event.error.message
        String postType = mContext.getResources().getText(post.isPage() ? R.string.page : R.string.post).toString().toLowerCase();
        String errorMessage = String.format(mContext.getResources().getText(R.string.error_upload).toString(), postType);
        errorMessage += ": " + error.message;
        return errorMessage;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
        SiteModel site = mSiteStore.getSiteByLocalId(event.post.getLocalSiteId());

        if (event.isError()) {
            AppLog.e(T.EDITOR, "Post upload failed. " + event.error.type + ": " + event.error.message);
            mPostUploadNotifier.updateNotificationError(event.post, site, buildErrorMessage(event.post, event.error),
                    false);
            mFirstPublishPosts.remove(event.post.getId());
        } else {
            // TODO: MediaStore?
            // WordPress.wpDB.deleteMediaFilesForPost(mPost);
            mPostUploadNotifier.cancelNotification(event.post);
            boolean isFirstTimePublish = mFirstPublishPosts.remove(event.post.getId());
            mPostUploadNotifier.updateNotificationSuccess(event.post, site, isFirstTimePublish);
        }

        finishUpload();
    }
}
