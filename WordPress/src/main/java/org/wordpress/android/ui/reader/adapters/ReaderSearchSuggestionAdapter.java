package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderSearchTable;

public class ReaderSearchSuggestionAdapter extends CursorAdapter {
    private final LayoutInflater mInflater;

    private static final int MAX_SUGGESTIONS = 5;

    public ReaderSearchSuggestionAdapter(Context context) {
        super(context, null, false);
        mInflater = LayoutInflater.from(context);
    }

    public void populate(String filter) {
        Cursor cursor = ReaderSearchTable.getQueryStringCursor(filter, MAX_SUGGESTIONS);
        swapCursor(cursor);
    }

    public String getSuggestion(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return cursor.getString(cursor.getColumnIndex(ReaderSearchTable.COL_QUERY));
    }

    private class SuggestionViewHolder {
        private final TextView textView;
        SuggestionViewHolder(View view) {
            textView = (TextView) view.findViewById(android.R.id.text1);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.reader_listitem_suggestion, parent, false);
        view.setTag(new SuggestionViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String query = cursor.getString(cursor.getColumnIndex(ReaderSearchTable.COL_QUERY));
        SuggestionViewHolder holder = (SuggestionViewHolder) view.getTag();
        holder.textView.setText(query);
    }
}
