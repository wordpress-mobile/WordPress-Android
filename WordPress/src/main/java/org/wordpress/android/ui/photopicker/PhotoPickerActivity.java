package org.wordpress.android.ui.photopicker;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.wordpress.android.R;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerOption;

import java.util.EnumSet;
import java.util.List;

public class PhotoPickerActivity extends AppCompatActivity
        implements PhotoPickerFragment.PhotoPickerListener {

    private static final String PICKER_FRAGMENT_TAG = "picker_fragment_tag";

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
            EnumSet<PhotoPickerOption> options = EnumSet.noneOf(PhotoPickerOption.class);

            Fragment fragment = PhotoPickerFragment.newInstance(this, options);
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment, PICKER_FRAGMENT_TAG)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commitAllowingStateLoss();
        }
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

    }

    @Override
    public void onPhotoPickerIconClicked(@NonNull PhotoPickerFragment.PhotoPickerIcon icon) {

    }
}
