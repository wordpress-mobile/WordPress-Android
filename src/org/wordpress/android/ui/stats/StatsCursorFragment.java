package org.wordpress.android.ui.stats;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import org.wordpress.android.R;
import org.wordpress.android.util.Utils;

public class StatsCursorFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int MAX_ITEMS_ON_TABLET = 10;
    private static final String ARGS_URI = "ARGS_URI";
    private static final String ARGS_ENTRY_LABEL = "ARGS_ENTRY_LABEL";
    private static final String ARGS_TOTALS_LABEL = "ARGS_TOTALS_LABEL";

    public static final String TAG = StatsCursorFragment.class.getSimpleName();
    
    private TextView mEntryLabel;
    private TextView mTotalsLabel;
    private ListView mListView;
    private LinearLayout mLinearLayout;

    private CursorAdapter mAdapter;
    private ContentObserver mContentObserver = new MyObserver(new Handler());

    public static StatsCursorFragment newInstance(Uri uri, int entryLabelResId, int totalsLabelResId) {
        
        StatsCursorFragment fragment = new StatsCursorFragment();
        
        Bundle args = new Bundle();
        args.putString(ARGS_URI, uri.toString());
        args.putInt(ARGS_ENTRY_LABEL, entryLabelResId);
        args.putInt(ARGS_TOTALS_LABEL, totalsLabelResId);
        fragment.setArguments(args);
        
        return fragment;
    }
    
    public Uri getUri() {
        return Uri.parse(getArguments().getString(ARGS_URI));
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_list_sub_fragment, container, false);
        
        mEntryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        mEntryLabel.setText(getEntryLabelResId());
        mTotalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        mTotalsLabel.setText(getTotalsLabelResId());
        
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(0, null, this);
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = new CursorLoader(getActivity(), getUri(), null, null, null, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mAdapter != null)
            mAdapter.swapCursor(data);
        if (isTablet()) {
            reloadLinearLayout();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.swapCursor(null);
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
        for (int i=0; i < count; i++) {
            View view = mAdapter.getView(i, null, mLinearLayout);
            mLinearLayout.addView(view);
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
}
