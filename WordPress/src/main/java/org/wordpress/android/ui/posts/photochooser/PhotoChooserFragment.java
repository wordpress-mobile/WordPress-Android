package org.wordpress.android.ui.posts.photochooser;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.DisplayUtils;

import java.util.ArrayList;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoChooserFragment extends Fragment {

    private static final int NUM_COLUMNS = 3;
    private static final String KEY_MULTI_SELECT_ENABLED = "multi_select_enabled";
    private static final String KEY_SELECTED_IMAGE_LIST = "selected_image_list";

    public enum PhotoChooserIcon {
        ANDROID_CAMERA,
        ANDROID_PICKER,
        WP_MEDIA
    }

    public interface OnPhotoChooserListener {
        void onPhotoTapped(Uri imageUri);
        void onPhotoDoubleTapped(Uri imageUri);
        void onPhotoLongPressed(Uri imageUri);
        void onIconClicked(PhotoChooserIcon icon);
    }

    private RecyclerView mRecycler;
    private View mPreviewFrame;
    private ActionMode mActionMode;

    public static PhotoChooserFragment newInstance() {
        Bundle args = new Bundle();
        PhotoChooserFragment fragment = new PhotoChooserFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(KEY_MULTI_SELECT_ENABLED)) {
                getAdapter().setMultiSelectEnabled(true);
                restoreSelectedImages(savedInstanceState);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isMultiSelectEnabled()) {
            outState.putBoolean(KEY_MULTI_SELECT_ENABLED, true);
            saveSelectedImages(outState);
        }
    }

    private void restoreSelectedImages(Bundle savedInstanceState) {
        if (!savedInstanceState.containsKey(KEY_SELECTED_IMAGE_LIST)) return;

        ArrayList<String> strings = savedInstanceState.getStringArrayList(KEY_SELECTED_IMAGE_LIST);
        if (strings == null || strings.size() == 0) return;

        ArrayList<Uri> uriList = new ArrayList<>();
        for (String stringUri: strings) {
            uriList.add(Uri.parse(stringUri));
        }
        getAdapter().setSelectedImageURIs(uriList);
    }

    private void saveSelectedImages(Bundle outState) {
        ArrayList<Uri> uriList = getAdapter().getSelectedImageURIs();
        if (uriList.size() == 0) return;

        ArrayList<String> stringUris = new ArrayList<>();
        for (Uri uri: uriList) {
            stringUris.add(uri.toString());
        }
        outState.putStringArrayList(KEY_SELECTED_IMAGE_LIST, stringUris);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.photo_chooser_fragment, container, false);

        mPreviewFrame = view.findViewById(R.id.frame_preview);

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), NUM_COLUMNS);

        mRecycler = (RecyclerView) view.findViewById(R.id.recycler);
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(layoutManager);

        loadDevicePhotos();

        return view;
    }

    /*
     *   - single tap adds the photo to post or selects it if multi-select is enabled
     *   - double tap previews the photo
     *   - long press enables multi-select
     */
    private final OnPhotoChooserListener mListener = new OnPhotoChooserListener() {
        @Override
        public void onPhotoTapped(Uri imageUri) {
            if (isMultiSelectEnabled()) {
                getAdapter().togglePhotoSelection(imageUri);
                updateActionModeTitle();
            } else if (getActivity() instanceof EditPostActivity) {
                EditPostActivity activity = (EditPostActivity) getActivity();
                activity.addMedia(imageUri);
                activity.hidePhotoChooser();
            }
        }

        @Override
        public void onPhotoLongPressed(Uri imageUri) {
            if (!isMultiSelectEnabled()) {
                enableMultiSelect(imageUri);
            }
        }

        @Override
        public void onPhotoDoubleTapped(Uri imageUri) {
            showPreview(imageUri);
        }

        @Override
        public void onIconClicked(PhotoChooserIcon icon) {
            if (getActivity() instanceof EditPostActivity) {
                EditPostActivity activity = (EditPostActivity) getActivity();
                activity.hidePhotoChooser();
                switch (icon) {
                    case ANDROID_CAMERA:
                        activity.launchCamera();
                        break;
                    case ANDROID_PICKER:
                        activity.launchPictureLibrary();
                        break;
                    case WP_MEDIA:
                        activity.startMediaGalleryAddActivity();
                        break;
                }
            }
        }
    };

    private void showPreview(Uri imageUri) {
        mRecycler.setEnabled(false);

        ImageView imgPreview = (ImageView) mPreviewFrame.findViewById(R.id.image_preview);
        imgPreview.setImageURI(imageUri);

        AniUtils.scaleIn(mPreviewFrame, AniUtils.Duration.SHORT);

        PhotoViewAttacher attacher = new PhotoViewAttacher(imgPreview);
        attacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float v, float v2) {
                hidePreview();
            }
        });
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                hidePreview();
            }
        });
    }

    public void hidePreview() {
        mRecycler.setEnabled(true);
        if (isPreviewShowing()) {
            AniUtils.scaleOut(mPreviewFrame, AniUtils.Duration.SHORT);
        }
    }

    public boolean isPreviewShowing() {
        return mPreviewFrame.getVisibility() == View.VISIBLE;
    }

    private boolean isMultiSelectEnabled() {
        return mAdapter != null && mAdapter.isMultiSelectEnabled();
    }

    private void enableMultiSelect(Uri imageUri) {
        getAdapter().setMultiSelectEnabled(true);
        getAdapter().togglePhotoSelection(imageUri);
        ((AppCompatActivity) getActivity()).startSupportActionMode(new ActionModeCallback());
        updateActionModeTitle();
    }

    private static int getPhotoChooserImageWidth(Context context) {
        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        return displayWidth / NUM_COLUMNS;
    }

    private static int getPhotoChooserImageHeight(Context context) {
        int imageWidth = getPhotoChooserImageWidth(context);
        return (int) (imageWidth * 0.75f);
    }

    private PhotoChooserAdapter mAdapter;
    private PhotoChooserAdapter getAdapter() {
        if (mAdapter == null) {
            int imageWidth = getPhotoChooserImageWidth(getActivity());
            int imageHeight = getPhotoChooserImageHeight(getActivity());
            mAdapter = new PhotoChooserAdapter(
                    getActivity(), imageWidth, imageHeight, mListener);
        }
        return mAdapter;
    }

    private void loadDevicePhotos() {
        mRecycler.setAdapter(getAdapter());
        getAdapter().loadDevicePhotos();
    }

    /*
     * inserts the passed list of images into the post and closes the photo chooser
     */
    private void insertPhotos(ArrayList<Uri> uriList) {
        if (!(getActivity() instanceof EditPostActivity)) return;

        EditPostActivity activity = (EditPostActivity) getActivity();
        for (Uri uri: uriList) {
            activity.addMedia(uri);
        }

        activity.hidePhotoChooser();
        finishActionMode();
    }

    private void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    private void updateActionModeTitle() {
        if (mActionMode == null) return;

        int numSelected = getAdapter().getNumSelected();
        String title = String.format(getString(R.string.cab_selected), numSelected);
        mActionMode.setTitle(title);
    }

    private final class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.photo_chooser_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.mnu_confirm_selection) {
                ArrayList<Uri> uriList = getAdapter().getSelectedImageURIs();
                insertPhotos(uriList);
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            getAdapter().setMultiSelectEnabled(false);
            mActionMode = null;
        }
    }
}
