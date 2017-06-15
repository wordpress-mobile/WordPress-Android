package org.wordpress.android.ui.photopicker;

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
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

    private boolean mAllowMultiSelect;
    private boolean mPhotosOnly;
    private boolean mDeviceOnly;

    private static final String ARG_ALLOW_MULTI_SELECT = "allow_multi_select";
    private static final String ARG_PHOTOS_ONLY = "photos_only";
    private static final String ARG_DEVICE_ONLY = "device_only";

    // TODO: a future PR should only request WRITE_EXTERNAL_STORAGE since that's all we need
    // to show photos. we should request CAMERA permission when the camera icon is tapped.
    private static final String[] PERMISSIONS =
            {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

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

        if (savedInstanceState == null && mAllowMultiSelect && isPermissionGranted()) {
            SmartToast.show(getActivity(), SmartToast.SmartToastType.PHOTO_PICKER_LONG_PRESS);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPermissions();
    }

    private void doIconClicked(@NonNull PhotoPickerIcon icon) {
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

        if (!isPermissionGranted()) return;

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

        if (!isPermissionGranted()) return;

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

    /*
     * returns true if all of the required permissions have been granted
     */
    private boolean isPermissionGranted() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /*
     * returns true if any of the required permissions have been denied AND the user checked "Never ask again"
     */
    private boolean isPermissionAlwaysDenied() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                if (WPPermissionUtils.isPermissionAlwaysDenied(getActivity(), permission)) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * load the photos if we have the necessary permissions, otherwise show the "soft ask" view
     * which asks the user to allow the permissions
     */
    public void checkPermissions() {
        if (!isAdded()) return;

        if (isPermissionGranted()) {
            showSoftAskView(false);
            if (mAdapter == null || mAdapter.isEmpty()) {
                reload();
            }
        } else {
            showSoftAskView(true);
        }
    }

    /*
     * request the camera and storage permissions required to access photos
     */
    private void requestPermissions() {
        ArrayList<String> list = new ArrayList<>();
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                list.add(permission);
            }
        }
        String[] array = list.toArray(new String[list.size()]);
        FragmentCompat.requestPermissions(this, array, WPPermissionUtils.PHOTO_PICKER_PERMISSION_REQUEST_CODE);
    }

    /*
     * open the device's settings page for this app so the user can edit permissions
     */
    private void showAppSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        WPPermissionUtils.setPermissionListAsked(requestCode, permissions, grantResults);

        if (requestCode == WPPermissionUtils.PHOTO_PICKER_PERMISSION_REQUEST_CODE) {
            checkPermissions();
        }
    }

    /*
     * shows the "soft ask" view which should appear when the necessary permissions haven't
     * been granted yet
     */
    private void showSoftAskView(boolean show) {
        if (!isAdded()) return;

        boolean isAlwaysDenied = isPermissionAlwaysDenied();

        if (show) {
            int labelId = isAlwaysDenied ?
                    R.string.photo_picker_soft_ask_permissions_denied : R.string.photo_picker_soft_ask_label;
            String appName = "<strong>" + getString(R.string.app_name) + "</strong>";
            String label = String.format(getString(labelId), appName);
            TextView txtLabel = (TextView) mSoftAskContainer.findViewById(R.id.text_soft_ask_label);
            txtLabel.setText(Html.fromHtml(label));

            // when the user taps Allow, request the required permissions unless the user already
            // denied them permanently, in which case take them to the device settings for this
            // app so the user can change permissions there
            TextView txtAllow = (TextView) mSoftAskContainer.findViewById(R.id.text_soft_ask_allow);
            int allowId = isAlwaysDenied ?
                    R.string.photo_picker_soft_ask_edit_permissions : R.string.photo_picker_soft_ask_allow;
            txtAllow.setText(allowId);
            txtAllow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isPermissionAlwaysDenied()) {
                        showAppSettings();
                    } else {
                        requestPermissions();
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
