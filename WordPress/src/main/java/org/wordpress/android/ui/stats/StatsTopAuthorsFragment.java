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
import org.wordpress.android.datasets.StatsTopAuthorsTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.FormatUtils;

/**
 * Fragment for top author stats. Has two pages, for Today's and Yesterday's stats.
 */
public class StatsTopAuthorsFragment extends StatsAbsPagedViewFragment {
    private static final Uri STATS_TOP_AUTHORS_URI = StatsContentProvider.STATS_TOP_AUTHORS_URI;
    private static final StatsTimeframe[] TIMEFRAMES = new StatsTimeframe[] { StatsTimeframe.TODAY, StatsTimeframe.YESTERDAY };

    public static final String TAG = StatsTopAuthorsFragment.class.getSimpleName();

    @Override
    protected Fragment getFragment(int position) {
        int entryLabelResId = R.string.stats_entry_authors;
        int totalsLabelResId = R.string.stats_totals_views;
        int emptyLabelResId = R.string.stats_empty_top_authors;

        Uri uri = Uri.parse(STATS_TOP_AUTHORS_URI.toString() + "?timeframe=" + TIMEFRAMES[position].name());

        StatsCursorFragment fragment = StatsCursorFragment.newInstance(uri, entryLabelResId, totalsLabelResId, emptyLabelResId, getLocalTableBlogID());
        fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));
        fragment.setCallback(this);
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

            String entry = cursor.getString(cursor.getColumnIndex(StatsTopAuthorsTable.Columns.NAME));
            String imageUrl = cursor.getString(cursor.getColumnIndex(StatsTopAuthorsTable.Columns.IMAGE_URL));
            int total = cursor.getInt(cursor.getColumnIndex(StatsTopAuthorsTable.Columns.VIEWS));

            holder.entryTextView.setText(entry);
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));

            // image
            holder.showNetworkImage(imageUrl);
        }
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_top_authors);
    }

    @Override
    protected String[] getTabTitles() {
        return StatsTimeframe.toStringArray(TIMEFRAMES);
    }

    @Override
    protected int getInnerFragmentID() {
        return R.id.stats_top_authors;
    }
}
