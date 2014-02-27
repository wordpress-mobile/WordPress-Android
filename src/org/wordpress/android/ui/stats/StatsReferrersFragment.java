package org.wordpress.android.ui.stats;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsReferrerGroupsTable;
import org.wordpress.android.datasets.StatsReferrersTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.StatUtils;

/**
 * Fragment for referrer stats. Has two pages, for Today's and Yesterday's stats.
 * Referrers contain expandable lists.
 */
public class StatsReferrersFragment extends StatsAbsPagedViewFragment {
    
    private static final Uri STATS_REFERRER_GROUP_URI = StatsContentProvider.STATS_REFERRER_GROUP_URI;
    private static final Uri STATS_REFERRERS_URI = StatsContentProvider.STATS_REFERRERS_URI;
    private static final StatsTimeframe[] TIMEFRAMES = new StatsTimeframe[] { StatsTimeframe.TODAY, StatsTimeframe.YESTERDAY };
    
    public static final String TAG = StatsReferrersFragment.class.getSimpleName();
    
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
        int entryLabelResId = R.string.stats_entry_referrers;
        int totalsLabelResId = R.string.stats_totals_views;
        int emptyLabelResId = R.string.stats_empty_referrers;
        
        Uri groupUri = Uri.parse(STATS_REFERRER_GROUP_URI.toString() + "?timeframe=" + TIMEFRAMES[position].name());
        Uri childrenUri = STATS_REFERRERS_URI;
        
        StatsCursorTreeFragment fragment = StatsCursorTreeFragment.newInstance(groupUri, childrenUri, entryLabelResId, totalsLabelResId, emptyLabelResId);
        CustomAdapter adapter = new CustomAdapter(null, getActivity());
        adapter.setCursorLoaderCallback(fragment);
        fragment.setListAdapter(adapter);
        return fragment;
    }


    public class CustomAdapter extends CursorTreeAdapter {
        private final LayoutInflater inflater;
        private StatsCursorLoaderCallback mCallback;

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
            view.setTag(new StatsViewHolder(view));
            return view;
        }

        @Override
        protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
            final StatsViewHolder holder = (StatsViewHolder) view.getTag();

            String name = cursor.getString(cursor.getColumnIndex(StatsReferrersTable.Columns.NAME));
            int total = cursor.getInt(cursor.getColumnIndex(StatsReferrersTable.Columns.TOTAL));

            // name, url
            if (name != null && name.startsWith("http")) {
                StatUtils.setTextHyperlink(holder.entryTextView, name, name);
            } else {
                holder.entryTextView.setText(name);
            }

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // no icon
            holder.networkImageView.setVisibility(View.GONE);
        }

        @Override
        protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
            View view = inflater.inflate(R.layout.stats_list_cell, parent, false);
            view.setTag(new StatsViewHolder(view));
            return view;
        }

        @Override
        protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
            final StatsViewHolder holder = (StatsViewHolder) view.getTag();

            String name = cursor.getString(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.NAME));
            int total = cursor.getInt(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.TOTAL));
            String url = cursor.getString(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.URL));
            String icon = cursor.getString(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.ICON));
            int children = cursor.getInt(cursor.getColumnIndex(StatsReferrerGroupsTable.Columns.CHILDREN));

            // name, url
            if (!TextUtils.isEmpty(url)) {
                StatUtils.setTextHyperlink(holder.entryTextView, url, name);
            } else {
                holder.entryTextView.setText(name);
            }

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // icon
            if (!TextUtils.isEmpty(icon)) {
                holder.networkImageView.setImageUrl(icon, WordPress.imageLoader);
            } else {
                holder.networkImageView.setImageDrawable(null);
            }

            // expand/collapse chevron
            holder.chevronImageView.setVisibility(children > 0 ? View.VISIBLE : View.GONE);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            Bundle bundle = new Bundle();
            bundle.putLong(StatsCursorLoaderCallback.BUNDLE_DATE, groupCursor.getLong(groupCursor.getColumnIndex("date")));
            bundle.putString(StatsCursorLoaderCallback.BUNDLE_GROUP_ID, groupCursor.getString(groupCursor.getColumnIndex("groupId")));
            mCallback.onUriRequested(groupCursor.getPosition(), STATS_REFERRERS_URI, bundle);
            return null;
        }
    }
    
    @Override
    public String getTitle() {
        return getString(R.string.stats_view_referrers);
    }

    @Override
    protected String[] getTabTitles() {
        return StatsTimeframe.toStringArray(TIMEFRAMES);
    }

}
