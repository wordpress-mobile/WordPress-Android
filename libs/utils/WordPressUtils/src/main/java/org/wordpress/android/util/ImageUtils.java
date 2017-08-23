package org.wordpress.android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class ImageUtils {
    public static int[] getImageSize(Uri uri, Context context){
        String path = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        if (uri.toString().contains("content:")) {
            String[] projection = new String[] { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA };
            Cursor cur = null;
            try {
                cur = context.getContentResolver().query(uri, projection, null, null, null);
                if (cur != null && cur.moveToFirst()) {
                    int dataColumn = cur.getColumnIndex(MediaStore.Images.Media.DATA);
                    path = cur.getString(dataColumn);
                }
            } catch (IllegalStateException stateException) {
                Log.d(ImageUtils.class.getName(), "IllegalStateException querying content:" + uri);
            } finally {
                SqlUtils.closeCursor(cur);
            }
        }

        if (TextUtils.isEmpty(path)) {
            //The file isn't ContentResolver, or it can't be access by ContentResolver. Try to access the file directly.
            path = uri.toString().replace("content://media", "");
            path = path.replace("file://", "");
        }

        BitmapFactory.decodeFile(path, options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        return new int[]{imageWidth, imageHeight};
    }

    // Read the orientation from ContentResolver. If it fails, read from EXIF.
    public static int getImageOrientation(Context ctx, String filePath) {
        if (TextUtils.isEmpty(filePath) || ctx == null) {
            AppLog.w(AppLog.T.UTILS, "Can't read orientation. Passed context or file is null or empty.");
            return 0;
        }
        Uri curStream;
        int orientation = 0;

        // Remove file protocol
        filePath = filePath.replace("file://", "");

        if (!filePath.contains("content://"))
            curStream = Uri.parse("content://media" + filePath);
        else
            curStream = Uri.parse(filePath);

        try {
            Cursor cur = ctx.getContentResolver().query(curStream, new String[]{MediaStore.Images.Media.ORIENTATION}, null, null, null);
            if (cur != null) {
                if (cur.moveToFirst()) {
                    orientation = cur.getInt(cur.getColumnIndex(MediaStore.Images.Media.ORIENTATION));
                }
                cur.close();
            }
        } catch (Exception errReadingContentResolver) {
            AppLog.e(AppLog.T.UTILS, "Error reading orientation of the file: " + filePath, errReadingContentResolver);
        }

        if (orientation == 0) {
            orientation = getExifOrientation(filePath);
        }

        return orientation;
    }


    private static int getExifOrientation(String path) {
        if (TextUtils.isEmpty(path)) {
            AppLog.w(AppLog.T.UTILS, "Can't read EXIF orientation. Passed path is empty.");
            return 0;
        }
        ExifInterface exif;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            AppLog.e(AppLog.T.UTILS, "Can't read EXIF orientation.", e);
            return 0;
        }

        int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);

        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return 0;
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    public static Bitmap downloadBitmap(String url) {
        final DefaultHttpClient client = new DefaultHttpClient();

        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                AppLog.w(AppLog.T.UTILS, "ImageDownloader Error " + statusCode
                        + " while retrieving bitmap from " + url);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    return BitmapFactory.decodeStream(inputStream);
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (Exception e) {
            // Could provide a more explicit error message for IOException or
            // IllegalStateException
            getRequest.abort();
            AppLog.w(AppLog.T.UTILS, "ImageDownloader Error while retrieving bitmap from " + url);
        }
        return null;
    }

    /** From http://developer.android.com/training/displaying-bitmaps/load-bitmap.html **/
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }


    public interface BitmapWorkerCallback {
        public void onBitmapReady(String filePath, ImageView imageView, Bitmap bitmap);
    }

    public static class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private final BitmapWorkerCallback callback;
        private int targetWidth;
        private int targetHeight;
        private String path;

        public BitmapWorkerTask(ImageView imageView, int width, int height, BitmapWorkerCallback callback) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
            this.callback = callback;
            targetWidth = width;
            targetHeight = height;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            path = params[0];

            BitmapFactory.Options bfo = new BitmapFactory.Options();
            bfo.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bfo);

            bfo.inSampleSize = calculateInSampleSize(bfo, targetWidth, targetHeight);
            bfo.inJustDecodeBounds = false;

            // get proper rotation
            int bitmapWidth = 0;
            int bitmapHeight = 0;
            try {
                File f = new File(path);
                ExifInterface exif = new ExifInterface(f.getPath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int angle = 0;
                if (orientation == ExifInterface.ORIENTATION_NORMAL) { // no need to rotate
                    return BitmapFactory.decodeFile(path, bfo);
                } else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                    angle = 90;
                } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                    angle = 180;
                } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                    angle = 270;
                }

                Matrix mat = new Matrix();
                mat.postRotate(angle);

                try {
                    Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(f), null, bfo);
                    if (bmp == null) {
                        AppLog.e(AppLog.T.UTILS, "can't decode bitmap: " + f.getPath());
                        return null;
                    }
                    bitmapWidth = bmp.getWidth();
                    bitmapHeight = bmp.getHeight();
                    return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), mat, true);
                } catch (OutOfMemoryError oom) {
                    AppLog.e(AppLog.T.UTILS, "OutOfMemoryError Error in setting image: " + oom);
                }
            } catch (IOException e) {
                AppLog.e(AppLog.T.UTILS, "Error in setting image", e);
            }

            return null;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference == null || bitmap == null)
                return;

            final ImageView imageView = imageViewReference.get();

            if (callback != null)
                callback.onBitmapReady(path, imageView, bitmap);

        }
    }

    public static String getTitleForWPImageSpan(Context ctx, String filePath) {
        if (filePath == null)
            return null;

        Uri curStream;
        String title;

        if (!filePath.contains("content://"))
            curStream = Uri.parse("content://media" + filePath);
        else
            curStream = Uri.parse(filePath);

        if (filePath.contains("video")) {
            return "Video";
        } else {
            String[] projection = new String[] { MediaStore.Images.Thumbnails.DATA };

            Cursor cur;
            try {
                cur = ctx.getContentResolver().query(curStream, projection, null, null, null);
            } catch (Exception e1) {
                AppLog.e(AppLog.T.UTILS, e1);
                return null;
            }
            File jpeg;
            if (cur != null) {
                String thumbData = "";
                if (cur.moveToFirst()) {
                    int dataColumn = cur.getColumnIndex(MediaStore.Images.Media.DATA);
                    thumbData = cur.getString(dataColumn);
                }
                cur.close();
                if (thumbData == null) {
                    return null;
                }
                jpeg = new File(thumbData);
            } else {
                String path = filePath.toString().replace("file://", "");
                jpeg = new File(path);
            }
            title = jpeg.getName();
            return title;
        }
    }

    /**
     * Resizes an image to be placed in the Post Content Editor
     *
     * @return resized bitmap
     */
    public static Bitmap getWPImageSpanThumbnailFromFilePath(Context context, String filePath, int targetWidth) {
        if (filePath == null || context == null) {
            return null;
        }

        Uri curUri = Uri.parse(filePath);

        if (filePath.contains("video")) {
            // Load the video thumbnail from the MediaStore
            int videoId = 0;
            try {
                videoId = Integer.parseInt(curUri.getLastPathSegment());
            } catch (NumberFormatException e) {
            }
            ContentResolver crThumb = context.getContentResolver();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            Bitmap videoThumbnail = MediaStore.Video.Thumbnails.getThumbnail(crThumb, videoId, MediaStore.Video.Thumbnails.MINI_KIND,
                    options);
            if (videoThumbnail != null) {
                return getScaledBitmapAtLongestSide(videoThumbnail, targetWidth);
            } else {
                return null;
            }
        } else {
            // Create resized bitmap
            int rotation = getImageOrientation(context, filePath);
            byte[] bytes = createThumbnailFromUri(context, curUri, targetWidth, null, rotation);

            if (bytes != null && bytes.length > 0) {
                try {
                    Bitmap resizedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (resizedBitmap != null) {
                        return getScaledBitmapAtLongestSide(resizedBitmap, targetWidth);
                    }
                } catch (OutOfMemoryError e) {
                    AppLog.e(AppLog.T.UTILS, "OutOfMemoryError Error in setting image: " + e);
                    return null;
                }
            }
        }

        return null;
    }

    /*
     Resize a bitmap to the targetSize on its longest side.
     */
    public static Bitmap getScaledBitmapAtLongestSide(Bitmap bitmap, int targetSize) {
        if (bitmap == null || bitmap.getWidth() <= targetSize && bitmap.getHeight() <= targetSize) {
            // Do not resize.
            return bitmap;
        }

        int targetWidth, targetHeight;
        if (bitmap.getHeight() > bitmap.getWidth()) {
            // Resize portrait bitmap
            targetHeight = targetSize;
            float percentage = (float) targetSize / bitmap.getHeight();
            targetWidth = (int)(bitmap.getWidth() * percentage);
        } else {
            // Resize landscape or square image
            targetWidth = targetSize;
            float percentage = (float) targetSize / bitmap.getWidth();
            targetHeight = (int)(bitmap.getHeight() * percentage);
        }

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }

    private static boolean resizeImageAndWriteToStream(Context context,
                                                    Uri imageUri,
                                                    String fileExtension,
                                                    int maxSize,
                                                    int orientation,
                                                    int quality,
                                                    OutputStream outStream) throws OutOfMemoryError, IOException {

        String realFilePath = MediaUtils.getRealPathFromURI(context, imageUri);

        // get just the image bounds
        BitmapFactory.Options optBounds = new BitmapFactory.Options();
        optBounds.inJustDecodeBounds = true;

        try {
            BitmapFactory.decodeFile(realFilePath, optBounds);
        } catch (OutOfMemoryError e) {
            AppLog.e(AppLog.T.UTILS, "OutOfMemoryError Error while decoding the original image: " + realFilePath, e);
            throw e;
        }

        // determine correct scale value (should be power of 2)
        // http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue/3549021#3549021
        int scale = 1;
        int originalPictureMaxSize = Math.max(optBounds.outWidth, optBounds.outHeight);
        if (maxSize > 0 && originalPictureMaxSize > maxSize) {
            double d = Math.pow(2, (int) Math.round(Math.log(maxSize / originalPictureMaxSize) / Math.log(0.5)));

            scale = (int) d;
        }

        BitmapFactory.Options optActual = new BitmapFactory.Options();
        optActual.inSampleSize = scale;

        // Get the roughly resized bitmap
        final Bitmap bmpResized;
        try {
            bmpResized = BitmapFactory.decodeFile(realFilePath, optActual);
        } catch (OutOfMemoryError e) {
            AppLog.e(AppLog.T.UTILS, "OutOfMemoryError Error while decoding the original image: " + realFilePath, e);
            throw e;
        }

        if (bmpResized == null) {
            AppLog.e(AppLog.T.UTILS, "Can't decode the resized picture.");
            throw new IOException("Can't decode the resized picture.");
        }

        // Resize the bitmap to exact size: calculate exact scale in order to resize accurately
        float scaleBy = getScaleImageBy(maxSize, bmpResized);

        Matrix matrix = new Matrix();
        matrix.postScale(scaleBy, scaleBy);

        // apply orientation
        if (orientation != 0) {
            matrix.setRotate(orientation);
        }

        Bitmap.CompressFormat fmt;
        if (fileExtension != null &&
                (fileExtension.equals("png") || fileExtension.equals(".png"))) {
            fmt = Bitmap.CompressFormat.PNG;
        } else {
            fmt = Bitmap.CompressFormat.JPEG;
        }

        final Bitmap bmpRotated;
        try {
            bmpRotated = Bitmap.createBitmap(bmpResized, 0, 0, bmpResized.getWidth(), bmpResized.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            AppLog.e(AppLog.T.UTILS, "OutOfMemoryError while creating the resized bitmap", e);
            throw e;
        } catch (NullPointerException e) {
            // See: https://github.com/wordpress-mobile/WordPress-Android/issues/1844
            AppLog.e(AppLog.T.UTILS, "Bitmap.createBitmap has thrown a NPE internally. This should never happen!", e);
            throw e;
        }

        if (bmpRotated == null) {
            // Fix an issue where bmpRotated is null even if the documentation doesn't say Bitmap.createBitmap can return null.
            AppLog.e(AppLog.T.UTILS, "bmpRotated is null even if the documentation doesn't say Bitmap.createBitmap can return null.");
            // See: https://github.com/wordpress-mobile/WordPress-Android/issues/1848
            throw new IOException("bmpRotated is null even if the documentation doesn't say Bitmap.createBitmap can return null.");
        }

        return bmpRotated.compress(fmt, quality, outStream);
    }

    /**
     * Given the path to an image, compress and resize it.
     * @param context the passed context
     * @param path the path to the original image
     * @param maxImageSize the maximum allowed width
     * @param quality the encoder quality
     * @return the path to the optimized image
     */
    public static String optimizeImage(Context context, String path, int maxImageSize, int quality) {
        if (context == null || TextUtils.isEmpty(path)) {
            return path;
        }

        File file = new File(path);
        if (!file.exists()) {
            return path;
        }

        String mimeType = MediaUtils.getMediaFileMimeType(file);
        if (mimeType.equals("image/gif")) {
            // Don't rescale gifs to maintain their quality
            return path;
        }

        Uri srcImageUri = Uri.parse(path);
        if (srcImageUri == null) {
            return path;
        }

        String fileName = MediaUtils.getMediaFileName(file, mimeType);
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileName).toLowerCase();

        int selectedMaxSize = getImageSize(srcImageUri, context)[0];
        if (selectedMaxSize == 0) {
            // Can't read the src dimensions.
            return path;
        }

        // do not optimize if original-size and 100% quality are set.
        if (maxImageSize == Integer.MAX_VALUE && quality == 100) {
            return path;
        }

        if (selectedMaxSize > maxImageSize) {
            selectedMaxSize = maxImageSize;
        }

        int orientation = getImageOrientation(context, path);

        File resizedImageFile;
        FileOutputStream out;

        try {
            resizedImageFile = File.createTempFile("wp-image-", "." + fileExtension);
            out = new FileOutputStream(resizedImageFile);
        } catch (IOException e) {
            AppLog.e(AppLog.T.MEDIA, "Failed to create the temp file on storage. Use the original picture instead.");
            return path;
        } catch (SecurityException e) {
            AppLog.e(AppLog.T.MEDIA, "Can't write the tmp file due to security restrictions. Use the original picture instead.");
            return path;
        }

        try {
            boolean res = resizeImageAndWriteToStream(context, srcImageUri, fileExtension, selectedMaxSize, orientation, quality, out);
            if (!res) {
                AppLog.w(AppLog.T.MEDIA, "Failed to compress the optimized image. Use the original picture instead.");
                return path;
            }
        } catch (IOException e) {
            AppLog.e(AppLog.T.MEDIA, "Failed to create optimized image. Use the original picture instead.");
            return path;
        } catch (OutOfMemoryError e) {
            AppLog.e(AppLog.T.MEDIA, "Can't optimize the picture due to low memory. Use the original picture instead.");
            return path;
        } finally {
            // close the stream
            try {
                out.flush();
                out.close();
            } catch (IOException e) {
                //nope
            }
        }

        String tempFilePath = resizedImageFile.getPath();
        if (!TextUtils.isEmpty(tempFilePath)) {
            return tempFilePath;
        } else {
            AppLog.e(AppLog.T.MEDIA, "Failed to create optimized image. Use the full picture instead.");
        }

        return path;
    }

    /**
     * Generate a thumbnail from a video url.
     * Note that this method could take time if network url.
     *
     * @param videoPath The path to the video on internet
     * @return the path to the picture on disk
     */
    public static Bitmap getVideoFrameFromVideo(String videoPath, int maxWidth) {
        if (TextUtils.isEmpty(videoPath) || maxWidth <= 0) {
            return null;
        }

        if (new File(videoPath).exists()) {
            // Local file
            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
            return ImageUtils.getScaledBitmapAtLongestSide(thumb, maxWidth);
        }

        // Not a local file. 
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        Bitmap bitmap = null;
        try {
            mediaMetadataRetriever.setDataSource(videoPath, new HashMap<String, String>());
            bitmap = mediaMetadataRetriever.getFrameAtTime();
        } catch (IllegalArgumentException e) {
            AppLog.e(AppLog.T.MEDIA, "The passed video path is invalid: " + videoPath);
        } catch (java.lang.RuntimeException e) {
            // I've see this kind of error on one of my testing device
            AppLog.e(AppLog.T.MEDIA, "The passed video path is invalid: " + videoPath);
        }
        finally {
            mediaMetadataRetriever.release();
        }

        if (bitmap == null) {
            AppLog.w(AppLog.T.MEDIA, "Failed to retrieve frame from the passed video path: " + videoPath);
            return null;
        }

        return getScaledBitmapAtLongestSide(bitmap, maxWidth);
    }

    /**
     * nbradbury - 21-Feb-2014 - similar to createThumbnail but more efficient since it doesn't
     * require passing the full-size image as an array of bytes[]
     */
    public static byte[] createThumbnailFromUri(Context context,
                                                Uri imageUri,
                                                int maxWidth,
                                                String fileExtension,
                                                int orientation) {
        if (context == null || imageUri == null || maxWidth <= 0)
            return null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            boolean res = resizeImageAndWriteToStream(context, imageUri, fileExtension, maxWidth, orientation, 75, stream);
            if (!res) {
                AppLog.w(AppLog.T.MEDIA, "Failed to compress the resized image. Use the full picture instead.");
                return null;
            }
        } catch (IOException e) {
            AppLog.e(AppLog.T.MEDIA, "Failed to create resized image. Use the full picture instead.");
            return null;
        } catch (OutOfMemoryError e) {
            AppLog.e(AppLog.T.MEDIA, "Can't resize the picture due to low memory. Use the full picture instead.");
            return null;
        }

        return stream.toByteArray();
    }

    public static Bitmap getCircularBitmap(final Bitmap bitmap) {
        if (bitmap==null)
            return null;

        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.RED);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * Returns the passed bitmap with rounded corners
     * @param bitmap - the bitmap to modify
     * @param radius - the radius of the corners
     * @param borderColor - the border to apply (use Color.TRANSPARENT for none)
     */
    public static Bitmap getRoundedEdgeBitmap(final Bitmap bitmap, int radius, int borderColor) {
        if (bitmap == null) {
            return null;
        }

        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.RED);
        canvas.drawRoundRect(rectF, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        if (borderColor != Color.TRANSPARENT) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            paint.setColor(borderColor);
            canvas.drawRoundRect(rectF, radius, radius, paint);
        }

        return output;
    }

    /**
     * Get the maximum size a thumbnail can be to fit in either portrait or landscape orientations.
     */
    public static int getMaximumThumbnailWidthForEditor(Context context) {
        int maximumThumbnailWidthForEditor;
        Point size = DisplayUtils.getDisplayPixelSize(context);
        int screenWidth = size.x;
        int screenHeight = size.y;
        maximumThumbnailWidthForEditor = (screenWidth > screenHeight) ? screenHeight : screenWidth;
        // 48dp of padding on each side so you can still place the cursor next to the image.
        int padding = DisplayUtils.dpToPx(context, 48) * 2;
        maximumThumbnailWidthForEditor -= padding;
        return maximumThumbnailWidthForEditor;
    }

    /**
     * Given the path to an image, rotate it by using EXIF info
     * @param context the passed context
     * @param path the path to the original image
     * @return the path to the rotated image or null
     */
    public static String rotateImageIfNecessary(Context context, String path) {
        if (context == null || TextUtils.isEmpty(path)) {
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        int orientation = getImageOrientation(context, path);
        // Do not rotate portrait pictures
        if (orientation == 0) {
            return  null;
        }

        String mimeType = MediaUtils.getMediaFileMimeType(file);
        if (mimeType.equals("image/gif")) {
            // Don't rotate gifs to maintain their quality
            return null;
        }

        Uri srcImageUri = Uri.parse(path);
        if (srcImageUri == null) {
            return null;
        }

        String fileName = MediaUtils.getMediaFileName(file, mimeType);
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileName).toLowerCase();

        int selectedWidth = getImageSize(srcImageUri, context)[0];
        if (selectedWidth == 0) {
            // Can't read the src dimensions.
            return null;
        }

        File rotatedImageFile;
        FileOutputStream out;

        try {
            // try to re-use the same name as prefix of the temp file
            String prefix;
            int dotPos = fileName.indexOf('.');
            if (dotPos > 0) {
                prefix = fileName.substring(0, dotPos);
            } else {
                prefix = fileName;
            }

            if (prefix.length() < 3) {
                // prefix must be at least 3 characters
                prefix = "wp-image";
            }

            rotatedImageFile = File.createTempFile(prefix, "." + fileExtension);
            out = new FileOutputStream(rotatedImageFile);
        } catch (IOException e) {
            AppLog.e(AppLog.T.MEDIA, "Failed to create the temp file on storage.");
            return null;
        } catch (SecurityException e) {
            AppLog.e(AppLog.T.MEDIA, "Can't write the tmp file due to security restrictions.");
            return null;
        }

        try {
            boolean res = resizeImageAndWriteToStream(context, srcImageUri, fileExtension, selectedWidth, orientation, 85, out);
            if (!res) {
                AppLog.w(AppLog.T.MEDIA, "Failed to compress the rotates image.");
                return null;
            }
        } catch (IOException e) {
            AppLog.e(AppLog.T.MEDIA, "Failed to create rotated image.");
            return null;
        } catch (OutOfMemoryError e) {
            AppLog.e(AppLog.T.MEDIA, "Can't rotate the picture due to low memory.");
            return null;
        } finally {
            // close the stream
            try {
                out.flush();
                out.close();
            } catch (IOException e) {
                //nope
            }
        }

        String tempFilePath = rotatedImageFile.getPath();
        if (!TextUtils.isEmpty(tempFilePath)) {
            return tempFilePath;
        } else {
            AppLog.e(AppLog.T.MEDIA, "Failed to create rotated image.");
        }

        return null;
    }


    /**
     * This is a wrapper around MediaStore.Images.Thumbnails.getThumbnail that takes in consideration
     * the orientation of the picture.
     *
     * @param contentResolver ContentResolver used to dispatch queries to MediaProvider.
     * @param id Original image id associated with thumbnail of interest.
     * @param kind The type of thumbnail to fetch. Should be either MINI_KIND or MICRO_KIND.
     *
     * @return A Bitmap instance. It could be null if the original image
     *         associated with origId doesn't exist or memory is not enough.
     */
    public static Bitmap getThumbnail(ContentResolver contentResolver, long id, int kind) {
        Cursor cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media.DATA}, // Which columns to return
                MediaStore.Images.Media._ID + "=?",       // Which rows to return
                new String[]{String.valueOf(id)},       // Selection arguments
                null);// order

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            String filepath = cursor.getString(0);
            cursor.close();
            int rotation = getExifOrientation(filepath);
            Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(contentResolver, id, kind, null);

            if (rotation != 0 && bitmap != null) {
                Matrix matrix = new Matrix();
                matrix.setRotate(rotation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            return bitmap;
        }

        return null;
    }

    private static float getScaleImageBy(float maxSize, Bitmap bmpResized) {
        int divideBy = Math.max(bmpResized.getHeight(), bmpResized.getWidth());
        float percentage = maxSize / divideBy;

        float proportionateHeight = bmpResized.getHeight() * percentage;
        int finalHeight = (int) Math.rint(proportionateHeight);

        float scaleWidth = maxSize / bmpResized.getWidth();
        float scaleHeight = ((float) finalHeight) / bmpResized.getHeight();

        return Math.min(scaleWidth, scaleHeight);
    }
}
