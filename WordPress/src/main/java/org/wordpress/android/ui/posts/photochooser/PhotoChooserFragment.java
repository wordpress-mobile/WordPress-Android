package org.wordpress.android.ui.posts.photochooser;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.DisplayUtils;

public class PhotoChooserFragment extends Fragment {

    private static final int NUM_COLUMNS = 4;

    public enum PhotoChooserIcon {
        ANDROID_CAMERA, ANDROID_PICKER
    }
    public interface OnPhotoChosenListener {
        void onPhotoChosen(Uri imageUri);
        void onIconClicked(PhotoChooserIcon icon);
    }

    private RecyclerView mRecycler;

    public static PhotoChooserFragment newInstance() {
        Bundle args = new Bundle();
        PhotoChooserFragment fragment = new PhotoChooserFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.photo_chooser_fragment, container, false);

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), NUM_COLUMNS);

        mRecycler = (RecyclerView) view.findViewById(R.id.recycler);
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(layoutManager);

        loadDevicePhotos();

        return view;
    }


    private final OnPhotoChosenListener mListener = new OnPhotoChosenListener() {
        @Override
        public void onPhotoChosen(Uri imageUri) {
            if (getActivity() instanceof EditPostActivity) {
                EditPostActivity activity = (EditPostActivity) getActivity();
                activity.addMedia(imageUri);
                activity.hidePhotoChooser();
            }
        }
        @Override
        public void onIconClicked(PhotoChooserIcon icon) {
            if (getActivity() instanceof EditPostActivity) {
                EditPostActivity activity = (EditPostActivity) getActivity();
                switch (icon) {
                    case ANDROID_CAMERA:
                        activity.launchCamera();
                        break;
                    case ANDROID_PICKER:
                        activity.launchPictureLibrary();
                        break;
                }

            }
        }
    };

    public static int getPhotoChooserImageWidth(Context context) {
        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        return displayWidth / NUM_COLUMNS;
    }

    public static int getPhotoChooserImageHeight(Context context) {
        int imageWidth = getPhotoChooserImageWidth(context);
        return (int) (imageWidth * 0.75f);
    }

    private void loadDevicePhotos() {
        int imageWidth = getPhotoChooserImageWidth(getActivity());
        int imageHeight = getPhotoChooserImageHeight(getActivity());
        PhotoChooserAdapter adapter = new PhotoChooserAdapter(
                getActivity(), imageWidth, imageHeight, mListener);
        mRecycler.setAdapter(adapter);
        adapter.loadDevicePhotos();
    }
}
