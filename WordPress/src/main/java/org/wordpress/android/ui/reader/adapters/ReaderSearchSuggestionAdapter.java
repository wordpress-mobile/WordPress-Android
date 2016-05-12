package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.database.MatrixCursor;
import android.support.v4.widget.SimpleCursorAdapter;

import org.wordpress.android.datasets.ReaderSearchTable;

import java.util.List;

public class ReaderSearchSuggestionAdapter extends SimpleCursorAdapter {
    private List<String> mSearchSuggestions;

    public ReaderSearchSuggestionAdapter(Context context) {
        super(context,
                android.R.layout.simple_list_item_1,
                null,
                new String[]{"query"},
                new int[]{android.R.id.text1},
                0);
        populate();
    }

    public void populate() {
        populate(null);
    }

    public void populate(String filter) {
        mSearchSuggestions = ReaderSearchTable.getQueryStrings(filter);
        MatrixCursor cursor = new MatrixCursor(new String[]{"_id", "query"});

        int id = 0;
        for (String query : mSearchSuggestions) {
            cursor.addRow(new Object[] {id++, query});
        }

        swapCursor(cursor);
    }

    public String getSuggestion(int position) {
        if (position < 0 || position > mSearchSuggestions.size() - 1) {
            return null;
        }
        return mSearchSuggestions.get(position);
    }

}
