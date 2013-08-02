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
import org.wordpress.android.datasets.StatsTopAuthorsTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.HorizontalTabView.TabListener;

public class StatsTopAuthorsFragment extends StatsAbsListViewFragment  implements TabListener {

    private static final String[] TITLES = new String[] { StatsTimeframe.TODAY.getLabel(), StatsTimeframe.YESTERDAY.getLabel() };
    
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
            return TITLES[position];
        }

    }

    @Override
    protected Fragment getFragment(int position) {
        int entryLabelResId = R.string.stats_entry_authors;
        int totalsLabelResId = R.string.stats_totals_views;
        StatsCursorFragment fragment = StatsCursorFragment.newInstance(StatsContentProvider.STATS_TOP_AUTHORS_URI, entryLabelResId, totalsLabelResId);
        fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));
        return fragment;
    }
    
    public class CustomCursorAdapter extends CursorAdapter {

        public CustomCursorAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            String entry = cursor.getString(cursor.getColumnIndex(StatsTopAuthorsTable.Columns.NAME));
            int total = cursor.getInt(cursor.getColumnIndex(StatsTopAuthorsTable.Columns.VIEWS));
            String imageUrl = cursor.getString(cursor.getColumnIndex(StatsTopAuthorsTable.Columns.IMAGE_URL));

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
    public String getTitle() {
        return getString(R.string.stats_view_top_authors);
    }

    @Override
    public String[] getTabTitles() {
        return TITLES;
    }

}
