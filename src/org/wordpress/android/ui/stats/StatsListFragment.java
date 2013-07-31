package org.wordpress.android.ui.stats;

import java.util.Locale;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public class StatsListFragment extends StatsAbsCategoryFragment {

    protected TextView mEntryLabel;
    protected TextView mTotalsLabel;
    protected ListView mListView;
    protected CursorAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.stats_list_sub_fragment, container, false);
        
        mEntryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        mEntryLabel.setText(getEntryLabel().toUpperCase(Locale.getDefault()));
        
        mTotalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        mTotalsLabel.setText(getTotalsLabel().toUpperCase(Locale.getDefault()));
        
        mListView = (ListView) view.findViewById(R.id.stats_list_listview);
        mAdapter = new StatsCursorAdapter(getActivity(), null, false, getCategory());
        mListView.setAdapter(mAdapter);
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    protected String getEntryLabel() {
        return getCategory().getCategoryLabel();
    }

    protected String getTotalsLabel() {
        return getCategory().getTotalsLabel();
    }
    
    @Override
    protected void refreshData() {
        if (WordPress.getCurrentBlog() == null)
            return;
        
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        Cursor cursor = WordPress.wpStatsDB.getStats(blogId, getCategory(), getTimeframe().toInt());
        mAdapter.swapCursor(cursor);
    }
}
