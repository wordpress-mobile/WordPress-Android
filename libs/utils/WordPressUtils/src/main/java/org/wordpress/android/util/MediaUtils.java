package org.wordpress.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.wordpress.android.util.AppLog.T;

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
    private static final int DEFAULT_MAX_IMAGE_WIDTH = 1024;

    public static boolean isValidImage(String url) {
        if (url == null) {
            return false;
        }
        url = url.toLowerCase();
        return url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".gif");
    }

    public static boolean isDocument(String url) {
        if (url == null) {
            return false;
        }
        url = url.toLowerCase();
        return url.endsWith(".doc") || url.endsWith(".docx") || url.endsWith(".odt") || url.endsWith(".pdf");
    }

    public static boolean isPowerpoint(String url) {
        if (url == null) {
            return false;
        }
        url = url.toLowerCase();
        return url.endsWith(".ppt") || url.endsWith(".pptx") || url.endsWith(".pps") || url.endsWith(".ppsx") ||
                url.endsWith(".key");
    }

    public static boolean isSpreadsheet(String url) {
        if (url == null) {
            return false;
        }
        url = url.toLowerCase();
        return url.endsWith(".xls") || url.endsWith(".xlsx");
    }

    public static boolean isVideo(String url) {
        if (url == null) {
            return false;
        }
        url = url.toLowerCase();
        return url.endsWith(".ogv") || url.endsWith(".mp4") || url.endsWith(".m4v") || url.endsWith(".mov") ||
                url.endsWith(".wmv") || url.endsWith(".avi") || url.endsWith(".mpg") || url.endsWith(".3gp") ||
                url.endsWith(".3g2") || url.contains("video");
    }

    public static boolean isAudio(String url) {
        if (url == null) {
            return false;
        }
        url = url.toLowerCase();
        return url.endsWith(".mp3") || url.endsWith(".ogg") || url.endsWith(".wav") || url.endsWith(".wma") ||
                url.endsWith(".aiff") || url.endsWith(".aif") || url.endsWith(".aac") || url.endsWith(".m4a");
    }

    /**
     * E.g. Jul 2, 2013 @ 21:57
     */
    public static String getDate(long ms) {
        Date date = new Date(ms);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy '@' HH:mm", Locale.ENGLISH);

        // The timezone on the website is at GMT
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        return sdf.format(date);
    }

    public static boolean isLocalFile(String state) {
        if (state == null) {
            return false;
        }

        return  (state.equals("queued") || state.equals("uploading") || state.equals("retry")
                || state.equals("failed"));
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
     * Get image width setting from the image width site setting string. This string can be an int, in this case it's
     * the maximum image width defined by the site.
     * Examples:
     *   "1000" will return 1000
     *   "Original Size" will return Integer.MAX_VALUE
     *   "Largeur originale" will return Integer.MAX_VALUE
     *   null will return Integer.MAX_VALUE
     * @param imageWidthSiteSettingString Image width site setting string
     * @return Integer.MAX_VALUE if image width is not defined or invalid, maximum image width in other cases.
     */
    public static int getImageWidthSettingFromString(String imageWidthSiteSettingString) {
        if (imageWidthSiteSettingString == null) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.valueOf(imageWidthSiteSettingString);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Calculate and return the maximum allowed image width by comparing the width of the image at its full size with
     * the maximum upload width set in the blog settings
     * @param imageWidth the image's natural (full) width
     * @param imageWidthSiteSettingString the maximum upload width set in the site settings
     * @return maximum allowed image width
     */
    public static int getMaximumImageWidth(int imageWidth, String imageWidthSiteSettingString) {
        int imageWidthBlogSetting = getImageWidthSettingFromString(imageWidthSiteSettingString);
        int imageWidthPictureSetting = imageWidth == 0 ? Integer.MAX_VALUE : imageWidth;

        if (Math.min(imageWidthPictureSetting, imageWidthBlogSetting) == Integer.MAX_VALUE) {
            // Default value in case of errors reading the picture size or the blog settings is set to Original size
            return DEFAULT_MAX_IMAGE_WIDTH;
        } else {
            return Math.min(imageWidthPictureSetting, imageWidthBlogSetting);
        }
    }

    public static int getMaximumImageWidth(Context context, Uri curStream, String imageWidthBlogSettingString) {
        int[] dimensions = ImageUtils.getImageSize(curStream, context);
        return getMaximumImageWidth(dimensions[0], imageWidthBlogSettingString);
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
