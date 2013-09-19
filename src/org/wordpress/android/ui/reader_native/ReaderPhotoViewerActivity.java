package org.wordpress.android.ui.reader_native;

import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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
    protected static final String ARG_IMAGE_URL = "image_url";

    private ProgressBar mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reader_photo_viewer);

        final WPNetworkImageView imageView = (WPNetworkImageView) findViewById(R.id.image_photo);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mProgress.setVisibility(View.VISIBLE);

        String imageUrl = getIntent().getStringExtra(ARG_IMAGE_URL);

        // use photon on non-https images, with the width set to the max size of the display - this
        // is important since many wp images contain ?w= or ?h= params which result in an image
        // reduced to a size that makes them appear very grainy when zoomed in here - also important
        // since using a photon url with a width prevents downloading huge images here
        if (imageUrl!=null && !UrlUtils.isHttps(imageUrl)) {
            Point pt = DisplayUtils.getDisplayPixelSize(this);
            int maxWidth = (pt.x > pt.y ? pt.x : pt.y);
            imageUrl = PhotonUtils.getPhotonImageUrl(imageUrl, maxWidth, 0);
        }

        imageView.setImageUrl(imageUrl, ImageType.PHOTO_FULL, new ImageListener() {
            @Override
            public void onImageLoaded(boolean succeeded) {
                mProgress.setVisibility(View.GONE);
                if (succeeded) {
                    new PhotoViewAttacher(imageView);
                } else {
                    ToastUtils.showToast(ReaderPhotoViewerActivity.this, R.string.reader_toast_err_view_image, ToastUtils.Duration.LONG);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
