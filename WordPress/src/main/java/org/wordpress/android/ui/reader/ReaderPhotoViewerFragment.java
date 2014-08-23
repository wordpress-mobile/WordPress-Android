package org.wordpress.android.ui.reader;

import android.app.Activity;
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
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageListener;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import uk.co.senab.photoview.PhotoViewAttacher;

public class ReaderPhotoViewerFragment extends Fragment {
    private String mImageUrl;
    private boolean mIsPrivate;
    private int mPosition;
    private ReaderPhotoListener mPhotoListener;

    private WPNetworkImageView mImageView;
    private ProgressBar mProgress;
    private TextView mTxtError;

    static interface ReaderPhotoListener {
        void onTapPhoto(int position);
        void onTapOutsidePhoto(int position);
        void onPhotoLoaded(int position);
    }

    /**
     * @param imageUrl the url of the image to load
     * @param position the position of the image in ReaderPhotoViewerActivity
     * @param isPrivate whether image is from a private blog
     */
    static ReaderPhotoViewerFragment newInstance(String imageUrl, int position, boolean isPrivate) {
        AppLog.d(AppLog.T.READER, "reader photo fragment > newInstance");

        Bundle args = new Bundle();
        args.putString(ReaderConstants.ARG_IMAGE_URL, imageUrl);
        args.putInt(ReaderConstants.ARG_POSITION, position);
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
            mPosition = args.getInt(ReaderConstants.ARG_POSITION);
            mIsPrivate = args.getBoolean(ReaderConstants.ARG_IS_PRIVATE);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ReaderPhotoListener) {
            mPhotoListener = (ReaderPhotoListener) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_fragment_photo_viewer, container, false);
        mImageView = (WPNetworkImageView) view.findViewById(R.id.image_photo);
        mProgress = (ProgressBar) view.findViewById(R.id.progress);
        mTxtError = (TextView) view.findViewById(R.id.text_error);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mImageUrl = savedInstanceState.getString(ReaderConstants.ARG_IMAGE_URL);
            mPosition = savedInstanceState.getInt(ReaderConstants.ARG_POSITION);
            mIsPrivate = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_PRIVATE);
        }
        showImage();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ReaderConstants.ARG_IMAGE_URL, mImageUrl);
        outState.putInt(ReaderConstants.ARG_POSITION, mPosition);
        outState.putBoolean(ReaderConstants.ARG_IS_PRIVATE, mIsPrivate);
        super.onSaveInstanceState(outState);
    }

    private void showImage() {
        if (!isAdded() || TextUtils.isEmpty(mImageUrl)) {
            return;
        }

        // use min of width/height so image will be cached the same size regardless
        // of device orientation
        Point pt = DisplayUtils.getDisplayPixelSize(getActivity());
        int maxWidth = Math.min(pt.x, pt.y);

        final String imageUrlToLoad;
        if (mIsPrivate) {
            imageUrlToLoad = ReaderUtils.getPrivateImageForDisplay(mImageUrl, maxWidth, 0);
        } else {
            imageUrlToLoad = PhotonUtils.getPhotonImageUrl(mImageUrl, maxWidth, 0);
        }

        mProgress.setVisibility(View.VISIBLE);

        mImageView.setImageUrl(imageUrlToLoad, ImageType.PHOTO_FULL, new ImageListener() {
            @Override
            public void onImageLoaded(boolean succeeded) {
                if (isAdded()) {
                    mProgress.setVisibility(View.GONE);
                    mTxtError.setVisibility(succeeded ? View.GONE : View.VISIBLE);
                    if (succeeded) {
                        createAttacher(mImageView);
                        if (mPhotoListener != null) {
                            mPhotoListener.onPhotoLoaded(mPosition);
                        }
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
}
