package org.wordpress.android.ui.reader;

import android.content.Context;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * used by ReaderPhotoViewerActivity to show full-width images - based on Volley's ImageView
 * but adds pinch/zoom and the ability to first load a lo-res version of the image
 */
public class ReaderPhotoView extends RelativeLayout {

    static interface ReaderPhotoListener {
        void onTapPhoto(int position);
        void onTapOutsidePhoto(int position);
        void onPhotoLoaded(int position);
    }

    private String mLoResImageUrl;
    private String mHiResImageUrl;
    private int mPosition;
    private ReaderPhotoListener mPhotoListener;

    private ImageLoader.ImageContainer mImageContainer;

    private final ImageView mImageView;
    private final ProgressBar mProgress;
    private final TextView mTxtError;

    public ReaderPhotoView(Context context) {
        this(context, null);
    }

    public ReaderPhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.reader_photo_view, this, true);
        mImageView = (ImageView) view.findViewById(R.id.image_photo);
        mProgress = (ProgressBar) view.findViewById(R.id.progress);
        mTxtError = (TextView) view.findViewById(R.id.text_error);
    }

    /**
     * @param imageUrl the url of the image to load
     * @param hiResWidth maximum width of the full-size image
     * @param isPrivate whether this is an image from a private blog
     * @param position the position of the image in ReaderPhotoViewerActivity
     * @param photoListener optional listener
     */
    public void setImageUrl(String imageUrl,
                            int hiResWidth,
                            boolean isPrivate,
                            int position,
                            ReaderPhotoListener photoListener) {
        int loResWidth = (int) (hiResWidth * 0.15f);
        if (isPrivate) {
            mLoResImageUrl = ReaderUtils.getPrivateImageForDisplay(imageUrl, loResWidth, 0);
            mHiResImageUrl = ReaderUtils.getPrivateImageForDisplay(imageUrl, hiResWidth, 0);
        } else {
            mLoResImageUrl = PhotonUtils.getPhotonImageUrl(imageUrl, loResWidth, 0);
            mHiResImageUrl = PhotonUtils.getPhotonImageUrl(imageUrl, hiResWidth, 0);
        }

        mPosition = position;
        mPhotoListener = photoListener;
        loadImageIfNecessary(false);
    }

    private void loadImageIfNecessary(boolean isInLayoutPass) {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 && height == 0) {
            return;
        }

        if (TextUtils.isEmpty(mLoResImageUrl)) {
            if (mImageContainer != null) {
                mImageContainer.cancelRequest();
                mImageContainer = null;
            }
            return;
        }

        if (mImageContainer != null && mImageContainer.getRequestUrl() != null) {
            if (mImageContainer.getRequestUrl().equals(mLoResImageUrl)) {
                return;
            } else if (mImageContainer.getRequestUrl().equals(mHiResImageUrl)) {
                return;
            } else {
                mImageContainer.cancelRequest();
            }
        }

        getLoResImage(isInLayoutPass);
    }

    private void getLoResImage(final boolean isInLayoutPass) {
        Point pt = DisplayUtils.getDisplayPixelSize(this.getContext());
        int maxSize = Math.min(pt.x, pt.y);

        showProgress();

        mImageContainer = WordPress.imageLoader.get(mLoResImageUrl,
                new ImageLoader.ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.READER, error);
                        hideProgress();
                        showError();
                    }

                    @Override
                    public void onResponse(final ImageLoader.ImageContainer response, boolean isImmediate) {
                        if (isImmediate && isInLayoutPass) {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    handleResponse(response, true);
                                    hideProgress();
                                }
                            });
                        } else {
                            handleResponse(response, true);
                            hideProgress();
                        }
                    }
                }, maxSize, maxSize);
    }

    private void getHiResImage() {
        Point pt = DisplayUtils.getDisplayPixelSize(this.getContext());
        int maxSize = Math.max(pt.x, pt.y);

        mImageContainer = WordPress.imageLoader.get(mHiResImageUrl,
                new ImageLoader.ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.READER, error);
                    }

                    @Override
                    public void onResponse(final ImageLoader.ImageContainer response, boolean isImmediate) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                handleResponse(response, false);
                            }
                        });
                    }
                }, maxSize, maxSize);
    }

    private void handleResponse(ImageLoader.ImageContainer response, boolean isLoResResponse) {
        if (response.getBitmap() != null) {
            mImageView.setImageBitmap(response.getBitmap());
            createAttacher(mImageView);

            if (isLoResResponse) {
                ReaderAnim.fadeIn(mImageView, ReaderAnim.Duration.MEDIUM);
                if (mPhotoListener != null) {
                    mPhotoListener.onPhotoLoaded(mPosition);
                }
                getHiResImage();
            }

            AppLog.d(AppLog.T.READER, "ReaderPhotoView > loaded "
                    + (isLoResResponse ? "lo-res image" : "hi-res image"));
        }
    }

    /*
     * attach the pinch/zoom handler
     */
    private void createAttacher(ImageView imageView) {
        PhotoViewAttacher attacher = new PhotoViewAttacher(imageView);
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float v, float v2) {
                if (mPhotoListener != null) {
                    mPhotoListener.onTapOutsidePhoto(mPosition);
                }
            }
        });
        attacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float v, float v2) {
                if (mPhotoListener != null) {
                    mPhotoListener.onTapPhoto(mPosition);
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
            loadImageIfNecessary(true);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mImageContainer != null) {
            mImageContainer.cancelRequest();
            mImageView.setImageDrawable(null);
            mImageContainer = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }
}
