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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ImageView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class ImageUtils {
    public static int[] getImageSize(Uri uri, Context context){
        String path = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        if (uri.toString().contains("content:")) {
            String[] projection = new String[] { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA };
            Cursor cur = context.getContentResolver().query(uri, projection, null, null, null);
            if (cur != null) {
                if (cur.moveToFirst()) {
                    int dataColumn = cur.getColumnIndex(MediaStore.Images.Media.DATA);
                    path = cur.getString(dataColumn);
                }
                cur.close();
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
            AppLog.e(AppLog.T.UTILS, errReadingContentResolver);
        }

        if (orientation == 0) {
            orientation = getExifOrientation(filePath);
        }

        return orientation;
    }


    public static int getExifOrientation(String path) {
        ExifInterface exif;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            AppLog.e(AppLog.T.UTILS, e);
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

        Uri curUri;
        if (!filePath.contains("content://")) {
            curUri = Uri.parse("content://media" + filePath);
        } else {
            curUri = Uri.parse(filePath);
        }

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
        if (bitmap.getWidth() <= targetSize && bitmap.getHeight() <= targetSize) {
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

    /**
     * nbradbury - 21-Feb-2014 - similar to createThumbnail but more efficient since it doesn't
     * require passing the full-size image as an array of bytes[]
     */
    public static byte[] createThumbnailFromUri(Context context,
                                                Uri imageUri,
                                                int maxWidth,
                                                String fileExtension,
                                                int rotation) {
        if (context == null || imageUri == null || maxWidth <= 0)
            return null;

        String filePath = null;
        if (imageUri.toString().contains("content:")) {
            String[] projection = new String[] { MediaStore.Images.Media.DATA };
            Cursor cur = context.getContentResolver().query(imageUri, projection, null, null, null);
            if (cur != null) {
                if (cur.moveToFirst()) {
                    int dataColumn = cur.getColumnIndex(MediaStore.Images.Media.DATA);
                    filePath = cur.getString(dataColumn);
                }
                cur.close();
            }
        }

        if (TextUtils.isEmpty(filePath)) {
            //access the file directly
            filePath = imageUri.toString().replace("content://media", "");
            filePath = filePath.replace("file://", "");
        }

        // get just the image bounds
        BitmapFactory.Options optBounds = new BitmapFactory.Options();
        optBounds.inJustDecodeBounds = true;

        try {
            BitmapFactory.decodeFile(filePath, optBounds);
        } catch (OutOfMemoryError e) {
            AppLog.e(AppLog.T.UTILS, "OutOfMemoryError Error in setting image: " + e);
            return null;
        }

        // determine correct scale value (should be power of 2)
        // http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue/3549021#3549021
        int scale = 1;
        if (maxWidth > 0 && optBounds.outWidth > maxWidth) {
            double d = Math.pow(2, (int) Math.round(Math.log(maxWidth / (double) optBounds.outWidth) / Math.log(0.5)));
            scale = (int) d;
        }

        BitmapFactory.Options optActual = new BitmapFactory.Options();
        optActual.inSampleSize = scale;

        // Get the roughly resized bitmap
        final Bitmap bmpResized;
        try {
            bmpResized = BitmapFactory.decodeFile(filePath, optActual);
        } catch (OutOfMemoryError e) {
            AppLog.e(AppLog.T.UTILS, "OutOfMemoryError Error in setting image: " + e);
            return null;
        }

        if (bmpResized == null) {
            return null;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // Now calculate exact scale in order to resize accurately
        float percentage = (float) maxWidth / bmpResized.getWidth();
        float proportionateHeight = bmpResized.getHeight() * percentage;
        int finalHeight = (int) Math.rint(proportionateHeight);

        float scaleWidth = ((float) maxWidth) / bmpResized.getWidth();
        float scaleHeight = ((float) finalHeight) / bmpResized.getHeight();

        float scaleBy = Math.min(scaleWidth, scaleHeight);

        // Resize the bitmap to exact size
        Matrix matrix = new Matrix();
        matrix.postScale(scaleBy, scaleBy);

        // apply rotation
        if (rotation != 0) {
            matrix.setRotate(rotation);
        }

        Bitmap.CompressFormat fmt;
        if (fileExtension != null && fileExtension.equalsIgnoreCase("png")) {
            fmt = Bitmap.CompressFormat.PNG;
        } else {
            fmt = Bitmap.CompressFormat.JPEG;
        }

        final Bitmap bmpRotated;
        try {
            bmpRotated = Bitmap.createBitmap(bmpResized, 0, 0, bmpResized.getWidth(), bmpResized.getHeight(), matrix,
                    true);
        } catch (OutOfMemoryError e) {
            AppLog.e(AppLog.T.UTILS, "OutOfMemoryError Error in setting image: " + e);
            return null;
        } catch (NullPointerException e) {
            // See: https://github.com/wordpress-mobile/WordPress-Android/issues/1844
            AppLog.e(AppLog.T.UTILS, "Bitmap.createBitmap has thrown a NPE internally. This should never happen: " + e);
            return null;
        }

        if (bmpRotated == null) {
            // Fix an issue where bmpRotated is null even if the documentation doesn't say Bitmap.createBitmap can return null.
            // See: https://github.com/wordpress-mobile/WordPress-Android/issues/1848
            return null;
        }

        bmpRotated.compress(fmt, 100, stream);
        bmpResized.recycle();
        bmpRotated.recycle();

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

        // outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.DKGRAY);
        canvas.drawOval(rectF, paint);

        return output;
    }

    public static Bitmap getRoundedEdgeBitmap(final Bitmap bitmap, int radius) {
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

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.DKGRAY);
        canvas.drawRoundRect(rectF, radius, radius, paint);

        return output;
    }
}
