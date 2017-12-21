package org.wordpress.android.widgets;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderThumbnailTable;
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.VolleyUtils;

import java.util.HashSet;
import java.util.concurrent.RejectedExecutionException;

/**
 * most of the code below is from Volley's NetworkImageView, but it's modified to support:
 *  (1) fading in downloaded images
 *  (2) manipulating images before display
 *  (3) automatically retrieving the thumbnail for YouTube & Vimeo videos
 */
public class WPNetworkImageView extends AppCompatImageView {
    public enum ImageType {
        NONE,
        PHOTO,
        PHOTO_ROUNDED,
        VIDEO,
        AVATAR,
        BLAVATAR,
        GONE_UNTIL_AVAILABLE,
        PLUGIN_ICON,
    }

    public interface ImageLoadListener {
        void onLoaded();
        void onError();
    }

    private ImageType mImageType = ImageType.NONE;
    private String mUrl;
    private ImageLoader.ImageContainer mImageContainer;

    private int mDefaultImageResId;
    private int mErrorImageResId;

    private int mCropWidth;
    private int mCropHeight;

    private static final HashSet<String> mUrlSkipList = new HashSet<>();

    public WPNetworkImageView(Context context) {
        this(context, null);
    }
    public WPNetworkImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public WPNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.wpNetworkImageView, 0, 0);

            try {
                if (a.hasValue(R.styleable.wpNetworkImageView_wpDefaultImageDrawable)) {
                    mDefaultImageResId = a.getResourceId(R.styleable.wpNetworkImageView_wpDefaultImageDrawable, 0);
                }
                if (a.hasValue(R.styleable.wpNetworkImageView_wpErrorImageDrawable)) {
                    mErrorImageResId = a.getResourceId(R.styleable.wpNetworkImageView_wpErrorImageDrawable, 0);
                }
            } finally {
                a.recycle();
            }
        }
    }

    public void setImageUrl(String url, ImageType imageType) {
        setImageUrl(url, imageType, null);
    }

    public void setImageUrl(String url, ImageType imageType, ImageLoadListener imageLoadListener) {
        setImageUrl(url, imageType, imageLoadListener, 0, 0);
    }

    public void setImageUrl(String url,
                            ImageType imageType,
                            ImageLoadListener imageLoadListener,
                            int cropWidth,
                            int cropHeight) {
        mUrl = url;
        mImageType = imageType;

        if (cropWidth > 0 && cropHeight > 0) {
            mCropWidth = cropWidth;
            mCropHeight = cropHeight;
        } else {
            mCropWidth = 0;
            mCropHeight = 0;
        }

        // The URL has potentially changed. See if we need to load it.
        loadImageIfNecessary(false, imageLoadListener);
    }

    /*
     * retrieves and displays the thumbnail for the passed video
     */
    public void setVideoUrl(final long postId, final String videoUrl) {
        mImageType = ImageType.VIDEO;

        if (TextUtils.isEmpty(videoUrl)) {
            showErrorImage();
            return;
        }

        // if this is a YouTube video we can determine the thumbnail url from the passed url,
        // otherwise check if we've already cached the thumbnail url for this video
        String thumbnailUrl;
        if (ReaderVideoUtils.isYouTubeVideoLink(videoUrl)) {
            thumbnailUrl = ReaderVideoUtils.getYouTubeThumbnailUrl(videoUrl);
        } else {
            thumbnailUrl = ReaderThumbnailTable.getThumbnailUrl(videoUrl);
        }
        if (!TextUtils.isEmpty(thumbnailUrl)) {
            setImageUrl(thumbnailUrl, ImageType.VIDEO);
            return;
        }

        if (MediaUtils.isValidImage(videoUrl)) {
            setImageUrl(videoUrl, ImageType.VIDEO);
        } else if (ReaderVideoUtils.isVimeoLink(videoUrl)) {
            // vimeo videos require network request to get thumbnail
            showDefaultImage();
            ReaderVideoUtils.requestVimeoThumbnail(videoUrl, new ReaderVideoUtils.VideoThumbnailListener() {
                @Override
                public void onResponse(boolean successful, String thumbnailUrl) {
                    if (successful) {
                        ReaderThumbnailTable.addThumbnail(postId, videoUrl, thumbnailUrl);
                        setImageUrl(thumbnailUrl, ImageType.VIDEO);
                    }
                }
            });
        } else {
            AppLog.d(AppLog.T.UTILS, "no video thumbnail for " + videoUrl);
            showErrorImage();
        }
    }

    /**
     * Loads the image for the view if it isn't already loaded.
     * @param isInLayoutPass True if this was invoked from a layout pass, false otherwise.
     */
    private void loadImageIfNecessary(final boolean isInLayoutPass,
                                      final ImageLoadListener imageLoadListener) {
        // do nothing if image type hasn't been set yet
        if (mImageType == ImageType.NONE) {
            return;
        }

        int width = getWidth();
        int height = getHeight();
        ScaleType scaleType = getScaleType();

        boolean wrapWidth = false, wrapHeight = false;
        if (getLayoutParams() != null) {
            wrapWidth = getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
            wrapHeight = getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        // if the view's bounds aren't known yet, and this is not a wrap-content/wrap-content
        // view, hold off on loading the image.
        boolean isFullyWrapContent = wrapWidth && wrapHeight;
        if (width == 0 && height == 0 && !isFullyWrapContent) {
            return;
        }

        // if the URL to be loaded in this view is empty, cancel any old requests and clear the
        // currently loaded image.
        if (TextUtils.isEmpty(mUrl)) {
            if (mImageContainer != null) {
                mImageContainer.cancelRequest();
                mImageContainer = null;
            }
            showErrorImage();
            return;
        }

        // if there was an old request in this view, check if it needs to be canceled.
        if (mImageContainer != null && mImageContainer.getRequestUrl() != null) {
            if (mImageContainer.getRequestUrl().equals(mUrl)) {
                // if the request is from the same URL and it's not GONE_UNTIL_AVAILABLE, return.
                if (mImageType != ImageType.GONE_UNTIL_AVAILABLE) {
                    // GONE_UNTIL_AVAILABLE image type will make a new request if the previous response wasn't a 404 response,
                    // Volley usually returns it from cache.
                    return;
                }
            } else {
                // if there is a pre-existing request, cancel it if it's fetching a different URL.
                mImageContainer.cancelRequest();
                showDefaultImage();
            }
        }

        // skip this URL if a previous request for it returned a 404
        if (mUrlSkipList.contains(mUrl)) {
            AppLog.d(AppLog.T.UTILS, "skipping image request " + mUrl);
            showErrorImage();
            if (imageLoadListener != null) {
                imageLoadListener.onError();
            }
            return;
        }


        // Calculate the max image width / height to use while ignoring WRAP_CONTENT dimens.
        int maxWidth = wrapWidth ? 0 : width;
        int maxHeight = wrapHeight ? 0 : height;

        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network.
        ImageLoader.ImageContainer newContainer = WordPress.sImageLoader.get(mUrl,
                new WPNetworkImageLoaderListener(mUrl, isInLayoutPass, imageLoadListener), maxWidth, maxHeight, scaleType);
        // update the ImageContainer to be the new bitmap container.
        mImageContainer = newContainer;
    }

    /**
     * Our implementation of ImageLoader.ImageListener that keeps a reference to the requested URL
     * and makes sure we're setting the correct requested picture on response.
     *
     * Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5100
     * This is a fix for those cases when WPNetworkImageView instances are used in UI items that can be recycled.
     * The cell containing WPNetworkImageView could be recycled while the image request was still underway,
     * so when the request completed it set the picture to the one requested prior to recycling.
     */
    private class WPNetworkImageLoaderListener implements ImageLoader.ImageListener {
        private final String mRequestedURL;
        private final ImageLoadListener mImageLoadListener;
        private final boolean mIsInLayoutPass;

        WPNetworkImageLoaderListener(final String requestedURL,
                                     final boolean isInLayoutPass,
                                     final ImageLoadListener imageLoadListener) {
            mRequestedURL = requestedURL;
            mIsInLayoutPass = isInLayoutPass;
            mImageLoadListener = imageLoadListener;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            // keep track of URLs that 404 so we can skip them the next time
            int statusCode = VolleyUtils.statusCodeFromVolleyError(error);
            if (statusCode == 404) {
                mUrlSkipList.add(mRequestedURL);
            }

            if (mUrl == null || !mUrl.equals(mRequestedURL)) {
                AppLog.w(AppLog.T.UTILS, "WPNetworkImageView > request no longer valid: " + mRequestedURL);
                return;
            }
            showErrorImage();

            if (mImageLoadListener != null) {
                mImageLoadListener.onError();
            }
        }

        @Override
        public void onResponse(final ImageLoader.ImageContainer response, boolean isImmediate) {
            if (mUrl == null || !mUrl.equals(mRequestedURL)) {
                AppLog.w(AppLog.T.UTILS, "WPNetworkImageView > request no longer valid: " + mRequestedURL);
                return;
            }
            // If this was an immediate response that was delivered inside of a layout
            // pass do not set the image immediately as it will trigger a requestLayout
            // inside of a layout. Instead, defer setting the image by posting back to
            // the main thread.
            if (isImmediate && mIsInLayoutPass) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        onResponse(response, false);
                    }
                });
                return;
            }
            handleResponse(response, isImmediate, mImageLoadListener);
        }
    }

    private static boolean canFadeInImageType(ImageType imageType) {
        return imageType == ImageType.PHOTO
                || imageType == ImageType.VIDEO;
    }

    private void handleResponse(ImageLoader.ImageContainer response, boolean isCached, ImageLoadListener
            imageLoadListener) {
        if (response != null && response.getBitmap() != null) {
            Bitmap bitmap = response.getBitmap();

            if (mImageType == ImageType.GONE_UNTIL_AVAILABLE) {
                setVisibility(View.VISIBLE);
            }
            
            // if cropping is requested, do it before further manipulation
            if (mCropWidth > 0 && mCropHeight > 0) {
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, mCropWidth, mCropHeight);
            }

            try {
                // Apply circular rounding to avatars in a background task
                if (mImageType == ImageType.AVATAR) {
                    new ShapeBitmapTask(ShapeType.CIRCLE, imageLoadListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bitmap);
                    return;
                } else if (mImageType == ImageType.PHOTO_ROUNDED) {
                    new ShapeBitmapTask(ShapeType.ROUNDED, imageLoadListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bitmap);
                    return;
                }
            } catch (RejectedExecutionException e) {
                AppLog.w(AppLog.T.UTILS, "Too many tasks already available in the default AsyncTask.THREAD_POOL_EXECUTOR queue. " +
                        "The current ShapeBitmapTask was rejected");
                showDefaultImage();
                return;
            }

            setImageBitmap(bitmap);

            // fade in photos/videos if not cached (not used for other image types since animation can be expensive)
            if (!isCached && canFadeInImageType(mImageType)) {
                fadeIn();
            }
        } else {
            showDefaultImage();
        }
    }

    public void resetImage() {
        if (mImageContainer != null) {
            // If the view was bound to an image request, cancel it and clear
            // out the image from the view.
            mImageContainer.cancelRequest();
            // also clear out the container so we can reload the image if necessary.
            mImageContainer = null;
        }
        setImageBitmap(null);
    }

    public void removeCurrentUrlFromSkiplist() {
        if (!TextUtils.isEmpty(mUrl)) {
            mUrlSkipList.remove(mUrl);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(true, null);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mImageContainer != null) {
            // If the view was bound to an image request, cancel it and clear
            // out the image from the view.
            mImageContainer.cancelRequest();
            setImageBitmap(null);
            // also clear out the container so we can reload the image if necessary.
            mImageContainer = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    private int getColorRes(@ColorRes int resId) {
        return ContextCompat.getColor(getContext(), resId);
    }

    public void setDefaultImageResId(@DrawableRes int resourceId) {
        mDefaultImageResId = resourceId;
    }

    public void setErrorImageResId(@DrawableRes int resourceId) {
        mErrorImageResId = resourceId;
    }

    public void showDefaultImage() {
        // use default image resource if one was supplied...
        if (mDefaultImageResId != 0) {
            setImageResource(mDefaultImageResId);
            return;
        }

        // ... otherwise use built-in default
        switch (mImageType) {
            case GONE_UNTIL_AVAILABLE:
                this.setVisibility(View.GONE);
                break;
            case NONE:
                // do nothing
                break;
            case AVATAR:
                // Grey circle for avatars
                setImageResource(R.drawable.shape_oval_grey_light);
                break;
            case PLUGIN_ICON:
                showDefaultPluginIcon();
                break;
            default :
                // light grey box for all others
                setImageDrawable(new ColorDrawable(getColorRes(R.color.grey_light)));
                break;
        }
    }

    private void showErrorImage() {
        if (mErrorImageResId != 0) {
            setImageResource(mErrorImageResId);
            return;
        }

        switch (mImageType) {
            case GONE_UNTIL_AVAILABLE:
                this.setVisibility(View.GONE);
                break;
            case NONE:
                // do nothing
                break;
            case AVATAR:
                // circular "mystery man" for failed avatars
                showDefaultGravatarImage();
                break;
            case BLAVATAR:
                showDefaultBlavatarImage();
                break;
            case PLUGIN_ICON:
                showDefaultPluginIcon();
                break;
            default :
                // grey box for all others
                setImageDrawable(new ColorDrawable(getColorRes(R.color.grey_lighten_30)));
                break;
        }
    }

    public void showDefaultGravatarImageAndNullifyUrl() {
        // Setting the image url `null` will result in showing the default image by calling `showErrorImage`
        setImageUrl(null, ImageType.AVATAR);
    }

    private void showDefaultGravatarImage() {
        if (getContext() == null) return;
        try {
            new ShapeBitmapTask(ShapeType.CIRCLE, null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, BitmapFactory.decodeResource(
                    getContext().getResources(),
                    R.drawable.ic_placeholder_gravatar_grey_lighten_20_100dp
            ));
        } catch (RejectedExecutionException e) {
            AppLog.w(AppLog.T.UTILS, "Too many tasks already available in the default AsyncTask.THREAD_POOL_EXECUTOR queue. " +
                    "The current DefaultGravatarImage was rejected");
        }
    }

    public void showDefaultBlavatarImageAndNullifyUrl() {
        // Setting the image url `null` will result in showing the default image by calling `showErrorImage`
        setImageUrl(null, ImageType.BLAVATAR);
    }

    private void showDefaultBlavatarImage() {
        setImageResource(R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp);
    }

    private void showDefaultPluginIcon() {
        setImageResource(R.drawable.plugin_placeholder);
    }

    // --------------------------------------------------------------------------------------------------


    private static final int FADE_TRANSITION = 250;

    private void fadeIn() {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(this, View.ALPHA, 0.25f, 1f);
        alpha.setDuration(FADE_TRANSITION);
        alpha.start();
    }

    // Circularizes or rounds the corners of a bitmap in a background thread
    private enum ShapeType { CIRCLE, ROUNDED }
    private class ShapeBitmapTask extends AsyncTask<Bitmap, Void, Bitmap> {
        private final ImageLoadListener mImageLoadListener;
        private final ShapeType mShapeType;
        private int mRoundedCornerRadiusPx;
        private static final int ROUNDED_CORNER_RADIUS_DP = 2;

        public ShapeBitmapTask(ShapeType shapeType, ImageLoadListener imageLoadListener) {
            mImageLoadListener = imageLoadListener;
            mShapeType = shapeType;
            if (mShapeType == ShapeType.ROUNDED) {
                mRoundedCornerRadiusPx = DisplayUtils.dpToPx(getContext(), ROUNDED_CORNER_RADIUS_DP);
            }
        }

        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            if (params == null || params.length == 0) return null;

            Bitmap bitmap = params[0];
            switch (mShapeType) {
                case CIRCLE:
                    return ImageUtils.getCircularBitmap(bitmap);
                case ROUNDED:
                    return ImageUtils.getRoundedEdgeBitmap(bitmap, mRoundedCornerRadiusPx, Color.TRANSPARENT);
                default:
                    return bitmap;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                setImageBitmap(bitmap);
                if (mImageLoadListener != null) {
                    mImageLoadListener.onLoaded();
                    fadeIn();
                }
            } else {
                if (mImageLoadListener != null) {
                    mImageLoadListener.onError();
                }
            }
        }
    }
}
