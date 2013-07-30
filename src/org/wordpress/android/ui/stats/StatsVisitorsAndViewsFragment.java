package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public class StatsVisitorsAndViewsFragment extends StatsAbsCategoryFragment {

    @Override
    protected void refreshData() {
        if (WordPress.getCurrentBlog() == null)
            return;
        
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());

    }
    
    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment, container, false);
        
        return view;
    }

}
