package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.wellsql.generated.MediaModelTable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaFilter;
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.MediaListPayload;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.CheckableFrameLayout;
import org.wordpress.android.ui.CustomSpinner;
import org.wordpress.android.ui.EmptyViewMessageType;
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import javax.inject.Inject;

/**
 * The grid displaying the media items.
 */
public class MediaGridFragment extends Fragment
        implements OnItemClickListener, MediaGridAdapterCallback, RecyclerListener, GridView.MultiChoiceModeListener {
    private static final String BUNDLE_SELECTED_STATES = "BUNDLE_SELECTED_STATES";
    private static final String BUNDLE_IN_MULTI_SELECT_MODE = "BUNDLE_IN_MULTI_SELECT_MODE";
    private static final String BUNDLE_SCROLL_POSITION = "BUNDLE_SCROLL_POSITION";
    private static final String BUNDLE_HAS_RETRIEVED_ALL_MEDIA = "BUNDLE_HAS_RETRIEVED_ALL_MEDIA";
    private static final String BUNDLE_FILTER = "BUNDLE_FILTER";
    private static final String BUNDLE_EMPTY_VIEW_MESSAGE = "BUNDLE_EMPTY_VIEW_MESSAGE";
    private static final String BUNDLE_FETCH_OFFSET = "BUNDLE_FETCH_OFFSET";

    private static final String BUNDLE_DATE_FILTER_SET = "BUNDLE_DATE_FILTER_SET";
    private static final String BUNDLE_DATE_FILTER_VISIBLE = "BUNDLE_DATE_FILTER_VISIBLE";
    private static final String BUNDLE_DATE_FILTER_START_YEAR = "BUNDLE_DATE_FILTER_START_YEAR";
    private static final String BUNDLE_DATE_FILTER_START_MONTH = "BUNDLE_DATE_FILTER_START_MONTH";
    private static final String BUNDLE_DATE_FILTER_START_DAY = "BUNDLE_DATE_FILTER_START_DAY";
    private static final String BUNDLE_DATE_FILTER_END_YEAR = "BUNDLE_DATE_FILTER_END_YEAR";
    private static final String BUNDLE_DATE_FILTER_END_MONTH = "BUNDLE_DATE_FILTER_END_MONTH";
    private static final String BUNDLE_DATE_FILTER_END_DAY = "BUNDLE_DATE_FILTER_END_DAY";

    private static final int NUM_PER_FETCH = 20;

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    private Filter mFilter = Filter.ALL;
    private String[] mFiltersText;
    private GridView mGridView;
    private MediaGridAdapter mGridAdapter;
    private MediaGridListener mListener;

    private boolean mIsRefreshing;
    private boolean mHasRetrievedAllMedia;
    private boolean mIsMultiSelect;
    private ActionMode mActionMode;
    private String mSearchTerm;

    private View mSpinnerContainer;
    private TextView mResultView;
    private CustomSpinner mSpinner;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;

    private LinearLayout mEmptyView;
    private TextView mEmptyViewTitle;
    private EmptyViewMessageType mEmptyViewMessageType = EmptyViewMessageType.NO_CONTENT;

    private int mOldMediaSyncOffset = 0;

    private boolean mIsDateFilterSet;
    private boolean mSpinnerHasLaunched;

    private int mStartYear, mStartMonth, mStartDay, mEndYear, mEndMonth, mEndDay;
    private AlertDialog mDatePickerDialog;

    private MenuItem mNewPostButton;
    private MenuItem mNewGalleryButton;

    private SiteModel mSite;

    private int mMediaFetchOffset = 0;

    public interface MediaGridListener {
        void onMediaItemSelected(long mediaId);
        void onRetryUpload(long mediaId);
    }

    public enum Filter {
        ALL, IMAGES, UNATTACHED, CUSTOM_DATE;

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
            if (position == Filter.CUSTOM_DATE.ordinal()) {
                mIsDateFilterSet = true;
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
            if (getArguments() != null) {
                mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
            } else {
                mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
            }
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
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
        // TODO: We want to inject the image loader in this class instead of using a static field.
        mGridAdapter = new MediaGridAdapter(getActivity(), mSite, null, 0, WordPress.sImageLoader);
        mGridAdapter.setCallback(this);

        View view = inflater.inflate(R.layout.media_grid_fragment, container);

        mGridView = (GridView) view.findViewById(R.id.media_gridview);
        mGridView.setOnItemClickListener(this);
        mGridView.setRecyclerListener(this);
        mGridView.setMultiChoiceModeListener(this);
        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setAdapter(mGridAdapter);

        mEmptyView = (LinearLayout) view.findViewById(R.id.empty_view);
        mEmptyViewTitle = (TextView) view.findViewById(R.id.empty_view_title);

        mResultView = (TextView) view.findViewById(R.id.media_filter_result_text);

        mSpinnerContainer = view.findViewById(R.id.media_filter_spinner_container);
        mSpinner = (CustomSpinner) view.findViewById(R.id.media_filter_spinner);
        mSpinner.setOnItemSelectedListener(mFilterSelectedListener);
        mSpinner.setOnItemSelectedEvenIfUnchangedListener(mFilterSelectedListener);

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
                        fetchAllMedia(0);
                    }
                });
        restoreState(savedInstanceState);
        setupSpinnerAdapter();

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
    public void fetchMoreData(int offset) {
        if (!mHasRetrievedAllMedia) {
            fetchAllMedia(offset);
        }
    }

    @Override
    public void onMovedToScrapHeap(View view) {
        // cancel image fetch requests if the view has been moved to recycler.

        View imageView = view.findViewById(R.id.media_grid_item_image);
        if (imageView != null) {
            // this tag is set in the MediaGridAdapter class
            String tag = (String) imageView.getTag();
            if (tag != null && tag.startsWith("http")) {
                // need a listener to cancel request, even if the listener does nothing
                ImageContainer container = WordPress.sImageLoader.get(tag, new ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) { }

                    @Override
                    public void onResponse(ImageContainer response, boolean isImmediate) { }

                });
                container.cancelRequest();
            }
        }

        CheckableFrameLayout layout = (CheckableFrameLayout) view.findViewById(R.id.media_grid_frame_layout);
        if (layout != null) {
            layout.setOnCheckedChangeListener(null);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mActionMode = mode;
        int selectCount = mGridAdapter.getSelectedItems().size();
        mode.setTitle(String.format(getString(R.string.cab_selected), selectCount));
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.media_multiselect, menu);
        mNewPostButton = menu.findItem(R.id.media_multiselect_actionbar_post);
        mNewGalleryButton = menu.findItem(R.id.media_multiselect_actionbar_gallery);
        setSwipeToRefreshEnabled(false);
        mIsMultiSelect = true;
        updateActionButtons(selectCount);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.media_multiselect_actionbar_post) {
            handleNewPost();
            return true;
        } else if (i == R.id.media_multiselect_actionbar_gallery) {
            handleMultiSelectPost();
            return true;
        } else if (i == R.id.media_multiselect_actionbar_trash) {
            handleMultiSelectDelete();
            return true;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mGridAdapter.clearSelection();
        setSwipeToRefreshEnabled(true);
        mIsMultiSelect = false;
        mActionMode = null;
        setFilterSpinnerVisible(mGridAdapter.getSelectedItems().size() == 0);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mGridAdapter.setItemSelected(position, checked);
        int selectCount = mGridAdapter.getSelectedItems().size();
        setFilterSpinnerVisible(selectCount == 0);
        mode.setTitle(String.format(getString(R.string.cab_selected), selectCount));
        updateActionButtons(selectCount);
    }

    @Override
    public boolean isInMultiSelect() {
        return mIsMultiSelect;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = ((MediaGridAdapter) parent.getAdapter()).getCursor();
        long mediaId = cursor.getLong(cursor.getColumnIndex(MediaModelTable.MEDIA_ID));
        mListener.onMediaItemSelected(mediaId);
    }

    @Override
    public void onRetryUpload(long mediaId) {
        mListener.onRetryUpload(mediaId);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(OnMediaChanged event) {
        if (event.isError()) {
            handleFetchAllMediaError(event.error.type);
            return;
        }

        switch (event.cause) {
            case FETCH_ALL_MEDIA:
                if (event.media != null) {
                    handleFetchAllMediaSuccess(event);
                }
                break;
        }
    }

    public void refreshSpinnerAdapter() {
        updateFilterText();
        updateSpinnerAdapter();
        setFilter(mFilter);
    }

    public void refreshMediaFromDB() {
        setFilter(mFilter);
        updateFilterText();
        updateSpinnerAdapter();
        if (isAdded() && mGridAdapter.getDataCount() == 0) {
            if (NetworkUtils.isNetworkAvailable(getActivity())) {
                if (!mHasRetrievedAllMedia) {
                    fetchAllMedia(0);
                }
            } else {
                updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            }
        }
    }

    public void search(String searchTerm) {
        mSearchTerm = searchTerm;
        Cursor cursor = mMediaStore.searchSiteMediaByTitleAsCursor(mSite, mSearchTerm);
        mGridAdapter.changeCursor(cursor);
    }

    public void setFilterVisibility(int visibility) {
        if (mSpinner != null) {
            mSpinner.setVisibility(visibility);
        }
    }

    public void setFilter(Filter filter) {
        mFilter = filter;
        Cursor cursor = filterItems(mFilter);
        if (filter != Filter.CUSTOM_DATE || cursor == null || cursor.getCount() == 0) {
            mResultView.setVisibility(View.GONE);
        }
        if (cursor != null && cursor.getCount() != 0) {
            mGridAdapter.swapCursor(cursor);
            hideEmptyView();
        } else {
            // No data to display. Clear the GridView and display a message in the empty view
            mGridAdapter.changeCursor(null);
        }
        if (filter != Filter.CUSTOM_DATE) {
            // Overwrite the LOADING and NO_CONTENT_CUSTOM_DATE messages
            if (mEmptyViewMessageType == EmptyViewMessageType.LOADING ||
                    mEmptyViewMessageType == EmptyViewMessageType.NO_CONTENT_CUSTOM_DATE) {
                updateEmptyView(EmptyViewMessageType.NO_CONTENT);
            } else {
                updateEmptyView(mEmptyViewMessageType);
            }
        } else {
            updateEmptyView(EmptyViewMessageType.NO_CONTENT_CUSTOM_DATE);
        }
    }

    public void clearSelectedItems() {
        mGridAdapter.clearSelection();
    }

    public void setFilterSpinnerVisible(boolean visible) {
        if (visible) {
            mSpinner.setEnabled(true);
            mSpinnerContainer.setEnabled(true);
            mSpinnerContainer.setVisibility(View.VISIBLE);
        } else {
            mSpinner.setEnabled(false);
            mSpinnerContainer.setEnabled(false);
            mSpinnerContainer.setVisibility(View.GONE);
        }
    }

    public void removeFromMultiSelect(long mediaId) {
        if (isInMultiSelect() && mGridAdapter.isItemSelected(mediaId)) {
            mGridAdapter.setItemSelected(mediaId, false);
            setFilterSpinnerVisible(mGridAdapter.getSelectedItems().size() == 0);
        }
    }

    public void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

    public void setSwipeToRefreshEnabled(boolean enabled) {
        mSwipeToRefreshHelper.setEnabled(enabled);
    }

    public void updateSpinnerAdapter() {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) mSpinner.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void updateFilterText() {
        int countAll = mMediaStore.getAllSiteMediaAsCursor(mSite).getCount();
        int countImages = mMediaStore.getNotDeletedSiteImagesAsCursor(mSite).getCount();
        int countUnattached = mMediaStore.getNotDeletedUnattachedMediaAsCursor(mSite).getCount();
        setFiltersText(countAll, countImages, countUnattached);
    }

    private Cursor setDateFilter() {
        GregorianCalendar startDate = new GregorianCalendar(mStartYear, mStartMonth, mStartDay);
        GregorianCalendar endDate = new GregorianCalendar(mEndYear, mEndMonth, mEndDay);

        // long one_day = 24 * 60 * 60 * 1000;
        // TODO: Filter images by date using `startDate.getTimeInMillis(), endDate.getTimeInMillis() + one_day`
        Cursor cursor = mMediaStore.getAllSiteMediaAsCursor(mSite);
        mGridAdapter.swapCursor(cursor);

        if (cursor != null && cursor.moveToFirst()) {
            mResultView.setVisibility(View.VISIBLE);
            hideEmptyView();
            DateFormat format = DateFormat.getDateInstance();
            String formattedStart = format.format(startDate.getTime());
            String formattedEnd = format.format(endDate.getTime());
            mResultView.setText(String.format(getString(R.string.media_gallery_date_range), formattedStart,
                    formattedEnd));
            return cursor;
        } else {
            updateEmptyView(EmptyViewMessageType.NO_CONTENT_CUSTOM_DATE);
        }
        return null;
    }

    private Cursor filterItems(Filter filter) {
        switch (filter) {
            case ALL:
                return mMediaStore.getAllSiteMediaAsCursor(mSite);
            case IMAGES:
                return mMediaStore.getNotDeletedSiteImagesAsCursor(mSite);
            case UNATTACHED:
                return mMediaStore.getNotDeletedUnattachedMediaAsCursor(mSite);
            case CUSTOM_DATE:
                // show date picker only when the user clicks on the spinner, not when we are doing syncing
                if (mIsDateFilterSet) {
                    mIsDateFilterSet = false;
                    showDatePicker();
                } else {
                    return setDateFilter();
                }
                break;
        }
        return null;
    }

    private void showDatePicker() {
        // Inflate your custom layout containing 2 DatePickers
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View customView = inflater.inflate(R.layout.date_range_dialog, null);

        // Define your date pickers
        final DatePicker dpStartDate = (DatePicker) customView.findViewById(R.id.dpStartDate);
        final DatePicker dpEndDate = (DatePicker) customView.findViewById(R.id.dpEndDate);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(customView); // Set the view of the dialog to your custom layout
        builder.setTitle("Select start and end date");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mStartYear = dpStartDate.getYear();
                mStartMonth = dpStartDate.getMonth();
                mStartDay = dpStartDate.getDayOfMonth();
                mEndYear = dpEndDate.getYear();
                mEndMonth = dpEndDate.getMonth();
                mEndDay = dpEndDate.getDayOfMonth();
                setDateFilter();

                dialog.dismiss();
            }
        });

        // Create and show the dialog
        mDatePickerDialog = builder.create();
        mDatePickerDialog.show();
    }

    /*
     * called by activity when blog is changed
     */
    protected void reset() {
        mGridAdapter.clearSelection();
        mGridView.setSelection(0);
        mGridView.requestFocusFromTouch();
        mGridView.setSelection(0);
        // TODO: We want to inject the image loader in this class instead of using a static field.
        mGridAdapter.setImageLoader(WordPress.sImageLoader);
        mGridAdapter.changeCursor(null);
        resetSpinnerAdapter();
        mHasRetrievedAllMedia = false;
    }

    private void updateEmptyView(EmptyViewMessageType emptyViewMessageType) {
        if (mEmptyView != null) {
            if (mGridAdapter.getDataCount() == 0) {
                int stringId = 0;
                switch (emptyViewMessageType) {
                    case LOADING:
                        stringId = R.string.media_fetching;
                        break;
                    case NO_CONTENT:
                        stringId = R.string.media_empty_list;
                        break;
                    case NETWORK_ERROR:
                        // Don't overwrite NO_CONTENT_CUSTOM_DATE message, since refresh is disabled with that filter on
                        if (mEmptyViewMessageType == EmptyViewMessageType.NO_CONTENT_CUSTOM_DATE) {
                            mEmptyView.setVisibility(View.VISIBLE);
                            return;
                        }
                        stringId = R.string.no_network_message;
                        break;
                    case PERMISSION_ERROR:
                        stringId = R.string.media_error_no_permission;
                        break;
                    case GENERIC_ERROR:
                        stringId = R.string.error_refresh_media;
                        break;
                    case NO_CONTENT_CUSTOM_DATE:
                        stringId = R.string.media_empty_list_custom_date;
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
        outState.putLongArray(BUNDLE_SELECTED_STATES, ListUtils.toLongArray(mGridAdapter.getSelectedItems()));
        outState.putInt(BUNDLE_SCROLL_POSITION, mGridView.getFirstVisiblePosition());
        outState.putBoolean(BUNDLE_HAS_RETRIEVED_ALL_MEDIA, mHasRetrievedAllMedia);
        outState.putBoolean(BUNDLE_IN_MULTI_SELECT_MODE, isInMultiSelect());
        outState.putInt(BUNDLE_FILTER, mFilter.ordinal());
        outState.putString(BUNDLE_EMPTY_VIEW_MESSAGE, mEmptyViewMessageType.name());
        outState.putInt(BUNDLE_FETCH_OFFSET, mMediaFetchOffset);

        outState.putBoolean(BUNDLE_DATE_FILTER_SET, mIsDateFilterSet);
        outState.putBoolean(BUNDLE_DATE_FILTER_VISIBLE, (mDatePickerDialog != null && mDatePickerDialog.isShowing()));
        outState.putInt(BUNDLE_DATE_FILTER_START_DAY, mStartDay);
        outState.putInt(BUNDLE_DATE_FILTER_START_MONTH, mStartMonth);
        outState.putInt(BUNDLE_DATE_FILTER_START_YEAR, mStartYear);
        outState.putInt(BUNDLE_DATE_FILTER_END_DAY, mEndDay);
        outState.putInt(BUNDLE_DATE_FILTER_END_MONTH, mEndMonth);
        outState.putInt(BUNDLE_DATE_FILTER_END_YEAR, mEndYear);
        outState.putSerializable(WordPress.SITE, mSite);
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

    private void resetSpinnerAdapter() {
        setFiltersText(0, 0, 0);
        updateSpinnerAdapter();
    }

    private void setFiltersText(int countAll, int countImages, int countUnattached) {
        mFiltersText[0] = getResources().getString(R.string.all) + " (" + countAll + ")";
        mFiltersText[1] = getResources().getString(R.string.images) + " (" + countImages + ")";
        mFiltersText[2] = getResources().getString(R.string.unattached) + " (" + countUnattached + ")";
        mFiltersText[3] = getResources().getString(R.string.custom_date) + "...";
    }

    private void updateActionButtons(int selectCount) {
        switch (selectCount) {
            case 1:
                mNewPostButton.setVisible(true);
                mNewGalleryButton.setVisible(false);
                break;
            default:
                mNewPostButton.setVisible(false);
                mNewGalleryButton.setVisible(true);
                break;
        }
    }

    private void handleNewPost() {
        if (!isAdded()) {
            return;
        }
        ArrayList<Long> ids = mGridAdapter.getSelectedItems();
        ActivityLauncher.newMediaPost(getActivity(), mSite, ids.iterator().next());
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
                                for (Long itemId : mGridAdapter.getSelectedItems()) {
                                    MediaModel media = mMediaStore.getSiteMediaWithId(mSite, itemId);
                                    media.setUploadState(MediaUploadState.DELETE.name());
                                    mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media));
                                }
                                mGridAdapter.clearSelection();
                                if (mActionMode != null) {
                                    mActionMode.finish();
                                }
                                refreshMediaFromDB();
                                refreshSpinnerAdapter();
                            }
                        }).setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void handleMultiSelectPost() {
        if (!isAdded()) {
            return;
        }
        ActivityLauncher.newGalleryPost(getActivity(), mSite, mGridAdapter.getSelectedItems());
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;

        boolean isInMultiSelectMode = savedInstanceState.getBoolean(BUNDLE_IN_MULTI_SELECT_MODE);

        if (savedInstanceState.containsKey(BUNDLE_SELECTED_STATES)) {
            ArrayList<Long> selectedItems = ListUtils.fromLongArray(savedInstanceState.getLongArray(BUNDLE_SELECTED_STATES));
            mGridAdapter.setSelectedItems(selectedItems);
            if (isInMultiSelectMode) {
                setFilterSpinnerVisible(mGridAdapter.getSelectedItems().size() == 0);
                mSwipeToRefreshHelper.setEnabled(false);
            }
        }

        mGridView.setSelection(savedInstanceState.getInt(BUNDLE_SCROLL_POSITION, 0));
        mHasRetrievedAllMedia = savedInstanceState.getBoolean(BUNDLE_HAS_RETRIEVED_ALL_MEDIA, false);
        mFilter = Filter.getFilter(savedInstanceState.getInt(BUNDLE_FILTER));
        mEmptyViewMessageType = EmptyViewMessageType.getEnumFromString(savedInstanceState.
                getString(BUNDLE_EMPTY_VIEW_MESSAGE));

        mIsDateFilterSet = savedInstanceState.getBoolean(BUNDLE_DATE_FILTER_SET, false);
        mStartDay = savedInstanceState.getInt(BUNDLE_DATE_FILTER_START_DAY);
        mStartMonth = savedInstanceState.getInt(BUNDLE_DATE_FILTER_START_MONTH);
        mStartYear = savedInstanceState.getInt(BUNDLE_DATE_FILTER_START_YEAR);
        mEndDay = savedInstanceState.getInt(BUNDLE_DATE_FILTER_END_DAY);
        mEndMonth = savedInstanceState.getInt(BUNDLE_DATE_FILTER_END_MONTH);
        mEndYear = savedInstanceState.getInt(BUNDLE_DATE_FILTER_END_YEAR);

        boolean datePickerShowing = savedInstanceState.getBoolean(BUNDLE_DATE_FILTER_VISIBLE);
        if (datePickerShowing)
            showDatePicker();
    }

    private void fetchAllMedia(int offset) {
        // do not refresh if there is no network
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            setRefreshing(false);
            return;
        }

        // do not refresh if custom date filter is shown
        if (mFilter == Filter.CUSTOM_DATE) {
            // TODO: could this be supported? What if media in list was deleted via web?
            setRefreshing(false);
            return;
        }

        // do not refresh if in search
        if (mSearchTerm != null && mSearchTerm.length() > 0) {
            setRefreshing(false);
            return;
        }

        if (offset == 0 || !mIsRefreshing) {
            if (offset == mOldMediaSyncOffset) {
                // we're pulling the same data again for some reason. Pull from the beginning.
                offset = 0;
            }
            mOldMediaSyncOffset = offset;

            mIsRefreshing = true;
            updateEmptyView(EmptyViewMessageType.LOADING);
            setRefreshing(true);
            mGridAdapter.setRefreshing(true);

            // TODO: figure out how to integrate `auto` to callback
            MediaFilter fetchFilter = new MediaFilter();
            fetchFilter.offset = mMediaFetchOffset;
            fetchFilter.number = NUM_PER_FETCH;
            mMediaFetchOffset += NUM_PER_FETCH;
            MediaListPayload payload = new MediaListPayload(mSite, null, fetchFilter);
            mDispatcher.dispatch(MediaActionBuilder.newFetchAllMediaAction(payload));
        }
    }

    private void handleFetchAllMediaSuccess(OnMediaChanged event) {
        MediaGridAdapter adapter = (MediaGridAdapter) mGridView.getAdapter();

        Cursor mediaCursor = mMediaStore.getAllSiteMediaAsCursor(mSite);
        adapter.swapCursor(mediaCursor);

        mHasRetrievedAllMedia = event.media.size() < NUM_PER_FETCH;
        adapter.setHasRetrievedAll(true);

        mIsRefreshing = false;

        // the activity may be gone by the time this finishes, so check for it
        if (getActivity() != null && isVisible()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshSpinnerAdapter();
                    updateEmptyView(EmptyViewMessageType.NO_CONTENT);
                    // TODO: Keep a reference to auto-refresh flag, so we can use it here
                    boolean auto = true;
                    if (!auto) {
                        mGridView.setSelection(0);
                    }
                    mGridAdapter.setRefreshing(false);
                    mSwipeToRefreshHelper.setRefreshing(false);
                }
            });
        }
    }

    private void handleFetchAllMediaError(MediaErrorType errorType) {
        AppLog.e(AppLog.T.MEDIA, "Media error occurred: " + errorType);
        final boolean isPermissionError = (errorType == MediaErrorType.UNAUTHORIZED);
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
        MediaGridAdapter adapter = (MediaGridAdapter) mGridView.getAdapter();
        mHasRetrievedAllMedia = true;
        adapter.setHasRetrievedAll(true);

        // the activity may be gone by the time we get this, so check for it
        if (getActivity() != null && MediaGridFragment.this.isVisible()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mIsRefreshing = false;
                    mGridAdapter.setRefreshing(false);
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
}
