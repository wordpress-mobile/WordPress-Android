package org.wordpress.android.ui.photopicker;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import org.wordpress.android.R;
import org.wordpress.android.ui.photopicker.PhotoPickerAdapter.PhotoPickerAdapterListener;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class PhotoPickerFragment extends Fragment {

    static final int NUM_COLUMNS = 3;

    public enum PhotoPickerIcon {
        ANDROID_CAMERA,
        ANDROID_PICKER,
        WP_MEDIA
    }

    public enum PhotoPickerOption {
        ALLOW_MULTI_SELECT,     // allow selecting more than one item
        PHOTOS_ONLY,            // show only photos (no videos)
        DEVICE_ONLY             // no WP media
    }

    /*
     * parent activity must implement this listener
     */
    public interface PhotoPickerListener {
        void onPhotoPickerMediaChosen(@NonNull List<Uri> uriList);
        void onPhotoPickerIconClicked(@NonNull PhotoPickerIcon icon);
    }

    private RecyclerView mRecycler;
    private PhotoPickerAdapter mAdapter;
    private View mBottomBar;
    private ActionMode mActionMode;
    private GridLayoutManager mGridManager;
    private Parcelable mRestoreState;
    private PhotoPickerListener mListener;

    private boolean mAllowMultiSelect;
    private boolean mPhotosOnly;
    private boolean mDeviceOnly;

    private static final String ARG_ALLOW_MULTI_SELECT = "allow_multi_select";
    private static final String ARG_PHOTOS_ONLY = "photos_only";
    private static final String ARG_DEVICE_ONLY = "device_only";

    public static PhotoPickerFragment newInstance(@NonNull PhotoPickerListener listener,
                                                  EnumSet<PhotoPickerOption> options) {
        Bundle args = new Bundle();
        PhotoPickerFragment fragment = new PhotoPickerFragment();
        fragment.setPhotoPickerListener(listener);
        if (options != null) {
            args.putBoolean(ARG_ALLOW_MULTI_SELECT, options.contains(PhotoPickerOption.ALLOW_MULTI_SELECT));
            args.putBoolean(ARG_PHOTOS_ONLY, options.contains(PhotoPickerOption.PHOTOS_ONLY));
            args.putBoolean(ARG_DEVICE_ONLY, options.contains(PhotoPickerOption.DEVICE_ONLY));
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mAllowMultiSelect = savedInstanceState.getBoolean(ARG_ALLOW_MULTI_SELECT, false);
            mPhotosOnly = savedInstanceState.getBoolean(ARG_PHOTOS_ONLY, false);
            mDeviceOnly = savedInstanceState.getBoolean(ARG_DEVICE_ONLY, false);
        }
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        mAllowMultiSelect = args != null && args.getBoolean(ARG_ALLOW_MULTI_SELECT);
        mPhotosOnly = args != null && args.getBoolean(ARG_PHOTOS_ONLY);
        mDeviceOnly = args != null && args.getBoolean(ARG_DEVICE_ONLY);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ARG_ALLOW_MULTI_SELECT, mAllowMultiSelect);
        outState.putBoolean(ARG_PHOTOS_ONLY, mPhotosOnly);
        outState.putBoolean(ARG_DEVICE_ONLY, mDeviceOnly);
        super.onSaveInstanceState(outState);
    }

    public void setOptions(EnumSet<PhotoPickerOption> options) {
        mAllowMultiSelect = options != null && options.contains(PhotoPickerOption.ALLOW_MULTI_SELECT);
        mPhotosOnly = options != null && options.contains(PhotoPickerOption.PHOTOS_ONLY);

        if (hasAdapter()) {
            getAdapter().setAllowMultiSelect(mAllowMultiSelect);
            getAdapter().setShowPhotosOnly(mPhotosOnly);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.photo_picker_fragment, container, false);

        mRecycler = (RecyclerView) view.findViewById(R.id.recycler);
        mRecycler.setHasFixedSize(true);

        mBottomBar = view.findViewById(R.id.bottom_bar);
        mBottomBar.findViewById(R.id.icon_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onPhotoPickerIconClicked(PhotoPickerIcon.ANDROID_CAMERA);
                }
            }
        });
        mBottomBar.findViewById(R.id.icon_picker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onPhotoPickerIconClicked(PhotoPickerIcon.ANDROID_PICKER);
                }
            }
        });

        View wpMediaIcon = mBottomBar.findViewById(R.id.icon_wpmedia);
        if (mDeviceOnly) {
            wpMediaIcon.setVisibility(View.GONE);
        } else {
            wpMediaIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onPhotoPickerIconClicked(PhotoPickerIcon.WP_MEDIA);
                    }
                }
            });
        }

        reload();

        return view;
    }

    void setPhotoPickerListener(PhotoPickerListener listener) {
        mListener = listener;
    }

    private void showBottomBar() {
        if (!isBottomBarShowing()) {
            AniUtils.animateBottomBar(mBottomBar, true);
        }
    }

    private void hideBottomBar() {
        if (isBottomBarShowing()) {
            AniUtils.animateBottomBar(mBottomBar, false);
        }
    }

    private boolean isBottomBarShowing() {
        return mBottomBar.getVisibility() == View.VISIBLE;
    }

    private final PhotoPickerAdapterListener mAdapterListener = new PhotoPickerAdapterListener() {
        @Override
        public void onItemTapped(Uri mediaUri) {
            if (mListener != null) {
                List<Uri> uriList = new ArrayList<>();
                uriList.add(mediaUri);
                mListener.onPhotoPickerMediaChosen(uriList);
            }
        }

        @Override
        public void onSelectedCountChanged(int count) {
            if (count == 0) {
                finishActionMode();
            } else {
                if (mActionMode == null) {
                    ((AppCompatActivity) getActivity()).startSupportActionMode(new ActionModeCallback());
                }
                updateActionModeTitle();
            }
        }

        @Override
        public void onAdapterLoaded(boolean isEmpty) {
            showEmptyView(isEmpty);
            if (mRestoreState != null) {
                mGridManager.onRestoreInstanceState(mRestoreState);
                mRestoreState = null;
            }
        }
    };

    private void showEmptyView(boolean show) {
        if (isAdded()) {
            getView().findViewById(R.id.text_empty).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private boolean hasAdapter() {
        return mAdapter != null;
    }

    private PhotoPickerAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new PhotoPickerAdapter(getActivity(), mAdapterListener);
            mAdapter.setAllowMultiSelect(mAllowMultiSelect);
            mAdapter.setShowPhotosOnly(mPhotosOnly);
        }
        return mAdapter;
    }

    /*
     * populates the adapter with media stored on the device
     */
    public void reload() {
        if (!isAdded()) {
            AppLog.w(AppLog.T.POSTS, "Photo picker > can't reload when not added");
            return;
        }

        // save the current state so we can restore it after loading
        if (mGridManager != null) {
            mRestoreState = mGridManager.onSaveInstanceState();
        }

        mGridManager = new GridLayoutManager(getActivity(), NUM_COLUMNS);
        mRecycler.setLayoutManager(mGridManager);
        mRecycler.setAdapter(getAdapter());
        getAdapter().refresh(true);
    }

    /*
     * similar to the above but only repopulates if changes are detected
     */
    public void refresh() {
        if (!isAdded()) {
            AppLog.w(AppLog.T.POSTS, "Photo picker > can't refresh when not added");
            return;
        }
        if (mGridManager == null || mAdapter == null) {
            reload();
        } else {
            getAdapter().refresh(false);
        }
    }

    public void finishActionMode() {
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
            inflater.inflate(R.menu.photo_picker_action_mode, menu);
            hideBottomBar();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.mnu_confirm_selection && mListener != null) {
                ArrayList<Uri> uriList = getAdapter().getSelectedURIs();
                mListener.onPhotoPickerMediaChosen(uriList);
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            getAdapter().setMultiSelectEnabled(false);
            mActionMode = null;
            showBottomBar();
        }
    }
}
