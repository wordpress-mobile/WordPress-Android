package org.wordpress.android.ui.stats;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsReferrerGroupsTable;
import org.wordpress.android.datasets.StatsReferrersTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.StatUtils;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Fragment for click stats. Has two pages, for Today's and Yesterday's stats.
 * Clicks contain expandable lists.
 */
public class StatsClicksFragment extends StatsAbsPagedViewFragment {

    private static final Uri STATS_CLICK_GROUP_URI = StatsContentProvider.STATS_CLICK_GROUP_URI;
    private static final Uri STATS_CLICKS_URI = StatsContentProvider.STATS_CLICKS_URI;

    private static final StatsTimeframe[] TIMEFRAMES = new StatsTimeframe[] { StatsTimeframe.TODAY, StatsTimeframe.YESTERDAY };
    
    public static final String TAG = StatsClicksFragment.class.getSimpleName();
    
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
    protected String[] getTabTitles() {
        return StatsTimeframe.toStringArray(TIMEFRAMES);
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_clicks);
    }
    

    @Override
    protected Fragment getFragment(int position) {
        int entryLabelResId = R.string.stats_entry_clicks_url;
        int totalsLabelResId = R.string.stats_totals_clicks;
        int emptyLabelResId = R.string.stats_empty_clicks;
        
        Uri groupUri = Uri.parse(STATS_CLICK_GROUP_URI.toString() + "?timeframe=" + TIMEFRAMES[position].name());
        Uri childrenUri = STATS_CLICKS_URI;
        
        StatsCursorTreeFragment fragment = StatsCursorTreeFragment.newInstance(groupUri, childrenUri, entryLabelResId, totalsLabelResId, emptyLabelResId);
        CustomAdapter adapter = new CustomAdapter(null, getActivity());
        adapter.setCursorLoaderCallback(fragment);
        fragment.setListAdapter(adapter);
        return fragment;
    }

    public class CustomAdapter extends CursorTreeAdapter {
        private StatsCursorLoaderCallback mCallback;
        private final DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        private final LayoutInflater inflater;

        public CustomAdapter(Cursor cursor, Context context) {
            super(cursor, context, true);
            inflater = LayoutInflater.from(context);
        }

        public void setCursorLoaderCallback(StatsCursorLoaderCallback callback) {
            mCallback = callback;
        }

        @Override
        protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
            View view = inflater.inflate(R.layout.stats_list_cell, parent, false);
            view.setTag(new StatsChildViewHolder(view));
            return view;
        }

        @Override
        protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
            final StatsChildViewHolder holder = (StatsChildViewHolder)view.getTag();

            String name = cursor.getString(cursor.getColumnIndex(StatsReferrersTable.Columns.NAME));
            int total = cursor.getInt(cursor.getColumnIndex(StatsReferrersTable.Columns.TOTAL));

            // name, url
            if (name != null && name.startsWith("http")) {
                StatUtils.hyperlinkEntryText(holder.entryTextView, name, name);
            } else {
                holder.entryTextView.setText(name);
            }

            // totals
            holder.totalsTextView.setText(formatter.format(total));

            // no icon
        }

        @Override
        protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
            View view = inflater.inflate(R.layout.stats_group_cell, parent, false);
            view.setTag(new StatsGroupViewHolder(view));
            return view;
        }

        @Override
        protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
            final StatsGroupViewHolder holder = (StatsGroupViewHolder) view.getTag();

            String name = cursor.getString(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.NAME));
            int total = cursor.getInt(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.TOTAL));
            String url = cursor.getString(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.URL));
            String icon = cursor.getString(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.ICON));
            int children = cursor.getInt(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.CHILDREN));

            boolean urlValid = (url != null && url.length() > 0); 
            
            // chevron
            toggleChevrons(children > 0, isExpanded, view);
            
            // name, url
            if (urlValid) {
                StatUtils.hyperlinkEntryText(holder.entryTextView, url, name);
            } else {
                holder.entryTextView.setText(name);
            }

            // totals
            holder.totalsTextView.setText(formatter.format(total));

            // icon
            holder.imageFrame.setVisibility(View.VISIBLE);
            if (icon != null && icon.length() > 0) {
                holder.networkImageView.setImageUrl(icon, WordPress.imageLoader);
                holder.networkImageView.setVisibility(View.VISIBLE);
                holder.errorImageView.setVisibility(View.GONE);
            } else {
                holder.networkImageView.setVisibility(View.GONE);
                holder.errorImageView.setVisibility(View.VISIBLE);
            }   
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            Bundle bundle = new Bundle();
            bundle.putLong(StatsCursorLoaderCallback.BUNDLE_DATE, groupCursor.getLong(groupCursor.getColumnIndex("date")));
            bundle.putString(StatsCursorLoaderCallback.BUNDLE_GROUP_ID, groupCursor.getString(groupCursor.getColumnIndex("groupId")));
            mCallback.onUriRequested(groupCursor.getPosition(), STATS_CLICKS_URI, bundle);
            return null;
        }

        private void toggleChevrons(boolean isVisible, boolean isExpanded, View view) {
            ImageView chevronUp = (ImageView) view.findViewById(R.id.stats_group_cell_chevron_up);
            ImageView chevronDown = (ImageView) view.findViewById(R.id.stats_group_cell_chevron_down);
            View frame = view.findViewById(R.id.stats_group_cell_chevron_frame);
            
            if (isVisible) {
                frame.setVisibility(View.VISIBLE);  
                if (isExpanded) {
                    chevronUp.setVisibility(View.VISIBLE);
                    chevronDown.setVisibility(View.GONE);
                } else {
                    chevronUp.setVisibility(View.GONE);
                    chevronDown.setVisibility(View.VISIBLE);
                }
            } else {
                frame.setVisibility(View.GONE);
            }
        }
        
    }

}
