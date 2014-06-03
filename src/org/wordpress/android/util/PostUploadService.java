package org.wordpress.android.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.support.v4.content.IntentCompat;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.FeatureSet;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostLocation;
import org.wordpress.android.ui.posts.PagesActivity;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.stats.AnalyticsTracker;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.ApiHelper;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostUploadService extends Service {
    private static Context context;
    private static final ArrayList<Post> listOfPosts = new ArrayList<Post>();
    private static NotificationManager nm;
    private static Post currentUploadingPost = null;
    private UploadPostTask currentTask = null;
    private FeatureSet mFeatureSet;

    public static void addPostToUpload(Post currentPost) {
        synchronized (listOfPosts) {
            listOfPosts.add(currentPost);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this.getApplicationContext();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        synchronized (listOfPosts) {
            if (listOfPosts.size() == 0 || context == null) {
                this.stopSelf();
                return;
            }
        }
        uploadNextPost();
    }

    private FeatureSet synchronousGetFeatureSet() {
        if (WordPress.getCurrentBlog() == null || !WordPress.getCurrentBlog().isDotcomFlag())
            return null;
        ApiHelper.GetFeatures task = new ApiHelper.GetFeatures();
        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        mFeatureSet = task.doSynchronously(apiArgs);
        return mFeatureSet;
    }

    private void uploadNextPost(){
        synchronized (listOfPosts) {
            if( currentTask == null ) { //make sure nothing is running
                currentUploadingPost = null;
                if ( listOfPosts.size() > 0 ) {
                    currentUploadingPost = listOfPosts.remove(0);
                    currentTask = new UploadPostTask();
                    currentTask.execute(currentUploadingPost);
                } else {
                    this.stopSelf();
                }
            }
        }
    }

    private void postUploaded() {
        synchronized (listOfPosts) {
            currentTask = null;
            currentUploadingPost = null;
        }
        uploadNextPost();
    }

    public static boolean isUploading(Post post) {
        if ( currentUploadingPost != null && currentUploadingPost.equals(post) )
            return true;
        if( listOfPosts != null && listOfPosts.size() > 0 && listOfPosts.contains(post))
            return true;
        return false;
    }

    private class UploadPostTask extends AsyncTask<Post, Boolean, Boolean> {
        private Post post;
        private String mErrorMessage = "";
        private boolean mIsMediaError = false;
        private boolean mErrorUnavailableVideoPress = false;
        private int featuredImageID = -1;
        private int notificationID;
        private Notification n;

        @Override
        protected void onPostExecute(Boolean postUploadedSuccessfully) {
            if (postUploadedSuccessfully) {
                WordPress.postUploaded(post.getLocalTableBlogId(), post.getRemotePostId(), post.isPage());
                nm.cancel(notificationID);
                WordPress.wpDB.deleteMediaFilesForPost(post);
            } else {
                String postOrPage = (String) (post.isPage() ? context.getResources().getText(R.string.page_id) : context.getResources()
                        .getText(R.string.post_id));
                Intent notificationIntent = new Intent(context, post.isPage() ? PagesActivity.class : PostsActivity.class);
                notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                          | Intent.FLAG_ACTIVITY_NEW_TASK
                                          | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
                notificationIntent.setAction(Intent.ACTION_MAIN);
                notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                notificationIntent.setData((Uri.parse("custom://wordpressNotificationIntent" + post.getLocalTableBlogId())));
                notificationIntent.putExtra(PostsActivity.EXTRA_VIEW_PAGES, post.isPage());
                notificationIntent.putExtra(PostsActivity.EXTRA_ERROR_MSG, mErrorMessage);
                if (mErrorUnavailableVideoPress) {
                    notificationIntent.putExtra(PostsActivity.EXTRA_ERROR_INFO_TITLE, getString(R.string.learn_more));
                    notificationIntent.putExtra(PostsActivity.EXTRA_ERROR_INFO_LINK, Constants.videoPressURL);
                }
                notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                n.flags |= Notification.FLAG_AUTO_CANCEL;
                n.icon = android.R.drawable.stat_notify_error;
                String errorText = context.getResources().getText(R.string.upload_failed).toString();
                if (mIsMediaError)
                    errorText = context.getResources().getText(R.string.media) + " " + context.getResources().getText(R.string.error);
                n.setLatestEventInfo(context, (mIsMediaError) ? errorText : context.getResources().getText(R.string.upload_failed),
                        (mIsMediaError) ? mErrorMessage : postOrPage + " " + errorText + ": " + mErrorMessage, pendingIntent);

                nm.notify(notificationID, n); // needs a unique id
            }

            postUploaded();
        }

        @Override
        protected Boolean doInBackground(Post... posts) {
            mErrorUnavailableVideoPress = false;
            post = posts[0];

            // add the uploader to the notification bar
            nm = (NotificationManager) SystemServiceFactory.get(context, Context.NOTIFICATION_SERVICE);

            String postOrPage = (String) (post.isPage() ? context.getResources().getText(R.string.page_id) : context.getResources()
                    .getText(R.string.post_id));
            String message = context.getResources().getText(R.string.uploading) + " " + postOrPage;
            n = new Notification(R.drawable.notification_icon, message, System.currentTimeMillis());

            Intent notificationIntent = new Intent(context, post.isPage() ? PagesActivity.class : PostsActivity.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                      | Intent.FLAG_ACTIVITY_NEW_TASK
                                      | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
            notificationIntent.setAction(Intent.ACTION_MAIN);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            notificationIntent.setData((Uri.parse("custom://wordpressNotificationIntent" + post.getLocalTableBlogId())));
            notificationIntent.putExtra(PostsActivity.EXTRA_VIEW_PAGES, post.isPage());
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, Intent.FLAG_ACTIVITY_CLEAR_TOP);

            n.setLatestEventInfo(context, message, message, pendingIntent);

            notificationID = (new Random()).nextInt() + post.getLocalTableBlogId();
            nm.notify(notificationID, n); // needs a unique id

            Blog blog = WordPress.wpDB.instantiateBlogByLocalId(post.getLocalTableBlogId());
            if (blog == null) {
                mErrorMessage = context.getString(R.string.blog_not_found);
                return false;
            }

            boolean isFirstTimePublishing = false;
            if (TextUtils.isEmpty(post.getPostStatus())) {
                post.setPostStatus("publish");
            }

            if (post.hasChangedFromLocalDraftToPublished()) {
                isFirstTimePublishing = true;
            }

            if (!post.isUploaded() && post.getPostStatus().equals("publish")) {
                isFirstTimePublishing = true;
            }

            Boolean publishThis = false;

            // These are used for stats purposes
            Boolean hasImage = false;
            Boolean hasVideo = false;
            Boolean hasCategory = false;
            Boolean hasTag = !post.getKeywords().equals("");


            String descriptionContent = "", moreContent = "";
            int moreCount = 1;
            if (!TextUtils.isEmpty(post.getMoreText()))
                moreCount++;
            String imgTags = "<img[^>]+android-uri\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
            Pattern pattern = Pattern.compile(imgTags);

            for (int x = 0; x < moreCount; x++) {
                if (x == 0)
                    descriptionContent = post.getDescription();
                else
                    moreContent = post.getMoreText();

                Matcher matcher;

                if (x == 0) {
                    matcher = pattern.matcher(descriptionContent);
                } else {
                    matcher = pattern.matcher(moreContent);
                }

                List<String> imageTags = new ArrayList<String>();
                while (matcher.find()) {
                    imageTags.add(matcher.group());
                }

                for (String tag : imageTags) {
                    Pattern p = Pattern.compile("android-uri=\"([^\"]+)\"");
                    Matcher m = p.matcher(tag);
                    if (m.find()) {
                        String imgPath = m.group(1);
                        if (!imgPath.equals("")) {
                            MediaFile mf = WordPress.wpDB.getMediaFile(imgPath, post);
                            if (mf != null) {
                                if (mf.isVideo()) {
                                    hasVideo = true;
                                } else {
                                    hasImage = true;
                                }
                                String imgHTML = uploadMediaFile(mf, blog);
                                if (imgHTML != null) {
                                    if (x == 0) {
                                        descriptionContent = descriptionContent.replace(tag, imgHTML);
                                    } else {
                                        moreContent = moreContent.replace(tag, imgHTML);
                                    }
                                } else {
                                    if (x == 0)
                                        descriptionContent = descriptionContent.replace(tag, "");
                                    else
                                        moreContent = moreContent.replace(tag, "");
                                    mIsMediaError = true;
                                }
                            }
                        }
                    }
                }
            }

            // If media file upload failed, let's stop here and prompt the user
            if (mIsMediaError)
                return false;

            JSONArray categoriesJsonArray = post.getJSONCategories();
            String[] postCategories = null;
            if (categoriesJsonArray != null) {
                if (categoriesJsonArray.length() > 0) {
                    hasCategory = true;
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

            if (!post.isPage() && post.isLocalDraft()) {
                // add the tagline
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

                if (prefs.getBoolean("wp_pref_signature_enabled", false)) {
                    String tagline = prefs.getString("wp_pref_post_signature", "");
                    if (!TextUtils.isEmpty(tagline)) {
                        String tag = "\n\n<span class=\"post_sig\">" + tagline + "</span>\n\n";
                        if (TextUtils.isEmpty(moreContent))
                            descriptionContent += tag;
                        else
                            moreContent += tag;
                    }
                }
            }

            // post format
            if (!post.isPage()) {
                if (!TextUtils.isEmpty(post.getPostFormat())) {
                    contentStruct.put("wp_post_format", post.getPostFormat());
                }
            }

            contentStruct.put("post_type", (post.isPage()) ? "page" : "post");
            contentStruct.put("title", post.getTitle());
            long pubDate = post.getDate_created_gmt();
            if (pubDate != 0) {
                Date date_created_gmt = new Date(pubDate);
                contentStruct.put("date_created_gmt", date_created_gmt);
                Date dateCreated = new Date(pubDate + (date_created_gmt.getTimezoneOffset() * 60000));
                contentStruct.put("dateCreated", dateCreated);
            }

            if (!moreContent.equals("")) {
                descriptionContent = descriptionContent.trim() + "<!--more-->" + moreContent;
                post.setMoreText("");
            }

            // get rid of the p and br tags that the editor adds.
            if (post.isLocalDraft()) {
                descriptionContent = descriptionContent.replace("<p>", "").replace("</p>", "\n").replace("<br>", "");
            }

            // gets rid of the weird character android inserts after images
            descriptionContent = descriptionContent.replaceAll("\uFFFC", "");

            contentStruct.put("description", descriptionContent);
            if (!post.isPage()) {
                contentStruct.put("mt_keywords", post.getKeywords());

                if (postCategories != null && postCategories.length > 0)
                    contentStruct.put("categories", postCategories);
            }

            contentStruct.put("mt_excerpt", post.getPostExcerpt());

            contentStruct.put((post.isPage()) ? "page_status" : "post_status", post.getPostStatus());
            if (post.supportsLocation()) {
                JSONObject remoteGeoLatitude = post.getCustomField("geo_latitude");
                JSONObject remoteGeoLongitude = post.getCustomField("geo_longitude");
                JSONObject remoteGeoPublic = post.getCustomField("geo_public");

                Map<Object, Object> hLatitude = new HashMap<Object, Object>();
                Map<Object, Object> hLongitude = new HashMap<Object, Object>();
                Map<Object, Object> hPublic = new HashMap<Object, Object>();

                try {
                    if (remoteGeoLatitude != null) {
                        hLatitude.put("id", remoteGeoLatitude.getInt("id"));
                    } else {
                        hLatitude.put("key", "geo_latitude");
                    }

                    if (remoteGeoLongitude != null) {
                        hLongitude.put("id", remoteGeoLongitude.getInt("id"));
                    } else {
                        hLongitude.put("key", "geo_longitude");
                    }

                    if (remoteGeoPublic != null) {
                        hPublic.put("id", remoteGeoPublic.getInt("id"));
                    } else {
                        hPublic.put("key", "geo_public");
                    }

                    if (post.hasLocation()) {
                        PostLocation location = post.getLocation();
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

            // featured image
            if (featuredImageID != -1) {
                contentStruct.put("wp_post_thumbnail", featuredImageID);
            }
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());
            if (!TextUtils.isEmpty(post.getQuickPostType())) {
                client.addQuickPostHeader(post.getQuickPostType());
            }
            n.setLatestEventInfo(context, message, message, n.contentIntent);
            nm.notify(notificationID, n);

            contentStruct.put("wp_password", post.getPassword());

            Object[] params;
            if (post.isLocalDraft() && !post.isUploaded())
                params = new Object[]{blog.getRemoteBlogId(), blog.getUsername(), blog.getPassword(),
                        contentStruct, publishThis};
            else
                params = new Object[]{post.getRemotePostId(), blog.getUsername(), blog.getPassword(), contentStruct,
                        publishThis};

            try {
                if (post.isLocalDraft() && !post.isUploaded()) {
                    Object newPostId = client.call("metaWeblog.newPost", params);
                    if (newPostId instanceof String) {
                        post.setRemotePostId((String) newPostId);
                    }
                } else {
                    client.call("metaWeblog.editPost", params);
                }

                post.setUploaded(true);
                post.setLocalChange(false);
                WordPress.wpDB.updatePost(post);

                if (isFirstTimePublishing) {
                    if (hasImage) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_PUBLISHED_POST_WITH_PHOTO);
                    }
                    if (hasVideo) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_PUBLISHED_POST_WITH_VIDEO);
                    }
                    if (hasCategory) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_PUBLISHED_POST_WITH_CATEGORIES);
                    }
                    if (hasTag) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_PUBLISHED_POST_WITH_TAGS);
                    }
                }

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


        private void setUploadPostErrorMessage(Exception e) {
            mErrorMessage = String.format(context.getResources().getText(R.string.error_upload).toString(), post.isPage() ? context
                    .getResources().getText(R.string.page).toString() : context.getResources().getText(R.string.post).toString())
                    + " " + e.getMessage();
            mIsMediaError = false;
            AppLog.e(T.EDITOR, mErrorMessage, e);
        }

        public String uploadMediaFile(MediaFile mediaFile, Blog blog) {
            String content = "";

            String curImagePath = mediaFile.getFilePath();
            if (curImagePath == null) {
                return null;
            }

            if (curImagePath.contains("video")) {
                // Upload the video
                XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                        blog.getHttppassword());
                // create temp file for media upload
                String tempFileName = "wp-" + System.currentTimeMillis();
                try {
                    context.openFileOutput(tempFileName, Context.MODE_PRIVATE);
                } catch (FileNotFoundException e) {
                    mErrorMessage = getResources().getString(R.string.file_error_create);
                    mIsMediaError = true;
                    return null;
                }

                Uri videoUri = Uri.parse(curImagePath);
                File videoFile = null;
                String mimeType = "", xRes = "", yRes = "";

                if (videoUri.toString().contains("content:")) {
                    String[] projection = new String[]{Video.Media._ID, Video.Media.DATA, Video.Media.MIME_TYPE, Video.Media.RESOLUTION};
                    Cursor cur = context.getContentResolver().query(videoUri, projection, null, null, null);

                    if (cur != null && cur.moveToFirst()) {
                        int mimeTypeColumn, resolutionColumn, dataColumn;

                        dataColumn = cur.getColumnIndex(Video.Media.DATA);
                        mimeTypeColumn = cur.getColumnIndex(Video.Media.MIME_TYPE);
                        resolutionColumn = cur.getColumnIndex(Video.Media.RESOLUTION);

                        mediaFile = new MediaFile();

                        String thumbData = cur.getString(dataColumn);
                        mimeType = cur.getString(mimeTypeColumn);

                        videoFile = new File(thumbData);
                        mediaFile.setFilePath(videoFile.getPath());
                        String resolution = cur.getString(resolutionColumn);
                        if (resolution != null) {
                            String[] resx = resolution.split("x");
                            xRes = resx[0];
                            yRes = resx[1];
                        } else {
                            // set the width of the video to the thumbnail width, else 640x480
                            if (!blog.getMaxImageWidth().equals("Original Size")) {
                                xRes = blog.getMaxImageWidth();
                                yRes = String.valueOf(Math.round(Integer.valueOf(blog.getMaxImageWidth()) * 0.75));
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
                    mErrorMessage = context.getResources().getString(R.string.error_media_upload);
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

                Object[] params = {1, blog.getUsername(), blog.getPassword(), m};

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
                        mIsMediaError = true;
                        return null;
                    }

                    Object result = uploadFileHelper(client, params, tempFile);
                    Map<?, ?> resultMap = (HashMap<?, ?>) result;
                    if (resultMap != null && resultMap.containsKey("url")) {
                        String resultURL = resultMap.get("url").toString();
                        if (resultMap.containsKey("videopress_shortcode")) {
                            resultURL = resultMap.get("videopress_shortcode").toString() + "\n";
                        } else {
                            resultURL = String.format(
                                    "<video width=\"%s\" height=\"%s\" controls=\"controls\"><source src=\"%s\" type=\"%s\" /><a href=\"%s\">Click to view video</a>.</video>",
                                    xRes, yRes, resultURL, mimeType, resultURL);
                        }
                        content = content + resultURL;
                    } else {
                        return null;
                    }
                } else {
                    mErrorMessage = getString(R.string.media_no_video_message);
                    mErrorUnavailableVideoPress = true;
                    return null;
                }
            } else {
                // Upload the image
                curImagePath = mediaFile.getFilePath();

                Uri imageUri = Uri.parse(curImagePath);
                File imageFile = null;
                String mimeType = "", path = "";
                int orientation;

                if (imageUri.toString().contains("content:")) {
                    String[] projection;
                    Uri imgPath;

                    projection = new String[]{Images.Media._ID, Images.Media.DATA, Images.Media.MIME_TYPE};

                    imgPath = imageUri;

                    Cursor cur = context.getContentResolver().query(imgPath, projection, null, null, null);
                    if (cur != null && cur.moveToFirst()) {
                        int dataColumn, mimeTypeColumn;
                        dataColumn = cur.getColumnIndex(Images.Media.DATA);
                        mimeTypeColumn = cur.getColumnIndex(Images.Media.MIME_TYPE);

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
                    mErrorMessage = context.getString(R.string.file_not_found);
                    mIsMediaError = true;
                    return null;
                }

                if (TextUtils.isEmpty(mimeType)) {
                    mimeType = MediaUtils.getMediaFileMimeType(imageFile);
                }
                String fileName = MediaUtils.getMediaFileName(imageFile, mimeType);
                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileName).toLowerCase();

                ImageHelper ih = new ImageHelper();
                orientation = ih.getImageOrientation(context, path);

                String resizedPictureURL = null;

                // We need to upload a resized version of the picture when the blog settings != original size, or when
                // the user has selected a smaller size for the current picture in the picture settings screen
                // We won't resize gif images to keep them awesome.
                boolean shouldUploadResizedVersion = false;
                // If it's not a gif and blog don't keep original size, there is a chance we need to resize
                if (!mimeType.equals("image/gif") && !blog.getMaxImageWidth().equals("Original Size")) {
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
                    byte[] bytes = ih.createThumbnailFromUri(context, imageUri, resizedMediaFile.getWidth(),
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
                            mErrorMessage = context.getString(R.string.error_media_upload);
                            mIsMediaError = true;
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
                            resizedPictureURL = uploadPicture(parameters, resizedMediaFile, blog);
                            if (resizedPictureURL == null) {
                                AppLog.w(T.POSTS, "failed to upload resized picture");
                                return null;
                            } else if (resizedImageFile != null && resizedImageFile.exists()) {
                                resizedImageFile.delete();
                            }
                        } else {
                            AppLog.w(T.POSTS, "failed to create resized picture");
                            mErrorMessage = context.getString(R.string.out_of_memory);
                            mIsMediaError = true;
                            return null;
                        }
                    }
                }

                String fullSizeUrl = null;
                // Upload the full size picture if "Original Size" is selected in settings, or if 'link to full size' is checked.
                if (!shouldUploadResizedVersion || blog.isFullSizeImage()) {
                    // try to upload the image
                    Map<String, Object> parameters = new HashMap<String, Object>();
                    parameters.put("name", fileName);
                    parameters.put("type", mimeType);
                    parameters.put("bits", mediaFile);
                    parameters.put("overwrite", true);

                    fullSizeUrl = uploadPicture(parameters, mediaFile, blog);
                    if (fullSizeUrl == null)
                        return null;
                }

                String alignment = "";
                switch (mediaFile.getHorizontalAlignment()) {
                    case 0:
                        alignment = "alignnone";
                        break;
                    case 1:
                        alignment = "alignleft";
                        break;
                    case 2:
                        alignment = "aligncenter";
                        break;
                    case 3:
                        alignment = "alignright";
                        break;
                }

                String alignmentCSS = "class=\"" + alignment + " size-full\" ";

                if (shouldAddImageWidthCSS) {
                    alignmentCSS += "style=\"max-width: " + mediaFile.getWidth() + "px\" ";
                }

                // Check if we uploaded a featured picture that is not added to the post content (normal case)
                if ((fullSizeUrl != null && fullSizeUrl.equalsIgnoreCase("")) ||
                        (resizedPictureURL != null && resizedPictureURL.equalsIgnoreCase(""))) {
                    return ""; // Not featured in post. Do not add to the content.
                }

                if (fullSizeUrl == null && resizedPictureURL != null) {
                    fullSizeUrl = resizedPictureURL;
                } else if (fullSizeUrl != null && resizedPictureURL == null){
                    resizedPictureURL = fullSizeUrl;
                }

                String mediaTitle = TextUtils.isEmpty(mediaFile.getTitle()) ? "" : mediaFile.getTitle();

                content = content + "<a href=\"" + fullSizeUrl + "\"><img title=\"" + mediaTitle + "\" "
                        + alignmentCSS + "alt=\"image\" src=\"" + resizedPictureURL + "\" /></a>";

                if (!TextUtils.isEmpty(mediaFile.getCaption())) {
                    content = String.format("[caption id=\"\" align=\"%s\" width=\"%d\" caption=\"%s\"]%s[/caption]",
                            alignment, mediaFile.getWidth(), TextUtils.htmlEncode(mediaFile.getCaption()), content);
                }
            }
            return content;
        }

        private String uploadPicture(Map<String, Object> pictureParams, MediaFile mf, Blog blog) {
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                    blog.getHttppassword());

            // create temporary upload file
            File tempFile;
            try {
                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(mf.getFileName());
                tempFile = createTempUploadFile(fileExtension);
            } catch (IOException e) {
                mIsMediaError = true;
                mErrorMessage = context.getString(R.string.file_not_found);
                return null;
            }

            Object[] params = { 1, blog.getUsername(), blog.getPassword(), pictureParams };
            Object result = uploadFileHelper(client, params, tempFile);
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

        private Object uploadFileHelper(XMLRPCClientInterface client, Object[] params, File tempFile) {
            try {
                return client.call("wp.uploadFile", params, tempFile);
            } catch (XMLRPCException e) {
                AppLog.e(T.API, e);
                mErrorMessage = context.getResources().getString(R.string.error_media_upload) + ": " + e.getMessage();
                return null;
            } catch (IOException e) {
                AppLog.e(T.API, e);
                mErrorMessage = context.getResources().getString(R.string.error_media_upload) + ": " + e.getMessage();
                return null;
            } catch (XmlPullParserException e) {
                AppLog.e(T.API, e);
                mErrorMessage = context.getResources().getString(R.string.error_media_upload) + ": " + e.getMessage();
                return null;
            } finally {
                // remove the temporary upload file now that we're done with it
                if (tempFile != null && tempFile.exists())
                    tempFile.delete();
            }
        }
    }

    private File createTempUploadFile(String fileExtension) throws IOException {
        return File.createTempFile("wp-", fileExtension, context.getCacheDir());
    }
}
