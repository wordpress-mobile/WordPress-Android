package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.database.MatrixCursor;
import android.support.v4.widget.SimpleCursorAdapter;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderSearchTable;

import java.util.List;

public class ReaderSearchSuggestionAdapter extends SimpleCursorAdapter {
    private List<String> mSearchSuggestions;

    public ReaderSearchSuggestionAdapter(Context context) {
        super(context,
                R.layout.reader_listitem_suggestion,
                null,
                new String[]{"query"},
                new int[]{android.R.id.text1},
                0);
    }

    /*
     * populates the suggestion list from previous suggestions with the passed filter applied - pass
     * null for the filter to show all suggestions
     */
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
