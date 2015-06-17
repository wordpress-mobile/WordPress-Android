package org.wordpress.android.ui.media;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.helpers.MediaGallery;
import org.wordpress.android.ui.media.MediaGallerySettingsFragment.MediaGallerySettingsCallback;
import org.wordpress.android.util.DisplayUtils;

import java.util.ArrayList;

/**
 * An activity where the user can manage a media gallery
 */
public class MediaGalleryActivity extends AppCompatActivity implements MediaGallerySettingsCallback {
    public static final int REQUEST_CODE = 3000;

    // params for the gallery
    public static final String PARAMS_MEDIA_GALLERY = "PARAMS_MEDIA_GALLERY";

    // launches media picker in onCreate() if set
    public static final String PARAMS_LAUNCH_PICKER = "PARAMS_LAUNCH_PICKER";

    // result of the gallery
    public static final String RESULT_MEDIA_GALLERY = "RESULT_MEDIA_GALLERY";

    private MediaGalleryEditFragment mMediaGalleryEditFragment;
    private MediaGallerySettingsFragment mMediaGallerySettingsFragment;

    private SlidingUpPanelLayout mSlidingPanelLayout;
    private boolean mIsPanelCollapsed = true;

    private MediaGallery mMediaGallery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(R.string.media_gallery_edit);

        setContentView(R.layout.media_gallery_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }

        FragmentManager fm = getFragmentManager();

        mMediaGallery = (MediaGallery) getIntent().getSerializableExtra(PARAMS_MEDIA_GALLERY);
        if (mMediaGallery == null) {
            mMediaGallery = new MediaGallery();
        }

        mMediaGalleryEditFragment = (MediaGalleryEditFragment) fm.findFragmentById(R.id.mediaGalleryEditFragment);
        mMediaGallerySettingsFragment = (MediaGallerySettingsFragment) fm.findFragmentById(
                R.id.mediaGallerySettingsFragment);
        if (savedInstanceState == null) {
            // if not null, the fragments will remember its state
            mMediaGallerySettingsFragment.setRandom(mMediaGallery.isRandom());
            mMediaGallerySettingsFragment.setNumColumns(mMediaGallery.getNumColumns());
            mMediaGallerySettingsFragment.setType(mMediaGallery.getType());
            mMediaGalleryEditFragment.setMediaIds(mMediaGallery.getIds());
        }

        mSlidingPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.media_gallery_root);
        if (mSlidingPanelLayout != null) {
            // sliding panel layout is on phone only

            mSlidingPanelLayout.setDragView(mMediaGallerySettingsFragment.getDragView());
            mSlidingPanelLayout.setPanelHeight(DisplayUtils.dpToPx(this, 48));
            mSlidingPanelLayout.setPanelSlideListener(new PanelSlideListener() {
                @Override
                public void onPanelSlide(View panel, float slideOffset) {
                }

                @Override
                public void onPanelExpanded(View panel) {
                    mMediaGallerySettingsFragment.onPanelExpanded();
                    mIsPanelCollapsed = false;
                }

                @Override
                public void onPanelCollapsed(View panel) {
                    mMediaGallerySettingsFragment.onPanelCollapsed();
                    mIsPanelCollapsed = true;
                }
            });
        }

        if (getIntent().hasExtra(PARAMS_LAUNCH_PICKER)) {
            handleAddMedia();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.media_gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add_media) {
            handleAddMedia();
            return true;
        } else if (item.getItemId() == R.id.menu_save) {
            handleSaveMedia();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MediaGalleryPickerActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                ArrayList<String> ids = data.getStringArrayListExtra(MediaGalleryPickerActivity.RESULT_IDS);
                mMediaGalleryEditFragment.setMediaIds(ids);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleAddMedia() {
        // need to make MediaGalleryAdd into an activity rather than a fragment because I can't add this fragment
        // on top of the slidingpanel layout (since it needs to be the root layout)

        ArrayList<String> mediaIds = mMediaGalleryEditFragment.getMediaIds();

        Intent intent = new Intent(this, MediaGalleryPickerActivity.class);
        intent.putExtra(MediaGalleryPickerActivity.PARAM_SELECTED_IDS, mediaIds);
        startActivityForResult(intent, MediaGalleryPickerActivity.REQUEST_CODE);
    }

    private void handleSaveMedia() {
        Intent intent = new Intent();
        ArrayList<String> ids = mMediaGalleryEditFragment.getMediaIds();
        boolean isRandom = mMediaGallerySettingsFragment.isRandom();
        int numColumns = mMediaGallerySettingsFragment.getNumColumns();
        String type = mMediaGallerySettingsFragment.getType();

        mMediaGallery.setIds(ids);
        mMediaGallery.setRandom(isRandom);
        mMediaGallery.setNumColumns(numColumns);
        mMediaGallery.setType(type);

        intent.putExtra(RESULT_MEDIA_GALLERY, mMediaGallery);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (mSlidingPanelLayout != null && !mIsPanelCollapsed) {
            mSlidingPanelLayout.collapsePane();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onReverseClicked() {
        mMediaGalleryEditFragment.reverseIds();
    }
}
