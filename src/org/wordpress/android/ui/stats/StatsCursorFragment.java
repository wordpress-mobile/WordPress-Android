package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.Utils;

/**
 * A fragment that appears as a 'page' in the {@link StatsAbsPagedViewFragment}. 
 * The fragment has a {@link ContentObserver} to listen for changes in the supplied URIs.
 * By implementing {@link LoaderCallbacks}, it asynchronously fetches new data to update itself.
 * <p>
 * For phone layouts, this fragment appears as a listview, with the CursorAdapter supplying the cells.
 * </p>
 * <p>
 * For tablet layouts, this fragment appears as a linearlayout, with a maximum of 10 entries. 
 * A linearlayout is necessary because a listview cannot be placed inside the scrollview of the tablet's root layout.
 * The linearlayout also gets its views from the CursorAdapter.
 * </p> 
 */
public class StatsCursorFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int MAX_ITEMS_ON_TABLET = 10;
    private static final String ARGS_URI = "ARGS_URI";
    private static final String ARGS_ENTRY_LABEL = "ARGS_ENTRY_LABEL";
    private static final String ARGS_TOTALS_LABEL = "ARGS_TOTALS_LABEL";
    private static final String ARGS_EMPTY_LABEL = "ARGS_EMPTY_LABEL";

    public static final String TAG = StatsCursorFragment.class.getSimpleName();
    
    private TextView mEntryLabel;
    private TextView mTotalsLabel;
    private TextView mEmptyLabel;
    private ListView mListView;
    private LinearLayout mLinearLayout;

    private CursorAdapter mAdapter;
    private ContentObserver mContentObserver = new MyObserver(new Handler());
    
    private StatsCursorInterface mCallback;
    
    public static StatsCursorFragment newInstance(Uri uri, int entryLabelResId, int totalsLabelResId, int emptyLabelResId) {
        
        StatsCursorFragment fragment = new StatsCursorFragment();
        
        Bundle args = new Bundle();
        args.putString(ARGS_URI, uri.toString());
        args.putInt(ARGS_ENTRY_LABEL, entryLabelResId);
        args.putInt(ARGS_TOTALS_LABEL, totalsLabelResId);
        args.putInt(ARGS_EMPTY_LABEL, emptyLabelResId);
        fragment.setArguments(args);
        
        return fragment;
    }
    
    private Uri getUri() {
        return Uri.parse(getArguments().getString(ARGS_URI));
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        try {
            mCallback = (StatsCursorInterface) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getParentFragment().toString() + " must implement " + StatsCursorInterface.class.getSimpleName());
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_list_fragment, container, false);
        
        mEntryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        mEntryLabel.setText(getEntryLabelResId());
        mTotalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        mTotalsLabel.setText(getTotalsLabelResId());
        mEmptyLabel = (TextView) view.findViewById(R.id.stats_list_empty_text);
        mEmptyLabel.setText(Html.fromHtml(getString(getEmptyLabelResId())));
        configureEmptyLabel();
        
        if (isTablet()) {
            mLinearLayout = (LinearLayout) view.findViewById(R.id.stats_list_linearlayout);
            mLinearLayout.setVisibility(View.VISIBLE);
        } else {
            mListView = (ListView) view.findViewById(R.id.stats_list_listview);
            mListView.setAdapter(mAdapter);
            mListView.setVisibility(View.VISIBLE);
        }
        
        return view;
    }

    private int getEntryLabelResId() {
        return getArguments().getInt(ARGS_ENTRY_LABEL);
    }

    private int getTotalsLabelResId() {
        return getArguments().getInt(ARGS_TOTALS_LABEL);
    }

    private int getEmptyLabelResId() {
        return getArguments().getInt(ARGS_EMPTY_LABEL);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(0, null, this);
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (WordPress.getCurrentBlog() == null)
            return null;
        
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        CursorLoader cursorLoader = new CursorLoader(getActivity(), getUri(), null, "blogId=?", new String[] { blogId }, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCallback.onCursorLoaded(getUri(), data);
        if (mAdapter != null)
            mAdapter.changeCursor(data);
        configureEmptyLabel();
        if (isTablet()) {
            reloadLinearLayout();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.changeCursor(null);
        configureEmptyLabel();
        if (isTablet()) {
            reloadLinearLayout();
        }
    }

    public void setListAdapter(CursorAdapter adapter) {
        mAdapter = adapter;
        if (isTablet()) {
            reloadLinearLayout();
        }
    }

    private void reloadLinearLayout() {
        if (mLinearLayout == null || mAdapter == null)
            return; 
        
        mLinearLayout.removeAllViews();
        
        // limit number of items to show otherwise it would cause performance issues on the linearlayout
        int count = Math.min(mAdapter.getCount(), MAX_ITEMS_ON_TABLET);
        for (int i = 0; i < count; i++) {
            View view = mAdapter.getView(i, null, mLinearLayout);
            if (i % 2 == 1)
                view.setBackgroundColor(getResources().getColor(R.color.stats_alt_row));
            mLinearLayout.addView(view);

            // add divider
            getActivity().getLayoutInflater().inflate(R.layout.stats_list_divider, mLinearLayout, true);
        }
        
    }
    
    @Override
    public void onResume() {
        super.onResume();
        getActivity().getContentResolver().registerContentObserver(getUri(), true, mContentObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(mContentObserver);
    }
    
    class MyObserver extends ContentObserver {      
       public MyObserver(Handler handler) {
          super(handler);           
       }

       @Override
       public void onChange(boolean selfChange) {
           if (isAdded())
               getLoaderManager().restartLoader(0, null, StatsCursorFragment.this);           
       }        
    }
    
    private boolean isTablet() {
        return Utils.isTablet();
    }

    private void configureEmptyLabel() {
        if (mAdapter == null || mAdapter.getCount() == 0)
            mEmptyLabel.setVisibility(View.VISIBLE);
        else
            mEmptyLabel.setVisibility(View.GONE);
    }
}
