package org.wordpress.android.ui.photopicker;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import org.wordpress.android.util.ImageUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

class ThumbnailLoader {
    private final Context mContext;
    private final ThreadPoolExecutor mExecutor;
    private final Handler mHandler;

    private static final long FADE_TRANSITION = 250;

    ThumbnailLoader(Context context) {
        mContext = context;
        int numCores = Runtime.getRuntime().availableProcessors();
        int maxThreads = numCores > 1 ? numCores / 2 : 1;
        mExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads);
        mExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        mHandler = new Handler(Looper.getMainLooper());
    }

    void loadThumbnail(ImageView imageView,
                       long imageId,
                       boolean isVideo,
                       boolean animate,
                       int maxSize) {
        Runnable task = new ThumbnailLoaderRunnable(imageView, imageId, isVideo, animate, maxSize);
        mExecutor.submit(task);
    }

    /*
     * task to load a device thumbnail and display it in an ImageView
     */
    class ThumbnailLoaderRunnable implements Runnable {
        private final WeakReference<ImageView> mWeakImageView;
        private final long mImageId;
        private final String mTag;
        private final boolean mIsVideo;
        private final boolean mAnimate;
        private final int mMaxSize;
        private Bitmap mThumbnail;

        ThumbnailLoaderRunnable(ImageView imageView,
                                long imageId,
                                boolean isVideo,
                                boolean animate,
                                int maxSize) {
            mWeakImageView = new WeakReference<>(imageView);
            mImageId = imageId;
            mIsVideo = isVideo;
            mAnimate = animate;
            mMaxSize = maxSize;

            mTag = Long.toString(mImageId);
            imageView.setTag(mTag);
        }

        private boolean isImageViewValid() {
            ImageView imageView = mWeakImageView.get();
            if (imageView != null && imageView.getTag() instanceof String) {
                // make sure this imageView has the original tag - it may
                // be different if the view was recycled
                String currentTag = (String) imageView.getTag();
                return mTag.equals(currentTag);
            } else {
                return false;
            }
        }

        @Override
        public void run() {
            Bitmap media;
            if (mIsVideo) {
                media = MediaStore.Video.Thumbnails.getThumbnail(
                        mContext.getContentResolver(),
                        mImageId,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null);
            } else {
                media = ImageUtils.getThumbnail(
                        mContext.getContentResolver(),
                        mImageId,
                        MediaStore.Images.Thumbnails.MINI_KIND);
            }

            if (media != null && media.getWidth() > mMaxSize) {
                mThumbnail = ImageUtils.getScaledBitmapAtLongestSide(media, mMaxSize);
            } else {
                mThumbnail = media;
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mThumbnail != null && isImageViewValid()) {
                        mWeakImageView.get().setImageBitmap(mThumbnail);
                        if (mAnimate) {
                            ObjectAnimator alpha = ObjectAnimator.ofFloat(
                                    mWeakImageView.get(), View.ALPHA, 0.25f, 1f);
                            alpha.setDuration(FADE_TRANSITION);
                            alpha.start();
                        }
                    }
                }
            });
        }
    }
}
