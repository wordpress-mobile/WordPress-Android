package org.wordpress.android.ui.photopicker;

import android.Manifest.permission;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.photopicker.PhotoPickerAdapter.PhotoPickerAdapterListener;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.SmartToast;
import org.wordpress.android.util.WPPermissionUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhotoPickerFragment extends Fragment {

    static final int NUM_COLUMNS = 3;

    public enum PhotoPickerIcon {
        ANDROID_CHOOSE_PHOTO,
        ANDROID_CHOOSE_VIDEO,
        ANDROID_CAPTURE_PHOTO,
        ANDROID_CAPTURE_VIDEO,
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
    private ViewGroup mSoftAskContainer;
    private ActionMode mActionMode;
    private GridLayoutManager mGridManager;
    private Parcelable mRestoreState;
    private PhotoPickerListener mListener;
    private PhotoPickerIcon mLastTappedIcon;

    private boolean mAllowMultiSelect;
    private boolean mPhotosOnly;
    private boolean mDeviceOnly;

    private static final String ARG_ALLOW_MULTI_SELECT = "allow_multi_select";
    private static final String ARG_PHOTOS_ONLY = "photos_only";
    private static final String ARG_DEVICE_ONLY = "device_only";

    public static PhotoPickerFragment newInstance(@NonNull PhotoPickerListener listener,
                                                  @NonNull EnumSet<PhotoPickerOption> options) {
        Bundle args = new Bundle();
        args.putBoolean(ARG_ALLOW_MULTI_SELECT, options.contains(PhotoPickerOption.ALLOW_MULTI_SELECT));
        args.putBoolean(ARG_PHOTOS_ONLY, options.contains(PhotoPickerOption.PHOTOS_ONLY));
        args.putBoolean(ARG_DEVICE_ONLY, options.contains(PhotoPickerOption.DEVICE_ONLY));

        PhotoPickerFragment fragment = new PhotoPickerFragment();
        fragment.setPhotoPickerListener(listener);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mAllowMultiSelect = args.getBoolean(ARG_ALLOW_MULTI_SELECT, false);
            mPhotosOnly = args.getBoolean(ARG_PHOTOS_ONLY, false);
            mDeviceOnly = args.getBoolean(ARG_DEVICE_ONLY, false);
        }
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        mAllowMultiSelect = args.getBoolean(ARG_ALLOW_MULTI_SELECT);
        mPhotosOnly = args.getBoolean(ARG_PHOTOS_ONLY);
        mDeviceOnly = args.getBoolean(ARG_DEVICE_ONLY);
    }

    public void setOptions(@NonNull EnumSet<PhotoPickerOption> options) {
        mAllowMultiSelect = options.contains(PhotoPickerOption.ALLOW_MULTI_SELECT);
        mPhotosOnly = options.contains(PhotoPickerOption.PHOTOS_ONLY);
        mDeviceOnly = options.contains(PhotoPickerOption.DEVICE_ONLY);

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
                if (mPhotosOnly) {
                    doIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_PHOTO);
                } else {
                    showCameraPopupMenu(v);
                }
            }
        });
        mBottomBar.findViewById(R.id.icon_picker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhotosOnly) {
                    doIconClicked(PhotoPickerIcon.ANDROID_CHOOSE_PHOTO);
                } else {
                    showPickerPopupMenu(v);
                }

            }
        });

        mSoftAskContainer = (ViewGroup) view.findViewById(R.id.container_soft_ask);

        View wpMediaIcon = mBottomBar.findViewById(R.id.icon_wpmedia);
        if (mDeviceOnly) {
            wpMediaIcon.setVisibility(View.GONE);
        } else {
            wpMediaIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doIconClicked(PhotoPickerIcon.WP_MEDIA);
                }
            });
        }

        if (savedInstanceState == null && mAllowMultiSelect && hasStoragePermission()) {
            SmartToast.show(getActivity(), SmartToast.SmartToastType.PHOTO_PICKER_LONG_PRESS);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkStoragePermission();
    }

    private void doIconClicked(@NonNull PhotoPickerIcon icon) {
        mLastTappedIcon = icon;

        if (icon == PhotoPickerIcon.ANDROID_CAPTURE_PHOTO || icon == PhotoPickerIcon.ANDROID_CAPTURE_VIDEO) {
            if (ContextCompat.checkSelfPermission(
                    getActivity(), permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
                return;
            }
        }

        switch (icon) {
            case ANDROID_CAPTURE_PHOTO:
                trackSelectedOtherSourceEvents(AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_CAPTURE_MEDIA, false);
                break;
            case ANDROID_CAPTURE_VIDEO:
                trackSelectedOtherSourceEvents(AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_CAPTURE_MEDIA, true);
                break;
            case ANDROID_CHOOSE_PHOTO:
                trackSelectedOtherSourceEvents(AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_DEVICE_LIBRARY, false);
                break;
            case ANDROID_CHOOSE_VIDEO:
                trackSelectedOtherSourceEvents(AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_DEVICE_LIBRARY, true);
                break;
            case WP_MEDIA:
                AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_WP_MEDIA);
                break;
        }

        if (mListener != null) {
            mListener.onPhotoPickerIconClicked(icon);
        }
    }

    private void showPickerPopupMenu(@NonNull View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);

        MenuItem itemPhoto = popup.getMenu().add(R.string.photo_picker_choose_photo);
        itemPhoto.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                doIconClicked(PhotoPickerIcon.ANDROID_CHOOSE_PHOTO);
                return true;
            }
        });

        MenuItem itemVideo = popup.getMenu().add(R.string.photo_picker_choose_video);
        itemVideo.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                doIconClicked(PhotoPickerIcon.ANDROID_CHOOSE_VIDEO);
                return true;
            }
        });

        popup.show();
    }

    private void showCameraPopupMenu(@NonNull View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);

        MenuItem itemPhoto = popup.getMenu().add(R.string.photo_picker_capture_photo);
        itemPhoto.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                doIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_PHOTO);
                return true;
            }
        });

        MenuItem itemVideo = popup.getMenu().add(R.string.photo_picker_capture_video);
        itemVideo.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                doIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_VIDEO);
                return true;
            }
        });

        popup.show();
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
                trackAddRecentMediaEvent(uriList);
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

        if (!hasStoragePermission()) return;

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

        if (!hasStoragePermission()) return;

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
                trackAddRecentMediaEvent(uriList);
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

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(
                getActivity(), permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isStoragePermissionAlwaysDenied() {
        return WPPermissionUtils.isPermissionAlwaysDenied(
                getActivity(), permission.WRITE_EXTERNAL_STORAGE);
    }

    /*
     * load the photos if we have the necessary permission, otherwise show the "soft ask" view
     * which asks the user to allow the permission
     */
    public void checkStoragePermission() {
        if (!isAdded()) return;

        if (hasStoragePermission()) {
            showSoftAskView(false);
            if (mAdapter == null || mAdapter.isEmpty()) {
                reload();
            }
        } else {
            showSoftAskView(true);
        }
    }

    private void requestStoragePermission() {
        String[] permissions = new String[] { permission.WRITE_EXTERNAL_STORAGE };
        FragmentCompat.requestPermissions(
                this, permissions, WPPermissionUtils.PHOTO_PICKER_STORAGE_PERMISSION_REQUEST_CODE);
    }

    private void requestCameraPermission() {
        String[] permissions = new String[] { permission.CAMERA };
        FragmentCompat.requestPermissions(
                this, permissions, WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        boolean checkForAlwaysDenied =
                requestCode == WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE;
        boolean allGranted = WPPermissionUtils.setPermissionListAsked(
                getActivity(), requestCode, permissions, grantResults, checkForAlwaysDenied);

        switch (requestCode) {
            case WPPermissionUtils.PHOTO_PICKER_STORAGE_PERMISSION_REQUEST_CODE:
                checkStoragePermission();
                break;
            case WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE:
                if (allGranted) {
                    doIconClicked(mLastTappedIcon);
                }
                break;
        }
    }

    /*
     * shows the "soft ask" view which should appear when storage permission hasn't been granted
     */
    private void showSoftAskView(boolean show) {
        if (!isAdded()) return;

        boolean isAlwaysDenied = isStoragePermissionAlwaysDenied();

        if (show) {
            String appName = "<strong>" + getString(R.string.app_name) + "</strong>";
            TextView txtLabel = (TextView) mSoftAskContainer.findViewById(R.id.text_soft_ask_label);
            String label;
            if (isAlwaysDenied) {
                String permissionName = "<strong>"
                        + WPPermissionUtils.getPermissionName(getActivity(), permission.WRITE_EXTERNAL_STORAGE)
                        + "</strong>";
                label = String.format(
                        getString(R.string.photo_picker_soft_ask_permissions_denied), appName, permissionName);
            } else {
                label = String.format(getString(R.string.photo_picker_soft_ask_label), appName);
            }
            txtLabel.setText(Html.fromHtml(label));

            // when the user taps Allow, request the required permissions unless the user already
            // denied them permanently, in which case take them to the device settings for this
            // app so the user can change permissions there
            TextView txtAllow = (TextView) mSoftAskContainer.findViewById(R.id.text_soft_ask_allow);
            int allowId = isAlwaysDenied ?
                    R.string.button_edit_permissions : R.string.photo_picker_soft_ask_allow;
            txtAllow.setText(allowId);
            txtAllow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStoragePermissionAlwaysDenied()) {
                        WPPermissionUtils.showAppSettings(getActivity());
                    } else {
                        requestStoragePermission();
                    }
                }
            });

            mSoftAskContainer.setVisibility(View.VISIBLE);
            hideBottomBar();
        } else if (mSoftAskContainer.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(mSoftAskContainer, AniUtils.Duration.MEDIUM);
            showBottomBar();
            refresh();
        }
    }

    private void trackAddRecentMediaEvent(List<Uri> uriList) {
        if (uriList == null) {
            AppLog.e(AppLog.T.MEDIA, "Cannot track new media events if uriList is null!!");
            return;
        }

        boolean isMultiselection = uriList.size() > 1;

        for (Uri mediaUri : uriList) {
            if (mediaUri != null) {
                boolean isVideo = MediaUtils.isVideo(mediaUri.toString());
                Map<String, Object> properties = AnalyticsUtils.getMediaProperties(getActivity(), isVideo, mediaUri, null);
                properties.put("is_part_of_multiselection", isMultiselection);
                if (isMultiselection) {
                    properties.put("number_of_media_selected", uriList.size());
                }
                AnalyticsTracker.track(AnalyticsTracker.Stat.MEDIA_PICKER_RECENT_MEDIA_SELECTED, properties);
            }
        }
    }

    private void trackSelectedOtherSourceEvents(AnalyticsTracker.Stat stat, boolean isVideo) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("is_video", isVideo);
        AnalyticsTracker.track(stat, properties);
    }
}
