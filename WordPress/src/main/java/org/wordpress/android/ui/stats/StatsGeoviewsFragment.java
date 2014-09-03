package org.wordpress.android.ui.stats;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import org.wordpress.android.R;
import org.wordpress.android.datasets.StatsGeoviewsTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.FormatUtils;

/**
 * Fragment for geoview (views by country) stats. Has two pages, for Today's and Yesterday's stats.
 */
public class StatsGeoviewsFragment extends StatsAbsPagedViewFragment {
    private static final Uri STATS_GEOVIEWS_URI = StatsContentProvider.STATS_GEOVIEWS_URI;

    private static final StatsTimeframe[] TIMEFRAMES = new StatsTimeframe[] { StatsTimeframe.TODAY, StatsTimeframe.YESTERDAY };

    public static final String TAG = StatsGeoviewsFragment.class.getSimpleName();

    @Override
    protected Fragment getFragment(int position) {
        int entryLabelResId = R.string.stats_entry_country;
        int totalsLabelResId = R.string.stats_totals_views;
        int emptyLabelResId = R.string.stats_empty_geoviews;

        Uri uri = Uri.parse(STATS_GEOVIEWS_URI.toString() + "?timeframe=" + TIMEFRAMES[position].name());

        StatsCursorFragment fragment = StatsCursorFragment.newInstance(uri, entryLabelResId, totalsLabelResId, emptyLabelResId, getLocalTableBlogID());
        fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));
        fragment.setCallback(this);
        return fragment;
    }

    public static class CustomCursorAdapter extends CursorAdapter {
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

            String entry = cursor.getString(cursor.getColumnIndex(StatsGeoviewsTable.Columns.COUNTRY));
            String imageUrl = cursor.getString(cursor.getColumnIndex(StatsGeoviewsTable.Columns.IMAGE_URL));
            int total = cursor.getInt(cursor.getColumnIndex(StatsGeoviewsTable.Columns.VIEWS));

            holder.entryTextView.setText(entry);
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // image (country flag)
            holder.showNetworkImage(imageUrl);
        }
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_views_by_country);
    }

    @Override
    protected String[] getTabTitles() {
        return StatsTimeframe.toStringArray(TIMEFRAMES);
    }

    @Override
    protected int getInnerFragmentID() {
        return R.id.stats_geoviews;
    }
}
