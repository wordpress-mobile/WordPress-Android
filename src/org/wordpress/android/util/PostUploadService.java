package org.wordpress.android.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.posts.PagesActivity;
import org.wordpress.android.ui.posts.PostsActivity;

public class PostUploadService extends Service {

    private static Context context;
    private static ArrayList<Post> listOfPosts = new ArrayList<Post>();
    private static NotificationManager nm;
    private UploadPostTask currentTask = null;
        
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

    private void uploadNextPost(){
        synchronized (listOfPosts) {
            if( currentTask == null ){ //make sure nothing is running
                if ( listOfPosts.size() > 0 ) {
                    Post currentPost = listOfPosts.remove(0);
                    currentTask = new UploadPostTask();
                    currentTask.execute(currentPost);
                } else {
                    this.stopSelf();
                }
            }
        }
    }

    private void postUploaded() {
        synchronized (listOfPosts) {
            currentTask = null;
        }
        uploadNextPost();
    }
    
    
    private class UploadPostTask extends AsyncTask<Post, Boolean, Boolean> {

        private Post post;
        String error = "";
        boolean mediaError = false;
        private int featuredImageID = -1;

        private int notificationID;
        private Notification n;
        
        @Override
        protected void onPostExecute(Boolean postUploadedSuccessfully) {

            if (postUploadedSuccessfully) {
                WordPress.postUploaded();
                nm.cancel(notificationID);
            } else {
                String postOrPage = (String) (post.isPage() ? context.getResources().getText(R.string.page_id) : context.getResources()
                        .getText(R.string.post_id));
                Intent notificationIntent = new Intent(context, (post.isPage()) ? PagesActivity.class : PostsActivity.class);
                notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                        | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
                notificationIntent.setAction("android.intent.action.MAIN");
                notificationIntent.addCategory("android.intent.category.LAUNCHER");
                notificationIntent.setData((Uri.parse("custom://wordpressNotificationIntent" + post.getBlogID())));
                notificationIntent.putExtra("errorMessage", error);
                notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                n.flags |= Notification.FLAG_AUTO_CANCEL;
                String errorText = context.getResources().getText(R.string.upload_failed).toString();
                if (mediaError)
                    errorText = context.getResources().getText(R.string.media) + " " + context.getResources().getText(R.string.error);
                n.setLatestEventInfo(context, (mediaError) ? errorText : context.getResources().getText(R.string.upload_failed),
                        (mediaError) ? error : postOrPage + " " + errorText + ": " + error, pendingIntent);

                nm.notify(notificationID, n); // needs a unique id
            }
            
            postUploaded();
        }

        @Override
        protected Boolean doInBackground(Post... posts) {

            post = posts[0];

            // add the uploader to the notification bar
            nm = (NotificationManager) context.getSystemService("notification");

            String postOrPage = (String) (post.isPage() ? context.getResources().getText(R.string.page_id) : context.getResources()
                    .getText(R.string.post_id));
            String message = context.getResources().getText(R.string.uploading) + " " + postOrPage;
            n = new Notification(R.drawable.notification_icon, message, System.currentTimeMillis());

            Intent notificationIntent = new Intent(context, PostsActivity.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                    | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
            notificationIntent.setAction("android.intent.action.MAIN");
            notificationIntent.addCategory("android.intent.category.LAUNCHER");
            notificationIntent.setData((Uri.parse("custom://wordpressNotificationIntent" + post.getBlogID())));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, Intent.FLAG_ACTIVITY_CLEAR_TOP);

            n.setLatestEventInfo(context, message, message, pendingIntent);

            notificationID = (new Random()).nextInt() + Integer.valueOf(post.getBlogID());
            nm.notify(notificationID, n); // needs a unique id

            if (post.getPost_status() == null) {
                post.setPost_status("publish");
            }
            Boolean publishThis = false;

            Spannable s;
            String descriptionContent = "", moreContent = "";
            int moreCount = 1;
            if (post.getMt_text_more() != null)
                moreCount++;
            String imgTags = "<img[^>]+android-uri\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
            Pattern pattern = Pattern.compile(imgTags);

            for (int x = 0; x < moreCount; x++) {
                if (post.isLocalDraft()) {
                    if (x == 0)
                        s = (Spannable) WPHtml.fromHtml(post.getDescription(), context, post);
                    else
                        s = (Spannable) WPHtml.fromHtml(post.getMt_text_more(), context, post);
                    WPImageSpan[] click_spans = s.getSpans(0, s.length(), WPImageSpan.class);

                    if (click_spans.length != 0) {

                        for (int i = 0; i < click_spans.length; i++) {
                            String prompt = context.getResources().getText(R.string.uploading_media_item) + String.valueOf(i + 1);
                            n.setLatestEventInfo(context, context.getResources().getText(R.string.uploading) + " " + postOrPage, prompt,
                                    n.contentIntent);
                            nm.notify(notificationID, n);
                            WPImageSpan wpIS = click_spans[i];
                            int start = s.getSpanStart(wpIS);
                            int end = s.getSpanEnd(wpIS);
                            MediaFile mf = new MediaFile();
                            mf.setPostID(post.getId());
                            mf.setTitle(wpIS.getTitle());
                            mf.setCaption(wpIS.getCaption());
                            mf.setDescription(wpIS.getDescription());
                            mf.setFeatured(wpIS.isFeatured());
                            mf.setFeaturedInPost(wpIS.isFeaturedInPost());
                            mf.setFileName(wpIS.getImageSource().toString());
                            mf.setHorizontalAlignment(wpIS.getHorizontalAlignment());
                            mf.setWidth(wpIS.getWidth());

                            String imgHTML = uploadMediaFile(mf);
                            if (imgHTML != null) {
                                SpannableString ss = new SpannableString(imgHTML);
                                s.setSpan(ss, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                s.removeSpan(wpIS);
                            } else {
                                s.removeSpan(wpIS);
                                mediaError = true;
                            }
                        }
                    }

                    if (x == 0)
                        descriptionContent = WPHtml.toHtml(s);
                    else
                        moreContent = WPHtml.toHtml(s);
                } else {
                    Matcher matcher;
                    if (x == 0) {
                        descriptionContent = post.getDescription();
                        matcher = pattern.matcher(descriptionContent);
                    } else {
                        moreContent = post.getMt_text_more();
                        matcher = pattern.matcher(moreContent);
                    }

                    List<String> imageTags = new ArrayList<String>();
                    while (matcher.find()) {
                        imageTags.add(matcher.group());
                    }

                    for (String tag : imageTags) {

                        Pattern p = Pattern.compile("android-uri=\"([^\"]+)\"");
                        Matcher m = p.matcher(tag);
                        String imgPath = "";
                        if (m.find()) {
                            imgPath = m.group(1);
                            if (!imgPath.equals("")) {
                                MediaFile mf = WordPress.wpDB.getMediaFile(imgPath, post);

                                if (mf != null) {
                                    String imgHTML = uploadMediaFile(mf);
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
                                        mediaError = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // If media file upload failed, let's stop here and prompt the user
            if (mediaError)
                return false;

            JSONArray categoriesJsonArray = post.getJSONCategories();
            String[] theCategories = null;
            if (categoriesJsonArray != null) {
                theCategories = new String[categoriesJsonArray.length()];
                for (int i = 0; i < categoriesJsonArray.length(); i++) {
                    try {
                        theCategories[i] = TextUtils.htmlEncode(categoriesJsonArray.getString(i));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            Map<String, Object> contentStruct = new HashMap<String, Object>();

            if (!post.isPage() && post.isLocalDraft()) {
                // add the tagline
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String tagline = "";

                    if (prefs.getBoolean("wp_pref_signature_enabled", false)) {
                        tagline = prefs.getString("wp_pref_post_signature", "");
                        if (tagline != null) {
                            String tag = "\n\n<span class=\"post_sig\">" + tagline + "</span>\n\n";
                            if (moreContent == "")
                                descriptionContent += tag;
                            else
                                moreContent += tag;
                        }
                    }

                // post format
                if (!post.getWP_post_format().equals("")) {
                    if (!post.getWP_post_format().equals("standard"))
                        contentStruct.put("wp_post_format", post.getWP_post_format());
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
                post.setMt_text_more("");
            }

            // get rid of the p and br tags that the editor adds.
            if (post.isLocalDraft()) {
                descriptionContent = descriptionContent.replace("<p>", "").replace("</p>", "\n").replace("<br>", "");
            }

            // gets rid of the weird character android inserts after images
            descriptionContent = descriptionContent.replaceAll("\uFFFC", "");

            contentStruct.put("description", descriptionContent);
            if (!post.isPage()) {
                if (post.getMt_keywords() != "") {
                    contentStruct.put("mt_keywords", post.getMt_keywords());
                }
                
                if (theCategories != null && theCategories.length > 0)
                    contentStruct.put("categories", theCategories);
            }

            if (post.getMt_excerpt() != null)
                contentStruct.put("mt_excerpt", post.getMt_excerpt());

            contentStruct.put((post.isPage()) ? "page_status" : "post_status", post.getPost_status());
            Double latitude = 0.0;
            Double longitude = 0.0;
            if (!post.isPage()) {
                latitude = (Double) post.getLatitude();
                longitude = (Double) post.getLongitude();

                if (latitude > 0) {
                    Map<Object, Object> hLatitude = new HashMap<Object, Object>();
                    hLatitude.put("key", "geo_latitude");
                    hLatitude.put("value", latitude);

                    Map<Object, Object> hLongitude = new HashMap<Object, Object>();
                    hLongitude.put("key", "geo_longitude");
                    hLongitude.put("value", longitude);

                    Map<Object, Object> hPublic = new HashMap<Object, Object>();
                    hPublic.put("key", "geo_public");
                    hPublic.put("value", 1);

                    Object[] geo = { hLatitude, hLongitude, hPublic };

                    contentStruct.put("custom_fields", geo);
                }
            }

            // featured image
            if (featuredImageID != -1)
                contentStruct.put("wp_post_thumbnail", featuredImageID);

            XMLRPCClient client = new XMLRPCClient(post.getBlog().getUrl(), post.getBlog().getHttpuser(), post.getBlog().getHttppassword());

            if (post.getQuickPostType() != null)
                client.addQuickPostHeader(post.getQuickPostType());

            n.setLatestEventInfo(context, message, message, n.contentIntent);
            nm.notify(notificationID, n);
            if (post.getWP_password() != null) {
                contentStruct.put("wp_password", post.getWP_password());
            }
            Object[] params;

            if (post.isLocalDraft() && !post.isUploaded())
                params = new Object[] { post.getBlog().getBlogId(), post.getBlog().getUsername(), post.getBlog().getPassword(),
                        contentStruct, publishThis };
            else
                params = new Object[] { post.getPostid(), post.getBlog().getUsername(), post.getBlog().getPassword(), contentStruct,
                        publishThis };

            try {
                client.call((post.isLocalDraft() && !post.isUploaded()) ? "metaWeblog.newPost" : "metaWeblog.editPost", params);
                post.setUploaded(true);
                post.setLocalChange(false);
                post.update();
                return true;
            } catch (final XMLRPCException e) {
                error = String.format(context.getResources().getText(R.string.error_upload).toString(), post.isPage() ? context
                        .getResources().getText(R.string.page).toString() : context.getResources().getText(R.string.post).toString())
                        + " " + cleanXMLRPCErrorMessage(e.getMessage());
                mediaError = false;
                Log.i("WP", error);
            }

            return false;
        }

        public String uploadMediaFile(MediaFile mf) {
            String content = "";

            // image variables
            String finalThumbnailUrl = null;
            String finalImageUrl = null;

            // check for image, and upload it
            if (mf.getFileName() != null) {

                String curImagePath = "";

                curImagePath = mf.getFileName();
                boolean video = false;
                if (curImagePath.contains("video")) {
                    video = true;
                }

                if (video) { // upload the video

                    XMLRPCClient client = new XMLRPCClient(post.getBlog().getUrl(), post.getBlog().getHttpuser(), post.getBlog()
                            .getHttppassword());
                    
                    // create temp file for media upload
                    String tempFileName = "wp-" + System.currentTimeMillis();
                    try {
                        context.openFileOutput(tempFileName, Context.MODE_PRIVATE);
                    } catch (FileNotFoundException e) {
                        error = getResources().getString(R.string.file_error_create);
                        mediaError = true;
                        return null;
                    }

                    File tempFile = context.getFileStreamPath(tempFileName);

                    Uri videoUri = Uri.parse(curImagePath);
                    File fVideo = null;
                    String mimeType = "", xRes = "", yRes = "";

                    if (videoUri.toString().contains("content:")) {
                        String[] projection;
                        Uri imgPath;

                        projection = new String[] { Video.Media._ID, Video.Media.DATA, Video.Media.MIME_TYPE, Video.Media.RESOLUTION };
                        imgPath = videoUri;

                        Cursor cur = context.getContentResolver().query(imgPath, projection, null, null, null);
                        String thumbData = "";

                        if (cur.moveToFirst()) {

                            int mimeTypeColumn, resolutionColumn, dataColumn;

                            dataColumn = cur.getColumnIndex(Video.Media.DATA);
                            mimeTypeColumn = cur.getColumnIndex(Video.Media.MIME_TYPE);
                            resolutionColumn = cur.getColumnIndex(Video.Media.RESOLUTION);

                            mf = new MediaFile();

                            thumbData = cur.getString(dataColumn);
                            mimeType = cur.getString(mimeTypeColumn);
                            if ( mimeType.equalsIgnoreCase( "video/mp4v-es" ) ) { //Fixes #533. See: http://tools.ietf.org/html/rfc3016
                                mimeType = "video/mp4";
                            }
                                                    
                            fVideo = new File(thumbData);
                            mf.setFilePath(fVideo.getPath());
                            String resolution = cur.getString(resolutionColumn);
                            if (resolution != null) {
                                String[] resx = resolution.split("x");
                                xRes = resx[0];
                                yRes = resx[1];
                            } else {
                                // set the width of the video to the
                                // thumbnail
                                // width, else 640x480
                                if (!post.getBlog().getMaxImageWidth().equals("Original Size")) {
                                    xRes = post.getBlog().getMaxImageWidth();
                                    yRes = String.valueOf(Math.round(Integer.valueOf(post.getBlog().getMaxImageWidth()) * 0.75));
                                } else {
                                    xRes = "640";
                                    yRes = "480";
                                }

                            }

                        }
                    } else { // file is not in media library
                        fVideo = new File(videoUri.toString().replace("file://", ""));
                    }
                    
                    if (fVideo == null) {
                        error = context.getResources().getString(R.string.error_media_upload) + ".";
                        return null;
                    }

                    String imageTitle = fVideo.getName();

                    // try to upload the video
                    Map<String, Object> m = new HashMap<String, Object>();

                    m.put("name", imageTitle);
                    m.put("type", mimeType);
                    m.put("bits", mf);
                    m.put("overwrite", true);

                    Object[] params = { 1, post.getBlog().getUsername(), post.getBlog().getPassword(), m };

                    Object result = null;

                    try {
                        result = (Object) client.call("wp.uploadFile", params, tempFile);
                    } catch (XMLRPCException e) {
                        error = context.getResources().getString(R.string.error_media_upload) + ": " + cleanXMLRPCErrorMessage(e.getMessage());
                        return null;
                    }

                    Map<?, ?> contentHash = (HashMap<?, ?>) result;

                    String resultURL = contentHash.get("url").toString();
                    if (contentHash.containsKey("videopress_shortcode")) {
                        resultURL = contentHash.get("videopress_shortcode").toString() + "\n";
                    } else {
                        resultURL = String
                                .format("<video width=\"%s\" height=\"%s\" controls=\"controls\"><source src=\"%s\" type=\"%s\" /><a href=\"%s\">Click to view video</a>.</video>",
                                        xRes, yRes, resultURL, mimeType, resultURL);
                    }

                    content = content + resultURL;

                } // end video
                else {
                    
                    curImagePath = mf.getFileName();

                    Uri imageUri = Uri.parse(curImagePath);
                    File jpeg = null;
                    String mimeType = "", orientation = "", path = "";

                    if (imageUri.toString().contains("content:")) {
                        String[] projection;
                        Uri imgPath;

                        projection = new String[] { Images.Media._ID, Images.Media.DATA, Images.Media.MIME_TYPE,
                                Images.Media.ORIENTATION };

                        imgPath = imageUri;

                        Cursor cur = context.getContentResolver().query(imgPath, projection, null, null, null);
                        String thumbData = "";

                        if (cur.moveToFirst()) {

                            int dataColumn, mimeTypeColumn, orientationColumn;

                            dataColumn = cur.getColumnIndex(Images.Media.DATA);
                            mimeTypeColumn = cur.getColumnIndex(Images.Media.MIME_TYPE);
                            orientationColumn = cur.getColumnIndex(Images.Media.ORIENTATION);

                            orientation = cur.getString(orientationColumn);
                            thumbData = cur.getString(dataColumn);
                            mimeType = cur.getString(mimeTypeColumn);
                            jpeg = new File(thumbData);
                            path = thumbData;
                            mf.setFilePath(jpeg.getPath());
                        }
                    } else { // file is not in media library
                        path = imageUri.toString().replace("file://", "");
                        jpeg = new File(path);
                        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
                        if (extension != null) {
                            MimeTypeMap mime = MimeTypeMap.getSingleton();
                            mimeType = mime.getMimeTypeFromExtension(extension);
                            if (mimeType == null)
                                mimeType = "image/jpeg";
                        }
                        mf.setFilePath(path);
                    }

                    // check if the file exists
                    if (jpeg == null) {
                        error = context.getString(R.string.file_not_found);
                        mediaError = true;
                        return null;
                    }

                    ImageHelper ih = new ImageHelper();
                    orientation = ih.getExifOrientation(path, orientation);

                    String imageTitle = jpeg.getName();
                   
                    String resizedPictureURL = null;
                    
                    //We need to upload a resized version of the picture when the blog settings != original size, or when 
                    //the user has selected a smaller size for the current picture in the picture settings screen
                    boolean shouldUploadResizedVersion = !post.getBlog().getMaxImageWidth().equals("Original Size");
                    if( shouldUploadResizedVersion == false ) {
                        //check the picture settings
                        int pictureSettingWidth = mf.getWidth();
                        
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(path, options);
                        int imageHeight = options.outHeight;
                        int imageWidth = options.outWidth;
                        int[] dimensions = { imageWidth, imageHeight};
                        if( dimensions[0] != 0 && dimensions[0] != pictureSettingWidth ) {
                            shouldUploadResizedVersion = true;
                        }
                    }
                         
                    if (shouldUploadResizedVersion) {
                        byte[] bytes;
                        byte[] finalBytes = null;
                        try {
                            bytes = new byte[(int) jpeg.length()];
                        } catch (OutOfMemoryError er) {
                            error = context.getString(R.string.out_of_memory);
                            mediaError = true;
                            return null;
                        }

                        DataInputStream in = null;
                        try {
                            in = new DataInputStream(new FileInputStream(jpeg));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        try {
                            in.readFully(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        String width = String.valueOf(mf.getWidth());

                        ImageHelper ih2 = new ImageHelper();
                        finalBytes = ih2.createThumbnail(bytes, width, orientation, false);

                        if (finalBytes == null) {
                            error = context.getString(R.string.out_of_memory);
                            mediaError = true;
                            return null;
                        }
                        
                        //upload picture
                        Map<String, Object> m = new HashMap<String, Object>();
                
                        m.put("name", imageTitle);
                        m.put("type", mimeType);
                        m.put("bits", finalBytes);
                        m.put("overwrite", true);
                        
                        resizedPictureURL = uploadPicture(m, mf);
                        if ( resizedPictureURL == null )
                            return null;
                    }

                    String fullsizeURL = null;
                    //Upload the full size picture if "Original Size" is selected in settings, or if 'link to full size' is checked.
                    if( !shouldUploadResizedVersion || post.getBlog().isFullSizeImage() ) {
                        // try to upload the image
                        Map<String, Object> m = new HashMap<String, Object>();
                        m.put("name", imageTitle);
                        m.put("type", mimeType);
                        m.put("bits", mf);
                        m.put("overwrite", true);
                        
                        fullsizeURL = uploadPicture(m, mf);
                        if ( fullsizeURL == null )
                            return null;
                    }
                    
                    String alignment = "";
                    switch (mf.getHorizontalAlignment()) {
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
                    
                    //Check if we uploaded a featured picture that is not added to the post content (normal case)
                    if( ( fullsizeURL != null && fullsizeURL.equalsIgnoreCase("") ) 
                          || 
                        ( resizedPictureURL != null && resizedPictureURL.equalsIgnoreCase("") ) ) {
                        return ""; //Not featured in post. Do not add to the content.
                    }
                    
                    if( fullsizeURL != null && resizedPictureURL != null ) {
                        
                    } else if( fullsizeURL == null ) {
                        fullsizeURL = resizedPictureURL;
                    } else {
                        resizedPictureURL = fullsizeURL;
                    }
                    
                    content = content + "<a href=\"" + fullsizeURL + "\"><img title=\"" + mf.getTitle() + "\" "
                            + alignmentCSS + "alt=\"image\" src=\"" + resizedPictureURL + "\" /></a>";

                    if (!mf.getCaption().equals("")) {
                        content = String.format("[caption id=\"\" align=\"%s\" width=\"%d\" caption=\"%s\"]%s[/caption]",
                                alignment, mf.getWidth(), TextUtils.htmlEncode(mf.getCaption()), content);
                    }
                }
            }// end image stuff
            return content;
        }
   

        private String uploadPicture(Map<String, Object> pictureParams, MediaFile mf) {
            
            XMLRPCClient client = new XMLRPCClient(post.getBlog().getUrl(), post.getBlog().getHttpuser(), post.getBlog().getHttppassword());
            
            // create temp file for media upload
            String tempFileName = "wp-" + System.currentTimeMillis();
            try {
                context.openFileOutput(tempFileName, Context.MODE_PRIVATE);
            } catch (FileNotFoundException e) {
                mediaError = true;
                error = context.getString(R.string.file_not_found);
                return null;
            }

            File tempFile = context.getFileStreamPath(tempFileName);
    
            Object[] params = { 1, post.getBlog().getUsername(), post.getBlog().getPassword(), pictureParams };
    
            Object result = null;
    
            try {
                result = (Object) client.call("wp.uploadFile", params, tempFile);
            } catch (XMLRPCException e) {
                error = context.getResources().getString(R.string.error_media_upload) + ": " + cleanXMLRPCErrorMessage(e.getMessage());
                mediaError = true;
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
                    e.printStackTrace();
                }
            }
            
            return pictureURL;
        }
    }
    
    public String cleanXMLRPCErrorMessage(String message) {
        if (message != null) {
            if (message.indexOf(": ") > -1)
                message = message.substring(message.indexOf(": ") + 2, message.length());
            if (message.indexOf("[code") > -1)
                message = message.substring(0, message.indexOf("[code"));
            return message;
        } else {
            return "";
        }
    }
}
