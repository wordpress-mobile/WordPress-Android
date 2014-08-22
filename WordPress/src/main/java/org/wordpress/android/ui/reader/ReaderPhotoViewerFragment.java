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
import android.widget.TextView;

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
    private TextView mTxtTitle;
    private String mTitle;

    static ReaderPhotoViewerFragment newInstance(String imageUrl, boolean isPrivate, String title) {
        AppLog.d(AppLog.T.READER, "reader photo fragment > newInstance");

        Bundle args = new Bundle();
        args.putString(ReaderConstants.ARG_IMAGE_URL, imageUrl);
        args.putBoolean(ReaderConstants.ARG_IS_PRIVATE, isPrivate);
        args.putString(ReaderConstants.ARG_TITLE, title);

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
            mTitle = args.getString(ReaderConstants.ARG_TITLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_fragment_photo_viewer, container, false);

        mTxtTitle = (TextView) view.findViewById(R.id.text_title);
        mTxtTitle.setText(mTitle);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        showImage();
        showTitle();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ReaderConstants.ARG_TITLE, mTitle);
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

    private void showTitle() {
        if (!isAdded() || TextUtils.isEmpty(mTitle)) {
            return;
        }
        if (mTxtTitle.getVisibility() != View.VISIBLE) {
            ReaderAnim.fadeInFadeOut(mTxtTitle, ReaderAnim.Duration.EXTRA_LONG);
        }
    }

    private void createAttacher(ImageView imageView) {
        if (!isAdded()) {
            return;
        }

        PhotoViewAttacher attacher = new PhotoViewAttacher(imageView);

        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float v, float v2) {
                showTitle();
            }
        });
        attacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float v, float v2) {
                showTitle();
            }
        });
    }

    private void handleImageLoadFailure() {
        if (isAdded()) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_view_image, ToastUtils.Duration.LONG);
        }
    }
}
