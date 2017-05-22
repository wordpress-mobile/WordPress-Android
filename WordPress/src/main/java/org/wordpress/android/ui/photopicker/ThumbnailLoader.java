package org.wordpress.android.ui.photopicker;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.util.ImageUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

class ThumbnailLoader {

    private final Context mContext;
    private final ThreadPoolExecutor mExecutor;
    private final Handler mHandler;
    private boolean mIsFadeEnabled = true;

    private static final int FADE_TRANSITION = 250;

    ThumbnailLoader(Context context) {
        mContext = context;
        int numCores = Runtime.getRuntime().availableProcessors();
        int maxThreads = numCores > 1 ? numCores / 2 : 1;
        mExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads);
        mExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        mHandler = new Handler(Looper.getMainLooper());
    }

    void loadThumbnail(ImageView imageView, long imageId, boolean isVideo) {
        Runnable task = new ThumbnailLoaderRunnable(imageView, imageId, isVideo);
        mExecutor.submit(task);
    }

    /*
     * temporarily disables the fade animation - called when multiple items are changed
     * to prevent unnecessary fade/flicker
     */
    void temporarilyDisableFade() {
        mIsFadeEnabled = false;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mIsFadeEnabled = true;
            }
        }, 500);
    }
    /*
     * task to load a device thumbnail and display it in an ImageView
     */
    class ThumbnailLoaderRunnable implements Runnable {
        private final WeakReference<ImageView> mWeakImageView;
        private final long mImageId;
        private final String mTag;
        private final boolean mIsVideo;
        private Bitmap mThumbnail;

        ThumbnailLoaderRunnable(ImageView imageView, long imageId, boolean isVideo) {
            mWeakImageView = new WeakReference<>(imageView);
            mImageId = imageId;
            mIsVideo = isVideo;

            mTag = Long.toString(mImageId);
            imageView.setTag(mTag);
            if (mIsFadeEnabled) {
                imageView.setImageResource(R.drawable.photo_picker_item_background);
            }
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
            if (mIsVideo) {
                mThumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                        mContext.getContentResolver(),
                        mImageId,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null);
            } else {
                mThumbnail = ImageUtils.getThumbnail(
                        mContext.getContentResolver(),
                        mImageId,
                        MediaStore.Images.Thumbnails.MINI_KIND);
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO: handle null thumbnail - show error image, maybe?
                    if (mThumbnail != null && isImageViewValid()) {
                        mWeakImageView.get().setImageBitmap(mThumbnail);
                        if (mIsFadeEnabled) {
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
