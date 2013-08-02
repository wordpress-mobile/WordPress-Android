package org.wordpress.android.ui.stats;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsClicksTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.HorizontalTabView.TabListener;

public class StatsClicksFragment extends StatsAbsListViewFragment implements TabListener {

    private static final String[] TITLES = new String[] { StatsTimeframe.TODAY.getLabel(), StatsTimeframe.YESTERDAY.getLabel() };
    
    public static final String TAG = StatsClicksFragment.class.getSimpleName();
    
    @Override
    public FragmentStatePagerAdapter getAdapter() {
        return new CustomPagerAdapter(getChildFragmentManager());
    }
    
    private class CustomPagerAdapter extends FragmentStatePagerAdapter {

        public CustomPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return getFragment(position);
        }

        @Override
        public int getCount() {
            return 2;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            String[] titles = getTabTitles();
            return titles[position];
        }
        
    }
    
    public String[] getTabTitles() {
        return TITLES;
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_clicks);
    }
    
    public static class CustomCursorAdapter extends CursorAdapter {

        public CustomCursorAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            String entry = cursor.getString(cursor.getColumnIndex(StatsClicksTable.Columns.URL));
            int total = cursor.getInt(cursor.getColumnIndex(StatsClicksTable.Columns.CLICKS));
            String imageUrl = cursor.getString(cursor.getColumnIndex(StatsClicksTable.Columns.IMAGE_URL));

            // entries
            TextView entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
            entryTextView.setText(entry);
            
            // totals
            TextView totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
            totalsTextView.setText(total + "");
            
            // image
            NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.stats_list_cell_image);
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageUrl(imageUrl, WordPress.imageLoader);
            
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.stats_list_cell, root, false);
        }

    }

    @Override
    protected Fragment getFragment(int position) {
        int entryLabelResId = R.string.stats_entry_clicks_url;
        int totalsLabelResId = R.string.stats_totals_clicks;
        StatsCursorFragment fragment = StatsCursorFragment.newInstance(StatsContentProvider.STATS_CLICKS_URI, entryLabelResId, totalsLabelResId);
        fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));
        return fragment;
    }

}
