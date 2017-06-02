package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.media.MediaBrowserActivity.MediaBrowserType;
import org.wordpress.android.ui.media.MediaGridAdapter.MediaGridAdapterCallback;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;

/**
 * The grid displaying the media items.
 */
@SuppressWarnings("ALL")
public class MediaGridFragment extends Fragment implements MediaGridAdapterCallback {
    private static final String BUNDLE_SELECTED_STATES = "BUNDLE_SELECTED_STATES";
    private static final String BUNDLE_IN_MULTI_SELECT_MODE = "BUNDLE_IN_MULTI_SELECT_MODE";
    private static final String BUNDLE_SCROLL_POSITION = "BUNDLE_SCROLL_POSITION";
    private static final String BUNDLE_HAS_RETRIEVED_ALL_MEDIA = "BUNDLE_HAS_RETRIEVED_ALL_MEDIA";
    private static final String BUNDLE_FILTER = "BUNDLE_FILTER";
    private static final String BUNDLE_EMPTY_VIEW_MESSAGE = "BUNDLE_EMPTY_VIEW_MESSAGE";

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject FluxCImageLoader mImageLoader;

    private Filter mFilter = Filter.ALL;
    private String[] mFiltersText;
    private MediaBrowserType mBrowserType;

    private RecyclerView mRecycler;
    private GridLayoutManager mGridManager;
    private MediaGridAdapter mGridAdapter;
    private MediaGridListener mListener;

    private boolean mIsRefreshing;
    private boolean mHasRetrievedAllMedia;

    private ActionMode mActionMode;
    private String mSearchTerm;

    private TextView mResultView;
    private AppCompatSpinner mSpinner;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;

    private LinearLayout mEmptyView;
    private TextView mEmptyViewTitle;
    private EmptyViewMessageType mEmptyViewMessageType = EmptyViewMessageType.NO_CONTENT;

    private boolean mSpinnerHasLaunched;

    private SiteModel mSite;

    public interface MediaGridListener {
        void onMediaItemSelected(View sourceView, int localMediaId);
        void onRetryUpload(int localMediaId);
    }

    public enum Filter {
        ALL, IMAGES, UNATTACHED;

        public static Filter getFilter(int filterPos) {
            if (filterPos > Filter.values().length)
                return ALL;
            else
                return Filter.values()[filterPos];
        }
    }

    private final OnItemSelectedListener mFilterSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // onItemSelected will be called during initialization, so ignore first call
            if (!mSpinnerHasLaunched) {
                mSpinnerHasLaunched = true;
                return;
            }
            setFilter(Filter.getFilter(position));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
            mBrowserType = (MediaBrowserType) getActivity().getIntent().getSerializableExtra(MediaBrowserActivity.ARG_BROWSER_TYPE);
            boolean imagesOnly = getActivity().getIntent().getBooleanExtra(MediaBrowserActivity.ARG_IMAGES_ONLY, false);
            if (imagesOnly) {
                mFilter = Filter.IMAGES;
            }
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mBrowserType = (MediaBrowserType) savedInstanceState.getSerializable(MediaBrowserActivity.ARG_BROWSER_TYPE);
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSpinnerAdapter();
        refreshMediaFromDB();
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mFiltersText = new String[Filter.values().length];

        View view = inflater.inflate(R.layout.media_grid_fragment, container);

        mRecycler = (RecyclerView) view.findViewById(R.id.recycler);
        mRecycler.setHasFixedSize(true);

        int numColumns = MediaGridAdapter.getColumnCount(getActivity());
        mGridManager = new GridLayoutManager(getActivity(), numColumns);
        mRecycler.setLayoutManager(mGridManager);

        mGridAdapter = new MediaGridAdapter(getActivity(), mSite, mImageLoader);
        mGridAdapter.setCallback(this);
        mGridAdapter.setAllowMultiselect(mBrowserType != MediaBrowserType.SINGLE_SELECT_PICKER);
        mGridAdapter.setShowPreviewIcon(mBrowserType.isPicker());
        mRecycler.setAdapter(mGridAdapter);

        mEmptyView = (LinearLayout) view.findViewById(R.id.empty_view);
        mEmptyViewTitle = (TextView) view.findViewById(R.id.empty_view_title);

        mResultView = (TextView) view.findViewById(R.id.media_filter_result_text);
        mSpinner = (AppCompatSpinner) view.findViewById(R.id.media_filter_spinner);

        // swipe to refresh setup
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(getActivity(),
                (CustomSwipeRefreshLayout) view.findViewById(R.id.ptr_layout), new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!isAdded()) {
                            return;
                        }
                        if (!NetworkUtils.checkConnection(getActivity())) {
                            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
                            mSwipeToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        fetchMediaList(false);
                    }
                });

        restoreState(savedInstanceState);

        // filter spinner doesn't show for pickers
        mSpinner.setVisibility(mBrowserType.isPicker() ? View.GONE : View.VISIBLE);
        if (!mBrowserType.isPicker()) {
            mSpinner.setOnItemSelectedListener(mFilterSelectedListener);
            setupSpinnerAdapter();
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (MediaGridListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MediaGridListener");
        }
    }

    @Override
    public void onAdapterFetchMoreData() {
        if (!mHasRetrievedAllMedia) {
            fetchMediaList(true);
        }
    }

    @Override
    public void onAdapterRetryUpload(int localMediaId) {
        mListener.onRetryUpload(localMediaId);
    }

    @Override
    public void onAdapterItemSelected(View sourceView, int position) {
        int localMediaId = mGridAdapter.getLocalMediaIdAtPosition(position);
        mListener.onMediaItemSelected(sourceView, localMediaId);
    }

    @Override
    public void onAdapterSelectionCountChanged(int count) {
        if (mBrowserType == MediaBrowserType.SINGLE_SELECT_PICKER) {
            return;
        }

        if (count == 0 && mActionMode != null) {
            mActionMode.finish();
        } else if (mActionMode == null) {
            ((AppCompatActivity) getActivity()).startSupportActionMode(new ActionModeCallback());
        }

        setFilterEnabled(count == 0);
        updateActionModeTitle(count);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaListFetched(OnMediaListFetched event) {
        if (event.isError()) {
            handleFetchAllMediaError(event.error.type);
            return;
        }
        handleFetchAllMediaSuccess(event);
    }

    public void refreshSpinnerAdapter() {
        updateFilterText();
        updateSpinnerAdapter();
    }

    public void refreshMediaFromDB() {
        if (!isAdded()) return;

        setFilter(mFilter);
        updateFilterText();
        updateSpinnerAdapter();
        if (mGridAdapter.getItemCount() == 0) {
            if (NetworkUtils.isNetworkAvailable(getActivity())) {
                if (!mHasRetrievedAllMedia) {
                    fetchMediaList(false);
                }
            } else {
                updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            }
        }
    }

    public void search(String searchTerm) {
        mSearchTerm = searchTerm;
        List<MediaModel> mediaList = mMediaStore.searchSiteMediaByTitle(mSite, mSearchTerm);
        mGridAdapter.setMediaList(mediaList);
    }

    public void setFilterEnabled(boolean enabled) {
        if (mSpinner != null) {
            mSpinner.setEnabled(enabled);
        }
    }

    public void setFilter(Filter filter) {
        mFilter = filter;
        List<MediaModel> mediaList = filterItems(mFilter);
        mGridAdapter.setMediaList(mediaList);
        if (mediaList.size() == 0) {
            mResultView.setVisibility(View.GONE);
        } else {
            hideEmptyView();
        }
        // Overwrite the LOADING message
        if (mEmptyViewMessageType == EmptyViewMessageType.LOADING) {
            updateEmptyView(EmptyViewMessageType.NO_CONTENT);
        } else {
            updateEmptyView(mEmptyViewMessageType);
        }
    }

    public void clearSelectedItems() {
        mGridAdapter.clearSelection();
    }

    public void removeFromMultiSelect(int localMediaId) {
        if (mGridAdapter.isInMultiSelect() && mGridAdapter.isItemSelected(localMediaId)) {
            mGridAdapter.removeSelectionByLocalId(localMediaId);
            setFilterEnabled(mGridAdapter.getSelectedItems().size() == 0);
        }
    }

    private void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

    private void setSwipeToRefreshEnabled(boolean enabled) {
        mSwipeToRefreshHelper.setEnabled(enabled);
    }

    public void updateSpinnerAdapter() {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) mSpinner.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void updateFilterText() {
        int countAll = mMediaStore.getAllSiteMedia(mSite).size();
        int countImages = mMediaStore.getSiteImages(mSite).size();
        int countUnattached = mMediaStore.getUnattachedSiteMedia(mSite).size();
        mFiltersText[0] = getResources().getString(R.string.all) + " (" + countAll + ")";
        mFiltersText[1] = getResources().getString(R.string.images) + " (" + countImages + ")";
        mFiltersText[2] = getResources().getString(R.string.unattached) + " (" + countUnattached + ")";
    }

    private List<MediaModel> filterItems(Filter filter) {
        switch (filter) {
            case IMAGES:
                return mMediaStore.getSiteImages(mSite);
            case UNATTACHED:
                return mMediaStore.getUnattachedSiteMedia(mSite);
            default:
                return mMediaStore.getAllSiteMedia(mSite);
        }
    }

    private void updateEmptyView(EmptyViewMessageType emptyViewMessageType) {
        if (mEmptyView != null) {
            if (mGridAdapter.getItemCount() == 0) {
                int stringId = 0;
                switch (emptyViewMessageType) {
                    case LOADING:
                        stringId = R.string.media_fetching;
                        break;
                    case NO_CONTENT:
                        stringId = R.string.media_empty_list;
                        break;
                    case NETWORK_ERROR:
                        stringId = R.string.no_network_message;
                        break;
                    case PERMISSION_ERROR:
                        stringId = R.string.media_error_no_permission;
                        break;
                    case GENERIC_ERROR:
                        stringId = R.string.error_refresh_media;
                        break;
                }

                mEmptyViewTitle.setText(getText(stringId));
                mEmptyViewMessageType = emptyViewMessageType;
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
            }
        }
    }

    private void hideEmptyView() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    private void saveState(Bundle outState) {
        outState.putIntArray(BUNDLE_SELECTED_STATES, ListUtils.toIntArray(mGridAdapter.getSelectedItems()));
        outState.putInt(BUNDLE_SCROLL_POSITION, mGridManager.findFirstCompletelyVisibleItemPosition());
        outState.putBoolean(BUNDLE_HAS_RETRIEVED_ALL_MEDIA, mHasRetrievedAllMedia);
        outState.putBoolean(BUNDLE_IN_MULTI_SELECT_MODE, mGridAdapter.isInMultiSelect());
        outState.putInt(BUNDLE_FILTER, mFilter.ordinal());
        outState.putString(BUNDLE_EMPTY_VIEW_MESSAGE, mEmptyViewMessageType.name());
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(MediaBrowserActivity.ARG_BROWSER_TYPE, mBrowserType);
    }

    private void setupSpinnerAdapter() {
        if (getActivity() == null) {
            return;
        }

        updateFilterText();

        Context context = WPActivityUtils.getThemedContext(getActivity());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_menu_dropdown_item, mFiltersText);
        mSpinner.setAdapter(adapter);
        mSpinner.setSelection(mFilter.ordinal());
    }

    private void updateActionModeTitle(int selectCount) {
        if (mActionMode != null) {
            mActionMode.setTitle(String.format(getString(R.string.cab_selected), selectCount));
        }
    }

    private void handleMultiSelectDelete() {
        if (!isAdded()) {
            return;
        }
        Builder builder = new AlertDialog.Builder(getActivity()).setMessage(R.string.confirm_delete_multi_media)
                                                                .setCancelable(true).setPositiveButton(
                        R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (getActivity() instanceof MediaBrowserActivity) {
                                    ((MediaBrowserActivity) getActivity()).deleteMedia(
                                            mGridAdapter.getSelectedItems());
                                }
                                // update upload state
                                for (int itemId : mGridAdapter.getSelectedItems()) {
                                    MediaModel media = mMediaStore.getMediaWithLocalId(itemId);
                                    media.setUploadState(MediaUploadState.DELETING.name());
                                    mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
                                }
                                mGridAdapter.clearSelection();
                                if (mActionMode != null) {
                                    mActionMode.finish();
                                }
                            }
                        }).setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;

        boolean isInMultiSelectMode = savedInstanceState.getBoolean(BUNDLE_IN_MULTI_SELECT_MODE);
        if (isInMultiSelectMode) {
            mGridAdapter.setInMultiSelect(true);
            if (savedInstanceState.containsKey(BUNDLE_SELECTED_STATES)) {
                ArrayList<Integer> selectedItems = ListUtils.fromIntArray(savedInstanceState.getIntArray(BUNDLE_SELECTED_STATES));
                mGridAdapter.setSelectedItems(selectedItems);
                setFilterEnabled(mGridAdapter.getSelectedItems().size() == 0);
                mSwipeToRefreshHelper.setEnabled(false);
            }
        }

        mFilter = Filter.getFilter(savedInstanceState.getInt(BUNDLE_FILTER));
        mHasRetrievedAllMedia = savedInstanceState.getBoolean(BUNDLE_HAS_RETRIEVED_ALL_MEDIA, false);
        mEmptyViewMessageType = EmptyViewMessageType.getEnumFromString(savedInstanceState.
                getString(BUNDLE_EMPTY_VIEW_MESSAGE));
    }

    private void fetchMediaList(boolean loadMore) {
        // do not refresh if there is no network
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            setRefreshing(false);
            return;
        }

        // do not refresh if in search
        if (mSearchTerm != null && mSearchTerm.length() > 0) {
            setRefreshing(false);
            return;
        }

        if (!mIsRefreshing) {
            mIsRefreshing = true;
            updateEmptyView(EmptyViewMessageType.LOADING);
            setRefreshing(true);

            FetchMediaListPayload payload = new FetchMediaListPayload(mSite, loadMore);
            mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload));
        }
    }

    private void handleFetchAllMediaSuccess(OnMediaListFetched event) {
        MediaGridAdapter adapter = (MediaGridAdapter) mRecycler.getAdapter();

        List<MediaModel> mediaList = mMediaStore.getAllSiteMedia(mSite);
        adapter.setMediaList(mediaList);

        mHasRetrievedAllMedia = !event.canLoadMore;
        adapter.setHasRetrievedAll(mHasRetrievedAllMedia);

        mIsRefreshing = false;

        // the activity may be gone by the time this finishes, so check for it
        if (getActivity() != null && isVisible()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshSpinnerAdapter();
                    updateEmptyView(EmptyViewMessageType.NO_CONTENT);
                    mSwipeToRefreshHelper.setRefreshing(false);
                }
            });
        }
    }

    private void handleFetchAllMediaError(MediaErrorType errorType) {
        AppLog.e(AppLog.T.MEDIA, "Media error occurred: " + errorType);
        final boolean isPermissionError = (errorType == MediaErrorType.AUTHORIZATION_REQUIRED);
        if (getActivity() != null) {
            if (!isPermissionError) {
                ToastUtils.showToast(getActivity(), getString(R.string.error_refresh_media),
                        Duration.LONG);
            } else {
                if (mEmptyView == null || mEmptyView.getVisibility() != View.VISIBLE) {
                    ToastUtils.showToast(getActivity(), getString(
                            R.string.media_error_no_permission));
                }
            }
        }
        MediaGridAdapter adapter = (MediaGridAdapter) mRecycler.getAdapter();
        mHasRetrievedAllMedia = true;
        adapter.setHasRetrievedAll(true);

        // the activity may be gone by the time we get this, so check for it
        if (getActivity() != null && MediaGridFragment.this.isVisible()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mIsRefreshing = false;
                    mSwipeToRefreshHelper.setRefreshing(false);
                    if (isPermissionError) {
                        updateEmptyView(EmptyViewMessageType.PERMISSION_ERROR);
                    } else {
                        updateEmptyView(EmptyViewMessageType.GENERIC_ERROR);
                    }
                }
            });
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
            intent.putExtra(MediaBrowserActivity.RESULT_IDS, ListUtils.toLongArray(remoteMediaIds));
        }
        getActivity().setResult(RESULT_OK, intent);
        getActivity().finish();
    }


    private final class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            int selectCount = mGridAdapter.getSelectedItemCount();
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.media_multiselect, menu);
            setSwipeToRefreshEnabled(false);
            mGridAdapter.setInMultiSelect(true);
            updateActionModeTitle(selectCount);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            MenuItem mnuTrash = menu.findItem(R.id.media_multiselect_actionbar_trash);
            mnuTrash.setVisible(!mBrowserType.isPicker());

            MenuItem mnuConfirm = menu.findItem(R.id.mnu_confirm_selection);
            mnuConfirm.setVisible(mBrowserType.isPicker());

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.media_multiselect_actionbar_trash) {
                handleMultiSelectDelete();
            } else if (item.getItemId() == R.id.mnu_confirm_selection) {
                setResultIdsAndFinish();
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            setSwipeToRefreshEnabled(true);
            mGridAdapter.setInMultiSelect(false);
            mActionMode = null;
            setFilterEnabled(mGridAdapter.getSelectedItems().size() == 0);
        }
    }
}
