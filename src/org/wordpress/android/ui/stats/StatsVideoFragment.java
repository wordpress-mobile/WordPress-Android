package org.wordpress.android.ui.stats;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import org.wordpress.android.R;
import org.wordpress.android.datasets.StatsVideosTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.ui.HorizontalTabView.TabListener;

public class StatsVideoFragment extends StatsAbsListViewFragment  implements TabListener {
    
    private static final String[] TITLES = new String[] { StatsTimeframe.TODAY.getLabel(), StatsTimeframe.YESTERDAY.getLabel(), "Summary" };
    
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
            return TITLES.length;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position]; 
        }

    }

    @Override
    protected Fragment getFragment(int position) {
        if (position < 2) {
            int entryLabelResId = R.string.stats_entry_video_plays;
            int totalsLabelResId = R.string.stats_totals_plays;
            StatsCursorFragment fragment = StatsCursorFragment.newInstance(StatsContentProvider.STATS_VIDEOS_URI, entryLabelResId, totalsLabelResId);
            fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));
            return fragment;
        } else {
            VideoSummaryFragment fragment = new VideoSummaryFragment();
            return fragment;
        }
    }
    
    public class CustomCursorAdapter extends CursorAdapter {

        public CustomCursorAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            String entry = cursor.getString(cursor.getColumnIndex(StatsVideosTable.Columns.NAME));
            String url = cursor.getString(cursor.getColumnIndex(StatsVideosTable.Columns.URL));
            int total = cursor.getInt(cursor.getColumnIndex(StatsVideosTable.Columns.PLAYS));

            // entries
            TextView entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
            if (url != null && url.length() > 0) {
                Spanned link = Html.fromHtml("<a href=\"" + url + "\">" + entry + "</a>");
                entryTextView.setText(link);
                entryTextView.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                entryTextView.setText(entry);
            }
            
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
        return getString(R.string.stats_view_video_plays);
    }

    
    public static class VideoSummaryFragment extends SherlockFragment {
        
        private TextView mHeader;
        private TextView mPlays;
        private TextView mImpressions;
        private TextView mPlaybackTotals;
        private TextView mPlaybackUnit;
        private TextView mBandwidth;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.stats_video_summary, container, false); 
            
            mHeader = (TextView) view.findViewById(R.id.stats_video_summary_header);
            mHeader.setText("Aggregated stat for August 2013");
            mPlays = (TextView) view.findViewById(R.id.stats_video_summary_plays_total);
            mImpressions = (TextView) view.findViewById(R.id.stats_video_summary_impressions_total);
            mPlaybackTotals = (TextView) view.findViewById(R.id.stats_video_summary_playback_length_total);
            mPlaybackUnit = (TextView) view.findViewById(R.id.stats_video_summary_playback_length_unit);
            mBandwidth = (TextView) view.findViewById(R.id.stats_video_summary_bandwidth_total);
            
            return view;
        }
        
    }

    @Override
    public String[] getTabTitles() {
        return TITLES;
    }
}
