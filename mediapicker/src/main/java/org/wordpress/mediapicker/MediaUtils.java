package org.wordpress.mediapicker;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.android.volley.toolbox.ImageLoader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MediaUtils {
    private static final long FADE_TIME_MS = 250;

    public static void fadeInImage(ImageView imageView, Bitmap image) {
        fadeInImage(imageView, image, FADE_TIME_MS);
    }

    public static void fadeInImage(ImageView imageView, Bitmap image, long duration) {
        if (imageView != null) {
            imageView.setImageBitmap(image);
            Animation alpha = new AlphaAnimation(0.25f, 1.0f);
            alpha.setDuration(duration);
            imageView.startAnimation(alpha);
            // Use the implementation below if you can figure out how to make it work on all devices
            // My Galaxy S3 (4.1.2) would not animate
//            imageView.setImageBitmap(image);
//            ObjectAnimator.ofFloat(imageView, View.ALPHA, 0.25f, 1.0f).setDuration(duration).start();
        }
    }

    public static Cursor getMediaStoreThumbnails(ContentResolver contentResolver, String[] columns) {
        Uri thumbnailUri = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
        return MediaStore.Images.Thumbnails.query(contentResolver, thumbnailUri, columns);
    }

    public static Cursor getDeviceMediaStoreVideos(ContentResolver contentResolver, String[] columns) {
        Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        return MediaStore.Video.query(contentResolver, videoUri, columns);
    }

    public static class BackgroundFetchThumbnail extends AsyncTask<Uri, String, Bitmap> {
        public static final int TYPE_IMAGE = 0;
        public static final int TYPE_VIDEO = 1;

        private static final int MAX_ACTIVE_FETCHES_DEFAULT = 24;
        private static final List<BackgroundFetchThumbnail> sActiveFetches = new ArrayList<>();

        private WeakReference<ImageView> mReference;
        private ImageLoader.ImageCache mCache;
        private int mType;
        private int mWidth;
        private int mHeight;
        private int mRotation;
        private int mMaxFetches;

        public BackgroundFetchThumbnail(ImageView resultStore, ImageLoader.ImageCache cache, int type, int width, int height, int rotation) {
            mReference = new WeakReference<>(resultStore);
            mMaxFetches = MAX_ACTIVE_FETCHES_DEFAULT;
            mCache = cache;
            mType = type;
            mWidth = width;
            mHeight = height;
            mRotation = rotation;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (sActiveFetches.size() > mMaxFetches) {
                sActiveFetches.remove(0).cancel(true);
            }

            sActiveFetches.add(this);
        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            String uri = params[0].toString();
            Bitmap bitmap = null;

            if (mType == TYPE_IMAGE) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(uri, options);
                options.inJustDecodeBounds = false;
                options.inSampleSize = calculateInSampleSize(options);
                bitmap = BitmapFactory.decodeFile(uri, options);

                if (bitmap != null) {
                    Matrix rotation = new Matrix();
                    rotation.setRotate(mRotation, bitmap.getWidth() / 2.0f, bitmap.getHeight() / 2.0f);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotation, false);
                }
            } else if (mType == TYPE_VIDEO) {
                // MICRO_KIND = 96 x 96
                // MINI_KIND  = 512 x 384
                bitmap = ThumbnailUtils.createVideoThumbnail(uri, MediaStore.Video.Thumbnails.MINI_KIND);
            }

            if (mCache != null && bitmap != null) {
                mCache.putBitmap(uri, bitmap);
            }

            return bitmap;
        }

        // http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
        private int calculateInSampleSize(BitmapFactory.Options options) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > mHeight || width > mWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > mHeight
                        && (halfWidth / inSampleSize) > mWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            sActiveFetches.remove(this);

            ImageView imageView = mReference.get();

            if (imageView != null) {
                if (imageView.getTag() == this) {
                    imageView.setTag(null);
                    if (result == null) {
                        imageView.setImageResource(R.drawable.ic_now_wallpaper_white);
                    } else {
                        fadeInImage(imageView, result);
                    }
                }
            }
        }

        public void setMaxFetches(int maxFetches) {
            mMaxFetches = maxFetches;
        }
    }
}
