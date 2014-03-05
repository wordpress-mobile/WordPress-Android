package org.wordpress.android.ui.reader;

import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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
import org.wordpress.android.widgets.photoview.PhotoViewAttacher;

/**
 * Created by nbradbury on 9/12/13.
 * Full-screen photo viewer, relies on widgets.photoview for pinch/zoom & double tap enlargement
 */
public class ReaderPhotoViewerActivity extends FragmentActivity {
    static final String ARG_IMAGE_URL = "image_url";
    private String mImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_photo_viewer);

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
            outState.putString(ARG_IMAGE_URL, mImageUrl);
    }

}
