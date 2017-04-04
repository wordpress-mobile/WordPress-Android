package org.wordpress.android.ui.photopicker;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.WordPressMediaUtils;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerOption;
import org.wordpress.android.util.AppLog;
import org.wordpress.passcodelock.AppLockManager;

import java.io.File;
import java.util.EnumSet;
import java.util.List;

public class PhotoPickerActivity extends AppCompatActivity
        implements PhotoPickerFragment.PhotoPickerListener {

    private static final String PICKER_FRAGMENT_TAG = "picker_fragment_tag";
    private static final String KEY_MEDIA_CAPTURE_PATH = "media_capture_path";

    // the uri of the selected image will be returned as a string in EXTRA_MEDIA_URI
    public static final String EXTRA_MEDIA_URI = "picker_media_uri";

    // the enum name of the source will be returned as a string in EXTRA_MEDIA_SOURCE
    public static final String EXTRA_MEDIA_SOURCE = "media_source";

    private String mMediaCapturePath;

    public enum PhotoPickerMediaSource {
        ANDROID_CAMERA,
        ANDROID_PICKER,
        APP_PICKER;

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
        setContentView(R.layout.photo_picker_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        // only show photos (no videos) and only from device (no wp media)
        EnumSet<PhotoPickerOption> options = EnumSet.of(
                PhotoPickerOption.PHOTOS_ONLY,
                PhotoPickerOption.DEVICE_ONLY);
        PhotoPickerFragment fragment = getPickerFragment();
        if (fragment == null) {
            fragment = PhotoPickerFragment.newInstance(this, options);
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, PICKER_FRAGMENT_TAG)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commitAllowingStateLoss();
        } else {
            fragment.setOptions(options);
            fragment.setPhotoPickerListener(this);
        }
    }

    private PhotoPickerFragment getPickerFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(PICKER_FRAGMENT_TAG);
        if (fragment != null) {
            return (PhotoPickerFragment) fragment;
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
                    pictureSelected(imageUri, PhotoPickerMediaSource.ANDROID_PICKER);
                }
                break;
            // user took a photo with the device camera
            case RequestCodes.TAKE_PHOTO:
                try {
                    File f = new File(mMediaCapturePath);
                    Uri capturedImageUri = Uri.fromFile(f);
                    pictureSelected(capturedImageUri, PhotoPickerMediaSource.ANDROID_CAMERA);
                } catch (RuntimeException e) {
                    AppLog.e(AppLog.T.MEDIA, e);
                }
                break;
        }
    }

    private void launchCamera() {
        WordPressMediaUtils.launchCamera(this, BuildConfig.APPLICATION_ID,
                new WordPressMediaUtils.LaunchCameraCallback() {
                    @Override
                    public void onMediaCapturePathReady(String mediaCapturePath) {
                        mMediaCapturePath = mediaCapturePath;
                        AppLockManager.getInstance().setExtendedTimeout();
                    }
                });
    }

    private void launchPictureLibrary() {
        WordPressMediaUtils.launchPictureLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void pictureSelected(@NonNull Uri mediaUri, @NonNull PhotoPickerMediaSource source) {
        Intent intent = new Intent()
                .putExtra(EXTRA_MEDIA_URI, mediaUri.toString())
                .putExtra(EXTRA_MEDIA_SOURCE, source.name());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onPhotoPickerMediaChosen(@NonNull List<Uri> uriList) {
        if (uriList.size() > 0) {
            pictureSelected(uriList.get(0), PhotoPickerMediaSource.APP_PICKER);
        }
    }

    @Override
    public void onPhotoPickerIconClicked(@NonNull PhotoPickerFragment.PhotoPickerIcon icon) {
        switch (icon) {
            case ANDROID_CAMERA:
                launchCamera();
                break;
            case ANDROID_PICKER:
                launchPictureLibrary();
                break;
        }
    }


}
