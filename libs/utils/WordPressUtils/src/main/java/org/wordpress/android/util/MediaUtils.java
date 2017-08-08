package org.wordpress.android.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.os.Environment.isExternalStorageRemovable;

public class MediaUtils {
    private static final int DEFAULT_MAX_IMAGE_WIDTH = 1024;
    private static final Pattern FILE_EXISTS_PATTERN = Pattern.compile("(.*?)(-([0-9]+))?(\\..*$)?");

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

        return state.equalsIgnoreCase("queued")
                || state.equalsIgnoreCase("uploading")
                || state.equalsIgnoreCase("retry")
                || state.equalsIgnoreCase("failed");
    }

    public static Uri getLastRecordedVideoUri(Activity activity) {
        String[] proj = { MediaStore.Video.Media._ID };
        Uri contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Video.VideoColumns.DATE_TAKEN + " DESC";
        CursorLoader loader = new CursorLoader(activity, contentUri, proj, null, null, sortOrder);
        Cursor cursor = loader.loadInBackground();
        cursor.moveToFirst();
        long value = cursor.getLong(0);
        SqlUtils.closeCursor(cursor);

        return Uri.parse(contentUri.toString() + "/" + value);
    }

    /**
     * Get image max size setting from the image max size setting string. This string can be an int, in this case it's
     * the maximum image width defined by the site.
     * Examples:
     *   "1000" will return 1000
     *   "Original Size" will return Integer.MAX_VALUE
     *   "Largeur originale" will return Integer.MAX_VALUE
     *   null will return Integer.MAX_VALUE
     * @param imageMaxSizeSiteSettingString Image max size site setting string
     * @return Integer.MAX_VALUE if image width is not defined or invalid, maximum image width in other cases.
     */
    public static int getImageMaxSizeSettingFromString(String imageMaxSizeSiteSettingString) {
        if (imageMaxSizeSiteSettingString == null) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.valueOf(imageMaxSizeSiteSettingString);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Calculate and return the maximum allowed image width by comparing the width of the image at its full size with
     * the maximum upload width set in the blog settings
     * @param imageSize the image's natural (full) width
     * @param imageMaxSizeSiteSettingString the maximum upload width set in the site settings
     * @return maximum allowed image width
     */
    public static int getMaximumImageSize(int imageSize, String imageMaxSizeSiteSettingString) {
        int imageMaxSizeBlogSetting = getImageMaxSizeSettingFromString(imageMaxSizeSiteSettingString);
        int imageWidthPictureSetting = imageSize == 0 ? Integer.MAX_VALUE : imageSize;

        if (Math.min(imageWidthPictureSetting, imageMaxSizeBlogSetting) == Integer.MAX_VALUE) {
            // Default value in case of errors reading the picture size or the blog settings is set to Original size
            return DEFAULT_MAX_IMAGE_WIDTH;
        } else {
            return Math.min(imageWidthPictureSetting, imageMaxSizeBlogSetting);
        }
    }

    public static int getMaximumImageSize(Context context, Uri curStream, String imageMaxSizeBlogSettingString) {
        int[] dimensions = ImageUtils.getImageSize(curStream, context);
        return getMaximumImageSize(dimensions[0], imageMaxSizeBlogSettingString);
    }

    public static boolean isInMediaStore(Uri mediaUri) {
        // Check if the image is externally hosted (Picasa/Google Photos for example)
        if (mediaUri != null && mediaUri.toString().startsWith("content://media/")) {
            return true;
        } else {
            return false;
        }
    }

    public static @Nullable String getFilenameFromURI(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        try {
            String result = null;
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndexDisplayName = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (columnIndexDisplayName == -1) {
                    return null;
                }
                result = cursor.getString(columnIndexDisplayName);
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static File getDiskCacheDir(Context context) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !isExternalStorageRemovable() ? context.getApplicationContext().getExternalCacheDir() :
                context.getCacheDir();
    }

    public static Uri downloadExternalMedia(Context context, Uri imageUri) {
        if (context == null || imageUri == null) {
            return null;
        }
        String mimeType = context.getContentResolver().getType(imageUri);
        File cacheDir = getDiskCacheDir(context);

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

            String fileName = getFilenameFromURI(context, imageUri);
            if (TextUtils.isEmpty(fileName)) {
                fileName = generateTimeStampedFileName(mimeType);
            }

            File f = getUniqueCacheFileForName(fileName, cacheDir, mimeType);

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
        } catch (IOException e) {
            AppLog.e(T.UTILS, e);
        }

        return null;
    }

    private static File getUniqueCacheFileForName(String fileName, File cacheDir, String mimeType) {
        File file = new File(cacheDir, fileName);

        while (file.exists()) {
            Matcher matcher = FILE_EXISTS_PATTERN.matcher(fileName);
            if (matcher.matches()) {
                String baseFileName = matcher.group(1);
                String existingDuplicationNumber = matcher.group(3);
                String fileType = StringUtils.notNullStr(matcher.group(4));

                if (existingDuplicationNumber == null) {
                    // Not a copy already
                    fileName = baseFileName + "-1" + fileType;
                } else {
                    fileName = baseFileName + "-" + (StringUtils.stringToInt(existingDuplicationNumber) + 1) + fileType;
                }
            } else {
                // Shouldn't happen, but in case our match fails fall back to timestamped file name
                fileName = generateTimeStampedFileName(mimeType);
            }
            file = new File(cacheDir, fileName);
        }
        return file;
    }

    public static String generateTimeStampedFileName(String mimeType) {
        return "wp-" + System.currentTimeMillis() + "." + getExtensionForMimeType(mimeType);
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
                String guessedContentType = null;
                try {
                    guessedContentType = uc.getContentType(); //internally calls guessContentTypeFromName(url.getFile()); and guessContentTypeFromStream(is);
                } catch (StringIndexOutOfBoundsException e) {
                    // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5699
                    AppLog.e(AppLog.T.MEDIA, "Error getting the content type for " + mediaFile.getPath() +" by using URLConnection.getContentType", e);
                }
                // check if returned "content/unknown"
                if (!TextUtils.isEmpty(guessedContentType) && !guessedContentType.equals("content/unknown")) {
                    mimeType = guessedContentType;
                }
            } catch (MalformedURLException e) {
                AppLog.e(AppLog.T.MEDIA, "MalformedURLException while trying to guess the content type for the file here " + mediaFile.getPath() + " with URLConnection", e);
            }
            catch (IOException e) {
                AppLog.e(AppLog.T.MEDIA, "Error while trying to guess the content type for the file here " + mediaFile.getPath() +" with URLConnection", e);
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
                AppLog.e(AppLog.T.MEDIA, "FileNotFoundException while trying to guess the content type for the file " + mediaFile.getPath(), e);
            } catch (IOException e) {
                AppLog.e(AppLog.T.MEDIA, "IOException while trying to guess the content type for the file " + mediaFile.getPath(), e);
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

    public static String getRealPathFromURI(final Context context, Uri uri) {
        String path;
        if ("content".equals(uri.getScheme())) {
            path = MediaUtils.getPath(context, uri);
        } else if ("file".equals(uri.getScheme())) {
            path = uri.getPath();
        } else {
            path = uri.toString();
        }
        return path;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * Based on paulburke's solution for aFileChooser - https://github.com/iPaulPro/aFileChooser
     *
     * @param context The context.
     * @param uri The Uri to query.
     */
    private static String getPath(final Context context, final Uri uri) {
        String path = getDocumentProviderPathKitkatOrHigher(context, uri);

        if (path != null) {
            return path;
        }

        // MediaStore (and general)
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getDocumentProviderPathKitkatOrHigher(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;

                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = MediaStore.MediaColumns._ID + "=?";

                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        final String column = MediaStore.MediaColumns.DATA;

        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndex(column);
                if (column_index != -1) {
                    return cursor.getString(column_index);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
