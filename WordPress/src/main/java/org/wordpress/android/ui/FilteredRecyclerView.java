package org.wordpress.android.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.FilterCriteria;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.RecyclerItemDecoration;


public class FilteredRecyclerView extends RelativeLayout {

    private ProgressBar mProgressLoadMore;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private Spinner mSpinner;
    private boolean mSelectingRememeberedFilterOnCreate = false;

    private RecyclerView mRecycler;
    private TextView mEmptyView;

    private FilterCriteria[] mFilterCriteriaOptions;
    private FilterCriteria mCurrentFilter;
    private Listener mListener;
    private SpinnerAdapter mSpinnerAdapter;
    private RecyclerView.Adapter<RecyclerView.ViewHolder> mAdapter;
    private AppLog.T mTAG;

    public FilteredRecyclerView(Context context) {
        super(context);
        init();
    }

    public FilteredRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FilteredRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

    public boolean isRefreshing(){
        return mSwipeToRefreshHelper.isRefreshing();
    }

    public void setCurrentFilter(FilterCriteria filter) {
        mCurrentFilter = filter;
        int position = mSpinnerAdapter.getIndexOfCriteria(filter);
        if (position > -1 && position != mSpinner.getSelectedItemPosition()) {
            mSpinner.setSelection(position);
        }
    }

    public FilterCriteria getCurrentFilter() {
        return mCurrentFilter;
    }

    public void setLifecycleListener(Listener listener){
        mListener = listener;
        mFilterCriteriaOptions = mListener.onLoadFilterCriteriaOptions();
        initAdapter();
        setCurrentFilter(mListener.onRecallSelection());
    }

    public void setAdapter(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter){
        mAdapter = adapter;
        mRecycler.setAdapter(mAdapter);
    }

    public RecyclerView.Adapter<RecyclerView.ViewHolder> getAdapter(){
        return mAdapter;
    }

    public void setSwipeToRefreshEnabled(boolean enable){
        mSwipeToRefreshHelper.setEnabled(enable);
    }

    public void setLogT(AppLog.T tag){
        mTAG = tag;
    }

    private void init() {
        inflate(getContext(), R.layout.filtered_list_component, this);

        int spacingHorizontal = 0;
        int spacingVertical = DisplayUtils.dpToPx(getContext(), 1);
        mRecycler = (RecyclerView) findViewById(R.id.recycler_view);
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecycler.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

        mEmptyView = (TextView) findViewById(R.id.empty_view);

        // progress bar that appears when loading more items
        mProgressLoadMore = (ProgressBar) findViewById(R.id.progress_loading);
        mProgressLoadMore.setVisibility(View.GONE);

        mSwipeToRefreshHelper = new SwipeToRefreshHelper(getContext(),
                (CustomSwipeRefreshLayout) findViewById(R.id.ptr_layout),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                if (!NetworkUtils.checkConnection(getContext())) {
                                    mSwipeToRefreshHelper.setRefreshing(false);
                                    updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
                                    return;
                                }
                                if (mListener != null){
                                    mListener.onLoadData();
                                }
                            }
                        });
                    }
                });


        if (mSpinner == null) {
            mSpinner = (Spinner) findViewById(R.id.filter_spinner);

            //changing spinner arrow color to mach custom spinner at ReaderPostListFragment
            Drawable spinnerBackground = mSpinner.getBackground();
            spinnerBackground.setColorFilter(ContextCompat.getColor(getContext(), R.color.grey), PorterDuff.Mode.SRC_ATOP);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                mSpinner.setBackgroundDrawable(spinnerBackground);
            } else {
                mSpinner.setBackground(spinnerBackground);
            }
        }

    }

    private void initAdapter(){
        mSpinnerAdapter = new SpinnerAdapter(getContext(), mFilterCriteriaOptions);

        mSelectingRememeberedFilterOnCreate = true;
        mSpinner.setAdapter(mSpinnerAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mSelectingRememeberedFilterOnCreate) {
                    mSelectingRememeberedFilterOnCreate = false;
                    return;
                }

                FilterCriteria selectedCriteria =
                        (FilterCriteria) mSpinnerAdapter.getItem(position);

                if (mCurrentFilter == selectedCriteria) {
                    AppLog.d(mTAG, "The selected STATUS is already active: " +
                            selectedCriteria.getLabel());
                    return;
                }

                AppLog.d(mTAG, "NEW STATUS : " + selectedCriteria.getLabel());
                setCurrentFilter(selectedCriteria);
                if (mListener != null) {
                    mListener.onFilterSelected(position, selectedCriteria);
                    setRefreshing(true);
                    mListener.onLoadData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nop
            }
        });

    }

    private boolean hasAdapter() {
        return (mAdapter != null);
    }

    public boolean emptyViewIsVisible(){
        return (mEmptyView != null && mEmptyView.getVisibility() == View.VISIBLE);
    }

    public void hideEmptyView() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    public void updateEmptyView(EmptyViewMessageType emptyViewMessageType) {
        if (!hasAdapter() || mEmptyView == null) return;

        if ((hasAdapter() && mAdapter.getItemCount() == 0) || !hasAdapter()) {
            String msg = null;
            if (mListener != null){
                msg  = mListener.onShowEmptyViewMessage(emptyViewMessageType);
            }

            if (msg == null){
                msg = getContext().getString(R.string.empty_list_default);
            }
            mEmptyView.setText(msg);
            mEmptyView.setVisibility(View.VISIBLE);

        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    /**
     * show/hide progress bar which appears at the bottom when loading more items
     */
    public void showLoadingProgress() {
        if (mProgressLoadMore != null) {
            mProgressLoadMore.setVisibility(View.VISIBLE);
        }
    }

    public void hideLoadingProgress() {
        if (mProgressLoadMore != null) {
            mProgressLoadMore.setVisibility(View.GONE);
        }
    }


    /*
     * adapter used by the filter spinner
     */
    private class SpinnerAdapter extends BaseAdapter {
        private final FilterCriteria[] mFilterValues;
        private final LayoutInflater mInflater;

        SpinnerAdapter(Context context, FilterCriteria[] filterValues) {
            super();
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mFilterValues = filterValues;
        }

        @Override
        public int getCount() {
            return (mFilterValues != null ? mFilterValues.length : 0);
        }

        @Override
        public Object getItem(int position) {
            if (position < 0 || position >= getCount())
                return "";
            return mFilterValues[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.filter_spinner_item, parent, false);
            } else {
                view = convertView;
            }

            final TextView text = (TextView) view.findViewById(R.id.text);
            FilterCriteria selectedCriteria = (FilterCriteria)getItem(position);
            text.setText(selectedCriteria.getLabel());
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            FilterCriteria selectedCriteria = (FilterCriteria)getItem(position);
            final TagViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.toolbar_spinner_dropdown_item, parent, false);
                holder = new TagViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (TagViewHolder) convertView.getTag();
            }

            holder.textView.setText(selectedCriteria.getLabel());
            return convertView;
        }

        private class TagViewHolder {
            private final TextView textView;
            TagViewHolder(View view) {
                textView = (TextView) view.findViewById(R.id.text);
            }
        }

        public int getIndexOfCriteria(FilterCriteria tm) {
            int pos = -1;
            if (tm != null && mFilterValues != null){
                for (int i = 0; i < mFilterValues.length; i++) {
                    FilterCriteria obj = mFilterValues[i];
                    if (obj.equals(tm)) {
                        pos = i;
                        return pos;
                    }
                }
            }
            return pos;
        }
    }

    public interface Listener {
        FilterCriteria[] onLoadFilterCriteriaOptions();
        void onLoadData();
        void onFilterSelected(int position, FilterCriteria criteria);
        FilterCriteria onRecallSelection();
        String onShowEmptyViewMessage(EmptyViewMessageType emptyViewMsgType);
    }

}
