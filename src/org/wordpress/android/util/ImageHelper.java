package org.wordpress.android.util;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

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
import android.provider.MediaStore.Video;
import android.util.FloatMath;
import android.util.Log;
import android.widget.ImageView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class ImageHelper {
    
    public static int[] getImageSize(Uri uri, Context context){
        String path = null;
        if (uri.toString().contains("content:")) {
            String[] projection;
            Uri imgPath;

            projection = new String[] { Images.Media._ID, Images.Media.DATA };

            imgPath = uri;

            Cursor cur = context.getContentResolver().query(imgPath, projection, null, null, null);
            String thumbData = "";

            if (cur.moveToFirst()) {
                int dataColumn;
                dataColumn = cur.getColumnIndex(Images.Media.DATA);
                thumbData = cur.getString(dataColumn);
                path = thumbData;
            }
        } else { // file is not in media library
            path = uri.toString().replace("file://", "");
        }
        
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile( path, options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        int[] dimensions = { imageWidth, imageHeight};
        return dimensions;
    }
    
    public byte[] createThumbnail(byte[] bytes, String sMaxImageWidth, String orientation, boolean tiny) {
        // creates a thumbnail and returns the bytes

        int finalHeight = 0;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);

        int width = opts.outWidth;

        int finalWidth = 500; // default to this if there's a problem
        // Change dimensions of thumbnail

        if (tiny) {
            finalWidth = 150;
        }

        byte[] finalBytes;

        if (sMaxImageWidth.equals("Original Size")) {
            finalBytes = bytes;
        } else {
            finalWidth = Integer.parseInt(sMaxImageWidth);
            if (finalWidth > width) {
                // don't resize
                finalBytes = bytes;
            } else {
                int sample = 0;

                float fWidth = width;
                sample = Double.valueOf(FloatMath.ceil(fWidth / 1200)).intValue();

                if (sample == 3) {
                    sample = 4;
                } else if (sample > 4 && sample < 8) {
                    sample = 8;
                }

                opts.inSampleSize = sample;
                opts.inJustDecodeBounds = false;

                try {
                    bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
                } catch (OutOfMemoryError e) {
                    // out of memory
                    return null;
                }
                
                if (bm == null)
                    return null;

                float percentage = (float) finalWidth / bm.getWidth();
                float proportionateHeight = bm.getHeight() * percentage;
                finalHeight = (int) Math.rint(proportionateHeight);

                float scaleWidth = ((float) finalWidth) / bm.getWidth();
                float scaleHeight = ((float) finalHeight) / bm.getHeight();

                float scaleBy = Math.min(scaleWidth, scaleHeight);

                // Create a matrix for the manipulation
                Matrix matrix = new Matrix();
                // Resize the bitmap
                matrix.postScale(scaleBy, scaleBy);
                if ((orientation != null) && (orientation.equals("90") || orientation.equals("180") || orientation.equals("270"))) {
                    matrix.postRotate(Integer.valueOf(orientation));
                }

                Bitmap resized;
                try {
                    resized = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
                } catch (OutOfMemoryError e) {
                    return null;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resized.compress(Bitmap.CompressFormat.JPEG, 85, baos);

                bm.recycle(); // free up memory
                resized.recycle();

                finalBytes = baos.toByteArray();
            }
        }

        return finalBytes;

    }

    public String getExifOrientation(String path, String orientation) {
        ExifInterface exif;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
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

    // borrowed from
    // http://android-developers.blogspot.com/2010/07/multithreading-for-performance.html
    class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        public BitmapDownloaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        // Actual download method, run in the task thread
        protected Bitmap doInBackground(String... params) {
            // params comes from the execute() call: params[0] is the url.
            return downloadBitmap(params[0]);
        }

        @Override
        // Once the image is downloaded, associates it to the imageView
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                if (imageView != null && bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    public static Bitmap downloadBitmap(String url) {
        final DefaultHttpClient client = new DefaultHttpClient();

        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                // Log.w("ImageDownloader", "Error " + statusCode +
                // " while retrieving bitmap from " + url);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    return bitmap;
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
            // Log.w("ImageDownloader", "Error while retrieving bitmap from " +
            // url);
        }
        return null;
    }

    public Map<String, Object> getImageBytesForPath(String filePath, Context ctx) {
        Uri curStream = null;
        String[] projection;
        Map<String, Object> mediaData = new HashMap<String, Object>();
        String title = "", orientation = "";
        byte[] bytes;
        if (filePath != null) {
            if (!filePath.contains("content://"))
                curStream = Uri.parse("content://media" + filePath);
            else
                curStream = Uri.parse(filePath);
        }
        if (curStream != null) {
            if (filePath.contains("video")) {
                int videoID = Integer.parseInt(curStream.getLastPathSegment());
                projection = new String[] { Video.Thumbnails._ID, Video.Thumbnails.DATA };
                ContentResolver crThumb = ctx.getContentResolver();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                Bitmap videoBitmap = MediaStore.Video.Thumbnails.getThumbnail(crThumb, videoID, MediaStore.Video.Thumbnails.MINI_KIND,
                        options);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                try {
                    videoBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    bytes = stream.toByteArray();
                    title = "Video";
                    videoBitmap = null;
                } catch (Exception e) {
                    return null;
                }

            } else {
                projection = new String[] { Images.Thumbnails._ID, Images.Thumbnails.DATA, Images.Media.ORIENTATION };

                String path = "";
                Cursor cur;
                try {
                    cur = ctx.getContentResolver().query(curStream, projection, null, null, null);
                } catch (Exception e1) {
                    return null;
                }
                File jpeg = null;
                if (cur != null) {
                    String thumbData = "";

                    if (cur.moveToFirst()) {

                        int dataColumn, orientationColumn;

                        dataColumn = cur.getColumnIndex(Images.Media.DATA);
                        thumbData = cur.getString(dataColumn);
                        orientationColumn = cur.getColumnIndex(Images.Media.ORIENTATION);
                        orientation = cur.getString(orientationColumn);
                        if (orientation == null)
                            orientation = "";
                    }

                    if (thumbData == null) {
                        return null;
                    }

                    jpeg = new File(thumbData);
                    path = thumbData;
                } else {
                    path = filePath.toString().replace("file://", "");
                    jpeg = new File(path);

                }

                title = jpeg.getName();

                try {
                    bytes = new byte[(int) jpeg.length()];
                } catch (Exception e) {
                    return null;
                } catch (OutOfMemoryError e) {
                    return null;
                }

                DataInputStream in = null;
                try {
                    in = new DataInputStream(new FileInputStream(jpeg));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }
                try {
                    in.readFully(bytes);
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }

                title = jpeg.getName();
                if (orientation == "") {
                    orientation = getExifOrientation(path, orientation);
                }
            }

            mediaData.put("bytes", bytes);
            mediaData.put("title", title);
            mediaData.put("orientation", orientation);

            return mediaData;

        } else {
            return null;
        }

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
                Log.w("WordPress", "-- Error in setting image");
            }   
            catch(OutOfMemoryError oom) {
                Log.w("WordPress", "-- OOM Error in setting image");
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

}
