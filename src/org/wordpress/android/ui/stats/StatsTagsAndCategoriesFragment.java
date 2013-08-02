
package org.wordpress.android.ui.stats;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.StatsTagsAndCategoriesTable;
import org.wordpress.android.models.StatsTagsandCategories.Type;
import org.wordpress.android.providers.StatsContentProvider;

public class StatsTagsAndCategoriesFragment extends StatsAbsViewFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_pager_fragment, container, false);

        FragmentManager fm = getChildFragmentManager();
        
        int entryLabelResId = R.string.stats_entry_tags_and_categories;
        int totalsLabelResId = R.string.stats_totals_views;

        StatsCursorFragment fragment = StatsCursorFragment.newInstance(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI, entryLabelResId, totalsLabelResId);
        fragment.setListAdapter(new CustomCursorAdapter(getActivity(), null));

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
            } else if (type.equals(Type.STAT.getLabel())) {
                entryTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stats_icon_tags, 0, 0, 0);
            } else {
                entryTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
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
        return getString(R.string.stats_view_tags_and_categories);
    }

}
