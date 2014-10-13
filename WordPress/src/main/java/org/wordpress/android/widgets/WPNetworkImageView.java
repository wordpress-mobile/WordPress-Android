package org.wordpress.android.widgets;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
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
import org.wordpress.android.util.VolleyUtils;

/**
 * most of the code below is from Volley's NetworkImageView, but it's modified to support:
 *  (1) fading in downloaded images
 *  (2) manipulating images before display
 *  (3) automatically retrieving the thumbnail for YouTube & Vimeo videos
 *  (4) adding a listener to determine when image request has completed or failed
 *  (5) automatically retrying mshot requests that return a 307
 */
public class WPNetworkImageView extends ImageView {
    public static enum ImageType {NONE,
                                  PHOTO,
                                  MSHOT,
                                  VIDEO,
                                  AVATAR}

    private ImageType mImageType = ImageType.NONE;
    private String mUrl;
    private ImageLoader.ImageContainer mImageContainer;

    private int mRetryCnt;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 2500;

    public interface ImageListener {
        public void onImageLoaded(boolean succeeded);
    }
    private ImageListener mImageListener;

    public WPNetworkImageView(Context context) {
        super(context);
    }
    public WPNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public WPNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public String getUrl() {
        return mUrl;
    }

    public void setImageUrl(String url, ImageType imageType) {
        setImageUrl(url, imageType, null);
    }
    public void setImageUrl(String url, ImageType imageType, ImageListener imageListener) {
        mUrl = url;
        mImageType = imageType;
        mImageListener = imageListener;
        mRetryCnt = 0;

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

    /*
     * retry the current image request after a brief delay
     */
    private void retry(final boolean isInLayoutPass) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AppLog.d(AppLog.T.READER, String.format("retrying image request (%d)", mRetryCnt));
                if (mImageContainer != null) {
                    mImageContainer.cancelRequest();
                    mImageContainer = null;
                }
                loadImageIfNecessary(isInLayoutPass);
            }
        }, RETRY_DELAY);
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

        // enforce a max size to reduce memory usage
        Point pt = DisplayUtils.getDisplayPixelSize(this.getContext());
        int maxSize = Math.max(pt.x, pt.y);

        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network.
        ImageLoader.ImageContainer newContainer = WordPress.imageLoader.get(mUrl,
                new ImageLoader.ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // mshot requests return a 307 if the mshot has never been requested,
                        // handle this by retrying request after a short delay to give time
                        // for server to generate the image
                        if (mImageType == ImageType.MSHOT
                                && mRetryCnt < MAX_RETRIES
                                && VolleyUtils.statusCodeFromVolleyError(error) == 307)
                        {
                            mRetryCnt++;
                            retry(isInLayoutPass);
                        } else {
                            showErrorImage();
                            if (mImageListener != null) {
                                mImageListener.onImageLoaded(false);
                            }
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

        // update the ImageContainer to be the new bitmap container.
        mImageContainer = newContainer;
    }

    private static boolean canFadeInImageType(ImageType imageType) {
        return imageType == ImageType.PHOTO
            || imageType == ImageType.VIDEO
            || imageType == ImageType.MSHOT;
    }

    private void handleResponse(ImageLoader.ImageContainer response,
                                boolean isCached,
                                boolean allowFadeIn) {
        if (response.getBitmap() != null) {
            setImageBitmap(response.getBitmap());

            // fade in photos/videos if not cached (not used for other image types since animation can be expensive)
            if (!isCached && allowFadeIn && canFadeInImageType(mImageType))
                fadeIn();

            if (mImageListener != null) {
                mImageListener.onImageLoaded(true);
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

    private int getColorRes(int resId) {
        return getContext().getResources().getColor(resId);
    }

    private void showDefaultImage() {
        switch (mImageType) {
            case NONE:
                // do nothing
                break;
            case MSHOT:
                // null default for mshots
                setImageDrawable(null);
                break;
            default :
                // light grey box for all others
                setImageDrawable(new ColorDrawable(getColorRes(R.color.grey_light)));
                break;
        }
    }

    void showErrorImage() {
        switch (mImageType) {
            case NONE:
                // do nothing
                break;
            case AVATAR:
                // "mystery man" for failed avatars
                setImageResource(R.drawable.gravatar_placeholder);
                break;
            default :
                // medium grey box for all others
                setImageDrawable(new ColorDrawable(getColorRes(R.color.grey_medium)));
                break;
        }
    }

    // --------------------------------------------------------------------------------------------------


    private static final int FADE_TRANSITION = 250;

    private void fadeIn() {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(this, View.ALPHA, 0.25f, 1f);
        alpha.setDuration(FADE_TRANSITION);
        alpha.start();
    }
}
