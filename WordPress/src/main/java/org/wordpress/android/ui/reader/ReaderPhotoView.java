package org.wordpress.android.ui.reader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.widgets.WPTextView;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * used by ReaderPhotoViewerActivity to show full-width images - based on Volley's ImageView
 * but adds pinch/zoom and the ability to first load a lo-res version of the image
 */
public class ReaderPhotoView extends RelativeLayout {

    static interface PhotoViewListener {
        void onTapPhotoView();
    }

    private PhotoViewListener mPhotoViewListener;
    private String mLoResImageUrl;
    private String mHiResImageUrl;

    private ImageContainer mLoResContainer;
    private ImageContainer mHiResContainer;

    private final ImageView mImageView;
    private final ProgressBar mProgress;
    private final WPTextView mTxtError;

    public ReaderPhotoView(Context context) {
        this(context, null);
    }

    public ReaderPhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // ImageView which contains the downloaded image
        mImageView = new ImageView(context);
        LayoutParams paramsImg = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        paramsImg.addRule(CENTER_IN_PARENT);
        this.addView(mImageView, paramsImg);

        // error text that appears when download fails
        mTxtError = new WPTextView(context);
        LayoutParams paramsTxt = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsTxt.addRule(CENTER_IN_PARENT);
        mTxtError.setGravity(Gravity.CENTER);
        mTxtError.setText(context.getString(R.string.reader_toast_err_view_image));
        mTxtError.setTextColor(context.getResources().getColor(R.color.grey_extra_light));
        mTxtError.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimensionPixelSize(R.dimen.text_sz_large));
        mTxtError.setVisibility(View.GONE);
        this.addView(mTxtError, paramsTxt);

        // progress bar which appears while downloading
        mProgress = new ProgressBar(context);
        LayoutParams paramsProgress = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsProgress.addRule(CENTER_IN_PARENT);
        mProgress.setVisibility(View.GONE);
        this.addView(mProgress, paramsProgress);
    }

    /**
     * @param imageUrl the url of the image to load
     * @param hiResWidth maximum width of the full-size image
     * @param isPrivate whether this is an image from a private blog
     * @param listener listener for taps on this view
     */
    public void setImageUrl(String imageUrl,
                            int hiResWidth,
                            boolean isPrivate,
                            PhotoViewListener listener) {
        int loResWidth = (int) (hiResWidth * 0.10f);
        if (isPrivate) {
            mLoResImageUrl = ReaderUtils.getPrivateImageForDisplay(imageUrl, loResWidth, 0);
            mHiResImageUrl = ReaderUtils.getPrivateImageForDisplay(imageUrl, hiResWidth, 0);
        } else {
            mLoResImageUrl = PhotonUtils.getPhotonImageUrl(imageUrl, loResWidth, 0);
            mHiResImageUrl = PhotonUtils.getPhotonImageUrl(imageUrl, hiResWidth, 0);
        }

        mPhotoViewListener = listener;
        loadLoResImage();
    }

    private boolean isRequestingUrl(ImageContainer container, String url) {
        return (container != null
             && container.getRequestUrl() != null
             && container.getRequestUrl().equals(url));
    }

    private boolean hasLayout() {
        // if the view's bounds aren't known yet, and this is not a wrap-content/wrap-content
        // view, hold off on loading the image.
        if (getWidth() == 0 && getHeight() == 0) {
            boolean isFullyWrapContent = getLayoutParams() != null
                    && getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT
                    && getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
            if (!isFullyWrapContent) {
                return false;
            }
        }

        return true;
    }

    private void loadLoResImage() {
        if (!hasLayout() || TextUtils.isEmpty(mLoResImageUrl)) {
            return;
        }

        // skip if this same image url is already being loaded
        if (isRequestingUrl(mLoResContainer, mLoResImageUrl)) {
            return;
        }

        showProgress();

        Point pt = DisplayUtils.getDisplayPixelSize(this.getContext());
        int maxSize = Math.min(pt.x, pt.y);

        mLoResContainer = WordPress.imageLoader.get(mLoResImageUrl,
                new ImageLoader.ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.READER, error);
                        hideProgress();
                        showError();
                    }

                    @Override
                    public void onResponse(final ImageContainer response, boolean isImmediate) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                handleResponse(response.getBitmap(), true);
                            }
                        });
                    }
                }, maxSize, maxSize);
    }

    private void loadHiResImage() {
        if (!hasLayout() || TextUtils.isEmpty(mHiResImageUrl)) {
            return;
        }

        if (isRequestingUrl(mHiResContainer, mHiResImageUrl)) {
            return;
        }

        Point pt = DisplayUtils.getDisplayPixelSize(this.getContext());
        int maxSize = Math.max(pt.x, pt.y);

        mHiResContainer = WordPress.imageLoader.get(mHiResImageUrl,
                new ImageLoader.ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.READER, error);
                    }

                    @Override
                    public void onResponse(final ImageContainer response, boolean isImmediate) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                handleResponse(response.getBitmap(), false);
                            }
                        });
                    }
                }, maxSize, maxSize);
    }

    private void handleResponse(Bitmap bitmap, boolean isLoRes) {
        if (bitmap != null) {
            hideProgress();

            // show the bitmap and attach the pinch/zoom handler
            mImageView.setImageBitmap(bitmap);
            setAttacher();

            // load hi-res image if this was the lo-res one
            if (isLoRes && !mLoResImageUrl.equals(mHiResImageUrl)) {
                loadHiResImage();
            }
        }
    }

    private void setAttacher() {
        PhotoViewAttacher attacher = new PhotoViewAttacher(mImageView);
        attacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float v, float v2) {
                if (mPhotoViewListener != null) {
                    mPhotoViewListener.onTapPhotoView();
                }
            }
        });
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float v, float v2) {
                if (mPhotoViewListener != null) {
                    mPhotoViewListener.onTapPhotoView();
                }
            }
        });
    }

    private void showError() {
        hideProgress();
        if (mTxtError != null) {
            mTxtError.setVisibility(View.VISIBLE);
        }
    }

    private void showProgress() {
        if (mProgress != null) {
            mProgress.setVisibility(View.VISIBLE);
            mProgress.bringToFront();
        }
    }

    private void hideProgress() {
        if (mProgress != null) {
            mProgress.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isInEditMode()) {
            loadLoResImage();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mLoResContainer != null || mHiResContainer != null) {
            mImageView.setImageDrawable(null);
        }
        if (mLoResContainer != null) {
            mLoResContainer.cancelRequest();
            mLoResContainer = null;
        }
        if (mHiResContainer != null) {
            mHiResContainer.cancelRequest();
            mHiResContainer = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }
}
