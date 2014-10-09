package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.Version;
import org.wordpress.android.widgets.WPImageSpan;
import org.wordpress.passcodelock.AppLockManager;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MediaUtils {
    public class RequestCode {
        public static final int ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY = 1000;
        public static final int ACTIVITY_REQUEST_CODE_TAKE_PHOTO = 1100;
        public static final int ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY = 1200;
        public static final int ACTIVITY_REQUEST_CODE_TAKE_VIDEO = 1300;
    }

    public interface LaunchCameraCallback {
        public void onMediaCapturePathReady(String mediaCapturePath);
    }

    public static boolean isValidImage(String url) {
        if (url == null)
            return false;

        if (url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".gif"))
            return true;
        return false;
    }

    private static boolean isDocument(String url) {
        if (url == null)
            return false;

        if (url.endsWith(".doc") || url.endsWith(".docx") || url.endsWith(".odt") || url.endsWith(".pdf"))
            return true;
        return false;
    }

    private static boolean isPowerpoint(String url) {
        if (url == null)
            return false;

        if (url.endsWith(".ppt") || url.endsWith(".pptx") || url.endsWith(".pps") || url.endsWith(".ppsx") || url.endsWith(".key"))
            return true;
        return false;
    }

    private static boolean isSpreadsheet(String url) {
        if (url == null)
            return false;

        if (url.endsWith(".xls") || url.endsWith(".xlsx"))
            return true;
        return false;
    }

    private static boolean isVideo(String url) {
        if (url == null)
            return false;
        if (url.endsWith(".ogv") || url.endsWith(".mp4") || url.endsWith(".m4v") || url.endsWith(".mov") ||
                url.endsWith(".wmv") || url.endsWith(".avi") || url.endsWith(".mpg") || url.endsWith(".3gp") || url.endsWith(".3g2"))
            return true;
        return false;
    }

    public static int getPlaceholder(String url) {
        if (isValidImage(url))
            return R.drawable.media_image_placeholder;
        else if(isDocument(url))
            return R.drawable.media_document;
        else if(isPowerpoint(url))
            return R.drawable.media_powerpoint;
        else if(isSpreadsheet(url))
            return R.drawable.media_spreadsheet;
        else if(isVideo(url))
            return R.drawable.media_movieclip;
        return 0;
    }

    /** E.g. Jul 2, 2013 @ 21:57 **/
    public static String getDate(long ms) {
        Date date = new Date(ms);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy '@' HH:mm", Locale.ENGLISH);

        // The timezone on the website is at GMT
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        return sdf.format(date);
    }


    public static void launchPictureLibrary(Fragment fragment) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        AppLockManager.getInstance().setExtendedTimeout();
        fragment.startActivityForResult(Intent.createChooser(intent, fragment.getString(R.string.pick_photo)), RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY);
    }

    public static void launchCamera(Fragment fragment, LaunchCameraCallback callback) {
        String state = android.os.Environment.getExternalStorageState();
        if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            showSDCardRequiredDialog(fragment.getActivity());
        } else {
            Intent intent = prepareLaunchCameraIntent(callback);
            fragment.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO);
            AppLockManager.getInstance().setExtendedTimeout();
        }
    }

    private static Intent prepareLaunchCameraIntent(LaunchCameraCallback callback) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        String mediaCapturePath = path + File.separator + "Camera" + File.separator + "wp-" + System.currentTimeMillis() + ".jpg";
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mediaCapturePath)));

        if (callback != null) {
            callback.onMediaCapturePathReady(mediaCapturePath);
        }

        // make sure the directory we plan to store the recording in exists
        File directory = new File(mediaCapturePath).getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            try {
                throw new IOException("Path to file could not be created.");
            } catch (IOException e) {
                AppLog.e(T.POSTS, e);
            }
        }
        return intent;
    }

    private static void showSDCardRequiredDialog(Activity activity) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(activity.getResources().getText(R.string.sdcard_title));
        dialogBuilder.setMessage(activity.getResources().getText(R.string.sdcard_message));
        dialogBuilder.setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    public static void launchVideoLibrary(Fragment fragment) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        AppLockManager.getInstance().setExtendedTimeout();
        fragment.startActivityForResult(Intent.createChooser(intent, fragment.getString(R.string.pick_video)), RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY);
    }

    public static void launchVideoCamera(Fragment fragment) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        fragment.startActivityForResult(intent, RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    public static boolean isLocalFile(String state) {
        if (state == null)
            return false;

        if (state.equals("queued") || state.equals("uploading") || state.equals("retry") || state.equals("failed"))
            return true;

        return false;
    }

    public static Uri getLastRecordedVideoUri(Activity activity) {
        String[] proj = { MediaStore.Video.Media._ID };
        Uri contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Video.VideoColumns.DATE_TAKEN + " DESC";
        CursorLoader loader = new CursorLoader(activity, contentUri, proj, null, null, sortOrder);
        Cursor cursor = loader.loadInBackground();
        cursor.moveToFirst();

        return Uri.parse(contentUri.toString() + "/" + cursor.getLong(0));
    }

    /**
     * This is a workaround for WP3.4.2 that deletes the media from the server when editing media properties within the app.
     * See: https://github.com/wordpress-mobile/WordPress-Android/issues/204
     * @return
     */
    public static boolean isWordPressVersionWithMediaEditingCapabilities() {
        if (WordPress.currentBlog == null)
            return false;

        if (WordPress.currentBlog.getWpVersion() == null)
            return true;

        if (WordPress.currentBlog.isDotcomFlag())
            return true;

        Version minVersion;
        Version currentVersion;
        try {
            minVersion = new Version("3.5.2");
            currentVersion = new Version(WordPress.currentBlog.getWpVersion());

            if( currentVersion.compareTo(minVersion) == -1 )
                return false;

        } catch (IllegalArgumentException e) {
            AppLog.e(T.POSTS, e);
        }

        return true;
    }

    public static boolean canDeleteMedia(String blogId, String mediaID) {
        Cursor cursor = WordPress.wpDB.getMediaFile(blogId, mediaID);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }
        String state = cursor.getString(cursor.getColumnIndex("uploadState"));
        cursor.close();
        if (state != null && state.equals("uploading")) {
            return false;
        }
        return true;
    }

    public static WPImageSpan prepareWPImageSpan(Context context, String blogId, final String mediaId) {
        Cursor cursor = WordPress.wpDB.getMediaFile(blogId, mediaId);
        if (cursor == null || !cursor.moveToFirst()){
            if (cursor != null)
                cursor.close();
            return null;
        }

        String url = cursor.getString(cursor.getColumnIndex("fileURL"));
        if (url == null) {
            cursor.close();
            return null;
        }

        String mimeType = cursor.getString(cursor.getColumnIndex("mimeType"));
        boolean isVideo = mimeType != null && mimeType.contains("video");

        Uri uri = Uri.parse(url);
        WPImageSpan imageSpan = new WPImageSpan(context,
                isVideo ? R.drawable.media_movieclip : R.drawable.dashicon_format_image_big_grey, uri);
        MediaFile mediaFile = imageSpan.getMediaFile();
        mediaFile.setMediaId(mediaId);
        mediaFile.setBlogId(blogId);
        mediaFile.setCaption(cursor.getString(cursor.getColumnIndex("caption")));
        mediaFile.setDescription(cursor.getString(cursor.getColumnIndex("description")));
        mediaFile.setTitle(cursor.getString(cursor.getColumnIndex("title")));
        mediaFile.setWidth(cursor.getInt(cursor.getColumnIndex("width")));
        mediaFile.setHeight(cursor.getInt(cursor.getColumnIndex("height")));
        mediaFile.setMimeType(mimeType);
        mediaFile.setFileName(cursor.getString(cursor.getColumnIndex("fileName")));
        mediaFile.setThumbnailURL(cursor.getString(cursor.getColumnIndex("thumbnailURL")));
        mediaFile.setDateCreatedGMT(cursor.getLong(cursor.getColumnIndex("date_created_gmt")));
        mediaFile.setVideoPressShortCode(cursor.getString(cursor.getColumnIndex("videoPressShortcode")));
        mediaFile.setFileURL(cursor.getString(cursor.getColumnIndex("fileURL")));
        mediaFile.setVideo(isVideo);
        mediaFile.save();
        cursor.close();

        return imageSpan;
    }

    // Calculate the minimun width between the blog setting and picture real width
    public static int getMinimumImageWidth(Context context, Uri curStream) {
        String imageWidth = WordPress.getCurrentBlog().getMaxImageWidth();
        int imageWidthBlogSetting = Integer.MAX_VALUE;

        if (!imageWidth.equals("Original Size")) {
            try {
                imageWidthBlogSetting = Integer.valueOf(imageWidth);
            } catch (NumberFormatException e) {
                AppLog.e(T.POSTS, e);
            }
        }

        int[] dimensions = ImageUtils.getImageSize(curStream, context);
        int imageWidthPictureSetting = dimensions[0] == 0 ? Integer.MAX_VALUE : dimensions[0];

        if (Math.min(imageWidthPictureSetting, imageWidthBlogSetting) == Integer.MAX_VALUE) {
            // Default value in case of errors reading the picture size and the blog settings is set to Original size
            return 1024;
        } else {
            return Math.min(imageWidthPictureSetting, imageWidthBlogSetting);
        }
    }

    public static void setWPImageSpanWidth(Context context, Uri curStream, WPImageSpan is) {
        MediaFile mediaFile = is.getMediaFile();
        if (mediaFile != null)
            mediaFile.setWidth(getMinimumImageWidth(context, curStream));
    }

    public static boolean isInMediaStore(Uri mediaUri) {
        // Check if the image is externally hosted (Picasa/Google Photos for example)
        if (mediaUri != null && mediaUri.toString().startsWith("content://media/")) {
            return true;
        } else {
            return false;
        }
    }

    public static Uri downloadExternalMedia(Context context, Uri imageUri) {
        if (context == null || imageUri == null) {
            return null;
        }
        File cacheDir = null;

        String mimeType = context.getContentResolver().getType(imageUri);
        boolean isVideo = (mimeType != null && mimeType.contains("video"));

        // If the device has an SD card
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            String mediaFolder = isVideo ? "video" : "images";
            cacheDir = new File(android.os.Environment.getExternalStorageDirectory() + "/WordPress/" + mediaFolder);
        } else {
            if (context.getApplicationContext() != null) {
                cacheDir = context.getApplicationContext().getCacheDir();
            }
        }

        if (cacheDir != null && !cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        try {
            InputStream input;
            // Download the file
            if (imageUri.toString().startsWith("content://")) {
                input = context.getContentResolver().openInputStream(imageUri);
                if (input == null) {
                    AppLog.e(T.UTILS, "openInputStream returned null");
                    return null;
                }
            } else {
                input = new URL(imageUri.toString()).openStream();
            }

            String fileName = "wp-" + System.currentTimeMillis();
            if (isVideo) {
                fileName += "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            }

            File f = new File(cacheDir, fileName);

            OutputStream output = new FileOutputStream(f);

            byte data[] = new byte[1024];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            return Uri.fromFile(f);
        } catch (FileNotFoundException e) {
            AppLog.e(T.UTILS, e);
        } catch (MalformedURLException e) {
            AppLog.e(T.UTILS, e);
        } catch (IOException e) {
            AppLog.e(T.UTILS, e);
        }

        return null;
    }

    public static String getMimeTypeOfInputStream(InputStream stream) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, options);
        return options.outMimeType;
    }

    public static String getMediaFileMimeType(File mediaFile) {
        String originalFileName = mediaFile.getName().toLowerCase();
        String mimeType = UrlUtils.getUrlMimeType(originalFileName);

        if (TextUtils.isEmpty(mimeType)) {
            try {
                String filePathForGuessingMime;
                if (mediaFile.getPath().contains("://")) {
                    filePathForGuessingMime = Uri.encode(mediaFile.getPath(), ":/");
                } else {
                    filePathForGuessingMime = "file://"+ Uri.encode(mediaFile.getPath(), "/");
                }
                URL urlForGuessingMime = new URL(filePathForGuessingMime);
                URLConnection uc = urlForGuessingMime.openConnection();
                String guessedContentType = uc.getContentType(); //internally calls guessContentTypeFromName(url.getFile()); and guessContentTypeFromStream(is);
                // check if returned "content/unknown"
                if (!TextUtils.isEmpty(guessedContentType) && !guessedContentType.equals("content/unknown")) {
                    mimeType = guessedContentType;
                }
            } catch (MalformedURLException e) {
                AppLog.e(AppLog.T.API, "MalformedURLException while trying to guess the content type for the file here " + mediaFile.getPath() + " with URLConnection", e);
            }
            catch (IOException e) {
                AppLog.e(AppLog.T.API, "Error while trying to guess the content type for the file here " + mediaFile.getPath() +" with URLConnection", e);
            }
        }

        // No mimeType yet? Try to decode the image and get the mimeType from there
        if (TextUtils.isEmpty(mimeType)) {
            try {
                DataInputStream inputStream = new DataInputStream(new FileInputStream(mediaFile));
                String mimeTypeFromStream = getMimeTypeOfInputStream(inputStream);
                if (!TextUtils.isEmpty(mimeTypeFromStream)) {
                    mimeType = mimeTypeFromStream;
                }
                inputStream.close();
            } catch (FileNotFoundException e) {
                AppLog.e(AppLog.T.API, "FileNotFoundException while trying to guess the content type for the file " + mediaFile.getPath(), e);
            } catch (IOException e) {
                AppLog.e(AppLog.T.API, "IOException while trying to guess the content type for the file " + mediaFile.getPath(), e);
            }
        }

        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "";
        } else {
            if (mimeType.equalsIgnoreCase("video/mp4v-es")) { //Fixes #533. See: http://tools.ietf.org/html/rfc3016
                mimeType = "video/mp4";
            }
        }

        return mimeType;
    }

    public static String getMediaFileName(File mediaFile, String mimeType) {
        String originalFileName = mediaFile.getName().toLowerCase();
        String extension = MimeTypeMap.getFileExtensionFromUrl(originalFileName);
        if (!TextUtils.isEmpty(extension))  //File name already has the extension in it
            return originalFileName;

        if (!TextUtils.isEmpty(mimeType)) { //try to get the extension from mimeType
            String fileExtension = getExtensionForMimeType(mimeType);
            if (!TextUtils.isEmpty(fileExtension)) {
                originalFileName += "." + fileExtension;
            }
        } else {
            //No mimetype and no extension!!
            AppLog.e(AppLog.T.API, "No mimetype and no extension for " + mediaFile.getPath());
        }

        return originalFileName;
    }

    public static String getExtensionForMimeType(String mimeType) {
        if (TextUtils.isEmpty(mimeType))
            return "";

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String fileExtensionFromMimeType = mimeTypeMap.getExtensionFromMimeType(mimeType);
        if (TextUtils.isEmpty(fileExtensionFromMimeType)) {
            // We're still without an extension - split the mime type and retrieve it
            String[] split = mimeType.split("/");
            fileExtensionFromMimeType = split.length > 1 ? split[1] : split[0];
        }

        return fileExtensionFromMimeType.toLowerCase();
    }
}
