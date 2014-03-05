package org.wordpress.android.ui.stats;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.datasets.StatsTopPostsAndPagesTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.FormatUtils;

/**
 * Fragment for top posts and pages stats. Has two pages, for Today's and Yesterday's stats.
 */
public class StatsTopPostsAndPagesFragment extends StatsAbsPagedViewFragment {
    
    private static final Uri STATS_TOP_POSTS_AND_PAGES_URI = StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI;
    private static final StatsTimeframe[] TIMEFRAMES = new StatsTimeframe[] { StatsTimeframe.TODAY, StatsTimeframe.YESTERDAY };

    public static final String TAG = StatsTopPostsAndPagesFragment.class.getSimpleName();

    @Override
    protected FragmentStatePagerAdapter getAdapter() {
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
            return TIMEFRAMES.length;
        }
        @Override
        public CharSequence getPageTitle(int position) {
            return TIMEFRAMES[position].getLabel(); 
        }
    }

    @Override
    protected Fragment getFragment(int position) {
        int entryLabelResId = R.string.stats_entry_posts_and_pages;
        int totalsLabelResId = R.string.stats_totals_views;
        int emptyLabelResId = R.string.stats_empty_top_posts;
        
        Uri uri = Uri.parse(STATS_TOP_POSTS_AND_PAGES_URI.toString() + "?timeframe=" + TIMEFRAMES[position].name());
        
        StatsCursorFragment fragment = StatsCursorFragment.newInstance(uri, entryLabelResId, totalsLabelResId, emptyLabelResId);
        fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));
        return fragment;
    }

    public class CustomCursorAdapter extends CursorAdapter {
        private final LayoutInflater inflater;

        public CustomCursorAdapter(Context context, Cursor c) {
            super(context, c, true);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            View view = inflater.inflate(R.layout.stats_list_cell, root, false);
            view.setTag(new StatsViewHolder(view));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final StatsViewHolder holder = (StatsViewHolder) view.getTag();

            final String entry = cursor.getString(cursor.getColumnIndex(StatsTopPostsAndPagesTable.Columns.TITLE));
            final String url = cursor.getString(cursor.getColumnIndex(StatsTopPostsAndPagesTable.Columns.URL));
            int total = cursor.getInt(cursor.getColumnIndex(StatsTopPostsAndPagesTable.Columns.VIEWS));

            // entries
            holder.setEntryTextOrLink(url, entry);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // no icon
            holder.networkImageView.setVisibility(View.GONE);
        }
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_top_posts_and_pages);
    }

    @Override
    protected String[] getTabTitles() {
        return StatsTimeframe.toStringArray(TIMEFRAMES);
    }
    
}
