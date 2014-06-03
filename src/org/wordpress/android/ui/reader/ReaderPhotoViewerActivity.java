package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
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
    static final String ARG_IS_PRIVATE = "is_private";

    private String mImageUrl;
    private boolean mIsPrivate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_photo_viewer);

        if (savedInstanceState != null) {
            mImageUrl = savedInstanceState.getString(ARG_IMAGE_URL);
            mIsPrivate = savedInstanceState.getBoolean(ARG_IS_PRIVATE);
        } else if (getIntent() != null) {
            mImageUrl = getIntent().getStringExtra(ARG_IMAGE_URL);
            mIsPrivate = getIntent().getBooleanExtra(ARG_IS_PRIVATE, false);
        }

        loadImage(mImageUrl);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_IMAGE_URL, mImageUrl);
        outState.putBoolean(ARG_IS_PRIVATE, mIsPrivate);
    }

    private void loadImage(String imageUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            handleImageLoadFailure();
            return;
        }

        // use https: if this is an image from a private post, otherwise use photon to enforce max size
        if (mIsPrivate) {
            imageUrl = UrlUtils.makeHttps(imageUrl);
        } else {
            Point pt = DisplayUtils.getDisplayPixelSize(this);
            int maxWidth = Math.max(pt.x, pt.y);
            imageUrl = PhotonUtils.getPhotonImageUrl(imageUrl, maxWidth, 0);
        }

        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
        progress.setVisibility(View.VISIBLE);

        final WPNetworkImageView imageView = (WPNetworkImageView) findViewById(R.id.image_photo);
        imageView.setImageUrl(imageUrl, ImageType.PHOTO_FULL, new ImageListener() {
            @Override
            public void onImageLoaded(boolean succeeded) {
                progress.setVisibility(View.GONE);
                if (succeeded) {
                    createAttacher(imageView);
                } else {
                    handleImageLoadFailure();
                }
            }
        });
    }

    private void createAttacher(ImageView imageView) {
        PhotoViewAttacher attacher = new PhotoViewAttacher(imageView);

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
