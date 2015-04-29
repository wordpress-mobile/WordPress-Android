package org.wordpress.android.ui.posts;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.IntentCompat;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.FeatureSet;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostLocation;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.SystemServiceFactory;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.WPImageSpan;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostUploadService extends Service {
    private static Context mContext;
    private static final ArrayList<Post> mPostsList = new ArrayList<Post>();
    private static Post mCurrentUploadingPost = null;
    private UploadPostTask mCurrentTask = null;
    private FeatureSet mFeatureSet;

    public static void addPostToUpload(Post currentPost) {
        synchronized (mPostsList) {
            mPostsList.add(currentPost);
            // Enable 'isUploading' flag for post
            currentPost.setUploading(true);
            WordPress.wpDB.updatePost(currentPost);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this.getApplicationContext();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cancel current task, it will reset post from "uploading" to "local draft"
        if (mCurrentTask != null) {
            AppLog.d(T.POSTS, "cancelling current upload task");
            mCurrentTask.cancel(true);
        }
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

    private FeatureSet synchronousGetFeatureSet() {
        if (WordPress.getCurrentBlog() == null || !WordPress.getCurrentBlog().isDotcomFlag()) {
            return null;
        }
        ApiHelper.GetFeatures task = new ApiHelper.GetFeatures();
        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        mFeatureSet = task.doSynchronously(apiArgs);

        return mFeatureSet;
    }

    private void uploadNextPost() {
        synchronized (mPostsList) {
            if (mCurrentTask == null) { //make sure nothing is running
                mCurrentUploadingPost = null;
                if (mPostsList.size() > 0) {
                    mCurrentUploadingPost = mPostsList.remove(0);
                    mCurrentTask = new UploadPostTask();
                    mCurrentTask.execute(mCurrentUploadingPost);
                } else {
                    stopSelf();
                }
            }
        }
    }

    private void postUploaded() {
        synchronized (mPostsList) {
            mCurrentTask = null;
            mCurrentUploadingPost = null;
        }
        uploadNextPost();
    }

    private class UploadPostTask extends AsyncTask<Post, Boolean, Boolean> {
        private Post mPost;
        private Blog mBlog;
        private PostUploadNotifier mPostUploadNotifier;

        private String mErrorMessage = "";
        private boolean mIsMediaError = false;
        private boolean mErrorUnavailableVideoPress = false;
        private XMLRPCClientInterface mClient;

        // Used for analytics
        private boolean mHasImage, mHasVideo, mHasCategory;

        @Override
        protected void onPostExecute(Boolean postUploadedSuccessfully) {
            // Update the 'isUploading' flag for post
            mPost.setUploading(false);
            WordPress.wpDB.updatePost(mPost);

            if (postUploadedSuccessfully) {
                WordPress.postUploaded(mPost.getLocalTableBlogId(), mPost.getRemotePostId(), mPost.isPage());
                mPostUploadNotifier.cancelNotification();
                if (mPost.hasFeaturedImage()) {
                    List<Object> args = new Vector<>();
                    args.add(WordPress.getCurrentBlog());
                    args.add(null);
                    args.add(mPost);
                    new ApiHelper.UpdatePostFeaturedImage().execute(args);
                }
                WordPress.wpDB.deleteMediaFilesForPost(mPost);
            } else {
                WordPress.postUploadFailed(mPost.getLocalTableBlogId());
                mPostUploadNotifier.updateNotificationWithError(mErrorMessage, mIsMediaError, mPost.isPage(),
                        mErrorUnavailableVideoPress);
            }

            postUploaded();
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);
            // mPostUploadNotifier and mPost can be null if onCancelled is called before doInBackground
            if (mPostUploadNotifier != null && mPost != null) {
                mPostUploadNotifier.updateNotificationWithError(mErrorMessage, mIsMediaError, mPost.isPage(),
                        mErrorUnavailableVideoPress);
                WordPress.postUploadFailed(mPost.getLocalTableBlogId());
            }
        }

        @Override
        protected Boolean doInBackground(Post... posts) {
            mErrorUnavailableVideoPress = false;
            mPost = posts[0];

            mPostUploadNotifier = new PostUploadNotifier(mPost);
            String postTitle = TextUtils.isEmpty(mPost.getTitle()) ? getString(R.string.untitled) : mPost.getTitle();
            String uploadingPostTitle = String.format(getString(R.string.posting_post), postTitle);
            String uploadingPostMessage = String.format(
                    getString(R.string.sending_content),
                    mPost.isPage() ? getString(R.string.page).toLowerCase() : getString(R.string.post).toLowerCase()
            );
            mPostUploadNotifier.updateNotificationMessage(uploadingPostTitle, uploadingPostMessage);

            mBlog = WordPress.wpDB.instantiateBlogByLocalId(mPost.getLocalTableBlogId());
            if (mBlog == null) {
                mErrorMessage = mContext.getString(R.string.blog_not_found);
                return false;
            }

            // Create the XML-RPC client
            mClient = XMLRPCFactory.instantiate(mBlog.getUri(), mBlog.getHttpuser(),
                    mBlog.getHttppassword());

            // featured image exists
            if (!TextUtils.isEmpty(mPost.getFeaturedImagePath())) {
                // from local: upload first, then get the ID
                if (!URLUtil.isNetworkUrl(mPost.getFeaturedImagePath())) {
                    setFeaturedImage();
                }
            } else if (mPost.isPublished()) {
                // path is empty, but id still exists = Remove FI
                // id is still from last featured image
                mPost.setFeaturedImageID(0);
                List<Object> args = new Vector<>();
                args.add(WordPress.getCurrentBlog());
                args.add(null);
                args.add(mPost);
                new ApiHelper.UpdatePostFeaturedImage().execute(args);
            }

            return uploadPost();
        }

        private void setFeaturedImage() {
            Bitmap featuredImageBitmap = ImageUtils.
                    getWPImageSpanThumbnailFromFilePath(mContext,
                            Uri.parse(mPost.getFeaturedImagePath()).getEncodedPath(),
                            200);
            WPImageSpan is = new WPImageSpan(mContext, featuredImageBitmap, Uri.parse(mPost.getFeaturedImagePath()));
            MediaFile featuredImage = is.getMediaFile();
            prepareMediaForUpload(featuredImage, mPost.getFeaturedImagePath());
            uploadImage(featuredImage);
        }

        private boolean uploadPost() {
            if (TextUtils.isEmpty(mPost.getPostStatus())) {
                mPost.setPostStatus(PostStatus.toString(PostStatus.PUBLISHED));
            }

            String descriptionContent = processPostMedia(mPost.getDescription());

            String moreContent = "";
            if (!TextUtils.isEmpty(mPost.getMoreText())) {
                moreContent = processPostMedia(mPost.getMoreText());
            }

            // If media file upload failed, let's stop here and prompt the user
            if (mIsMediaError) {
                return false;
            }

            JSONArray categoriesJsonArray = mPost.getJSONCategories();
            String[] postCategories = null;
            if (categoriesJsonArray != null) {
                if (categoriesJsonArray.length() > 0) {
                    mHasCategory = true;
                }

                postCategories = new String[categoriesJsonArray.length()];
                for (int i = 0; i < categoriesJsonArray.length(); i++) {
                    try {
                        postCategories[i] = TextUtils.htmlEncode(categoriesJsonArray.getString(i));
                    } catch (JSONException e) {
                        AppLog.e(T.POSTS, e);
                    }
                }
            }

            Map<String, Object> contentStruct = new HashMap<String, Object>();

            if (!mPost.isPage() && mPost.isLocalDraft()) {
                // add the tagline
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

                if (prefs.getBoolean(getString(R.string.pref_key_post_sig_enabled), false)) {
                    String tagline = prefs.getString(getString(R.string.pref_key_post_sig), "");
                    if (!TextUtils.isEmpty(tagline)) {
                        String tag = "\n\n<span class=\"post_sig\">" + tagline + "</span>\n\n";
                        if (TextUtils.isEmpty(moreContent))
                            descriptionContent += tag;
                        else
                            moreContent += tag;
                    }
                }
            }

            // Post format
            if (!mPost.isPage()) {
                if (!TextUtils.isEmpty(mPost.getPostFormat())) {
                    contentStruct.put("wp_post_format", mPost.getPostFormat());
                }
            }

            contentStruct.put("post_type", (mPost.isPage()) ? "page" : "post");
            contentStruct.put("title", mPost.getTitle());
            long pubDate = mPost.getDate_created_gmt();
            if (pubDate != 0) {
                Date date_created_gmt = new Date(pubDate);
                contentStruct.put("date_created_gmt", date_created_gmt);
                Date dateCreated = new Date(pubDate + (date_created_gmt.getTimezoneOffset() * 60000));
                contentStruct.put("dateCreated", dateCreated);
            }

            if (!TextUtils.isEmpty(moreContent)) {
                descriptionContent = descriptionContent.trim() + "<!--more-->" + moreContent;
                mPost.setMoreText("");
            }

            // get rid of the p and br tags that the editor adds.
            if (mPost.isLocalDraft()) {
                descriptionContent = descriptionContent.replace("<p>", "").replace("</p>", "\n").replace("<br>", "");
            }

            // gets rid of the weird character android inserts after images
            descriptionContent = descriptionContent.replaceAll("\uFFFC", "");

            contentStruct.put("description", descriptionContent);
            if (!mPost.isPage()) {
                contentStruct.put("mt_keywords", mPost.getKeywords());

                if (postCategories != null && postCategories.length > 0) {
                    contentStruct.put("categories", postCategories);
                }
            }

            contentStruct.put("mt_excerpt", mPost.getPostExcerpt());
            contentStruct.put((mPost.isPage()) ? "page_status" : "post_status", mPost.getPostStatus());

            // Geolocation
            if (mPost.supportsLocation()) {
                JSONObject remoteGeoLatitude = mPost.getCustomField("geo_latitude");
                JSONObject remoteGeoLongitude = mPost.getCustomField("geo_longitude");
                JSONObject remoteGeoPublic = mPost.getCustomField("geo_public");

                Map<Object, Object> hLatitude = new HashMap<Object, Object>();
                Map<Object, Object> hLongitude = new HashMap<Object, Object>();
                Map<Object, Object> hPublic = new HashMap<Object, Object>();

                try {
                    if (remoteGeoLatitude != null) {
                        hLatitude.put("id", remoteGeoLatitude.getInt("id"));
                    }

                    if (remoteGeoLongitude != null) {
                        hLongitude.put("id", remoteGeoLongitude.getInt("id"));
                    }

                    if (remoteGeoPublic != null) {
                        hPublic.put("id", remoteGeoPublic.getInt("id"));
                    }

                    if (mPost.hasLocation()) {
                        PostLocation location = mPost.getLocation();
                        if (!hLatitude.containsKey("id")) {
                            hLatitude.put("key", "geo_latitude");
                        }

                        if (!hLongitude.containsKey("id")) {
                            hLongitude.put("key", "geo_longitude");
                        }

                        if (!hPublic.containsKey("id")) {
                            hPublic.put("key", "geo_public");
                        }

                        hLatitude.put("value", location.getLatitude());
                        hLongitude.put("value", location.getLongitude());
                        hPublic.put("value", 1);
                    }
                } catch (JSONException e) {
                    AppLog.e(T.EDITOR, e);
                }

                if (!hLatitude.isEmpty() && !hLongitude.isEmpty() && !hPublic.isEmpty()) {
                    Object[] geo = {hLatitude, hLongitude, hPublic};
                    contentStruct.put("custom_fields", geo);
                }
            }

            if (!TextUtils.isEmpty(mPost.getQuickPostType())) {
                mClient.addQuickPostHeader(mPost.getQuickPostType());
            }

            contentStruct.put("wp_password", mPost.getPassword());

            Object[] params;
            if (mPost.isLocalDraft() && !mPost.isUploaded())
                params = new Object[]{mBlog.getRemoteBlogId(), mBlog.getUsername(), mBlog.getPassword(),
                        contentStruct, false};
            else
                params = new Object[]{mPost.getRemotePostId(), mBlog.getUsername(), mBlog.getPassword(), contentStruct,
                        false};

            try {
                if (mPost.isLocalDraft() && !mPost.isUploaded()) {
                    Object newPostId = mClient.call("metaWeblog.newPost", params);
                    if (newPostId instanceof String) {
                        mPost.setRemotePostId((String) newPostId);
                    }
                } else {
                    mClient.call("metaWeblog.editPost", params);
                }

                mPost.setUploaded(true);
                mPost.setLocalChange(false);
                WordPress.wpDB.updatePost(mPost);
                trackUploadAnalytics();
                return true;
            } catch (final XMLRPCException e) {
                setUploadPostErrorMessage(e);
            } catch (IOException e) {
                setUploadPostErrorMessage(e);
            } catch (XmlPullParserException e) {
                setUploadPostErrorMessage(e);
            }

            return false;
        }

        private void prepareMediaForUpload(MediaFile mediaFile, String path) {
            File file = new File(path);
            Blog blog = WordPress.getCurrentBlog();
            long currentTime = System.currentTimeMillis();
            String mimeType = MediaUtils.getMediaFileMimeType(file);
            String fileName = MediaUtils.getMediaFileName(file, mimeType);

            mediaFile.setBlogId(String.valueOf(blog.getLocalTableBlogId()));
            mediaFile.setFileName(fileName);
            mediaFile.setFilePath(path);
            mediaFile.setUploadState("queued");
            mediaFile.setDateCreatedGMT(currentTime);
            mediaFile.setMediaId(String.valueOf(currentTime));
            mediaFile.setFeatured(true);

            if (mimeType != null && mimeType.startsWith("image")) {
                // get width and height
                BitmapFactory.Options bfo = new BitmapFactory.Options();
                bfo.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, bfo);
                mediaFile.setWidth(bfo.outWidth);
                mediaFile.setHeight(bfo.outHeight);
            }

            if (!TextUtils.isEmpty(mimeType)) {
                mediaFile.setMimeType(mimeType);
            }
        }

        private void trackUploadAnalytics() {
            mPost.getStatusEnum();

            boolean isFirstTimePublishing = false;
            if (mPost.hasChangedFromLocalDraftToPublished() ||
                    (!mPost.isUploaded() && mPost.getStatusEnum() == PostStatus.PUBLISHED)) {
                isFirstTimePublishing = true;
            }

            if (isFirstTimePublishing) {
                if (mHasImage) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_PUBLISHED_POST_WITH_PHOTO);
                }
                if (mHasVideo) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_PUBLISHED_POST_WITH_VIDEO);
                }
                if (mHasCategory) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_PUBLISHED_POST_WITH_CATEGORIES);
                }
                if (!TextUtils.isEmpty(mPost.getKeywords())) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_PUBLISHED_POST_WITH_TAGS);
                }
            }
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

            mPostUploadNotifier.setTotalMediaItems(totalMediaItems);

            int mediaItemCount = 0;
            for (String tag : imageTags) {
                Pattern p = Pattern.compile("android-uri=\"([^\"]+)\"");
                Matcher m = p.matcher(tag);
                if (m.find()) {
                    String imageUri = m.group(1);
                    if (!imageUri.equals("")) {
                        MediaFile mediaFile = WordPress.wpDB.getMediaFile(imageUri, mPost);
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
                            mPostUploadNotifier.setCurrentMediaItem(mediaItemCount);
                            mPostUploadNotifier.updateNotificationIcon(imageIcon);

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
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileName).toLowerCase();

            int orientation = ImageUtils.getImageOrientation(mContext, path);

            String resizedPictureURL = null;

            // We need to upload a resized version of the picture when the blog settings != original size, or when
            // the user has selected a smaller size for the current picture in the picture settings screen
            // We won't resize gif images to keep them awesome.
            boolean shouldUploadResizedVersion = false;
            // If it's not a gif and blog don't keep original size, there is a chance we need to resize
            if (!mimeType.equals("image/gif") && !mBlog.getMaxImageWidth().equals("Original Size")) {
                //check the picture settings
                int pictureSettingWidth = mediaFile.getWidth();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, options);
                int imageHeight = options.outHeight;
                int imageWidth = options.outWidth;
                int[] dimensions = {imageWidth, imageHeight};
                if (dimensions[0] != 0 && dimensions[0] != pictureSettingWidth) {
                    shouldUploadResizedVersion = true;
                }
            }

            boolean shouldAddImageWidthCSS = false;

            if (shouldUploadResizedVersion) {
                MediaFile resizedMediaFile = new MediaFile(mediaFile);
                // Create resized image
                byte[] bytes = ImageUtils.createThumbnailFromUri(mContext, imageUri, resizedMediaFile.getWidth(),
                        fileExtension, orientation);

                if (bytes == null) {
                    // We weren't able to resize the image, so we will upload the full size image with css to resize it
                    shouldUploadResizedVersion = false;
                    shouldAddImageWidthCSS = true;
                } else {
                    // Save temp image
                    String tempFilePath;
                    File resizedImageFile;
                    try {
                        resizedImageFile = File.createTempFile("wp-image-", fileExtension);
                        FileOutputStream out = new FileOutputStream(resizedImageFile);
                        out.write(bytes);
                        out.close();
                        tempFilePath = resizedImageFile.getPath();
                    } catch (IOException e) {
                        AppLog.w(T.POSTS, "failed to create image temp file");
                        mErrorMessage = mContext.getString(R.string.error_media_upload);
                        return null;
                    }

                    // upload resized picture
                    if (!TextUtils.isEmpty(tempFilePath)) {
                        resizedMediaFile.setFilePath(tempFilePath);
                        Map<String, Object> parameters = new HashMap<String, Object>();

                        parameters.put("name", fileName);
                        parameters.put("type", mimeType);
                        parameters.put("bits", resizedMediaFile);
                        parameters.put("overwrite", true);
                        resizedPictureURL = uploadImageFile(parameters, resizedMediaFile, mBlog);
                        if (resizedPictureURL == null) {
                            AppLog.w(T.POSTS, "failed to upload resized picture");
                            return null;
                        } else if (resizedImageFile.exists()) {
                            resizedImageFile.delete();
                        }
                    } else {
                        AppLog.w(T.POSTS, "failed to create resized picture");
                        mErrorMessage = mContext.getString(R.string.out_of_memory);
                        return null;
                    }
                }
            }

            String fullSizeUrl = null;
            // Upload the full size picture if "Original Size" is selected in settings,
            // or if 'link to full size' is checked.
            if (!shouldUploadResizedVersion || mBlog.isFullSizeImage()) {
                Map<String, Object> parameters = new HashMap<String, Object>();
                parameters.put("name", fileName);
                parameters.put("type", mimeType);
                parameters.put("bits", mediaFile);
                parameters.put("overwrite", true);

                fullSizeUrl = uploadImageFile(parameters, mediaFile, mBlog);
                if (fullSizeUrl == null) {
                    mErrorMessage = mContext.getString(R.string.error_media_upload);
                    return null;
                }
            }

            return mediaFile.getImageHtmlForUrls(fullSizeUrl, resizedPictureURL, shouldAddImageWidthCSS);
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
                        // set the width of the video to the thumbnail width, else 640x480
                        if (!mBlog.getMaxImageWidth().equals("Original Size")) {
                            xRes = mBlog.getMaxImageWidth();
                            yRes = String.valueOf(Math.round(Integer.valueOf(mBlog.getMaxImageWidth()) * 0.75));
                        } else {
                            xRes = "640";
                            yRes = "480";
                        }
                    }
                }
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

            Object[] params = {1, mBlog.getUsername(), mBlog.getPassword(), m};

            FeatureSet featureSet = synchronousGetFeatureSet();
            boolean selfHosted = WordPress.currentBlog != null && !WordPress.currentBlog.isDotcomFlag();
            boolean isVideoEnabled = selfHosted || (featureSet != null && mFeatureSet.isVideopressEnabled());
            if (isVideoEnabled) {
                File tempFile;
                try {
                    String fileExtension = MimeTypeMap.getFileExtensionFromUrl(videoName);
                    tempFile = createTempUploadFile(fileExtension);
                } catch (IOException e) {
                    mErrorMessage = getResources().getString(R.string.file_error_create);
                    return null;
                }

                Object result = uploadFileHelper(params, tempFile);
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
            } else {
                mErrorMessage = getString(R.string.media_no_video_message);
                mErrorUnavailableVideoPress = true;
                return null;
            }
        }


        private void setUploadPostErrorMessage(Exception e) {
            mErrorMessage = String.format(mContext.getResources().getText(R.string.error_upload).toString(),
                    mPost.isPage() ? mContext.getResources().getText(R.string.page).toString() :
                            mContext.getResources().getText(R.string.post).toString()) + " " + e.getMessage();
            mIsMediaError = false;
            AppLog.e(T.EDITOR, mErrorMessage, e);
        }

        private String uploadImageFile(Map<String, Object> pictureParams, MediaFile mf, Blog blog) {
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

            Object[] params = {1, blog.getUsername(), blog.getPassword(), pictureParams};
            Object result = uploadFileHelper(params, tempFile);
            if (result == null) {
                mIsMediaError = true;
                return null;
            }

            Map<?, ?> contentHash = (HashMap<?, ?>) result;
            String pictureURL = contentHash.get("url").toString();

            if (mf.isFeatured()) {
                try {
                    if (contentHash.get("id") != null) {
                        mPost.setFeaturedImageID(
                                Integer.parseInt(contentHash.get("id").toString()));
                        if (!mf.isFeaturedInPost())
                            return "";
                    }
                } catch (NumberFormatException e) {
                    AppLog.e(T.POSTS, e);
                }
            }

            return pictureURL;
        }

        private Object uploadFileHelper(Object[] params, final File tempFile) {
            // Create listener for tracking upload progress in the notification
            if (mClient instanceof XMLRPCClient) {
                XMLRPCClient xmlrpcClient = (XMLRPCClient) mClient;
                xmlrpcClient.setOnBytesUploadedListener(new XMLRPCClient.OnBytesUploadedListener() {
                    @Override
                    public void onBytesUploaded(long uploadedBytes) {
                        if (tempFile.length() == 0) {
                            return;
                        }
                        float percentage = (uploadedBytes * 100) / tempFile.length();
                        mPostUploadNotifier.updateNotificationProgress(percentage);
                    }
                });
            }

            try {
                return mClient.call("wp.uploadFile", params, tempFile);
            } catch (XMLRPCException e) {
                AppLog.e(T.API, e);
                mErrorMessage = mContext.getResources().getString(R.string.error_media_upload) + ": " + e.getMessage();
                return null;
            } catch (IOException e) {
                AppLog.e(T.API, e);
                mErrorMessage = mContext.getResources().getString(R.string.error_media_upload) + ": " + e.getMessage();
                return null;
            } catch (XmlPullParserException e) {
                AppLog.e(T.API, e);
                mErrorMessage = mContext.getResources().getString(R.string.error_media_upload) + ": " + e.getMessage();
                return null;
            } finally {
                // remove the temporary upload file now that we're done with it
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }
    }

    private File createTempUploadFile(String fileExtension) throws IOException {
        return File.createTempFile("wp-", fileExtension, mContext.getCacheDir());
    }

    private class PostUploadNotifier {

        private final NotificationManager mNotificationManager;
        private final NotificationCompat.Builder mNotificationBuilder;

        private final int mNotificationId;
        private int mNotificationErrorId = 0;
        private int mTotalMediaItems;
        private int mCurrentMediaItem;
        private float mItemProgressSize;

        public PostUploadNotifier(Post post) {
            // add the uploader to the notification bar
            mNotificationManager = (NotificationManager) SystemServiceFactory.get(mContext,
                    Context.NOTIFICATION_SERVICE);

            mNotificationBuilder = new NotificationCompat.Builder(getApplicationContext());
            mNotificationBuilder.setSmallIcon(android.R.drawable.stat_sys_upload);

            Intent notificationIntent = new Intent(mContext, post.isPage() ? PagesActivity.class : PostsActivity.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                    | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
            notificationIntent.setAction(Intent.ACTION_MAIN);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            notificationIntent.setData((Uri.parse("custom://wordpressNotificationIntent"
                    + post.getLocalTableBlogId())));
            notificationIntent.putExtra(PostsActivity.EXTRA_VIEW_PAGES, post.isPage());
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            mNotificationBuilder.setContentIntent(pendingIntent);

            mNotificationId = (new Random()).nextInt() + post.getLocalTableBlogId();
            startForeground(mNotificationId, mNotificationBuilder.build());
        }


        public void updateNotificationMessage(String title, String message) {
            if (title != null) {
                mNotificationBuilder.setContentTitle(title);
            }

            if (message != null) {
                mNotificationBuilder.setContentText(message);
            }

            mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
        }

        public void updateNotificationIcon(Bitmap icon) {
            if (icon != null) {
                mNotificationBuilder.setLargeIcon(icon);
            }

            mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
        }

        public void cancelNotification() {
            mNotificationManager.cancel(mNotificationId);
        }

        public void updateNotificationWithError(String mErrorMessage, boolean isMediaError, boolean isPage,
                                                boolean isVideoPressError) {
            AppLog.d(T.POSTS, "updateNotificationWithError: " + mErrorMessage);

            Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
            String postOrPage = (String) (isPage ? mContext.getResources().getText(R.string.page_id)
                    : mContext.getResources().getText(R.string.post_id));
            Intent notificationIntent = new Intent(mContext, isPage ? PagesActivity.class : PostsActivity.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
            notificationIntent.setAction(Intent.ACTION_MAIN);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            notificationIntent.putExtra(PostsActivity.EXTRA_VIEW_PAGES, isPage);
            notificationIntent.putExtra(PostsActivity.EXTRA_ERROR_MSG, mErrorMessage);
            if (isVideoPressError) {
                notificationIntent.putExtra(PostsActivity.EXTRA_ERROR_INFO_TITLE, getString(R.string.learn_more));
                notificationIntent.putExtra(PostsActivity.EXTRA_ERROR_INFO_LINK, Constants.videoPressURL);
            }
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            String errorText = mContext.getResources().getText(R.string.upload_failed).toString();
            if (isMediaError) {
                errorText = mContext.getResources().getText(R.string.media) + " "
                        + mContext.getResources().getText(R.string.error);
            }

            notificationBuilder.setSmallIcon(android.R.drawable.stat_notify_error);
            notificationBuilder.setContentTitle((isMediaError) ? errorText :
                    mContext.getResources().getText(R.string.upload_failed));
            notificationBuilder.setContentText((isMediaError) ? mErrorMessage : postOrPage + " " + errorText
                    + ": " + mErrorMessage);
            notificationBuilder.setContentIntent(pendingIntent);
            notificationBuilder.setAutoCancel(true);
            if (mNotificationErrorId == 0) {
                mNotificationErrorId = mNotificationId + (new Random()).nextInt();
            }
            mNotificationManager.notify(mNotificationErrorId, notificationBuilder.build());
        }

        public void updateNotificationProgress(float progress) {
            if (mTotalMediaItems == 0) {
                return;
            }

            // Simple way to show progress of entire post upload
            // Would be better if we could get total bytes for all media items.
            double currentChunkProgress = (mItemProgressSize * progress) / 100;

            if (mCurrentMediaItem > 1) {
                currentChunkProgress += mItemProgressSize * (mCurrentMediaItem - 1);
            }

            mNotificationBuilder.setProgress(100, (int)Math.ceil(currentChunkProgress), false);
            mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
        }

        public void setTotalMediaItems(int totalMediaItems) {
            if (totalMediaItems <= 0) {
                totalMediaItems = 1;
            }

            mTotalMediaItems = totalMediaItems;
            mItemProgressSize = 100.0f / mTotalMediaItems;
        }

        public void setCurrentMediaItem(int currentItem) {
            mCurrentMediaItem = currentItem;

            mNotificationBuilder.setContentText(String.format(getString(R.string.uploading_total), mCurrentMediaItem,
                    mTotalMediaItems));
        }
    }
}
