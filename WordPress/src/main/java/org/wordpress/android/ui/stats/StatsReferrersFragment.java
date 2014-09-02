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
 * Fragment for referrer stats. Has two pages, for Today's and Yesterday's stats.
 * Referrers contain expandable lists.
 */
public class StatsReferrersFragment extends StatsAbsPagedViewFragment {
    private static final Uri STATS_REFERRER_GROUP_URI = StatsContentProvider.STATS_REFERRER_GROUP_URI;
    private static final Uri STATS_REFERRERS_URI = StatsContentProvider.STATS_REFERRERS_URI;
    private static final StatsTimeframe[] TIMEFRAMES = new StatsTimeframe[] { StatsTimeframe.TODAY, StatsTimeframe.YESTERDAY };

    public static final String TAG = StatsReferrersFragment.class.getSimpleName();

    @Override
    protected Fragment getFragment(int position) {
        Uri groupUri = Uri.parse(STATS_REFERRER_GROUP_URI.toString() + "?timeframe=" + TIMEFRAMES[position].name());
        Uri childrenUri = STATS_REFERRERS_URI;

        StatsCursorTreeFragment fragment = StatsCursorTreeFragment.newInstance(groupUri, childrenUri,
                R.string.stats_entry_referrers, R.string.stats_totals_views, R.string.stats_empty_referrers_title,
                R.string.stats_empty_referrers_desc, getLocalTableBlogID());
        CustomAdapter adapter = new CustomAdapter(null, getActivity());
        adapter.setCursorLoaderCallback(fragment);
        fragment.setListAdapter(adapter);
        fragment.setCallback(this);
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

    @Override
    protected int getInnerFragmentID() {
        return R.id.stats_referrers;
    }
}
