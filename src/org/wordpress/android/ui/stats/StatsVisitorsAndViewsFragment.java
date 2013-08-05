package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;

public class StatsVisitorsAndViewsFragment extends StatsAbsViewFragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment, container, false);
        
        return view;
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_visitors_and_views);
    }

    @Override
    public void refresh() {
        // TODO Auto-generated method stub
        
    }

}
