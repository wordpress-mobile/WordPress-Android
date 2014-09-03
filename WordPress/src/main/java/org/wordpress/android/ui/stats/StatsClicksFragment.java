package org.wordpress.android.ui.stats;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;

import org.wordpress.android.R;
import org.wordpress.android.datasets.StatsReferrerGroupsTable;
import org.wordpress.android.datasets.StatsReferrersTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.FormatUtils;

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
    protected String[] getTabTitles() {
        return StatsTimeframe.toStringArray(TIMEFRAMES);
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_clicks);
    }

    @Override
    protected int getInnerFragmentID() {
        return R.id.stats_clicks;
    }

    @Override
    protected Fragment getFragment(int position) {
        Uri groupUri = Uri.parse(STATS_CLICK_GROUP_URI.toString() + "?timeframe=" + TIMEFRAMES[position].name());
        Uri childrenUri = STATS_CLICKS_URI;

        StatsCursorTreeFragment fragment = StatsCursorTreeFragment.newInstance(groupUri, childrenUri,
                R.string.stats_entry_clicks_url, R.string.stats_totals_clicks, R.string.stats_empty_clicks_title,
                R.string.stats_empty_clicks_desc, getLocalTableBlogID());
        CustomAdapter adapter = new CustomAdapter(null, getActivity());
        adapter.setCursorLoaderCallback(fragment);
        fragment.setListAdapter(adapter);
        fragment.setCallback(this);
        return fragment;
    }

    public class CustomAdapter extends CursorTreeAdapter {
        private StatsCursorLoaderCallback mCallback;
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
            view.setTag(new StatsViewHolder(view));
            return view;
        }

        @Override
        protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
            final StatsViewHolder holder = (StatsViewHolder)view.getTag();

            String name = cursor.getString(cursor.getColumnIndex(StatsReferrersTable.Columns.NAME));
            int total = cursor.getInt(cursor.getColumnIndex(StatsReferrersTable.Columns.TOTAL));

            // name, url
            holder.setEntryTextOrLink(name, name);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // no icon, make it invisible so children are indented
            holder.networkImageView.setVisibility(View.INVISIBLE);
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
            holder.setEntryTextOrLink(url, name);

            // totals
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // icon
            holder.showNetworkImage(icon);

            // expand/collapse chevron
            holder.chevronImageView.setVisibility(children > 0 ? View.VISIBLE : View.GONE);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            Bundle bundle = new Bundle();
            bundle.putLong(StatsCursorLoaderCallback.BUNDLE_DATE, groupCursor.getLong(groupCursor.getColumnIndex("date")));
            bundle.putString(StatsCursorLoaderCallback.BUNDLE_GROUP_ID, groupCursor.getString(groupCursor.getColumnIndex("groupId")));
            mCallback.onUriRequested(groupCursor.getPosition(), STATS_CLICKS_URI, bundle);
            return null;
        }
    }
}
