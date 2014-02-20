package org.wordpress.android.widgets;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderThumbnailTable;
import org.wordpress.android.util.ReaderVideoUtils;
import org.wordpress.android.util.SysUtils;

/**
 * Created by nbradbury on 7/30/13.
 * most of the code below is from Volley's NetworkImageView, but it's modified to support:
 *  (1) fading in downloaded images
 *  (2) manipulating images before display
 *  (3) automatically retrieving the thumbnail for YouTube & Vimeo videos
 *  (4) adding a listener to determine when image has completed downloading (or failed)
 */
public class WPNetworkImageView extends ImageView {
    public static enum ImageType {PHOTO,
                                  PHOTO_FULL,
                                  VIDEO,
                                  AVATAR}
    private ImageType mImageType = ImageType.PHOTO;
    private String mUrl;
    private ImageLoader.ImageContainer mImageContainer;

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

    public void setImageUrl(String url, ImageType imageType) {
        setImageUrl(url, imageType, null);
    }
    public void setImageUrl(String url, ImageType imageType, ImageListener imageListener) {
        mUrl = url;
        mImageType = imageType;
        mImageListener = imageListener;

        if (TextUtils.isEmpty(mUrl)) {
            showErrorImage(mImageType);
        } else {
            // The URL has potentially changed. See if we need to load it.
            loadImageIfNecessary(false);
        }
    }

    /*
     * retrieves and displays the thumbnail for the passed video
     */
    public void setVideoUrl(final long postId, final String videoUrl) {
        mImageType = ImageType.VIDEO;

        if (TextUtils.isEmpty(videoUrl)) {
            showDefaultImage(ImageType.VIDEO);
            return;
        }

        // if we already have a cached thumbnail for this video, show it immediately
        String cachedThumbnail = ReaderThumbnailTable.getThumbnailUrl(videoUrl);
        if (!TextUtils.isEmpty(cachedThumbnail)) {
            setImageUrl(cachedThumbnail, ImageType.VIDEO);
            return;
        }

        showDefaultImage(ImageType.VIDEO);

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
            showErrorImage(mImageType);
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
                showDefaultImage(mImageType);
            }
        }

        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network.
        ImageLoader.ImageContainer newContainer = WordPress.imageLoader.get(mUrl,
                new ImageLoader.ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showErrorImage(mImageType);
                        if (mImageListener != null)
                            mImageListener.onImageLoaded(false);
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
                });

        // update the ImageContainer to be the new bitmap container.
        mImageContainer = newContainer;
    }

    private void handleResponse(ImageLoader.ImageContainer response,
                                boolean isCached,
                                boolean allowFadeIn) {
        if (response.getBitmap() != null) {
            setImageBitmap(response.getBitmap());

            // fade in photos/videos if not cached (not used for other image types since animation can be expensive)
            if (!isCached && allowFadeIn && (mImageType == ImageType.PHOTO || mImageType == ImageType.VIDEO))
                fadeIn();

            if (mImageListener!=null)
                mImageListener.onImageLoaded(true);
        } else {
            showDefaultImage(mImageType);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(true);
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

    private void showErrorImage(ImageType imageType) {
        switch (imageType) {
            case PHOTO_FULL:
                // null default for full-screen photos
                setImageDrawable(null);
                break;
            case AVATAR:
                // "mystery man" for failed avatars
                setImageResource(R.drawable.placeholder);
                break;
            default :
                // medium grey box for all others
                setImageDrawable(new ColorDrawable(getColorRes(R.color.grey_medium)));
                break;
        }
    }

    private void showDefaultImage(ImageType imageType) {
        switch (imageType) {
            case PHOTO_FULL:
                // null default for full-screen photos
                setImageDrawable(null);
                break;
            default :
                // light grey box for all others
                setImageDrawable(new ColorDrawable(getColorRes(R.color.grey_light)));
                break;
        }
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mImageType == ImageType.VIDEO)
            drawVideoOverlay(canvas);
    }

    private void drawVideoOverlay(Canvas canvas) {
        if (canvas==null)
            return;

        Bitmap overlay = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_reader_video_overlay, null);
        int overlaySize = getContext().getResources().getDimensionPixelSize(R.dimen.reader_video_overlay_size);

        // use the size of the view rather than the canvas
        int srcWidth = this.getWidth();
        int srcHeight = this.getHeight();

        // skip if overlay is larger than source image
        if (overlaySize > srcWidth || overlaySize > srcHeight)
            return;

        final int left = (srcWidth / 2) - (overlaySize / 2);
        final int top = (srcHeight / 2) - (overlaySize / 2);
        final Rect rcDst = new Rect(left, top, left + overlaySize, top + overlaySize);

        canvas.drawBitmap(overlay, null, rcDst, new Paint(Paint.FILTER_BITMAP_FLAG));

        overlay.recycle();
    }

    // --------------------------------------------------------------------------------------------------


    private static final int FADE_TRANSITION = 250;

    @SuppressLint("NewApi")
    private void fadeIn() {
        // use faster property animation if device supports it
        if (SysUtils.isGteAndroid4()) {
            ObjectAnimator alpha = ObjectAnimator.ofFloat(this, View.ALPHA, 0.25f, 1f);
            alpha.setDuration(FADE_TRANSITION);
            alpha.start();
        } else {
            AlphaAnimation animation = new AlphaAnimation(0.25f, 1f);
            animation.setDuration(FADE_TRANSITION);
            this.startAnimation(animation);
        }
    }
}
