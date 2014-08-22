package org.wordpress.android.ui.reader;

import android.app.Fragment;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import uk.co.senab.photoview.PhotoViewAttacher;

public class ReaderPhotoViewerFragment extends Fragment {
    private String mImageUrl;
    private boolean mIsPrivate;

    static ReaderPhotoViewerFragment newInstance(String imageUrl, boolean isPrivate) {
        AppLog.d(AppLog.T.READER, "reader photo fragment > newInstance");

        Bundle args = new Bundle();
        args.putString(ReaderConstants.ARG_IMAGE_URL, imageUrl);
        args.putBoolean(ReaderConstants.ARG_IS_PRIVATE, isPrivate);

        ReaderPhotoViewerFragment fragment = new ReaderPhotoViewerFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args != null) {
            mImageUrl = args.getString(ReaderConstants.ARG_IMAGE_URL);
            mIsPrivate = args.getBoolean(ReaderConstants.ARG_IS_PRIVATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_fragment_photo_viewer, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        showImage();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ReaderConstants.ARG_IMAGE_URL, mImageUrl);
        outState.putBoolean(ReaderConstants.ARG_IS_PRIVATE, mIsPrivate);
        super.onSaveInstanceState(outState);
    }

    private void showImage() {
        if (!isAdded()) {
            return;
        }

        if (TextUtils.isEmpty(mImageUrl)) {
            handleImageLoadFailure();
            return;
        }

        final String imageUrl;
        Point pt = DisplayUtils.getDisplayPixelSize(getActivity());
        int maxWidth = Math.max(pt.x, pt.y);
        if (mIsPrivate) {
            imageUrl = ReaderUtils.getPrivateImageForDisplay(mImageUrl, maxWidth, 0);
        } else {
            imageUrl = PhotonUtils.getPhotonImageUrl(mImageUrl, maxWidth, 0);
        }

        final ProgressBar progress = (ProgressBar) getView().findViewById(R.id.progress);
        progress.setVisibility(View.VISIBLE);

        final WPNetworkImageView imageView = (WPNetworkImageView) getView().findViewById(R.id.image_photo);
        imageView.setImageUrl(imageUrl, WPNetworkImageView.ImageType.PHOTO_FULL, new WPNetworkImageView.ImageListener() {
            @Override
            public void onImageLoaded(boolean succeeded) {
                if (isAdded()) {
                    progress.setVisibility(View.GONE);
                    if (succeeded) {
                        createAttacher(imageView);
                    } else {
                        handleImageLoadFailure();
                    }
                }
            }
        });
    }

    private void createAttacher(ImageView imageView) {
        if (!isAdded()) {
            return;
        }

        PhotoViewAttacher attacher = new PhotoViewAttacher(imageView);

        // tapping outside the photo closes the activity
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float v, float v2) {
                // TODO
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
        if (isAdded()) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_view_image, ToastUtils.Duration.LONG);
        }
    }
}
