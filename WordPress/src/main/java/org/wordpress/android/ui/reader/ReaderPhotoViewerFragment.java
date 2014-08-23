package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.PhotonUtils;

public class ReaderPhotoViewerFragment extends Fragment {
    private String mImageUrl;
    private boolean mIsPrivate;
    private int mPosition;
    private ReaderPhotoView.ReaderPhotoListener mPhotoListener;

    private ReaderPhotoView mPhotoView;

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
        if (activity instanceof ReaderPhotoView.ReaderPhotoListener) {
            mPhotoListener = (ReaderPhotoView.ReaderPhotoListener) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_fragment_photo_viewer, container, false);
        mPhotoView = (ReaderPhotoView) view.findViewById(R.id.photo_view);
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

        // use max of width/height so image will be cached the same size regardless of orientation
        Point pt = DisplayUtils.getDisplayPixelSize(getActivity());
        int hiResWidth = Math.max(pt.x, pt.y);
        int loResWidth = (int) (hiResWidth * 0.10f);

        final String loResImageUrl;
        final String hiResImageUrl;
        if (mIsPrivate) {
            loResImageUrl = ReaderUtils.getPrivateImageForDisplay(mImageUrl, loResWidth, 0);
            hiResImageUrl = ReaderUtils.getPrivateImageForDisplay(mImageUrl, hiResWidth, 0);
        } else {
            loResImageUrl = PhotonUtils.getPhotonImageUrl(mImageUrl, loResWidth, 0);
            hiResImageUrl = PhotonUtils.getPhotonImageUrl(mImageUrl, hiResWidth, 0);
        }

        mPhotoView.setImageUrl(loResImageUrl, hiResImageUrl, mPosition, mPhotoListener);
    }


}
