package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;

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
        mPhotoView = new ReaderPhotoView(container.getContext());
        mPhotoView.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                           ViewGroup.LayoutParams.MATCH_PARENT));
        return mPhotoView;
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

        mPhotoView.setImageUrl(mImageUrl, hiResWidth, mIsPrivate, mPosition, mPhotoListener);
    }


}
