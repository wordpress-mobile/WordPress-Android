package org.wordpress.android.ui.media;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;

import javax.inject.Inject;

/**
 * An activity where the user can add new images to their media gallery or where the user
 * can choose a single image to embed into their post.
 */
public class MediaGalleryPickerActivity extends AppCompatActivity
        implements MultiChoiceModeListener, ActionMode.Callback, MediaGridAdapter.MediaGridAdapterCallback,
                   AdapterView.OnItemClickListener {
    public static final int REQUEST_CODE = 4000;
    public static final String PARAM_SELECT_ONE_ITEM = "PARAM_SELECT_ONE_ITEM";
    public static final String PARAM_SELECTED_IDS = "PARAM_SELECTED_IDS";
    public static final String RESULT_IDS = "RESULT_IDS";
    public static final String TAG = MediaGalleryPickerActivity.class.getSimpleName();

    private static final String STATE_FILTERED_ITEMS = "STATE_FILTERED_ITEMS";
    private static final String STATE_SELECTED_ITEMS = "STATE_SELECTED_ITEMS";
    private static final String STATE_IS_SELECT_ONE_ITEM = "STATE_IS_SELECT_ONE_ITEM";

    private GridView mGridView;
    private MediaGridAdapter mGridAdapter;
    private ActionMode mActionMode;

    private ArrayList<Long> mFilteredItems;
    private boolean mIsSelectOneItem;
    private boolean mIsFetching;
    private boolean mHasRetrievedAllMedia;

    private SiteModel mSite;

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

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
            ArrayList<Integer> list = ListUtils.fromIntArray(savedInstanceState.getIntArray(STATE_SELECTED_ITEMS));
            selectedItems.addAll(list);
            mFilteredItems = ListUtils.fromLongArray(savedInstanceState.getLongArray(STATE_FILTERED_ITEMS));
            mIsSelectOneItem = savedInstanceState.getBoolean(STATE_IS_SELECT_ONE_ITEM, mIsSelectOneItem);
        }


        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        setContentView(R.layout.media_gallery_picker_layout);
        mGridView = (GridView) findViewById(R.id.media_gallery_picker_gridview);
        mGridView.setMultiChoiceModeListener(this);
        mGridView.setOnItemClickListener(this);
        // TODO: We want to inject the image loader in this class instead of using a static field.
        mGridAdapter = new MediaGridAdapter(this, mSite, WordPress.sImageLoader);
        mGridAdapter.setSelectedItems(selectedItems);
        mGridAdapter.setCallback(this);
        // TODO:
        //mGridView.setAdapter(mGridAdapter);
        if (mIsSelectOneItem) {
            setTitle(R.string.select_from_media_library);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        } else {
            mActionMode = startActionMode(this);
            mActionMode.setTitle(String.format(getString(R.string.cab_selected),
                    mGridAdapter.getSelectedItems().size()));
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
            MediaGridAdapter adapter = (MediaGridAdapter) mGridView.getAdapter();
            mHasRetrievedAllMedia = true;
            adapter.setHasRetrievedAll(true);
            String message = null;
            switch (event.error.type) {
                case GENERIC_ERROR:
                    message = getString(R.string.error_refresh_media);
                    break;
            }

            if (message != null) {
                Toast.makeText(MediaGalleryPickerActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            // the activity may be done by the time we get this, so check for it
            if (!isFinishing()) {
                mGridAdapter.setRefreshing(false);
            }
        } else {
            MediaGridAdapter adapter = (MediaGridAdapter) mGridView.getAdapter();
            mHasRetrievedAllMedia = !event.canLoadMore;
            adapter.setHasRetrievedAll(mHasRetrievedAllMedia);
            if (mMediaStore.getSiteMediaCount(mSite) == 0 && mHasRetrievedAllMedia) {
                // There is no media at all
                noMediaFinish();
            }

            // the activity may be gone by the time this finishes, so check for it
            if (!isFinishing()) {
                mGridAdapter.setRefreshing(false);
                if (mFilteredItems != null && !mFilteredItems.isEmpty()) {
                    Cursor cursor = mMediaStore.getSiteImagesExcludingIdsAsCursor(mSite, mFilteredItems);
                    mGridAdapter.setCursor(cursor);
                } else {
                    Cursor cursor = mMediaStore.getSiteImagesAsCursor(mSite);
                    mGridAdapter.setCursor(cursor);
                }
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mIsSelectOneItem) {
            // Single select, just finish the activity once an item is selected
            mGridAdapter.setItemSelectedByPosition(position, true);
            setResultIdsAndFinish();
        } else {
            mGridAdapter.toggleItemSelected(position);
            mActionMode.setTitle(String.format(getString(R.string.cab_selected),
                    mGridAdapter.getSelectedItems().size()));
        }
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mGridAdapter.setItemSelectedByPosition(position, checked);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        setResultIdsAndFinish();
    }

    @Override
    public void fetchMoreData() {
        if (!mHasRetrievedAllMedia) {
            refreshMediaFromServer(true);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onRetryUpload(int localMediaId) {
    }

    @Override
    public boolean isInMultiSelect() {
        return false;
    }

    private void refreshViews() {
        final Cursor cursor;
        if (mFilteredItems != null) {
            cursor = mMediaStore.getSiteImagesExcludingIdsAsCursor(mSite, mFilteredItems);
        } else {
            cursor = mMediaStore.getAllSiteMediaAsCursor(mSite);
            mGridAdapter.setCursor(cursor);
        }
        if (cursor.getCount() == 0) {
            refreshMediaFromServer(false);
        }
    }

    private void refreshMediaFromServer(boolean loadMore) {
        if (!mIsFetching) {
            mIsFetching = true;
            mGridAdapter.setRefreshing(true);

            FetchMediaListPayload payload = new FetchMediaListPayload(mSite, loadMore);
            mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload));
        }
    }

    private void setResultIdsAndFinish() {
        Intent intent = new Intent();
        if (!mGridAdapter.getSelectedItems().isEmpty()) {
            ArrayList<Long> remoteMediaIds = new ArrayList<>();
            for (Integer localId : mGridAdapter.getSelectedItems()) {
                remoteMediaIds.add(mMediaStore.getMediaWithLocalId(localId).getMediaId());
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
}
