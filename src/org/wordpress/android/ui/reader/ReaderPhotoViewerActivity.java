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
    private String mImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_photo_viewer);

        if (savedInstanceState != null && savedInstanceState.containsKey(ReaderConstants.ARG_IMAGE_URL)) {
            mImageUrl = savedInstanceState.getString(ReaderConstants.ARG_IMAGE_URL);
        } else if (getIntent().hasExtra(ReaderConstants.ARG_IMAGE_URL)) {
            mImageUrl = getIntent().getStringExtra(ReaderConstants.ARG_IMAGE_URL);
            // use photon to enforce max size unless this is https
            if (!UrlUtils.isHttps(mImageUrl)) {
                Point pt = DisplayUtils.getDisplayPixelSize(this);
                int maxWidth = Math.max(pt.x, pt.y);
                mImageUrl = PhotonUtils.getPhotonImageUrl(mImageUrl, maxWidth, 0);
            }
        }

        final WPNetworkImageView imageView = (WPNetworkImageView) findViewById(R.id.image_photo);
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress);

        if (!TextUtils.isEmpty(mImageUrl)) {
            progress.setVisibility(View.VISIBLE);
            imageView.setImageUrl(mImageUrl, ImageType.PHOTO_FULL, new ImageListener() {
                @Override
                public void onImageLoaded(boolean succeeded) {
                    progress.setVisibility(View.GONE);
                    if (succeeded) {
                        new PhotoViewAttacher(imageView);
                    } else {
                        ToastUtils.showToast(ReaderPhotoViewerActivity.this, R.string.reader_toast_err_view_image, ToastUtils.Duration.LONG);
                    }
                }
            });
        } else {
            imageView.setImageResource(R.drawable.ic_error);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mImageUrl != null)
            outState.putString(ReaderConstants.ARG_IMAGE_URL, mImageUrl);
    }
}
