package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.internal.widget.IcsAdapterView;
import com.actionbarsherlock.internal.widget.IcsAdapterView.OnItemSelectedListener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.CheckableFrameLayout;
import org.wordpress.android.ui.CustomSpinner;
import org.wordpress.android.ui.MultiSelectGridView;
import org.wordpress.android.ui.MultiSelectGridView.MultiSelectListener;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.media.MediaGridAdapter.MediaGridAdapterCallback;
import org.wordpress.android.util.NetworkUtils;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.SyncMediaLibraryTask.Callback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * The grid displaying the media items.
 * It appears as 2 columns on phone and 1 column on tablet (essentially a listview)
 */
public class MediaGridFragment extends Fragment implements OnItemClickListener,
        MediaGridAdapterCallback, RecyclerListener, MultiSelectListener {
    private static final String BUNDLE_CHECKED_STATES = "BUNDLE_CHECKED_STATES";
    private static final String BUNDLE_IN_MULTI_SELECT_MODE = "BUNDLE_IN_MULTI_SELECT_MODE";
    private static final String BUNDLE_SCROLL_POSITION = "BUNDLE_SCROLL_POSITION";
    private static final String BUNDLE_HAS_RETREIEVED_ALL_MEDIA = "BUNDLE_HAS_RETREIEVED_ALL_MEDIA";
    private static final String BUNDLE_FILTER = "BUNDLE_FILTER";

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
    private MultiSelectGridView mGridView;
    private MediaGridAdapter mGridAdapter;
    private MediaGridListener mListener;

    private ArrayList<String> mCheckedItems;

    private boolean mIsRefreshing = false;
    private boolean mHasRetrievedAllMedia = false;
    private String mSearchTerm;

    private View mSpinnerContainer;
    private TextView mResultView;
    private LinearLayout mEmptyView;
    private TextView mEmptyViewTitle;
    private CustomSpinner mSpinner;

    private int mOldMediaSyncOffset = 0;

    private boolean mIsDateFilterSet = false;
    private boolean mSpinnerHasLaunched = false;

    private int mStartYear, mStartMonth, mStartDay, mEndYear, mEndMonth, mEndDay;
    private AlertDialog mDatePickerDialog;

    public interface MediaGridListener {
        public void onMediaItemListDownloadStart();
        public void onMediaItemListDownloaded();
        public void onMediaItemSelected(String mediaId);
        public void onMultiSelectChange(int count);
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

    private OnItemSelectedListener mFilterSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(IcsAdapterView<?> parent, View view, int position, long id) {
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
        public void onNothingSelected(IcsAdapterView<?> parent) { }

    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mCheckedItems = new ArrayList<String>();
        mFiltersText = new String[Filter.values().length];

        mGridAdapter = new MediaGridAdapter(getActivity(), null, 0, mCheckedItems);
        mGridAdapter.setCallback(this);

        View view = inflater.inflate(R.layout.media_grid_fragment, container);

        mGridView = (MultiSelectGridView) view.findViewById(R.id.media_gridview);
        mGridView.setOnItemClickListener(this);
        mGridView.setRecyclerListener(this);
        mGridView.setMultiSelectListener(this);
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

        restoreState(savedInstanceState);

        setupSpinnerAdapter();

        return view;
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;

        boolean isInMultiSelectMode = savedInstanceState.getBoolean(BUNDLE_IN_MULTI_SELECT_MODE);

        if (savedInstanceState.containsKey(BUNDLE_CHECKED_STATES)) {
            mCheckedItems.addAll(savedInstanceState.getStringArrayList(BUNDLE_CHECKED_STATES));
            if (isInMultiSelectMode) {
                mListener.onMultiSelectChange(mCheckedItems.size());
                onMultiSelectChange(mCheckedItems.size());
            }
            mGridView.setMultiSelectModeActive(isInMultiSelectMode);
        }

        mGridView.setSelection(savedInstanceState.getInt(BUNDLE_SCROLL_POSITION, 0));
        mHasRetrievedAllMedia = savedInstanceState.getBoolean(BUNDLE_HAS_RETREIEVED_ALL_MEDIA, false);
        mFilter = Filter.getFilter(savedInstanceState.getInt(BUNDLE_FILTER));

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
        outState.putStringArrayList(BUNDLE_CHECKED_STATES, mCheckedItems);
        outState.putInt(BUNDLE_SCROLL_POSITION, mGridView.getFirstVisiblePosition());
        outState.putBoolean(BUNDLE_HAS_RETREIEVED_ALL_MEDIA, mHasRetrievedAllMedia);
        outState.putBoolean(BUNDLE_IN_MULTI_SELECT_MODE, isInMultiSelect());
        outState.putInt(BUNDLE_FILTER, mFilter.ordinal());

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
        if (getActivity() == null || WordPress.getCurrentBlog() == null)
            return;

        updateFilterText();

        Context context = ((WPActionBarActivity) getActivity()).getSupportActionBar().getThemedContext();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.sherlock_spinner_dropdown_item, mFiltersText);
        mSpinner.setAdapter(adapter);
        mSpinner.setSelection(mFilter.ordinal());
    }

    public void refreshSpinnerAdapter() {
        updateFilterText();
        updateSpinnerAdapter();
        setFilter(mFilter);
    }

    public void resetSpinnerAdapter() {
        setFiltersText(0, 0, 0);
        updateSpinnerAdapter();
    }

    private void updateFilterText() {
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

    private void updateSpinnerAdapter() {
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

        if (!NetworkUtils.isNetworkAvailable(this.getActivity()))
            mHasRetrievedAllMedia = true;

        refreshSpinnerAdapter();
        refreshMediaFromDB();
    }

    public void refreshMediaFromDB() {
        setFilter(mFilter);
        if (mGridAdapter.getDataCount() == 0 && !mHasRetrievedAllMedia) {
            refreshMediaFromServer(0, true);
        }
    }

    public void refreshMediaFromServer(int offset, final boolean auto) {

        // do not refresh if custom date filter is shown
        if(WordPress.getCurrentBlog() == null || mFilter == Filter.CUSTOM_DATE)
            return;

        // do not refresh if in search
        if (mSearchTerm != null && mSearchTerm.length() > 0)
            return;

        if(offset == 0 || !mIsRefreshing) {

            if (offset == mOldMediaSyncOffset) {
                // we're pulling the same data again for some reason. Pull from the beginning.
                offset = 0;
            }
            mOldMediaSyncOffset = offset;

            mIsRefreshing = true;
            mListener.onMediaItemListDownloadStart();
            mGridAdapter.setRefreshing(true);

            List<Object> apiArgs = new ArrayList<Object>();
            apiArgs.add(WordPress.getCurrentBlog());

            Callback callback = new Callback() {

                // refersh db from server. If returned count is 0, we've retrieved all the media.
                // stop retrieving until the user manually refreshes

                @Override
                public void onSuccess(int count) {
                    MediaGridAdapter adapter = (MediaGridAdapter) mGridView.getAdapter();
                    mHasRetrievedAllMedia = (count == 0);
                    adapter.setHasRetrieviedAll(mHasRetrievedAllMedia);

                    mIsRefreshing = false;

                    // the activity may be gone by the time this finishes, so check for it
                    if (getActivity() != null && MediaGridFragment.this.isVisible()) {
                        getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                refreshSpinnerAdapter();
                                setFilter(mFilter);
                                if (!auto)
                                    mGridView.setSelection(0);


                                mListener.onMediaItemListDownloaded();
                                mGridAdapter.setRefreshing(false);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                    if (errorType != ApiHelper.ErrorType.NO_ERROR) {
                        if (getActivity() != null) {
                            String message = errorType == ApiHelper.ErrorType.NO_UPLOAD_FILES_CAP ? getString(
                                    R.string.media_error_no_permission) : getString(R.string.error_refresh_media);
                            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                        }
                        MediaGridAdapter adapter = (MediaGridAdapter) mGridView.getAdapter();
                        mHasRetrievedAllMedia = true;
                        adapter.setHasRetrieviedAll(mHasRetrievedAllMedia);
                    }

                    // the activity may be cone by the time we get this, so check for it
                    if (getActivity() != null && MediaGridFragment.this.isVisible()) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mIsRefreshing = false;
                                mListener.onMediaItemListDownloaded();
                                mGridAdapter.setRefreshing(false);
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

    public boolean isRefreshing() {
        return mIsRefreshing;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = ((MediaGridAdapter) parent.getAdapter()).getCursor();
        String mediaId = cursor.getString(cursor.getColumnIndex("mediaId"));
        mListener.onMediaItemSelected(mediaId);
    }

    public void setFilterVisibility(int visibility) {
        if (mSpinner != null)
            mSpinner.setVisibility(visibility);
    }

    private void setEmptyViewVisible(boolean visible) {
        setEmptyViewVisible(visible, -1);
    }

    private void setEmptyViewVisible(boolean visible, int messageId) {
        if (visible) {
            mGridView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            if (messageId != -1) {
                mEmptyViewTitle.setText(getResources().getString(messageId));
            }
        } else {
            mEmptyView.setVisibility(View.GONE);
            mGridView.setVisibility(View.VISIBLE);
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
            setEmptyViewVisible(false);
        } else {
            if (filter != Filter.CUSTOM_DATE) {
                setEmptyViewVisible(true, R.string.media_empty_list);
            }
        }
    }

    public Cursor setDateFilter() {
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
            setEmptyViewVisible(false);

            SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy");
            fmt.setCalendar(startDate);
            String formattedStart = fmt.format(startDate.getTime());
            String formattedEnd = fmt.format(endDate.getTime());

            // TODO: replace hard-coded text with string resource
            mResultView.setText("Displaying media from " + formattedStart + " to " + formattedEnd);
            return cursor;
        } else {
            setEmptyViewVisible(true, R.string.media_empty_list_custom_date);
        }
        return null;
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

    public void showDatePicker() {
        // Inflate your custom layout containing 2 DatePickers
        LayoutInflater inflater = (LayoutInflater) getActivity().getLayoutInflater();
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
        if (!mHasRetrievedAllMedia)
            refreshMediaFromServer(offset, true);
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

    @Override
    public void onMultiSelectChange(int count) {
        if (count == 0) {
            // enable filtering when not in multiselect
            mSpinner.setEnabled(true);
            mSpinnerContainer.setEnabled(true);
            mSpinnerContainer.setVisibility(View.VISIBLE);
        } else {
            // disable filtering on multiselect
            mSpinner.setEnabled(false);
            mSpinnerContainer.setEnabled(false);
            mSpinnerContainer.setVisibility(View.GONE);
        }

        mListener.onMultiSelectChange(count);
    }

    @Override
    public boolean isInMultiSelect() {
        return mGridView.isInMultiSelectMode();
    }

    public ArrayList<String> getCheckedItems() {
        return mCheckedItems;
    }

    public void clearCheckedItems() {
        mGridView.cancelSelection();
    }

    @Override
    public void onRetryUpload(String mediaId) {
        mListener.onRetryUpload(mediaId);
    }

    public boolean hasRetrievedAllMediaFromServer() {
        return mHasRetrievedAllMedia;
    }

    public void reset() {
        mCheckedItems.clear();
        mGridView.setSelection(0);
        mGridView.requestFocusFromTouch();
        mGridView.setSelection(0);
        mGridAdapter.changeCursor(null);

        resetSpinnerAdapter();

        mHasRetrievedAllMedia = false;
    }

    public void removeFromMultiSelect(String mediaId) {
        if (isInMultiSelect()) {
            mCheckedItems.remove(mediaId);
            mListener.onMultiSelectChange(mCheckedItems.size());
            onMultiSelectChange(mCheckedItems.size());
        }
    }

}
