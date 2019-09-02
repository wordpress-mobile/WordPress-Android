package org.wordpress.android.ui.photopicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.posts.FeaturedImageHelper;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.ui.posts.FeaturedImageHelperKt.EMPTY_LOCAL_POST_ID;

public class PhotoPickerActivity extends AppCompatActivity
        implements PhotoPickerFragment.PhotoPickerListener {
    private static final String PICKER_FRAGMENT_TAG = "picker_fragment_tag";
    private static final String KEY_MEDIA_CAPTURE_PATH = "media_capture_path";

    public static final String EXTRA_MEDIA_URI = "media_uri";
    public static final String EXTRA_MEDIA_ID = "media_id";
    public static final String EXTRA_MEDIA_QUEUED = "media_queued";

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
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        setContentView(R.layout.photo_picker_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        if (savedInstanceState == null) {
            mBrowserType = (MediaBrowserType) getIntent().getSerializableExtra(PhotoPickerFragment.ARG_BROWSER_TYPE);
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            mLocalPostId = getIntent().getIntExtra(LOCAL_POST_ID, EMPTY_LOCAL_POST_ID);
        } else {
            mBrowserType = (MediaBrowserType) savedInstanceState.getSerializable(PhotoPickerFragment.ARG_BROWSER_TYPE);
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
        outState.putSerializable(PhotoPickerFragment.ARG_BROWSER_TYPE, mBrowserType);
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
                if (data != null) {
                    Uri imageUri = data.getData();
                    if (imageUri != null) {
                        doMediaUriSelected(imageUri, PhotoPickerMediaSource.ANDROID_PICKER);
                    }
                }
                break;
            // user took a photo with the device camera
            case RequestCodes.TAKE_PHOTO:
                try {
                    WPMediaUtils.scanMediaFile(this, mMediaCapturePath);
                    File f = new File(mMediaCapturePath);
                    Uri capturedImageUri = Uri.fromFile(f);
                    doMediaUriSelected(capturedImageUri, PhotoPickerMediaSource.ANDROID_CAMERA);
                } catch (RuntimeException e) {
                    AppLog.e(AppLog.T.MEDIA, e);
                }
                break;
            // user selected from WP media library, extract the media ID and pass to caller
            case RequestCodes.SINGLE_SELECT_MEDIA_PICKER:
                if (data.hasExtra(MediaBrowserActivity.RESULT_IDS)) {
                    ArrayList<Long> ids =
                            ListUtils.fromLongArray(data.getLongArrayExtra(MediaBrowserActivity.RESULT_IDS));
                    if (ids != null && ids.size() == 1) {
                        doMediaIdSelected(ids.get(0), PhotoPickerMediaSource.WP_MEDIA_PICKER);
                    }
                }
                break;
            // user selected a stock photo
            case RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT:
                if (data != null && data.hasExtra(EXTRA_MEDIA_ID)) {
                    long mediaId = data.getLongExtra(EXTRA_MEDIA_ID, 0);
                    doMediaIdSelected(mediaId, PhotoPickerMediaSource.STOCK_MEDIA_PICKER);
                }
                break;
        }
    }

    private void launchCamera() {
        WPMediaUtils.launchCamera(this, BuildConfig.APPLICATION_ID,
                                  new WPMediaUtils.LaunchCameraCallback() {
                                      @Override
                                      public void onMediaCapturePathReady(String mediaCapturePath) {
                                          mMediaCapturePath = mediaCapturePath;
                                      }
                                  });
    }

    private void launchPictureLibrary() {
        WPMediaUtils.launchPictureLibrary(this, false);
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

    private void doMediaUriSelected(@NonNull Uri mediaUri, @NonNull PhotoPickerMediaSource source) {
        // if user chose a featured image, we need to upload it and return the uploaded media object
        if (mBrowserType == MediaBrowserType.FEATURED_IMAGE_PICKER) {
            final String mimeType = getContentResolver().getType(mediaUri);
            WPMediaUtils.fetchMediaAndDoNext(this, mediaUri,
                                             new WPMediaUtils.MediaFetchDoNext() {
                                                 @Override
                                                 public void doNext(Uri uri) {
                                                     mFeaturedImageHelper
                                                             .queueFeaturedImageForUpload(PhotoPickerActivity.this,
                                                                     mLocalPostId, mSite, uri, mimeType);
                                                     Intent intent = new Intent()
                                                             .putExtra(EXTRA_MEDIA_QUEUED, true);
                                                     setResult(RESULT_OK, intent);
                                                     finish();
                                                 }
                                             });
        } else {
            Intent intent = new Intent()
                    .putExtra(EXTRA_MEDIA_URI, mediaUri.toString())
                    .putExtra(EXTRA_MEDIA_SOURCE, source.name());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void doMediaIdSelected(long mediaId, @NonNull PhotoPickerMediaSource source) {
        Intent data = new Intent()
                .putExtra(EXTRA_MEDIA_ID, mediaId)
                .putExtra(EXTRA_MEDIA_SOURCE, source.name());
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onPhotoPickerMediaChosen(@NonNull List<Uri> uriList) {
        if (uriList.size() > 0) {
            doMediaUriSelected(uriList.get(0), PhotoPickerMediaSource.APP_PICKER);
        }
    }

    @Override
    public void onPhotoPickerIconClicked(@NonNull PhotoPickerFragment.PhotoPickerIcon icon, boolean multiple) {
        switch (icon) {
            case ANDROID_CAPTURE_PHOTO:
                launchCamera();
                break;
            case ANDROID_CHOOSE_PHOTO:
                launchPictureLibrary();
                break;
            case WP_MEDIA:
                launchWPMediaLibrary();
                break;
            case STOCK_MEDIA:
                launchStockMediaPicker();
                break;
        }
    }
}
