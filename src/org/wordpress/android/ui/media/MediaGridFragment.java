
package org.wordpress.android.ui.media;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

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
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.internal.widget.IcsAdapterView;
import com.actionbarsherlock.internal.widget.IcsAdapterView.OnItemSelectedListener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.SyncMediaLibraryTask;
import org.xmlrpc.android.ApiHelper.SyncMediaLibraryTask.Callback;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.CheckableFrameLayout;
import org.wordpress.android.ui.CustomSpinner;
import org.wordpress.android.ui.MultiSelectGridView;
import org.wordpress.android.ui.MultiSelectGridView.MultiSelectListener;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.media.MediaGridAdapter.MediaGridAdapterCallback;

public class MediaGridFragment extends Fragment implements OnItemClickListener, MediaGridAdapterCallback, RecyclerListener, MultiSelectListener {
    
    private static final String BUNDLE_CHECKED_STATES = "BUNDLE_CHECKED_STATES";
    private static final String BUNDLE_IN_MULTI_SELECT_MODE = "BUNDLE_IN_MULTI_SELECT_MODE";
    private static final String BUNDLE_SCROLL_POSITION = "BUNDLE_SCROLL_POSITION";
    private static final String BUNDLE_HAS_RETREIEVED_ALL_MEDIA = "BUNDLE_HAS_RETREIEVED_ALL_MEDIA";

    private Cursor mCursor;
    private Filter mFilter = Filter.ALL;
    private String[] mFiltersText;
    private MultiSelectGridView mGridView;
    private MediaGridAdapter mGridAdapter;
    private MediaGridListener mListener;

    private ArrayList<String> mCheckedItems;
    
    private boolean mIsRefreshing = false;
    
    private int mSavedFirstVisiblePosition = 0;
    private boolean mHasRetrievedAllMedia;

    private View mSpinnerContainer;
    private TextView mResultView;
    private CustomSpinner mSpinner;

    private int mOldMediaSyncOffset;

    private boolean mUserClickedCustomDateFilter = false;

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
            mCursor = null;
            if (position == Filter.CUSTOM_DATE.ordinal()) {
                mUserClickedCustomDateFilter = true;
            }
            setFilter(Filter.getFilter(position));

        }

        @Override
        public void onNothingSelected(IcsAdapterView<?> parent) { }
        
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.media_grid_fragment, container);

        mGridView = (MultiSelectGridView) view.findViewById(R.id.media_gridview);
        mGridView.setOnItemClickListener(this);
        mGridView.setRecyclerListener(this);
        mGridView.setMultiSelectListener(this);

        mResultView = (TextView) view.findViewById(R.id.media_filter_result_text);

        mSpinnerContainer = view.findViewById(R.id.media_filter_spinner_container);
        mSpinnerContainer.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mSpinner != null && !isInMultiSelect()) {
                    mSpinner.performClick();
                }
            }

        });

        mFiltersText = new String[Filter.values().length];
        mSpinner = (CustomSpinner) view.findViewById(R.id.media_filter_spinner);
        mSpinner.setOnItemSelectedListener(mFilterSelectedListener);
        mSpinner.setOnItemSelectedEvenIfUnchangedListener(mFilterSelectedListener);
        setupSpinnerAdapter();

        mCheckedItems = new ArrayList<String>();
        restoreState(savedInstanceState);

        return view;
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;
        
        boolean isInMultiSelectMode = savedInstanceState.getBoolean(BUNDLE_IN_MULTI_SELECT_MODE);
        
        if (savedInstanceState.containsKey(BUNDLE_CHECKED_STATES)) {
            mCheckedItems = savedInstanceState.getStringArrayList(BUNDLE_CHECKED_STATES);
            if (isInMultiSelectMode) {
                mListener.onMultiSelectChange(mCheckedItems.size());
                onMultiSelectChange(mCheckedItems.size());
            }
            mGridView.setMultiSelectModeEnabled(isInMultiSelectMode);
        }
        
        mSavedFirstVisiblePosition = savedInstanceState.getInt(BUNDLE_SCROLL_POSITION, 0);
        mHasRetrievedAllMedia = savedInstanceState.getBoolean(BUNDLE_HAS_RETREIEVED_ALL_MEDIA, false);
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
    }

    private void setupSpinnerAdapter() {
        if (getActivity() == null || WordPress.getCurrentBlog() == null)
            return;

        updateFilterText();

        Context context = ((WPActionBarActivity) getActivity()).getSupportActionBar().getThemedContext();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.sherlock_spinner_dropdown_item, mFiltersText);
        mSpinner.setAdapter(adapter);
    }

    public void refreshSpinnerAdapter() {
        updateFilterText();
        updateSpinnerAdapter();
    }
    
    public void resetSpinnerAdapter() {
        setFiltersText(0, 0, 0);
        updateSpinnerAdapter();
    }

    private void updateFilterText() {
        if (WordPress.currentBlog == null)
            return;

        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());

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
        if (adapter != null)
            adapter.notifyDataSetChanged();
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

        setupSpinnerAdapter();
        refreshMediaFromDB();
    }

    public void refreshMediaFromDB() {
        setFilter(mFilter);

        if (mGridAdapter == null) {
            mGridAdapter = new MediaGridAdapter(getActivity(), null, 0, mCheckedItems);
            mGridAdapter.setCallback(this);
            mGridView.setAdapter(mGridAdapter);
            mGridView.setSelection(mSavedFirstVisiblePosition);
        }
        
        if (mCursor != null && mCursor.getCount() == 0 && !mHasRetrievedAllMedia) {
            refreshMediaFromServer(0, true);
        }
        
        mGridAdapter.swapCursor(mCursor);
    }

    public void refreshMediaFromServer(int offset, final boolean auto) {
        if(WordPress.getCurrentBlog() == null)
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

                @Override
                public void onSuccess(int count) {
                    MediaGridAdapter adapter = (MediaGridAdapter) mGridView.getAdapter();
                    mHasRetrievedAllMedia = (count == 0);
                    adapter.setHasRetrieviedAll(mHasRetrievedAllMedia);
                    
                    mIsRefreshing = false;

                    if (getActivity() != null  && MediaGridFragment.this.isVisible()) {
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
                public void onFailure(int errorCode) {

                    if (errorCode == SyncMediaLibraryTask.NO_UPLOAD_FILES_CAP) {
                        Toast.makeText(getActivity(), "You do not have permission to view the media library", Toast.LENGTH_SHORT).show();
                        MediaGridAdapter adapter = (MediaGridAdapter) mGridView.getAdapter();
                        mHasRetrievedAllMedia = true;
                        adapter.setHasRetrieviedAll(mHasRetrievedAllMedia);
                    }
                    
                    if (getActivity() != null  && MediaGridFragment.this.isVisible()) {
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
        Blog blog = WordPress.getCurrentBlog();
        if (blog != null) {
            String blogId = String.valueOf(blog.getBlogId());
            mCursor = WordPress.wpDB.getMediaFilesForBlog(blogId, searchTerm);
            mGridAdapter.changeCursor(mCursor);
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

    public void setFilter(Filter filter) {
        mFilter = filter;
        mCursor = filterItems(mFilter);

        if (mGridAdapter != null && mCursor != null) {
            mGridAdapter.swapCursor(mCursor);
            mResultView.setVisibility(View.GONE);
        } else {
            if (filter != Filter.CUSTOM_DATE) {
                mResultView.setVisibility(View.VISIBLE);
                mResultView.setText(getResources().getString(R.string.empty_fields));
            }
        }

    }

    public void setDateFilter() {
        Blog blog = WordPress.getCurrentBlog();

        if (blog == null)
            return;

        String blogId = String.valueOf(blog.getBlogId());

        GregorianCalendar startDate = new GregorianCalendar(startYear, startMonth, startDay);
        GregorianCalendar endDate = new GregorianCalendar(endYear, endMonth, endDay);

        long one_day = 24 * 60 * 60 * 1000;
        mCursor = WordPress.wpDB.getMediaFilesForBlog(blogId, startDate.getTimeInMillis(), endDate.getTimeInMillis() + one_day);
        mGridAdapter.swapCursor(mCursor);

        if (mCursor != null && mCursor.getCount() > 0 && mGridAdapter != null) {
            mResultView.setVisibility(View.VISIBLE);

            SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy");
            fmt.setCalendar(startDate);
            String formattedStart = fmt.format(startDate.getTime());
            String formattedEnd = fmt.format(endDate.getTime());

            mResultView.setText("Displaying media from " + formattedStart + " to " + formattedEnd);
        } else {

            mResultView.setVisibility(View.VISIBLE);
            mResultView.setText(getResources().getString(R.string.empty_fields));

        }
    }

    private Cursor filterItems(Filter filter) {
        Blog blog = WordPress.getCurrentBlog();

        if (blog == null)
            return null;

        String blogId = String.valueOf(blog.getBlogId());

        switch (filter) {
            case ALL:
                return WordPress.wpDB.getMediaFilesForBlog(blogId);
            case IMAGES:
                return WordPress.wpDB.getMediaImagesForBlog(blogId);
            case UNATTACHED:
                return WordPress.wpDB.getMediaUnattachedForBlog(blogId);
            case CUSTOM_DATE:
                // show date picker only when the user clicks on the spinner, not when we are doing syncing
                if (mUserClickedCustomDateFilter) {
                    mUserClickedCustomDateFilter = false;
                    showDatePicker();
                } else {
                    setDateFilter();
                }
                break;
        }
        return null;
    }

    private int startYear, startMonth, startDay, endYear, endMonth, endDay;

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
                startYear = dpStartDate.getYear();
                startMonth = dpStartDate.getMonth();
                startDay = dpStartDate.getDayOfMonth();
                endYear = dpEndDate.getYear();
                endMonth = dpEndDate.getMonth();
                endDay = dpEndDate.getDayOfMonth();
                setDateFilter();

                dialog.dismiss();
            }
        });

        // Create and show the dialog
        builder.create().show();
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
        if (mGridAdapter != null) {
            mGridAdapter.swapCursor(null);
        }

        resetSpinnerAdapter();
        
        clearCheckedItems();
        
        mHasRetrievedAllMedia = false;
    }
    
}
