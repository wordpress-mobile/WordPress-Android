package org.wordpress.android.ui.stats;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.StatsTagsAndCategoriesTable;
import org.wordpress.android.models.StatsTagsandCategories.Type;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.FormatUtils;

import java.util.Locale;

/**
 * Fragment for tags and categories stats. Only a single page.
 */
public class StatsTagsAndCategoriesFragment extends StatsAbsViewFragment implements StatsCursorInterface {
    private static final Uri STATS_TAGS_AND_CATEGORIES_URI = StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI;

    public static final String TAG = StatsTagsAndCategoriesFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_pager_fragment, container, false);

        TextView tv = (TextView) view.findViewById(R.id.stats_pager_title);
        tv.setText(getTitle().toUpperCase(Locale.getDefault()));

        FragmentManager fm = getFragmentManager();

        int entryLabelResId = R.string.stats_entry_tags_and_categories;
        int totalsLabelResId = R.string.stats_totals_views;
        int emptyLabelResId = R.string.stats_empty_tags_and_categories;
        StatsCursorFragment fragment = StatsCursorFragment.newInstance(STATS_TAGS_AND_CATEGORIES_URI, entryLabelResId, totalsLabelResId, emptyLabelResId, getLocalTableBlogID());
        fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));
        fragment.setCallback(this);

        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.stats_pager_container, fragment, StatsCursorFragment.TAG);
        ft.commit();

        return view;
    }

    public class CustomCursorAdapter extends CursorAdapter {
        public CustomCursorAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String entry = cursor.getString(cursor.getColumnIndex(StatsTagsAndCategoriesTable.Columns.TOPIC));
            int total = cursor.getInt(cursor.getColumnIndex(StatsTagsAndCategoriesTable.Columns.VIEWS));
            String type = cursor.getString(cursor.getColumnIndex(StatsTagsAndCategoriesTable.Columns.TYPE));

            // entries
            TextView entryTextView = (TextView) view.findViewById(R.id.stats_list_cell_entry);
            entryTextView.setText(entry);

            // tag and category icons
            if (type.equals(Type.CATEGORY.getLabel())) {
                entryTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stats_icon_categories, 0, 0, 0);
            } else if (type.equals(Type.TAG.getLabel())) {
                entryTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stats_icon_tags, 0, 0, 0);
            } else {
                entryTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }

            // totals
            TextView totalsTextView = (TextView) view.findViewById(R.id.stats_list_cell_total);
            totalsTextView.setText(FormatUtils.formatDecimal(total));

        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup root) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.stats_list_cell, root, false);
        }

    }

    @Override
    protected String getTitle() {
        return getString(R.string.stats_view_tags_and_categories);
    }

    @Override
    public void onCursorLoaded(Uri uri, Cursor cursor) {
        // StatsCursorInterface callback: do nothing
    }
}
