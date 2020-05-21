package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageManager.RequestListener;
import org.wordpress.android.util.image.ImageType;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * used by ReaderPhotoViewerActivity to show full-width images - based on Volley's ImageView
 * but adds pinch/zoom and the ability to first load a lo-res version of the image
 */
public class ReaderPhotoView extends RelativeLayout {
    public interface PhotoViewListener {
        void onTapPhotoView();
    }

    private PhotoViewListener mPhotoViewListener;
    private String mLoResImageUrl;
    private String mHiResImageUrl;

    private final ImageView mImageView;
    private final ProgressBar mProgress;
    private final TextView mTxtError;
    private boolean mIsInitialLayout = true;
    private final ImageManager mImageManager;

    public ReaderPhotoView(Context context) {
        this(context, null);
    }

    public ReaderPhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.reader_photo_view, this);

        // ImageView which contains the downloaded image
        mImageView = findViewById(R.id.image_photo);

        // error text that appears when download fails
        mTxtError = findViewById(R.id.text_error);

        // progress bar which appears while downloading
        mProgress = findViewById(R.id.progress_loading);

        mImageManager = ImageManager.getInstance();
    }

    /**
     * @param imageUrl   the url of the image to load
     * @param hiResWidth maximum width of the full-size image
     * @param isPrivate  whether this is an image from a private blog
     * @param listener   listener for taps on this view
     */
    public void setImageUrl(String imageUrl,
                            int hiResWidth,
                            boolean isPrivate,
                            boolean isPrivateAtSite,
                            PhotoViewListener listener) {
        int loResWidth = (int) (hiResWidth * 0.10f);
        mLoResImageUrl = ReaderUtils
                .getResizedImageUrl(imageUrl, loResWidth, 0, isPrivate, isPrivateAtSite, PhotonUtils.Quality.LOW);
        mHiResImageUrl = ReaderUtils
                .getResizedImageUrl(imageUrl, hiResWidth, 0, isPrivate, isPrivateAtSite, PhotonUtils.Quality.MEDIUM);

        mPhotoViewListener = listener;
        loadImage();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

    private void loadImage() {
        if (!hasLayout()) {
            return;
        }

        showProgress();

        mImageManager
                .loadWithResultListener(mImageView, ImageType.IMAGE, mHiResImageUrl, ScaleType.CENTER, mLoResImageUrl,
                new RequestListener<Drawable>() {
                    @Override
                    public void onLoadFailed(@Nullable Exception e, @Nullable Object model) {
                        if (e != null) {
                            AppLog.e(AppLog.T.READER, e);
                        }
                        boolean lowResNotLoadedYet = isLoading();
                        if (lowResNotLoadedYet) {
                            hideProgress();
                            showError();
                        }
                    }

                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Object model) {
                        handleResponse();
                    }
                });
    }

    private void handleResponse() {
        hideProgress();
        hideError();
        // attach the pinch/zoom handler
        setAttacher();
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

    private void hideError() {
        hideProgress();
        if (mTxtError != null) {
            mTxtError.setVisibility(View.GONE);
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

    private boolean isLoading() {
        return mProgress != null && mProgress.getVisibility() == VISIBLE;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isInEditMode()) {
            if (mIsInitialLayout) {
                mIsInitialLayout = false;
                AppLog.d(AppLog.T.READER, "reader photo > initial layout");
                if (mLoResImageUrl != null && mHiResImageUrl != null) {
                    loadImage();
                }
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mIsInitialLayout = true;
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }
}
