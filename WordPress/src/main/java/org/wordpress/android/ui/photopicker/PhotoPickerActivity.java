package org.wordpress.android.ui.photopicker;

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
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerOption;

import java.util.EnumSet;
import java.util.List;

public class PhotoPickerActivity extends AppCompatActivity
        implements PhotoPickerFragment.PhotoPickerListener {

    private static final String PICKER_FRAGMENT_TAG = "picker_fragment_tag";
    public static final String EXTRA_MEDIA_URI = "picker_media_uri";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_picker_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        if (!hasPhotoPickerFragment()) {
            EnumSet<PhotoPickerOption> options = EnumSet.of(PhotoPickerOption.PHOTOS_ONLY);

            Fragment fragment = PhotoPickerFragment.newInstance(this, options);
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, PICKER_FRAGMENT_TAG)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commitAllowingStateLoss();
        }
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

    private PhotoPickerFragment getPickerFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(PICKER_FRAGMENT_TAG);
        if (fragment != null) {
            return (PhotoPickerFragment) fragment;
        }
        return null;
    }

    private boolean hasPhotoPickerFragment() {
        return getPickerFragment() != null;
    }

    @Override
    public void onPhotoPickerMediaChosen(@NonNull List<Uri> uriList) {
        if (uriList.size() > 0) {
            Uri mediaUri = uriList.get(0);
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_MEDIA_URI, mediaUri.toString()));
            finish();
        }
    }

    @Override
    public void onPhotoPickerIconClicked(@NonNull PhotoPickerFragment.PhotoPickerIcon icon) {
        // TODO
    }
}
