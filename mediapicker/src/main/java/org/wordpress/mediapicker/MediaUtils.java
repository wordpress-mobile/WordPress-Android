package org.wordpress.mediapicker;

import android.animation.ObjectAnimator;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

public class MediaUtils {
    public static class BackgroundFetchThumbnail extends AsyncTask<Uri, String, Bitmap> {
        private static final long FADE_TIME_MS = 500;

        public enum THUMB_TYPE {
            IMAGE, VIDEO
        }

        private WeakReference<ImageView> mReference;
        private THUMB_TYPE mType;
        private int mWidth;
        private int mHeight;

        public BackgroundFetchThumbnail(ImageView resultStore, THUMB_TYPE type, int width, int height) {
            mReference = new WeakReference<>(resultStore);
            mType = type;
            mWidth = width;
            mHeight = height;
        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            String uri = params[0].toString();
            Bitmap bitmap = null;

            // TODO: cache
            if (mType == THUMB_TYPE.IMAGE) {
                Bitmap imageBitmap = BitmapFactory.decodeFile(uri);
                bitmap = ThumbnailUtils.extractThumbnail(imageBitmap, mWidth, mHeight);
            } else if (mType == THUMB_TYPE.VIDEO) {
                bitmap = ThumbnailUtils.createVideoThumbnail(uri, MediaStore.Video.Thumbnails.MINI_KIND);
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            ImageView imageView = mReference.get();

            if (imageView != null) {
                if (imageView.getTag() == this) {
                    imageView.setImageBitmap(result);
                    fadeInImage(imageView, result, FADE_TIME_MS);
                }
            }
        }
    }

    public static void fadeInImage(ImageView imageView, Bitmap image, long duration) {
        if (imageView != null) {
            imageView.setImageBitmap(image);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(imageView, View.ALPHA, 0.25f, 1f);
            alpha.setDuration(duration);
            alpha.start();
        }
    }
}
