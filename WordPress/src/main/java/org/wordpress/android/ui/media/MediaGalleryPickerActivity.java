package org.wordpress.android.ui.media;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * An activity where the user can add new images to their media gallery or where the user
 * can choose a single image to embed into their post.
 */
public class MediaGalleryPickerActivity extends AppCompatActivity
        implements MediaGridAdapter.MediaGridAdapterCallback {

    public static final int REQUEST_CODE = 4000;
    public static final String PARAM_SELECT_ONE_ITEM = "PARAM_SELECT_ONE_ITEM";
    public static final String PARAM_SELECTED_IDS = "PARAM_SELECTED_IDS";
    public static final String RESULT_IDS = "RESULT_IDS";
    public static final String TAG = MediaGalleryPickerActivity.class.getSimpleName();

    private static final String STATE_FILTERED_ITEMS = "STATE_FILTERED_ITEMS";
    private static final String STATE_SELECTED_ITEMS = "STATE_SELECTED_ITEMS";
    private static final String STATE_IS_SELECT_ONE_ITEM = "STATE_IS_SELECT_ONE_ITEM";

    private RecyclerView mRecycler;
    private MediaGridAdapter mGridAdapter;
    private GridLayoutManager mGridManager;
    private ActionMode mActionMode;

    private ArrayList<Long> mFilteredItems;
    private boolean mIsSelectOneItem;
    private boolean mIsFetching;
    private boolean mHasRetrievedAllMedia;

    private SiteModel mSite;

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject FluxCImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        ArrayList<Integer> selectedItems = new ArrayList<>();
        mIsSelectOneItem = getIntent().getBooleanExtra(PARAM_SELECT_ONE_ITEM, false);

        ArrayList<Integer> prevSelectedItems = ListUtils.fromIntArray(getIntent().getIntArrayExtra(PARAM_SELECTED_IDS));
        if (prevSelectedItems != null) {
            selectedItems.addAll(prevSelectedItems);
        }

        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mIsSelectOneItem = savedInstanceState.getBoolean(STATE_IS_SELECT_ONE_ITEM, mIsSelectOneItem);
            if (savedInstanceState.containsKey(STATE_SELECTED_ITEMS)) {
                ArrayList<Integer> list = ListUtils.fromIntArray(savedInstanceState.getIntArray(STATE_SELECTED_ITEMS));
                selectedItems.addAll(list);
            }
            if (savedInstanceState.containsKey(STATE_FILTERED_ITEMS)) {
                mFilteredItems = ListUtils.fromLongArray(savedInstanceState.getLongArray(STATE_FILTERED_ITEMS));
            }
        } else {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        setContentView(R.layout.media_gallery_picker_layout);
        mRecycler = (RecyclerView) findViewById(R.id.recycler);

        int numColumns = MediaGridAdapter.getColumnCount(this);
        mGridManager = new GridLayoutManager(this, numColumns);
        mRecycler.setLayoutManager(mGridManager);

        mGridAdapter = new MediaGridAdapter(this, mSite, mImageLoader);
        mGridAdapter.setCallback(this);

        mRecycler.setAdapter(mGridAdapter);

        if (mIsSelectOneItem) {
            mGridAdapter.setAllowMultiselect(false);
            setTitle(R.string.select_from_media_library);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        } else {
            mGridAdapter.setAllowMultiselect(true);
            mGridAdapter.setInMultiSelect(true);
            mGridAdapter.setSelectedItems(selectedItems);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED, new Intent());
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(STATE_SELECTED_ITEMS, ListUtils.toIntArray(mGridAdapter.getSelectedItems()));
        outState.putLongArray(STATE_FILTERED_ITEMS, ListUtils.toLongArray(mFilteredItems));
        outState.putBoolean(STATE_IS_SELECT_ONE_ITEM, mIsSelectOneItem);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaListFetched(OnMediaListFetched event) {
        mIsFetching = false;
        if (event.isError()) {
            mHasRetrievedAllMedia = true;
            mGridAdapter.setHasRetrievedAll(true);
            String message = null;
            switch (event.error.type) {
                case GENERIC_ERROR:
                    message = getString(R.string.error_refresh_media);
                    break;
            }

            if (message != null) {
                Toast.makeText(MediaGalleryPickerActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        } else {
            mHasRetrievedAllMedia = !event.canLoadMore;
            mGridAdapter.setHasRetrievedAll(mHasRetrievedAllMedia);
            if (mMediaStore.getSiteMediaCount(mSite) == 0 && mHasRetrievedAllMedia) {
                // There is no media at all
                noMediaFinish();
            }

            // the activity may be gone by the time this finishes, so check for it
            if (!isFinishing()) {
                List<MediaModel> mediaList;
                if (mFilteredItems != null && !mFilteredItems.isEmpty()) {
                    mediaList = mMediaStore.getSiteImagesExcludingIds(mSite, mFilteredItems);
                } else {
                    mediaList = mMediaStore.getSiteImages(mSite);
                }
                mGridAdapter.setMediaList(mediaList);
            }
        }
    }

    @Override
    public void onAdapterFetchMoreData() {
        if (!mHasRetrievedAllMedia) {
            refreshMediaFromServer(true);
        }
    }

    @Override
    public void onAdapterRetryUpload(int localMediaId) {
    }

    @Override
    public void onAdapterItemSelected(View sourceView, int position) {
        if (mIsSelectOneItem) {
            // Single select, just finish the activity once an item is selected
            Intent intent = new Intent();
            int localId = mGridAdapter.getLocalMediaIdAtPosition(position);
            ArrayList<Long> remoteMediaIds = new ArrayList<>();
            MediaModel media = mMediaStore.getMediaWithLocalId(localId);
            if (media != null) {
                remoteMediaIds.add(media.getMediaId());
                intent.putExtra(RESULT_IDS, ListUtils.toLongArray(remoteMediaIds));
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }

    @Override
    public void onAdapterSelectionCountChanged(int count) {
        if (count == 0 && mActionMode != null) {
            mActionMode.finish();
        } else if (mActionMode == null) {
            startActionMode(new ActionModeCallback());
        }

        updateActionModeTitle(count);
    }

    private void updateActionModeTitle(int count) {
        if (mActionMode != null) {
            mActionMode.setTitle(String.format(getString(R.string.cab_selected), count));
        }
    }

    private void refreshViews() {
        List<MediaModel> mediaList;
        if (mFilteredItems != null) {
            mediaList = mMediaStore.getSiteImagesExcludingIds(mSite, mFilteredItems);
        } else {
            mediaList = mMediaStore.getAllSiteMedia(mSite);
        }
        mGridAdapter.setMediaList(mediaList);
        if (mediaList.size() == 0) {
            refreshMediaFromServer(false);
        }
    }

    private void refreshMediaFromServer(boolean loadMore) {
        if (!mIsFetching) {
            mIsFetching = true;

            FetchMediaListPayload payload = new FetchMediaListPayload(mSite, loadMore);
            mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload));
        }
    }

    private void setResultIdsAndFinish() {
        Intent intent = new Intent();
        if (mGridAdapter.getSelectedItemCount() > 0) {
            ArrayList<Long> remoteMediaIds = new ArrayList<>();
            for (Integer localId : mGridAdapter.getSelectedItems()) {
                MediaModel media = mMediaStore.getMediaWithLocalId(localId);
                if (media != null) {
                    remoteMediaIds.add(media.getMediaId());
                }
            }
            intent.putExtra(RESULT_IDS, ListUtils.toLongArray(remoteMediaIds));
        }
        setResult(RESULT_OK, intent);
        finish();
    }

    private void noMediaFinish() {
        ToastUtils.showToast(this, R.string.media_empty_list, ToastUtils.Duration.LONG);
        // Delay activity finish
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 1500);
    }

    private final class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            updateActionModeTitle(mGridAdapter.getSelectedItemCount());
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            setResultIdsAndFinish();
        }
    }
}
