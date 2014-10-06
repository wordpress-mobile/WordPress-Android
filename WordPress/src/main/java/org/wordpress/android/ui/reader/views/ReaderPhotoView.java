package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * used by ReaderPhotoViewerActivity to show full-width images - based on Volley's ImageView
 * but adds pinch/zoom and the ability to first load a lo-res version of the image
 */
public class ReaderPhotoView extends RelativeLayout {

    public static interface PhotoViewListener {
        void onTapPhotoView();
    }

    private PhotoViewListener mPhotoViewListener;
    private String mLoResImageUrl;
    private String mHiResImageUrl;

    private ImageContainer mLoResContainer;
    private ImageContainer mHiResContainer;

    private final ImageView mImageView;
    private final ProgressBar mProgress;
    private final TextView mTxtError;
    private boolean mIsInitialLayout = true;

    public ReaderPhotoView(Context context) {
        this(context, null);
    }

    public ReaderPhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.reader_photo_view, this);

        // ImageView which contains the downloaded image
        mImageView = (ImageView) findViewById(R.id.image_photo);

        // error text that appears when download fails
        mTxtError = (TextView) findViewById(R.id.text_error);

        // progress bar which appears while downloading
        mProgress = (ProgressBar) findViewById(R.id.progress_loading);
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
        mLoResImageUrl = ReaderUtils.getResizedImageUrl(imageUrl, loResWidth, 0, isPrivate);
        mHiResImageUrl = ReaderUtils.getResizedImageUrl(imageUrl, hiResWidth, 0, isPrivate);

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
            AppLog.d(AppLog.T.READER, "reader photo > already requesting lo-res");
            return;
        }

        Point pt = DisplayUtils.getDisplayPixelSize(this.getContext());
        int maxSize = Math.min(pt.x, pt.y);

        showProgress();

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
            AppLog.d(AppLog.T.READER, "reader photo > already requesting hi-res");
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
            if (mIsInitialLayout) {
                mIsInitialLayout = false;
                AppLog.d(AppLog.T.READER, "reader photo > initial layout");
                post(new Runnable() {
                    @Override
                    public void run() {
                        loadLoResImage();
                    }
                });
            }
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
        mIsInitialLayout = true;
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }
}
