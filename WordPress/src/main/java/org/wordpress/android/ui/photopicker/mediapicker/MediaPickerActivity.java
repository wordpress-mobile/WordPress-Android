package org.wordpress.android.ui.photopicker.mediapicker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.imageeditor.preview.PreviewImageFragment;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment;
import org.wordpress.android.ui.posts.FeaturedImageHelper;
import org.wordpress.android.ui.posts.FeaturedImageHelper.EnqueueFeaturedImageResult;
import org.wordpress.android.ui.posts.editor.ImageEditorTracker;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.ui.RequestCodes.IMAGE_EDITOR_EDIT_IMAGE;
import static org.wordpress.android.ui.media.MediaBrowserActivity.ARG_BROWSER_TYPE;
import static org.wordpress.android.ui.posts.FeaturedImageHelperKt.EMPTY_LOCAL_POST_ID;

public class MediaPickerActivity extends LocaleAwareActivity
        implements PhotoPickerFragment.PhotoPickerListener {
    private static final String PICKER_FRAGMENT_TAG = "picker_fragment_tag";
    private static final String KEY_MEDIA_CAPTURE_PATH = "media_capture_path";

    public static final String EXTRA_MEDIA_URIS = "media_uris";
    public static final String EXTRA_MEDIA_ID = "media_id";
    public static final String EXTRA_MEDIA_QUEUED = "media_queued";
    public static final String EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED = "launch_wpstories_camera_requested";

    // the enum name of the source will be returned as a string in EXTRA_MEDIA_SOURCE
    public static final String EXTRA_MEDIA_SOURCE = "media_source";

    public static final String LOCAL_POST_ID = "local_post_id";

    private String mMediaCapturePath;
    private MediaBrowserType mBrowserType;

    // note that the site isn't required and may be null
    private SiteModel mSite;

    // note that the local post id isn't required (default value is EMPTY_LOCAL_POST_ID)
    private Integer mLocalPostId;

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject FeaturedImageHelper mFeaturedImageHelper;
    @Inject ImageEditorTracker mImageEditorTracker;

    public enum PhotoPickerMediaSource {
        ANDROID_CAMERA,
        ANDROID_PICKER,
        APP_PICKER,
        WP_MEDIA_PICKER,
        STOCK_MEDIA_PICKER;

        public static PhotoPickerMediaSource fromString(String strSource) {
            if (strSource != null) {
                for (PhotoPickerMediaSource source : PhotoPickerMediaSource.values()) {
                    if (source.name().equalsIgnoreCase(strSource)) {
                        return source;
                    }
                }
            }
            return null;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        setContentView(R.layout.photo_picker_activity);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        if (savedInstanceState == null) {
            mBrowserType = (MediaBrowserType) getIntent().getSerializableExtra(ARG_BROWSER_TYPE);
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mLocalPostId = getIntent().getIntExtra(LOCAL_POST_ID, EMPTY_LOCAL_POST_ID);
        } else {
            mBrowserType = (MediaBrowserType) savedInstanceState.getSerializable(ARG_BROWSER_TYPE);
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mLocalPostId = savedInstanceState.getInt(LOCAL_POST_ID, EMPTY_LOCAL_POST_ID);
        }

        PhotoPickerFragment fragment = getPickerFragment();
        if (fragment == null) {
            fragment = PhotoPickerFragment.newInstance(this, mBrowserType, mSite);
            getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, fragment, PICKER_FRAGMENT_TAG)
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                .commitAllowingStateLoss();
        } else {
            fragment.setPhotoPickerListener(this);
        }
        updateTitle(mBrowserType, actionBar);
    }

    private void updateTitle(MediaBrowserType browserType, ActionBar actionBar) {
        if (browserType.isImagePicker() && browserType.isVideoPicker()) {
            actionBar.setTitle(R.string.photo_picker_photo_or_video_title);
        } else if (browserType.isVideoPicker()) {
            actionBar.setTitle(R.string.photo_picker_video_title);
        } else {
            actionBar.setTitle(R.string.photo_picker_title);
        }
    }

    private PhotoPickerFragment getPickerFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(PICKER_FRAGMENT_TAG);
        if (fragment != null) {
            return (PhotoPickerFragment) fragment;
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_BROWSER_TYPE, mBrowserType);
        outState.putInt(LOCAL_POST_ID, mLocalPostId);
        if (mSite != null) {
            outState.putSerializable(WordPress.SITE, mSite);
        }
        if (!TextUtils.isEmpty(mMediaCapturePath)) {
            outState.putString(KEY_MEDIA_CAPTURE_PATH, mMediaCapturePath);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mMediaCapturePath = savedInstanceState.getString(KEY_MEDIA_CAPTURE_PATH);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            // user chose a photo from the device library
            case RequestCodes.PICTURE_LIBRARY:
            case RequestCodes.VIDEO_LIBRARY:
                if (data != null) {
                    doMediaUrisSelected(WPMediaUtils.retrieveMediaUris(data), PhotoPickerMediaSource.ANDROID_PICKER);
                }
                break;
            case RequestCodes.TAKE_PHOTO:
                try {
                    WPMediaUtils.scanMediaFile(this, mMediaCapturePath);
                    File f = new File(mMediaCapturePath);
                    List<Uri> capturedImageUri = Collections.singletonList(Uri.fromFile(f));
                    doMediaUrisSelected(capturedImageUri, PhotoPickerMediaSource.ANDROID_CAMERA);
                } catch (RuntimeException e) {
                    AppLog.e(AppLog.T.MEDIA, e);
                }
                break;
            // user selected from WP media library, extract the media ID and pass to caller
            case RequestCodes.MULTI_SELECT_MEDIA_PICKER:
            case RequestCodes.SINGLE_SELECT_MEDIA_PICKER:
                if (data.hasExtra(MediaBrowserActivity.RESULT_IDS)) {
                    ArrayList<Long> ids =
                            ListUtils.fromLongArray(data.getLongArrayExtra(MediaBrowserActivity.RESULT_IDS));
                    doMediaIdsSelected(ids, PhotoPickerMediaSource.WP_MEDIA_PICKER);
                }
                break;
            // user selected a stock photo
            case RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT:
                if (data != null && data.hasExtra(EXTRA_MEDIA_ID)) {
                    long mediaId = data.getLongExtra(EXTRA_MEDIA_ID, 0);
                    ArrayList<Long> ids = new ArrayList<>();
                    ids.add(mediaId);
                    doMediaIdsSelected(ids, PhotoPickerMediaSource.STOCK_MEDIA_PICKER);
                }
                break;
            case IMAGE_EDITOR_EDIT_IMAGE:
                if (data != null && data.hasExtra(PreviewImageFragment.ARG_EDIT_IMAGE_DATA)) {
                    List<Uri> uris = WPMediaUtils.retrieveImageEditorResult(data);
                    doMediaUrisSelected(uris, PhotoPickerMediaSource.APP_PICKER);
                }
                break;
        }
    }

    private void launchCameraForImage() {
        WPMediaUtils.launchCamera(this, BuildConfig.APPLICATION_ID,
                mediaCapturePath -> mMediaCapturePath = mediaCapturePath);
    }

    private void launchCameraForVideo() {
        WPMediaUtils.launchVideoCamera(this);
    }

    private void launchPictureLibrary(boolean multiSelect) {
        WPMediaUtils.launchPictureLibrary(this, multiSelect);
    }

    private void launchVideoLibrary(boolean multiSelect) {
        WPMediaUtils.launchVideoLibrary(this, multiSelect);
    }

    private void launchWPMediaLibrary() {
        if (mSite != null) {
            ActivityLauncher.viewMediaPickerForResult(this, mSite, mBrowserType);
        } else {
            ToastUtils.showToast(this, R.string.blog_not_found);
        }
    }

    private void launchStockMediaPicker() {
        if (mSite != null) {
            ActivityLauncher.showStockMediaPickerForResult(this,
                    mSite, RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT);
        } else {
            ToastUtils.showToast(this, R.string.blog_not_found);
        }
    }

    private void launchWPStoriesCamera() {
        Intent intent = new Intent()
                .putExtra(EXTRA_LAUNCH_WPSTORIES_CAMERA_REQUESTED, true);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void doMediaUrisSelected(@NonNull List<? extends Uri> mediaUris, @NonNull PhotoPickerMediaSource source) {
        // if user chose a featured image, we need to upload it and return the uploaded media object
        if (mBrowserType == MediaBrowserType.FEATURED_IMAGE_PICKER) {
            Uri mediaUri = mediaUris.get(0);
            final String mimeType = getContentResolver().getType(mediaUri);

            mFeaturedImageHelper.trackFeaturedImageEvent(
                FeaturedImageHelper.TrackableEvent.IMAGE_PICKED,
                mLocalPostId
            );

            WPMediaUtils.fetchMediaAndDoNext(this, mediaUri,
                                             new WPMediaUtils.MediaFetchDoNext() {
                                                 @Override
                                                 public void doNext(Uri uri) {
                                                     EnqueueFeaturedImageResult queueImageResult = mFeaturedImageHelper
                                                             .queueFeaturedImageForUpload(mLocalPostId, mSite, uri,
                                                                     mimeType);
                                                     // we intentionally display a toast instead of a snackbar as a
                                                     // Snackbar is tied to an Activity and the activity is finished
                                                     // right after this call
                                                     switch (queueImageResult) {
                                                         case FILE_NOT_FOUND:
                                                             Toast.makeText(getApplicationContext(),
                                                                     R.string.file_not_found, Toast.LENGTH_SHORT)
                                                                  .show();
                                                             break;
                                                         case INVALID_POST_ID:
                                                             Toast.makeText(getApplicationContext(),
                                                                     R.string.error_generic, Toast.LENGTH_SHORT)
                                                                  .show();
                                                             break;
                                                         case SUCCESS:
                                                             // noop
                                                             break;
                                                     }
                                                     Intent intent = new Intent()
                                                             .putExtra(EXTRA_MEDIA_QUEUED, true);
                                                     setResult(RESULT_OK, intent);
                                                     finish();
                                                 }
                                             });
        } else {
            Intent intent = new Intent()
                    .putExtra(EXTRA_MEDIA_URIS, convertUrisListToStringArray(mediaUris))
                    .putExtra(EXTRA_MEDIA_SOURCE, source.name())
                    // set the browserType in the result, so caller can distinguish and handle things as needed
                    .putExtra(ARG_BROWSER_TYPE, mBrowserType);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void doMediaIdsSelected(ArrayList<Long> mediaIds, @NonNull PhotoPickerMediaSource source) {
        if (mediaIds != null && mediaIds.size() > 0) {
            if (mBrowserType == MediaBrowserType.WP_STORIES_MEDIA_PICKER) {
                // TODO WPSTORIES add TRACKS (see how it's tracked below? maybe do along the same lines)
                Intent data = new Intent()
                        .putExtra(MediaBrowserActivity.RESULT_IDS, ListUtils.toLongArray(mediaIds))
                        .putExtra(ARG_BROWSER_TYPE, mBrowserType)
                        .putExtra(EXTRA_MEDIA_SOURCE, source.name());
                setResult(RESULT_OK, data);
                finish();
            } else {
                // if user chose a featured image, track image picked event
                if (mBrowserType == MediaBrowserType.FEATURED_IMAGE_PICKER) {
                    mFeaturedImageHelper.trackFeaturedImageEvent(
                            FeaturedImageHelper.TrackableEvent.IMAGE_PICKED,
                            mLocalPostId
                    );
                }

                Intent data = new Intent()
                        .putExtra(EXTRA_MEDIA_ID, mediaIds.get(0))
                        .putExtra(EXTRA_MEDIA_SOURCE, source.name());
                setResult(RESULT_OK, data);
                finish();
            }
        } else {
            throw new IllegalArgumentException("call to doMediaIdsSelected with null or empty mediaIds array");
        }
    }

    @Override
    public void onPhotoPickerMediaChosen(@NonNull List<? extends Uri> uriList) {
        if (uriList.size() > 0) {
            doMediaUrisSelected(uriList, PhotoPickerMediaSource.APP_PICKER);
        }
    }

    @Override
    public void onPhotoPickerIconClicked(@NonNull PhotoPickerFragment.PhotoPickerIcon icon, boolean multiple) {
        switch (icon) {
            case ANDROID_CAPTURE_PHOTO:
                launchCameraForImage();
                break;
            case ANDROID_CHOOSE_PHOTO:
                launchPictureLibrary(multiple);
                break;
            case ANDROID_CAPTURE_VIDEO:
                launchCameraForVideo();
                break;
            case ANDROID_CHOOSE_VIDEO:
                launchVideoLibrary(multiple);
                break;
            case WP_MEDIA:
                launchWPMediaLibrary();
                break;
            case STOCK_MEDIA:
                launchStockMediaPicker();
                break;
            case WP_STORIES_CAPTURE:
                launchWPStoriesCamera();
                break;
        }
    }

    private String[] convertUrisListToStringArray(List<? extends Uri> uris) {
        String[] stringUris = new String[uris.size()];
        for (int i = 0; i < uris.size(); i++) {
            stringUris[i] = uris.get(i).toString();
        }
        return stringUris;
    }
}
