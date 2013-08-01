package org.wordpress.android.ui.stats;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.StatsSearchEngineTermsTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.HorizontalTabView.TabListener;

public class StatsSearchEngineTermsFragment extends StatsAbsListViewFragment  implements TabListener {

    @Override
    public FragmentPagerAdapter getAdapter() {
        return new CustomPagerAdapter(getChildFragmentManager());
    }
    
    private class CustomPagerAdapter extends FragmentPagerAdapter {

        public CustomPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            int entryLabelResId = R.string.stats_entry_search_engine_terms;
            int totalsLabelResId = R.string.stats_totals_views;
            StatsCursorFragment fragment = StatsCursorFragment.newInstance(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, entryLabelResId, totalsLabelResId);
            mFragmentMap.put(position, fragment);
            fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0)
                return StatsTimeframe.TODAY.getLabel();
            else if (position == 1)
                return StatsTimeframe.YESTERDAY.getLabel();
            else 
                return ""; 
        }
        
    }
    
    public class CustomCursorAdapter extends CursorAdapter {

        public CustomCursorAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            String entry = cursor.getString(cursor.getColumnIndex(StatsSearchEngineTermsTable.Columns.SEARCH));
            int total = cursor.getInt(cursor.getColumnIndex(StatsSearchEngineTermsTable.Columns.VIEWS));

            // entries
            TextView entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
            entryTextView.setText(entry);
            
            // totals
            TextView totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
            totalsTextView.setText(total + "");
            
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.stats_list_cell, root, false);
        }

    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_search_engine_terms);
    }

}
