package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
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

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.CheckableFrameLayout;
import org.wordpress.android.ui.CustomSpinner;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.media.MediaGridAdapter.MediaGridAdapterCallback;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.SyncMediaLibraryTask.Callback;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * The grid displaying the media items.
 */
public class MediaGridFragment extends Fragment
        implements OnItemClickListener, MediaGridAdapterCallback, RecyclerListener {
    private static final String BUNDLE_SELECTED_STATES = "BUNDLE_SELECTED_STATES";
    private static final String BUNDLE_IN_MULTI_SELECT_MODE = "BUNDLE_IN_MULTI_SELECT_MODE";
    private static final String BUNDLE_SCROLL_POSITION = "BUNDLE_SCROLL_POSITION";
    private static final String BUNDLE_HAS_RETREIEVED_ALL_MEDIA = "BUNDLE_HAS_RETREIEVED_ALL_MEDIA";
    private static final String BUNDLE_FILTER = "BUNDLE_FILTER";
    private static final String BUNDLE_EMPTY_VIEW_MESSAGE = "BUNDLE_EMPTY_VIEW_MESSAGE";

    private static final String BUNDLE_DATE_FILTER_SET = "BUNDLE_DATE_FILTER_SET";
    private static final String BUNDLE_DATE_FILTER_VISIBLE = "BUNDLE_DATE_FILTER_VISIBLE";
    private static final String BUNDLE_DATE_FILTER_START_YEAR = "BUNDLE_DATE_FILTER_START_YEAR";
    private static final String BUNDLE_DATE_FILTER_START_MONTH = "BUNDLE_DATE_FILTER_START_MONTH";
    private static final String BUNDLE_DATE_FILTER_START_DAY = "BUNDLE_DATE_FILTER_START_DAY";
    private static final String BUNDLE_DATE_FILTER_END_YEAR = "BUNDLE_DATE_FILTER_END_YEAR";
    private static final String BUNDLE_DATE_FILTER_END_MONTH = "BUNDLE_DATE_FILTER_END_MONTH";
    private static final String BUNDLE_DATE_FILTER_END_DAY = "BUNDLE_DATE_FILTER_END_DAY";

    private Filter mFilter = Filter.ALL;
    private String[] mFiltersText;
    private GridView mGridView;
    private MediaGridAdapter mGridAdapter;
    private MediaGridListener mListener;

    private boolean mIsRefreshing;
    private boolean mHasRetrievedAllMedia;
    private boolean mIsMultiSelect;
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

    public interface MediaGridListener {
        public void onMediaItemListDownloadStart();
        public void onMediaItemListDownloaded();
        public void onMediaItemSelected(String mediaId);
        public void onRetryUpload(String mediaId);
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
            // need this to stop the bug where onItemSelected is called during initialization, before user input
            if (!mSpinnerHasLaunched) {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mFiltersText = new String[Filter.values().length];
        mGridAdapter = new MediaGridAdapter(getActivity(), null, 0, MediaImageLoader.getInstance());
        mGridAdapter.setCallback(this);

        View view = inflater.inflate(R.layout.media_grid_fragment, container);

        mGridView = (GridView) view.findViewById(R.id.media_gridview);
        mGridView.setOnItemClickListener(this);
        mGridView.setRecyclerListener(this);
        mGridView.setMultiChoiceModeListener(new MultiChoiceModeListener());
        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setAdapter(mGridAdapter);

        mEmptyView = (LinearLayout) view.findViewById(R.id.empty_view);
        mEmptyViewTitle = (TextView) view.findViewById(R.id.empty_view_title);

        mResultView = (TextView) view.findViewById(R.id.media_filter_result_text);

        mSpinner = (CustomSpinner) view.findViewById(R.id.media_filter_spinner);
        mSpinner.setOnItemSelectedListener(mFilterSelectedListener);
        mSpinner.setOnItemSelectedEvenIfUnchangedListener(mFilterSelectedListener);

        mSpinnerContainer = view.findViewById(R.id.media_filter_spinner_container);
        mSpinnerContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isInMultiSelect()) {
                    mSpinnerHasLaunched = true;
                    mSpinner.performClick();
                }
            }

        });

        // swipe to refresh setup
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(getActivity(),
                (CustomSwipeRefreshLayout) view.findViewById(R.id.ptr_layout),
                new RefreshListener() {
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
                        refreshMediaFromServer(0, false);
                    }
                });
        restoreState(savedInstanceState);
        setupSpinnerAdapter();

        return view;
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;

        boolean isInMultiSelectMode = savedInstanceState.getBoolean(BUNDLE_IN_MULTI_SELECT_MODE);

        if (savedInstanceState.containsKey(BUNDLE_SELECTED_STATES)) {
            ArrayList selectedItems = savedInstanceState.getStringArrayList(BUNDLE_SELECTED_STATES);
            mGridAdapter.setSelectedItems(selectedItems);
            if (isInMultiSelectMode) {
                setFilterSpinnerVisible(mGridAdapter.getSelectedItems().size() == 0);
                mSwipeToRefreshHelper.setEnabled(false);
            }
        }

        mGridView.setSelection(savedInstanceState.getInt(BUNDLE_SCROLL_POSITION, 0));
        mHasRetrievedAllMedia = savedInstanceState.getBoolean(BUNDLE_HAS_RETREIEVED_ALL_MEDIA, false);
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState(outState);
    }

    private void saveState(Bundle outState) {
        outState.putStringArrayList(BUNDLE_SELECTED_STATES, mGridAdapter.getSelectedItems());
        outState.putInt(BUNDLE_SCROLL_POSITION, mGridView.getFirstVisiblePosition());
        outState.putBoolean(BUNDLE_HAS_RETREIEVED_ALL_MEDIA, mHasRetrievedAllMedia);
        outState.putBoolean(BUNDLE_IN_MULTI_SELECT_MODE, isInMultiSelect());
        outState.putInt(BUNDLE_FILTER, mFilter.ordinal());
        outState.putString(BUNDLE_EMPTY_VIEW_MESSAGE, mEmptyViewMessageType.name());

        outState.putBoolean(BUNDLE_DATE_FILTER_SET, mIsDateFilterSet);
        outState.putBoolean(BUNDLE_DATE_FILTER_VISIBLE, (mDatePickerDialog != null && mDatePickerDialog.isShowing()));
        outState.putInt(BUNDLE_DATE_FILTER_START_DAY, mStartDay);
        outState.putInt(BUNDLE_DATE_FILTER_START_MONTH, mStartMonth);
        outState.putInt(BUNDLE_DATE_FILTER_START_YEAR, mStartYear);
        outState.putInt(BUNDLE_DATE_FILTER_END_DAY, mEndDay);
        outState.putInt(BUNDLE_DATE_FILTER_END_MONTH, mEndMonth);
        outState.putInt(BUNDLE_DATE_FILTER_END_YEAR, mEndYear);
    }

    private void setupSpinnerAdapter() {
        if (getActivity() == null || WordPress.getCurrentBlog() == null) {
            return;
        }

        updateFilterText();

        Context context = WPActivityUtils.getThemedContext(getActivity());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.spinner_menu_dropdown_item, mFiltersText);
        mSpinner.setAdapter(adapter);
        mSpinner.setSelection(mFilter.ordinal());
    }

    public void refreshSpinnerAdapter() {
        updateFilterText();
        updateSpinnerAdapter();
        setFilter(mFilter);
    }

    void resetSpinnerAdapter() {
        setFiltersText(0, 0, 0);
        updateSpinnerAdapter();
    }

    void updateFilterText() {
        if (WordPress.currentBlog == null)
            return;

        String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());

        int countAll = WordPress.wpDB.getMediaCountAll(blogId);
        int countImages = WordPress.wpDB.getMediaCountImages(blogId);
        int countUnattached = WordPress.wpDB.getMediaCountUnattached(blogId);

        setFiltersText(countAll, countImages, countUnattached);
    }

    private void setFiltersText(int countAll, int countImages, int countUnattached) {
        mFiltersText[0] = getResources().getString(R.string.all) + " (" + countAll + ")";
        mFiltersText[1] = getResources().getString(R.string.images) + " (" + countImages + ")";
        mFiltersText[2] = getResources().getString(R.string.unattached) + " (" + countUnattached + ")";
        mFiltersText[3] = getResources().getString(R.string.custom_date) + "...";
    }

    void updateSpinnerAdapter() {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) mSpinner.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
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
    public void onResume() {
        super.onResume();
        refreshSpinnerAdapter();
        refreshMediaFromDB();
    }

    public void refreshMediaFromDB() {
        setFilter(mFilter);
        if (isAdded() && mGridAdapter.getDataCount() == 0) {
            if (NetworkUtils.isNetworkAvailable(getActivity())) {
                if (!mHasRetrievedAllMedia) {
                    refreshMediaFromServer(0, true);
                }
            } else {
                updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            }
        }
    }

    public void refreshMediaFromServer(int offset, final boolean auto) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            setRefreshing(false);
            return;
        }

        // do not refresh if custom date filter is shown
        if (WordPress.getCurrentBlog() == null || mFilter == Filter.CUSTOM_DATE) {
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
            mListener.onMediaItemListDownloadStart();
            mGridAdapter.setRefreshing(true);

            List<Object> apiArgs = new ArrayList<Object>();
            apiArgs.add(WordPress.getCurrentBlog());

            Callback callback = new Callback() {
                // refresh db from server. If returned count is 0, we've retrieved all the media.
                // stop retrieving until the user manually refreshes

                @Override
                public void onSuccess(int count) {
                    MediaGridAdapter adapter = (MediaGridAdapter) mGridView.getAdapter();
                    mHasRetrievedAllMedia = (count == 0);
                    adapter.setHasRetrievedAll(mHasRetrievedAllMedia);

                    mIsRefreshing = false;

                    // the activity may be gone by the time this finishes, so check for it
                    if (getActivity() != null && MediaGridFragment.this.isVisible()) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshSpinnerAdapter();
                                updateEmptyView(EmptyViewMessageType.NO_CONTENT);
                                if (!auto) {
                                    mGridView.setSelection(0);
                                }
                                mListener.onMediaItemListDownloaded();
                                mGridAdapter.setRefreshing(false);
                                mSwipeToRefreshHelper.setRefreshing(false);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                    if (errorType != ApiHelper.ErrorType.NO_ERROR) {
                        if (getActivity() != null) {
                            if (errorType != ApiHelper.ErrorType.NO_UPLOAD_FILES_CAP) {
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
                        adapter.setHasRetrievedAll(mHasRetrievedAllMedia);
                    }

                    // the activity may be cone by the time we get this, so check for it
                    if (getActivity() != null && MediaGridFragment.this.isVisible()) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mIsRefreshing = false;
                                mListener.onMediaItemListDownloaded();
                                mGridAdapter.setRefreshing(false);
                                mSwipeToRefreshHelper.setRefreshing(false);
                                if (errorType == ApiHelper.ErrorType.NO_UPLOAD_FILES_CAP) {
                                    updateEmptyView(EmptyViewMessageType.PERMISSION_ERROR);
                                } else {
                                    updateEmptyView(EmptyViewMessageType.GENERIC_ERROR);
                                }
                            }
                        });
                    }
                }
            };

            ApiHelper.SyncMediaLibraryTask getMediaTask = new ApiHelper.SyncMediaLibraryTask(offset, mFilter, callback);
            getMediaTask.execute(apiArgs);
        }
    }

    public void search(String searchTerm) {
        mSearchTerm = searchTerm;
        Blog blog = WordPress.getCurrentBlog();
        if (blog != null) {
            String blogId = String.valueOf(blog.getLocalTableBlogId());
            Cursor cursor = WordPress.wpDB.getMediaFilesForBlog(blogId, searchTerm);
            mGridAdapter.changeCursor(cursor);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = ((MediaGridAdapter) parent.getAdapter()).getCursor();
        String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));
        mListener.onMediaItemSelected(mediaId);
    }

    public void setFilterVisibility(int visibility) {
        if (mSpinner != null) {
            mSpinner.setVisibility(visibility);
        }
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

    Cursor setDateFilter() {
        Blog blog = WordPress.getCurrentBlog();

        if (blog == null)
            return null;

        String blogId = String.valueOf(blog.getLocalTableBlogId());

        GregorianCalendar startDate = new GregorianCalendar(mStartYear, mStartMonth, mStartDay);
        GregorianCalendar endDate = new GregorianCalendar(mEndYear, mEndMonth, mEndDay);

        long one_day = 24 * 60 * 60 * 1000;
        Cursor cursor = WordPress.wpDB.getMediaFilesForBlog(blogId, startDate.getTimeInMillis(), endDate.getTimeInMillis() + one_day);
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

    public void clearSelectedItems() {
        mGridAdapter.clearSelection();
    }

    private Cursor filterItems(Filter filter) {
        Blog blog = WordPress.getCurrentBlog();

        if (blog == null)
            return null;

        String blogId = String.valueOf(blog.getLocalTableBlogId());

        switch (filter) {
            case ALL:
                return WordPress.wpDB.getMediaFilesForBlog(blogId);
            case IMAGES:
                return WordPress.wpDB.getMediaImagesForBlog(blogId);
            case UNATTACHED:
                return WordPress.wpDB.getMediaUnattachedForBlog(blogId);
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

    void showDatePicker() {
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

    @Override
    public void fetchMoreData(int offset) {
        if (!mHasRetrievedAllMedia) {
            refreshMediaFromServer(offset, true);
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
                ImageContainer container = WordPress.imageLoader.get(tag, new ImageListener() {
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

    @Override
    public void onRetryUpload(String mediaId) {
        mListener.onRetryUpload(mediaId);
    }

    public boolean hasRetrievedAllMediaFromServer() {
        return mHasRetrievedAllMedia;
    }

    /*
     * called by activity when blog is changed
     */
    protected void reset() {
        mGridAdapter.clearSelection();
        mGridView.setSelection(0);
        mGridView.requestFocusFromTouch();
        mGridView.setSelection(0);
        mGridAdapter.setImageLoader(MediaImageLoader.getInstance());
        mGridAdapter.changeCursor(null);
        resetSpinnerAdapter();
        mHasRetrievedAllMedia = false;
    }

    public void removeFromMultiSelect(String mediaId) {
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

    @Override
    public boolean isInMultiSelect() {
        return mIsMultiSelect;
    }

    public class MultiChoiceModeListener implements GridView.MultiChoiceModeListener {
        private MenuItem mNewPostButton;
        private MenuItem mNewGalleryButton;

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
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

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

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

        public void onDestroyActionMode(ActionMode mode) {
            mGridAdapter.clearSelection();
            setSwipeToRefreshEnabled(true);
            mIsMultiSelect = false;
            setFilterSpinnerVisible(mGridAdapter.getSelectedItems().size() == 0);
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            mGridAdapter.setItemSelected(position, checked);
            int selectCount = mGridAdapter.getSelectedItems().size();
            setFilterSpinnerVisible(selectCount == 0);
            mode.setTitle(String.format(getString(R.string.cab_selected), selectCount));
            updateActionButtons(selectCount);
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
            ArrayList<String> ids = mGridAdapter.getSelectedItems();
            Intent i = new Intent(getActivity(), EditPostActivity.class);
            i.setAction(EditPostActivity.NEW_MEDIA_POST);
            i.putExtra(EditPostActivity.NEW_MEDIA_POST_EXTRA, ids.iterator().next());
            startActivity(i);
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
            Intent i = new Intent(getActivity(), EditPostActivity.class);
            i.setAction(EditPostActivity.NEW_MEDIA_GALLERY);
            i.putStringArrayListExtra(EditPostActivity.NEW_MEDIA_GALLERY_EXTRA_IDS,
                    mGridAdapter.getSelectedItems());
            startActivity(i);
        }
    }
}
