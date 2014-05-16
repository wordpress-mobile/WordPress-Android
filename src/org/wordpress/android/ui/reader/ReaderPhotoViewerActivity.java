package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import org.wordpress.android.R;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageListener;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Full-screen photo viewer
 */
public class ReaderPhotoViewerActivity extends Activity {
    static final String ARG_IMAGE_URL = "image_url";
    private String mImageUrl;
    private WPNetworkImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_photo_viewer);
        mImageView = (WPNetworkImageView) findViewById(R.id.image_photo);

        if (savedInstanceState != null && savedInstanceState.containsKey(ARG_IMAGE_URL)) {
            mImageUrl = savedInstanceState.getString(ARG_IMAGE_URL);
        } else if (getIntent().hasExtra(ARG_IMAGE_URL)) {
            mImageUrl = getIntent().getStringExtra(ARG_IMAGE_URL);
            // use photon to enforce max size unless this is https
            if (!UrlUtils.isHttps(mImageUrl)) {
                Point pt = DisplayUtils.getDisplayPixelSize(this);
                int maxWidth = Math.max(pt.x, pt.y);
                mImageUrl = PhotonUtils.getPhotonImageUrl(mImageUrl, maxWidth, 0);
            }
        }

        loadImage();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mImageUrl != null) {
            outState.putString(ARG_IMAGE_URL, mImageUrl);
        }
    }

    private void loadImage() {
        if (TextUtils.isEmpty(mImageUrl)) {
            handleImageLoadFailure();
            return;
        }

        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
        progress.setVisibility(View.VISIBLE);

        mImageView.setImageUrl(mImageUrl, ImageType.PHOTO_FULL, new ImageListener() {
            @Override
            public void onImageLoaded(boolean succeeded) {
                progress.setVisibility(View.GONE);
                if (succeeded) {
                    createAttacher();
                } else {
                    handleImageLoadFailure();
                }
            }
        });
    }

    private void createAttacher() {
        PhotoViewAttacher attacher = new PhotoViewAttacher(mImageView);

        // tapping outside the photo closes the activity
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float v, float v2) {
                finish();
            }
        });
        attacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float v, float v2) {
                // do nothing - photo tap listener must be assigned or else tapping the photo
                // will fire the onViewTapListener() above
            }
        });
    }

    private void handleImageLoadFailure() {
        ToastUtils.showToast(this, R.string.reader_toast_err_view_image, ToastUtils.Duration.LONG);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
