package org.wordpress.android.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.TextUtils;
import android.view.Display;
import android.widget.ImageView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.wordpress.android.util.AppLog.T;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class ImageHelper {

    public static int[] getImageSize(Uri uri, Context context){
        String path = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        if (uri.toString().contains("content:")) {
            String[] projection = new String[] { Images.Media._ID, Images.Media.DATA };
            Cursor cur = context.getContentResolver().query(uri, projection, null, null, null);
            if (cur != null) {
                if (cur.moveToFirst()) {
                    int dataColumn = cur.getColumnIndex(Images.Media.DATA);
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

    //Read the orientation from ContentResolver. If it fails, read from EXIF.
    public int getImageOrientation(Context ctx, String filePath) {
        Uri curStream;
        String orientation = null;
        
        if (!filePath.contains("content://"))
            curStream = Uri.parse("content://media" + filePath);
        else
            curStream = Uri.parse(filePath);

        try {
            Cursor cur = ctx.getContentResolver().query(curStream, new String[] { Images.Media.ORIENTATION }, null, null, null);
            if (cur != null) {
                if(cur.moveToFirst()) {
                    orientation = cur.getString(cur.getColumnIndex(Images.Media.ORIENTATION));
                }
                cur.close();
            }
        } catch (Exception errReadingContentResolver) {
            AppLog.e(T.UTILS, errReadingContentResolver);
        }
        
        if (TextUtils.isEmpty(orientation)) {
            orientation = getExifOrientation(filePath, "");
        }
        
        int calculatedOrientation;
        try {
            calculatedOrientation = (TextUtils.isEmpty(orientation) ? 0 : Integer.valueOf(orientation));
        } catch (NumberFormatException e) {
            AppLog.e(T.UTILS, e);
            calculatedOrientation = 0;
        }
        
        return calculatedOrientation;
    }
    
    
    
    public String getExifOrientation(String path, String orientation) {
        ExifInterface exif;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            AppLog.e(T.UTILS, e);
            return orientation;
        }
        String exifOrientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        if (exifOrientation != null) {
            if (exifOrientation.equals("1")) {
                orientation = "0";
            } else if (exifOrientation.equals("3")) {
                orientation = "180";
            } else if (exifOrientation.equals("6")) {
                orientation = "90";
            } else if (exifOrientation.equals("8")) {
                orientation = "270";
            }
        } else {
            orientation = "0";
        }
        return orientation;
    }

    public static Bitmap downloadBitmap(String url) {
        final DefaultHttpClient client = new DefaultHttpClient();

        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                AppLog.w(T.UTILS, "ImageDownloader Error " + statusCode
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
            AppLog.w(T.UTILS, "ImageDownloader Error while retrieving bitmap from " + url);
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
            try {
                File f = new File(path);
                ExifInterface exif = new ExifInterface(f.getPath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                int angle = 0;

                if (orientation == ExifInterface.ORIENTATION_NORMAL) { // no need to rotate
                    return BitmapFactory.decodeFile(path, bfo);
                } else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                    angle = 90;
                } 
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                    angle = 180;
                } 
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                    angle = 270;
                }

                Matrix mat = new Matrix();
                mat.postRotate(angle);

                Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(f), null, bfo);
                return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), mat, true);                 
            }
            catch (IOException e) {
                AppLog.e(T.UTILS, "Error in setting image", e);
            }   
            catch(OutOfMemoryError oom) {
                AppLog.e(T.UTILS, "OutOfMemoryError Error in setting image: " + oom);
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


    public  String getTitleForWPImageSpan(Context ctx, String filePath) {
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
            String[] projection = new String[] { Images.Thumbnails.DATA };
            
            Cursor cur;
            try {
                cur = ctx.getContentResolver().query(curStream, projection, null, null, null);
            } catch (Exception e1) {
                AppLog.e(T.UTILS, e1);
                return null;
            }
            File jpeg;
            if (cur != null) {
                String thumbData = "";
                if (cur.moveToFirst()) {
                    int dataColumn = cur.getColumnIndex(Images.Media.DATA);
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
     * @param ctx
     * @param filePath
     * @return resized bitmap
     */
    public Bitmap getThumbnailForWPImageSpan(Context ctx, String filePath) {
        if (filePath==null)
            return null;
        
        Display display = ((Activity)ctx).getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        if (width > height)
            width = height;

        Uri curUri;
        
        if (!filePath.contains("content://"))
            curUri = Uri.parse("content://media" + filePath);
        else
            curUri = Uri.parse(filePath);
        
        if (filePath.contains("video")) {
            int videoId = 0;
            try {
                videoId = Integer.parseInt(curUri.getLastPathSegment());
            } catch (NumberFormatException e) {
            }
            ContentResolver crThumb = ctx.getContentResolver();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            return MediaStore.Video.Thumbnails.getThumbnail(crThumb, videoId, MediaStore.Video.Thumbnails.MINI_KIND,
                    options);
        } else {
            int[] dimensions = getImageSize(curUri, ctx);
            float conversionFactor = 0.40f;
            if (dimensions[0] > dimensions[1]) //width > height
                conversionFactor = 0.60f;
            int resizedWitdh = (int) (width * conversionFactor);

            // create resized picture
            int rotation = getImageOrientation(ctx, filePath);
            byte[] bytes = createThumbnailFromUri(ctx, curUri, resizedWitdh, null, rotation);

            // upload resized picture
            if (bytes != null && bytes.length > 0) {
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } else {
                return null;
            }
        }
    }
    
    public Bitmap getThumbnailForWPImageSpan(Bitmap largeBitmap, int resizeWidth) {
        
        if (largeBitmap.getWidth() < resizeWidth)
            return largeBitmap; //Do not resize.
        
        float percentage = (float) resizeWidth / largeBitmap.getWidth();
        float proportionateHeight = largeBitmap.getHeight() * percentage;
        int resizeHeight = (int) Math.rint(proportionateHeight);

        return Bitmap.createScaledBitmap(largeBitmap, resizeWidth, resizeHeight, true);
    }

    /**
     * nbradbury - 21-Feb-2014 - similar to createThumbnail but more efficient since it doesn't
     * require passing the full-size image as an array of bytes[]
     */
    public byte[] createThumbnailFromUri(Context context,
            Uri imageUri,
            int maxWidth,
            String fileExtension,
            int rotation) {
        if (context == null || imageUri == null)
            return null;

        String filePath = null;
        if (imageUri.toString().contains("content:")) {
            String[] projection = new String[] { Images.Media.DATA };
            Cursor cur = context.getContentResolver().query(imageUri, projection, null, null, null);
            if (cur != null) {
                if (cur.moveToFirst()) {
                    int dataColumn = cur.getColumnIndex(Images.Media.DATA);
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

        BitmapFactory.decodeFile(filePath, optBounds);

        // determine correct scale value (should be power of 2)
        // http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue/3549021#3549021
        int scale = 1;
        if (maxWidth > 0 && optBounds.outWidth > maxWidth) {
            double d = Math.pow(2, (int) Math.round(Math.log(maxWidth / (double) optBounds.outWidth) / Math.log(0.5)));
            scale = (int) d;
        }

        BitmapFactory.Options optActual = new BitmapFactory.Options();
        optActual.inSampleSize = scale;

        Bitmap bmpResized;

        bmpResized = BitmapFactory.decodeFile(filePath, optActual);

        if (bmpResized == null)
            return null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final Bitmap.CompressFormat fmt;
        if (fileExtension != null && fileExtension.equalsIgnoreCase("png")) {
            fmt = Bitmap.CompressFormat.PNG;
        } else {
            fmt = Bitmap.CompressFormat.JPEG;
        }

        // apply rotation
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.setRotate(rotation);
            final Bitmap bmpRotated = Bitmap.createBitmap(bmpResized, 0, 0, bmpResized.getWidth(), bmpResized.getHeight(), matrix, true);
            bmpRotated.compress(fmt, 100, stream);
            bmpResized.recycle();
            bmpRotated.recycle();
        } else {
            bmpResized.compress(fmt, 100, stream);
            bmpResized.recycle();
        }

        return stream.toByteArray();
    }

}
