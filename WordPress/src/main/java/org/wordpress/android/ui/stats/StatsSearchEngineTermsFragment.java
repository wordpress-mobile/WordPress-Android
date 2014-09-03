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
import org.wordpress.android.datasets.StatsSearchEngineTermsTable;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.FormatUtils;

/**
 * Fragment for search engine term stats. Has two pages, for Today's and Yesterday's stats.
 */
public class StatsSearchEngineTermsFragment extends StatsAbsPagedViewFragment {
    private static final Uri STATS_SEARCH_ENGINE_TERMS_URI = StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI;
    private static final StatsTimeframe[] TIMEFRAMES = new StatsTimeframe[] { StatsTimeframe.TODAY, StatsTimeframe.YESTERDAY };

    public static final String TAG = StatsSearchEngineTermsFragment.class.getSimpleName();

    @Override
    protected Fragment getFragment(int position) {
        Uri uri = Uri.parse(STATS_SEARCH_ENGINE_TERMS_URI.toString() + "?timeframe=" + TIMEFRAMES[position].name());

        StatsCursorFragment fragment = StatsCursorFragment.newInstance(uri, R.string.stats_entry_search_engine_terms,
                R.string.stats_totals_views, R.string.stats_empty_search_engine_terms_title,
                R.string.stats_empty_search_engine_terms_desc, getLocalTableBlogID());
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
        public void bindView(View view, Context context, Cursor cursor) {
            final StatsViewHolder holder = (StatsViewHolder) view.getTag();

            String entry = cursor.getString(cursor.getColumnIndex(StatsSearchEngineTermsTable.Columns.SEARCH));
            int total = cursor.getInt(cursor.getColumnIndex(StatsSearchEngineTermsTable.Columns.VIEWS));

            holder.entryTextView.setText(entry);
            holder.totalsTextView.setText(FormatUtils.formatDecimal(total));
            holder.networkImageView.setVisibility(View.GONE);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            View view = inflater.inflate(R.layout.stats_list_cell, root, false);
            view.setTag(new StatsViewHolder(view));
            return view;
        }
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_search_engine_terms);
    }

    @Override
    protected String[] getTabTitles() {
        return StatsTimeframe.toStringArray(TIMEFRAMES);
    }

    @Override
    protected int getInnerFragmentID() {
        return R.id.stats_search;
    }
}
