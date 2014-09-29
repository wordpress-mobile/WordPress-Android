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
import org.wordpress.android.ui.reader.views.ReaderPhotoView;
import org.wordpress.android.ui.reader.views.ReaderPhotoView.PhotoViewListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;

public class ReaderPhotoViewerFragment extends Fragment {
    private String mImageUrl;
    private boolean mIsPrivate;

    private ReaderPhotoView mPhotoView;
    private PhotoViewListener mPhotoViewListener;

    /**
     * @param imageUrl the url of the image to load
     * @param isPrivate whether image is from a private blog
     */
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
        mPhotoView = (ReaderPhotoView) view.findViewById(R.id.photo_view);

        if (savedInstanceState != null) {
            mImageUrl = savedInstanceState.getString(ReaderConstants.ARG_IMAGE_URL);
            mIsPrivate = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_PRIVATE);
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof PhotoViewListener) {
            mPhotoViewListener = (PhotoViewListener) activity;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        showImage();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ReaderConstants.ARG_IMAGE_URL, mImageUrl);
        outState.putBoolean(ReaderConstants.ARG_IS_PRIVATE, mIsPrivate);
        super.onSaveInstanceState(outState);
    }

    private void showImage() {
        if (isAdded() && !TextUtils.isEmpty(mImageUrl)) {
            // use max of width/height so image is cached the same regardless of orientation
            Point pt = DisplayUtils.getDisplayPixelSize(getActivity());
            int hiResWidth = Math.max(pt.x, pt.y);
            mPhotoView.setImageUrl(mImageUrl, hiResWidth, mIsPrivate, mPhotoViewListener);
        }
    }
}
