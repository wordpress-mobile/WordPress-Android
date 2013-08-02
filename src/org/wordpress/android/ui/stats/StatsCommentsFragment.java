package org.wordpress.android.ui.stats;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsMostCommentedTable;
import org.wordpress.android.datasets.StatsTopCommentersTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.HorizontalTabView.TabListener;

public class StatsCommentsFragment extends StatsAbsListViewFragment implements TabListener {

    private static final String[] TITLES = new String[] { "Top Recent Commenters", "Most Commented", "Summary" };
    
    private static final int TOP_COMMENTERS = 0;
    private static final int MOST_COMMENTED = 1;
    
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
            if (position == 0) {
                int entryLabelResId = R.string.stats_entry_top_commenter;
                int totalsLabelResId = R.string.stats_totals_comments;
                StatsCursorFragment fragment = StatsCursorFragment.newInstance(StatsContentProvider.STATS_TOP_COMMENTERS_URI, entryLabelResId, totalsLabelResId);
                mFragmentMap.put(position, fragment);
                fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null, TOP_COMMENTERS));
                return fragment;
            } else if (position == 1) {
                int entryLabelResId = R.string.stats_entry_most_commented;
                int totalsLabelResId = R.string.stats_totals_comments;
                StatsCursorFragment fragment = StatsCursorFragment.newInstance(StatsContentProvider.STATS_MOST_COMMENTED_URI, entryLabelResId, totalsLabelResId);
                fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null, MOST_COMMENTED));
                mFragmentMap.put(position, fragment);
                return fragment;
            } else {
                CommentsSummaryFragment fragment = new CommentsSummaryFragment();
                mFragmentMap.put(position, fragment);
                return fragment;
            }
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }
        
    }
    
    public class CustomCursorAdapter extends CursorAdapter {

        private int mType;

        public CustomCursorAdapter(Context context, Cursor c, int type) {
            super(context, c, true);
            mType = type;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            // entry
            String entry;
            if (mType == TOP_COMMENTERS)
                entry = cursor.getString(cursor.getColumnIndex(StatsTopCommentersTable.Columns.NAME));
            else 
                entry = cursor.getString(cursor.getColumnIndex(StatsMostCommentedTable.Columns.POST));

            TextView entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
            entryTextView.setText(entry);

            
            // totals
            int total;
            if (mType == TOP_COMMENTERS)
                total = cursor.getInt(cursor.getColumnIndex(StatsTopCommentersTable.Columns.COMMENTS));
            else 
                total = cursor.getInt(cursor.getColumnIndex(StatsMostCommentedTable.Columns.COMMENTS));
            
            TextView totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
            totalsTextView.setText(total + "");
            
            // image 
            String imageUrl;
            if (mType == TOP_COMMENTERS) {
                imageUrl = cursor.getString(cursor.getColumnIndex(StatsTopCommentersTable.Columns.IMAGE_URL));
                
                NetworkImageView imageView = (NetworkImageView) view.findViewById(R.id.stats_list_cell_image);
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageUrl(imageUrl, WordPress.imageLoader);
            }
            
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.stats_list_cell, root, false);
        }

    }
    
    @Override
    public String getTitle() {
        return getString(R.string.stats_view_comments);
    }

    public static class CommentsSummaryFragment extends SherlockFragment {
        
        private TextView mPerMonthText;
        private TextView mTotalText;
        private TextView mActiveDayText;
        private TextView mActiveTimeText;
        private TextView mMostCommentedText;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.stats_comments_summary, container, false);
            
            mPerMonthText = (TextView) view.findViewById(R.id.stats_comments_summary_per_month_count);
            mTotalText = (TextView) view.findViewById(R.id.stats_comments_summary_total_count);
            mActiveDayText = (TextView) view.findViewById(R.id.stats_comments_summary_most_active_day_text);
            mActiveTimeText = (TextView) view.findViewById(R.id.stats_comments_summary_most_active_time_text);
            mMostCommentedText = (TextView) view.findViewById(R.id.stats_comments_summary_most_commented_text);
            
            return view;
        }
        
    }
    
}
