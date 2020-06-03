package org.wordpress.android.ui.photopicker;

import android.Manifest.permission;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.imageeditor.preview.PreviewImageFragment.Companion.EditImageData;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.photopicker.PhotoPickerAdapter.PhotoPickerAdapterListener;
import org.wordpress.android.ui.prefs.EmptyViewRecyclerView;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.WPPermissionUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhotoPickerFragment extends Fragment {
    private static final String KEY_LAST_TAPPED_ICON = "last_tapped_icon";
    private static final String KEY_SELECTED_POSITIONS = "selected_positions";

    static final int NUM_COLUMNS = 3;
    public static final String ARG_BROWSER_TYPE = "browser_type";

    public enum PhotoPickerIcon {
        ANDROID_CHOOSE_PHOTO(true),
        ANDROID_CHOOSE_VIDEO(true),
        ANDROID_CAPTURE_PHOTO(true),
        ANDROID_CAPTURE_VIDEO(true),
        ANDROID_CHOOSE_PHOTO_OR_VIDEO(true),
        WP_MEDIA(false),
        STOCK_MEDIA(true),
        GIF(true);

        private boolean mRequiresUploadPermission;

        PhotoPickerIcon(boolean requiresUploadPermission) {
            this.mRequiresUploadPermission = requiresUploadPermission;
        }

        public boolean requiresUploadPermission() {
            return mRequiresUploadPermission;
        }
    }

    /*
     * parent activity must implement this listener
     */
    public interface PhotoPickerListener {
        void onPhotoPickerMediaChosen(@NonNull List<Uri> uriList);

        void onPhotoPickerIconClicked(@NonNull PhotoPickerIcon icon, boolean allowMultipleSelection);
    }

    private EmptyViewRecyclerView mRecycler;
    private PhotoPickerAdapter mAdapter;
    private View mMediaSourceBottomBar;
    private View mInsertEditBottomBar;
    private ActionableEmptyView mSoftAskView;
    private ActionMode mActionMode;
    private GridLayoutManager mGridManager;
    private Parcelable mRestoreState;
    private PhotoPickerListener mListener;
    private PhotoPickerIcon mLastTappedIcon;
    private MediaBrowserType mBrowserType;
    private SiteModel mSite;
    private ArrayList<Integer> mSelectedPositions;

    public static PhotoPickerFragment newInstance(@NonNull PhotoPickerListener listener,
                                                  @NonNull MediaBrowserType browserType,
                                                  @Nullable SiteModel site) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_BROWSER_TYPE, browserType);
        if (site != null) {
            args.putSerializable(WordPress.SITE, site);
        }

        PhotoPickerFragment fragment = new PhotoPickerFragment();
        fragment.setPhotoPickerListener(listener);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBrowserType = (MediaBrowserType) getArguments().getSerializable(ARG_BROWSER_TYPE);
        mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);

        if (savedInstanceState != null) {
            String savedLastTappedIconName = savedInstanceState.getString(KEY_LAST_TAPPED_ICON);
            mLastTappedIcon = savedLastTappedIconName == null ? null : PhotoPickerIcon.valueOf(savedLastTappedIconName);
            if (savedInstanceState.containsKey(KEY_SELECTED_POSITIONS)) {
                mSelectedPositions = savedInstanceState.getIntegerArrayList(KEY_SELECTED_POSITIONS);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.photo_picker_fragment, container, false);

        mRecycler = view.findViewById(R.id.recycler);
        mRecycler.setEmptyView(view.findViewById(R.id.actionable_empty_view));
        mRecycler.setHasFixedSize(true);

        // disable thumbnail loading during a fling to conserve memory
        final int minDistance = WPMediaUtils.getFlingDistanceToDisableThumbLoading(getActivity());
        mRecycler.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (Math.abs(velocityY) > minDistance) {
                    getAdapter().setLoadThumbnails(false);
                }
                return false;
            }
        });
        mRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    getAdapter().setLoadThumbnails(true);
                }
            }
        });

        mMediaSourceBottomBar = view.findViewById(R.id.container_media_source_bar);
        mInsertEditBottomBar = view.findViewById(R.id.container_insert_edit_bar);

        if (!canShowMediaSourceBottomBar()) {
            mMediaSourceBottomBar.setVisibility(View.GONE);
        } else {
            View camera = mMediaSourceBottomBar.findViewById(R.id.icon_camera);
            if (mBrowserType.isGutenbergPicker()) {
                camera.setVisibility(View.GONE);
            } else {
                camera.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mBrowserType.isImagePicker() && mBrowserType.isVideoPicker()) {
                            showCameraPopupMenu(v);
                        } else if (mBrowserType.isImagePicker()) {
                            doIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_PHOTO);
                        } else if (mBrowserType.isVideoPicker()) {
                            doIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_VIDEO);
                        } else {
                            AppLog.e(T.MEDIA, "This code should be unreachable. If you see this message one of "
                                              + "the MediaBrowserTypes isn't setup correctly.");
                        }
                    }
                });
            }
            mMediaSourceBottomBar.findViewById(R.id.icon_picker).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBrowserType == MediaBrowserType.GRAVATAR_IMAGE_PICKER
                        || mBrowserType == MediaBrowserType.SITE_ICON_PICKER) {
                        doIconClicked(PhotoPickerIcon.ANDROID_CHOOSE_PHOTO);
                    } else {
                        performActionOrShowPopup(v);
                    }
                }
            });

            // choosing from WP media requires a site and should be hidden in gutenberg picker
            View wpMedia = mMediaSourceBottomBar.findViewById(R.id.icon_wpmedia);
            if (mSite == null || mBrowserType.isGutenbergPicker()) {
                wpMedia.setVisibility(View.GONE);
            } else {
                wpMedia.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doIconClicked(PhotoPickerIcon.WP_MEDIA);
                    }
                });
            }
        }

        if (canShowInsertEditBottomBar()) {
            mInsertEditBottomBar.findViewById(R.id.text_edit).setOnClickListener(v -> {
                ArrayList<EditImageData.InputData> inputData =
                        WPMediaUtils.createListOfEditImageInputData(requireContext(), getAdapter().getSelectedURIs());
                ActivityLauncher.openImageEditor(getActivity(), inputData);
            });

            mInsertEditBottomBar.findViewById(R.id.text_insert).setOnClickListener(v -> performInsertAction());
        }

        mSoftAskView = view.findViewById(R.id.soft_ask_view);

        return view;
    }

    private boolean canShowMediaSourceBottomBar() {
        if (mBrowserType == MediaBrowserType.AZTEC_EDITOR_PICKER && DisplayUtils.isLandscape(getActivity())) {
            return true;
        } else if (mBrowserType == MediaBrowserType.AZTEC_EDITOR_PICKER) {
            return false;
        }

        return true;
    }

    private boolean canShowInsertEditBottomBar() {
        return mBrowserType.isGutenbergPicker() && !mBrowserType.isVideoPicker();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_LAST_TAPPED_ICON, mLastTappedIcon == null ? null : mLastTappedIcon.name());

        if (hasAdapter() && getAdapter().getNumSelected() > 0) {
            ArrayList<Integer> selectedItems = getAdapter().getSelectedPositions();
            outState.putIntegerArrayList(KEY_SELECTED_POSITIONS, selectedItems);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkStoragePermission();
    }

    public void doIconClicked(@NonNull PhotoPickerIcon icon) {
        mLastTappedIcon = icon;

        if (icon == PhotoPickerIcon.ANDROID_CAPTURE_PHOTO || icon == PhotoPickerIcon.ANDROID_CAPTURE_VIDEO) {
            if (ContextCompat.checkSelfPermission(
                    getActivity(), permission.CAMERA) != PackageManager.PERMISSION_GRANTED || !hasStoragePermission()) {
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
            case STOCK_MEDIA:
                break;
            case GIF:
                break;
        }

        if (mListener != null) {
            mListener.onPhotoPickerIconClicked(icon, mBrowserType.canMultiselect());
        }
    }

    public void performActionOrShowPopup(@NonNull View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);

        if (mBrowserType.isImagePicker()) {
            MenuItem itemPhoto = popup.getMenu().add(R.string.photo_picker_choose_photo);
            itemPhoto.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    doIconClicked(PhotoPickerIcon.ANDROID_CHOOSE_PHOTO);
                    return true;
                }
            });
        }

        if (mBrowserType.isVideoPicker()) {
            MenuItem itemVideo = popup.getMenu().add(R.string.photo_picker_choose_video);
            itemVideo.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    doIconClicked(PhotoPickerIcon.ANDROID_CHOOSE_VIDEO);
                    return true;
                }
            });
        }

        if (mSite != null && !mBrowserType.isGutenbergPicker()) {
            MenuItem itemStock = popup.getMenu().add(R.string.photo_picker_stock_media);
            itemStock.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    doIconClicked(PhotoPickerIcon.STOCK_MEDIA);
                    return true;
                }
            });

            if (BuildConfig.TENOR_AVAILABLE) {
                MenuItem itemGif = popup.getMenu().add(R.string.photo_picker_gif);
                itemGif.setOnMenuItemClickListener(item -> {
                    doIconClicked(PhotoPickerIcon.GIF);
                    return true;
                });
            }
        }

        // if the menu has a single item, perform the action right away
        if (popup.getMenu().size() == 1) {
            popup.getMenu().performIdentifierAction(popup.getMenu().getItem(0).getItemId(), 0);
        } else {
            popup.show();
        }
    }

    public void showCameraPopupMenu(@NonNull View view) {
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

    public void setPhotoPickerListener(PhotoPickerListener listener) {
        mListener = listener;
    }

    private void showBottomBar(View bottomBar) {
        if (!isBottomBarShowing(bottomBar)) {
            AniUtils.animateBottomBar(bottomBar, true);
        }
    }

    private void hideBottomBar(View bottomBar) {
        if (isBottomBarShowing(bottomBar)) {
            AniUtils.animateBottomBar(bottomBar, false);
        }
    }

    private boolean isBottomBarShowing(View bottomBar) {
        return bottomBar.getVisibility() == View.VISIBLE;
    }

    private final PhotoPickerAdapterListener mAdapterListener = new PhotoPickerAdapterListener() {
        @Override
        public void onSelectedCountChanged(int count) {
            if (count == 0) {
                finishActionMode();
            } else {
                FragmentActivity activity = getActivity();
                if (activity == null) {
                    return;
                }
                if (mActionMode == null) {
                    ((AppCompatActivity) activity).startSupportActionMode(new ActionModeCallback());
                }
                updateActionModeTitle();
            }
        }

        @Override
        public void onAdapterLoaded(boolean isEmpty) {
            // restore previous selection
            if (mSelectedPositions != null) {
                getAdapter().setSelectedPositions(mSelectedPositions);
                mSelectedPositions = null;
            }
            // restore previous state
            if (mRestoreState != null) {
                mGridManager.onRestoreInstanceState(mRestoreState);
                mRestoreState = null;
            }
        }
    };

    private boolean hasAdapter() {
        return mAdapter != null;
    }

    private PhotoPickerAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new PhotoPickerAdapter(getActivity(), mBrowserType, mAdapterListener);
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

        if (!hasStoragePermission()) {
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

        if (!hasStoragePermission()) {
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
        if (mActionMode == null) {
            return;
        }
        String title;
        if (mBrowserType.canMultiselect()) {
            int numSelected = getAdapter().getNumSelected();
            title = String.format(getString(R.string.cab_selected), numSelected);
            mActionMode.setTitle(title);
        } else {
            if (mBrowserType.isImagePicker() && mBrowserType.isVideoPicker()) {
                mActionMode.setTitle(R.string.photo_picker_use_media);
            } else if (mBrowserType.isVideoPicker()) {
                mActionMode.setTitle(R.string.photo_picker_use_video);
            } else {
                mActionMode.setTitle(R.string.photo_picker_use_photo);
            }
        }
    }

    private final class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;
            if (canShowInsertEditBottomBar()) {
                showBottomBar(mInsertEditBottomBar);
            } else {
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.photo_picker_action_mode, menu);
            }
            hideBottomBar(mMediaSourceBottomBar);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            AccessibilityUtils.setActionModeDoneButtonContentDescription(getActivity(), getString(R.string.cancel));
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.mnu_confirm_selection && mListener != null) {
                performInsertAction();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            if (canShowMediaSourceBottomBar()) {
                showBottomBar(mMediaSourceBottomBar);
            }
            hideBottomBar(mInsertEditBottomBar);
            getAdapter().clearSelection();
        }
    }

    private void performInsertAction() {
        ArrayList<Uri> uriList = getAdapter().getSelectedURIs();
        mListener.onPhotoPickerMediaChosen(uriList);
        trackAddRecentMediaEvent(uriList);
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
    private void checkStoragePermission() {
        if (!isAdded()) {
            return;
        }

        if (hasStoragePermission()) {
            showSoftAskView(false);
            if (!hasAdapter()) {
                reload();
            }
        } else {
            showSoftAskView(true);
        }
    }

    private void requestStoragePermission() {
        String[] permissions = new String[]{permission.WRITE_EXTERNAL_STORAGE};
        requestPermissions(
                permissions, WPPermissionUtils.PHOTO_PICKER_STORAGE_PERMISSION_REQUEST_CODE);
    }

    private void requestCameraPermission() {
        // in addition to CAMERA permission we also need a storage permission, to store media from the camera
        String[] permissions = new String[]{permission.CAMERA, permission.WRITE_EXTERNAL_STORAGE};
        requestPermissions(permissions, WPPermissionUtils.PHOTO_PICKER_CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
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
        if (!isAdded()) {
            return;
        }

        boolean isAlwaysDenied = isStoragePermissionAlwaysDenied();

        if (show) {
            String appName = "<strong>" + getString(R.string.app_name) + "</strong>";
            String label;

            if (isAlwaysDenied) {
                String permissionName = "<strong>"
                                        + WPPermissionUtils.getPermissionName(getActivity(),
                        permission.WRITE_EXTERNAL_STORAGE)
                                        + "</strong>";
                label = String.format(getString(R.string.photo_picker_soft_ask_permissions_denied), appName,
                        permissionName);
            } else {
                label = String.format(getString(R.string.photo_picker_soft_ask_label), appName);
            }

            mSoftAskView.title.setText(Html.fromHtml(label));

            // when the user taps Allow, request the required permissions unless the user already
            // denied them permanently, in which case take them to the device settings for this
            // app so the user can change permissions there
            int allowId = isAlwaysDenied ? R.string.button_edit_permissions : R.string.photo_picker_soft_ask_allow;
            mSoftAskView.button.setText(allowId);
            mSoftAskView.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isStoragePermissionAlwaysDenied()) {
                        WPPermissionUtils.showAppSettings(getActivity());
                    } else {
                        requestStoragePermission();
                    }
                }
            });

            mSoftAskView.setVisibility(View.VISIBLE);
            hideBottomBar(mMediaSourceBottomBar);
        } else if (mSoftAskView.getVisibility() == View.VISIBLE) {
            AniUtils.fadeOut(mSoftAskView, AniUtils.Duration.MEDIUM);
            if (canShowMediaSourceBottomBar()) {
                showBottomBar(mMediaSourceBottomBar);
            }
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
                Map<String, Object> properties =
                        AnalyticsUtils.getMediaProperties(getActivity(), isVideo, mediaUri, null);
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
