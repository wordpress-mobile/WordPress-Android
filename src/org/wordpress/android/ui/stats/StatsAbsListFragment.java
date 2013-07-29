package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;

public abstract class StatsAbsListFragment extends StatsAbsCategoryFragment {

    protected Button mViewSummariesBtn;
    protected TextView mEntryLabel;
    protected TextView mTotalsLabel;
    protected ListView mListView;
    private TextView mDebugText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.stats_list_sub_fragment, container, false);
        
        mViewSummariesBtn = (Button) view.findViewById(R.id.stats_list_view_summaries_btn);
        mEntryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        mEntryLabel.setText(getEntryLabel());
        mTotalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        mTotalsLabel.setText(getTotalsLabel());
        mListView = (ListView) view.findViewById(R.id.stats_list_listview);
        
        mDebugText = (TextView) view.findViewById(R.id.stats_list_timeframe_debug);
        mDebugText.setText(getTimeframe().name());
        
        return view;
    }

    protected String getEntryLabel() {
        return getCategory().getCategoryLabel();
    }

    protected String getTotalsLabel() {
        return getCategory().getTotalsLabel();
    }
}
