package org.wordpress.android.ui.media;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.MultiSelectGridView;
import org.wordpress.android.ui.MultiSelectGridView.MultiSelectListener;
import org.wordpress.android.util.ToastUtils;
import org.xmlrpc.android.ApiHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity where the user can add new images to their media gallery or where the user
 * can choose a single image to embed into their post.
 */
public class MediaGalleryPickerActivity extends Activity
        implements MultiSelectListener, ActionMode.Callback, MediaGridAdapter.MediaGridAdapterCallback,
                   AdapterView.OnItemClickListener {
    private MultiSelectGridView mGridView;
    private MediaGridAdapter mGridAdapter;
    private ActionMode mActionMode;

    private ArrayList<String> mFilteredItems;
    private boolean mIsSelectOneItem;
    private boolean mIsRefreshing;
    private boolean mHasRetrievedAllMedia;

    private static final String STATE_FILTERED_ITEMS = "STATE_FILTERED_ITEMS";
    private static final String STATE_SELECTED_ITEMS = "STATE_SELECTED_ITEMS";
    private static final String STATE_IS_SELECT_ONE_ITEM = "STATE_IS_SELECT_ONE_ITEM";

    public static final int REQUEST_CODE = 4000;
    public static final String PARAM_SELECT_ONE_ITEM = "PARAM_SELECT_ONE_ITEM";
    private static final String PARAM_FILTERED_IDS = "PARAM_FILTERED_IDS";
    public static final String PARAM_SELECTED_IDS = "PARAM_SELECTED_IDS";
    public static final String RESULT_IDS = "RESULT_IDS";
    public static final String TAG = MediaGalleryPickerActivity.class.getSimpleName();

    private int mOldMediaSyncOffset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<String> checkedItems = new ArrayList<String>();
        mFilteredItems = getIntent().getStringArrayListExtra(PARAM_FILTERED_IDS);
        mIsSelectOneItem = getIntent().getBooleanExtra(PARAM_SELECT_ONE_ITEM, false);

        ArrayList<String> prevSelectedItems = getIntent().getStringArrayListExtra(PARAM_SELECTED_IDS);
        if( prevSelectedItems != null )
            checkedItems.addAll(prevSelectedItems);

        if (savedInstanceState != null) {
            checkedItems.addAll(savedInstanceState.getStringArrayList(STATE_SELECTED_ITEMS));
            mFilteredItems = savedInstanceState.getStringArrayList(STATE_FILTERED_ITEMS);
            mIsSelectOneItem = savedInstanceState.getBoolean(STATE_IS_SELECT_ONE_ITEM, mIsSelectOneItem);
        }

        setContentView(R.layout.media_gallery_picker_layout);
        mGridView = (MultiSelectGridView) findViewById(R.id.media_gallery_picker_gridview);
        mGridView.setMultiSelectListener(this);
        if (mIsSelectOneItem) {
            mGridView.setOnItemClickListener(this);
            setTitle(R.string.select_from_media_library);
            mGridView.setHighlightSelectModeEnabled(false);
            mGridView.setMultiSelectModeEnabled(false);
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        } else {
            mActionMode = startActionMode(this);
            mActionMode.setTitle(checkedItems.size() + " selected");
            mGridView.setMultiSelectModeActive(true);
        }
        mGridAdapter = new MediaGridAdapter(this, null, 0, checkedItems, MediaImageLoader.getInstance());
        mGridAdapter.setCallback(this);
        mGridView.setAdapter(mGridAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshViews();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_SELECTED_ITEMS, mGridAdapter.getCheckedItems());
        outState.putStringArrayList(STATE_FILTERED_ITEMS, mFilteredItems);
        outState.putBoolean(STATE_IS_SELECT_ONE_ITEM, mIsSelectOneItem);
    }

    private void refreshViews() {
        if (WordPress.getCurrentBlog() == null)
            return;
        final String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
        Cursor cursor = WordPress.wpDB.getMediaImagesForBlog(blogId, mFilteredItems);
        if (cursor.getCount() == 0) {
            refreshMediaFromServer(0);
        } else {
            mGridAdapter.swapCursor(cursor);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED, new Intent());
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMultiSelectChange(int count) {
        mActionMode.setTitle(count + " selected");
        // stay always in multi-select mode, even when count reaches 0
        if (count == 0 && !mIsSelectOneItem)
            mGridView.setMultiSelectModeActive(true);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Single select, just finish the activity once an item is selected
        Intent intent = new Intent();
        intent.putExtra(RESULT_IDS, mGridAdapter.getCheckedItems());
        setResult(RESULT_OK, intent);
        finish();
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
    public void onDestroyActionMode(ActionMode mode) {
        Intent intent = new Intent();
        intent.putExtra(RESULT_IDS, mGridAdapter.getCheckedItems());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void fetchMoreData(int offset) {
        if (!mHasRetrievedAllMedia)
            refreshMediaFromServer(offset);
    }

    @Override
    public void onRetryUpload(String mediaId) {
    }

    @Override
    public boolean isInMultiSelect() {
        return false;
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

    void refreshMediaFromServer(int offset) {
        if (offset == 0 || !mIsRefreshing) {
            if (offset == mOldMediaSyncOffset) {
                // we're pulling the same data again for some reason. Pull from the beginning.
                offset = 0;
            }
            mOldMediaSyncOffset = offset;
            mIsRefreshing = true;
            mGridAdapter.setRefreshing(true);

            List<Object> apiArgs = new ArrayList<Object>();
            apiArgs.add(WordPress.getCurrentBlog());

            ApiHelper.SyncMediaLibraryTask.Callback callback = new ApiHelper.SyncMediaLibraryTask.Callback() {
                // refersh db from server. If returned count is 0, we've retrieved all the media.
                // stop retrieving until the user manually refreshes

                @Override
                public void onSuccess(int count) {
                    MediaGridAdapter adapter = (MediaGridAdapter) mGridView.getAdapter();
                    mHasRetrievedAllMedia = (count == 0);
                    adapter.setHasRetrievedAll(mHasRetrievedAllMedia);
                    String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
                    if (WordPress.wpDB.getMediaCountAll(blogId) == 0 && count == 0) {
                        // There is no media at all
                        noMediaFinish();
                    }
                    mIsRefreshing = false;

                    // the activity may be gone by the time this finishes, so check for it
                    if (!isFinishing()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //mListener.onMediaItemListDownloaded();
                                mGridAdapter.setRefreshing(false);
                                String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
                                Cursor cursor = WordPress.wpDB.getMediaImagesForBlog(blogId, mFilteredItems);
                                mGridAdapter.swapCursor(cursor);

                            }
                        });
                    }
                }

                @Override
                public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                    if (errorType != ApiHelper.ErrorType.NO_ERROR) {
                        String message = errorType == ApiHelper.ErrorType.NO_UPLOAD_FILES_CAP
                                ? getString(R.string.media_error_no_permission)
                                : getString(R.string.error_refresh_media);
                        Toast.makeText(MediaGalleryPickerActivity.this, message, Toast.LENGTH_SHORT).show();
                        MediaGridAdapter adapter = (MediaGridAdapter) mGridView.getAdapter();
                        mHasRetrievedAllMedia = true;
                        adapter.setHasRetrievedAll(mHasRetrievedAllMedia);
                    }

                    // the activity may be cone by the time we get this, so check for it
                    if (!isFinishing()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mIsRefreshing = false;
                                mGridAdapter.setRefreshing(false);
                            }
                        });
                    }

                }
            };

            ApiHelper.SyncMediaLibraryTask getMediaTask = new ApiHelper.SyncMediaLibraryTask(offset, MediaGridFragment.Filter.ALL, callback);
            getMediaTask.execute(apiArgs);
        }
    }
}
