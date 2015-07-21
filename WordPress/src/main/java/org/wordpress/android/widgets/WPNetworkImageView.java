package org.wordpress.android.widgets;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderThumbnailTable;
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.VolleyUtils;

import java.util.HashSet;

/**
 * most of the code below is from Volley's NetworkImageView, but it's modified to support:
 *  (1) fading in downloaded images
 *  (2) manipulating images before display
 *  (3) automatically retrieving the thumbnail for YouTube & Vimeo videos
 */
public class WPNetworkImageView extends ImageView {
    public enum ImageType {
        NONE,
        PHOTO,
        VIDEO,
        AVATAR,
        BLAVATAR
    }

    private ImageType mImageType = ImageType.NONE;
    private String mUrl;
    private ImageLoader.ImageContainer mImageContainer;

    private int mDefaultImageResId;
    private int mErrorImageResId;

    private static final HashSet<String> mUrlSkipList = new HashSet<>();

    public WPNetworkImageView(Context context) {
        super(context);
    }
    public WPNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public WPNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setImageUrl(String url, ImageType imageType) {
        mUrl = url;
        mImageType = imageType;

        // The URL has potentially changed. See if we need to load it.
        loadImageIfNecessary(false);
    }

    /*
     * retrieves and displays the thumbnail for the passed video
     */
    public void setVideoUrl(final long postId, final String videoUrl) {
        mImageType = ImageType.VIDEO;

        if (TextUtils.isEmpty(videoUrl)) {
            showDefaultImage();
            return;
        }

        // if we already have a cached thumbnail for this video, show it immediately
        String cachedThumbnail = ReaderThumbnailTable.getThumbnailUrl(videoUrl);
        if (!TextUtils.isEmpty(cachedThumbnail)) {
            setImageUrl(cachedThumbnail, ImageType.VIDEO);
            return;
        }

        showDefaultImage();

        // vimeo videos require network request to get thumbnail
        if (ReaderVideoUtils.isVimeoLink(videoUrl)) {
            ReaderVideoUtils.requestVimeoThumbnail(videoUrl, new ReaderVideoUtils.VideoThumbnailListener() {
                @Override
                public void onResponse(boolean successful, String thumbnailUrl) {
                    if (successful) {
                        ReaderThumbnailTable.addThumbnail(postId, videoUrl, thumbnailUrl);
                        setImageUrl(thumbnailUrl, ImageType.VIDEO);
                    }
                }
            });
        }
    }

    /**
     * Loads the image for the view if it isn't already loaded.
     * @param isInLayoutPass True if this was invoked from a layout pass, false otherwise.
     */
    private void loadImageIfNecessary(final boolean isInLayoutPass) {
        // do nothing if image type hasn't been set yet
        if (mImageType == ImageType.NONE) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        boolean isFullyWrapContent = getLayoutParams() != null
                && getLayoutParams().height == LayoutParams.WRAP_CONTENT
                && getLayoutParams().width == LayoutParams.WRAP_CONTENT;
        // if the view's bounds aren't known yet, and this is not a wrap-content/wrap-content
        // view, hold off on loading the image.
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
                // if the request is from the same URL, return.
                return;
            } else {
                // if there is a pre-existing request, cancel it if it's fetching a different URL.
                mImageContainer.cancelRequest();
                showDefaultImage();
            }
        }

        // skip this URL if a previous request for it returned a 404
        if (mUrlSkipList.contains(mUrl)) {
            AppLog.d(AppLog.T.READER, "skipping image request " + mUrl);
            showErrorImage();
            return;
        }

        // enforce a max size to reduce memory usage
        Point pt = DisplayUtils.getDisplayPixelSize(this.getContext());
        int maxSize = Math.max(pt.x, pt.y);

        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network.
        mImageContainer = WordPress.imageLoader.get(mUrl,
                new ImageLoader.ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showErrorImage();
                        // keep track of URLs that 404 so we can skip them the next time
                        int statusCode = VolleyUtils.statusCodeFromVolleyError(error);
                        if (statusCode == 404) {
                            mUrlSkipList.add(mUrl);
                        }
                    }

                    @Override
                    public void onResponse(final ImageLoader.ImageContainer response, boolean isImmediate) {
                        // If this was an immediate response that was delivered inside of a layout
                        // pass do not set the image immediately as it will trigger a requestLayout
                        // inside of a layout. Instead, defer setting the image by posting back to
                        // the main thread.
                        if (isImmediate && isInLayoutPass) {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    // don't fade in the image since we know it's cached
                                    handleResponse(response, true, false);
                                }
                            });
                        } else {
                            handleResponse(response, isImmediate, true);
                        }
                    }
                }, maxSize, maxSize);
    }

    private static boolean canFadeInImageType(ImageType imageType) {
        return imageType == ImageType.PHOTO
            || imageType == ImageType.VIDEO;
    }

    private void handleResponse(ImageLoader.ImageContainer response,
                                boolean isCached,
                                boolean allowFadeIn) {
        if (response.getBitmap() != null) {
            Bitmap bitmap = response.getBitmap();

            // Apply circular rounding to avatars in a background task
            if (mImageType == ImageType.AVATAR) {
                new CircularizeBitmapTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bitmap);
                return;
            }

            setImageBitmap(bitmap);

            // fade in photos/videos if not cached (not used for other image types since animation can be expensive)
            if (!isCached && allowFadeIn && canFadeInImageType(mImageType)) {
                fadeIn();
            }
        } else {
            showDefaultImage();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isInEditMode()) {
            loadImageIfNecessary(true);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mImageContainer != null) {
            // If the view was bound to an image request, cancel it and clear
            // out the image from the view.
            mImageContainer.cancelRequest();
            setImageDrawable(null);
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
        return getContext().getResources().getColor(resId);
    }

    public void setDefaultImageResId(@DrawableRes int resourceId) {
        mDefaultImageResId = resourceId;
    }

    public void setErrorImageResId(@DrawableRes int resourceId) {
        mErrorImageResId = resourceId;
    }

    private void showDefaultImage() {
        // use default image resource if one was supplied...
        if (mDefaultImageResId != 0) {
            setImageResource(mDefaultImageResId);
            return;
        }

        // ... otherwise use built-in default
        switch (mImageType) {
            case NONE:
                // do nothing
                break;
            case AVATAR:
                // Grey circle for avatars
                setImageResource(R.drawable.shape_oval_grey_light);
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
            case NONE:
                // do nothing
                break;
            case AVATAR:
                // circular "mystery man" for failed avatars
                showDefaultGravatarImage();
                break;
            case BLAVATAR:
                setImageResource(R.drawable.blavatar_placeholder);
                break;
            default :
                // grey box for all others
                setImageDrawable(new ColorDrawable(getColorRes(R.color.grey_lighten_30)));
                break;
        }
    }

    public void showDefaultGravatarImage() {
        if (getContext() == null) return;
        new CircularizeBitmapTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, BitmapFactory.decodeResource(
                getContext().getResources(),
                R.drawable.gravatar_placeholder
        ));
    }

    // --------------------------------------------------------------------------------------------------


    private static final int FADE_TRANSITION = 250;

    private void fadeIn() {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(this, View.ALPHA, 0.25f, 1f);
        alpha.setDuration(FADE_TRANSITION);
        alpha.start();
    }

    // Circularizes a bitmap in a background thread
    private class CircularizeBitmapTask extends AsyncTask<Bitmap, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            if (params == null || params.length == 0) return null;

            Bitmap bitmap = params[0];
            return ImageUtils.getCircularBitmap(bitmap);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                setImageBitmap(bitmap);
            }
        }
    }
}
