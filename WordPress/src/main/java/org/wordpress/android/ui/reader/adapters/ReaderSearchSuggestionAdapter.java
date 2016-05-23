package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderSearchTable;

public class ReaderSearchSuggestionAdapter extends CursorAdapter {
    private static final int MAX_SUGGESTIONS = 5;
    private String mCurrentFilter;

    public ReaderSearchSuggestionAdapter(Context context) {
        super(context, null, false);
    }

    /*
     * populate the adapter using a cursor containing past searches that match the filter
     */
    public void setFilter(String filter) {
        // skip if unchanged
        if (isCurrentFilter(filter) && getCursor() != null) {
            return;
        }
        Cursor cursor = ReaderSearchTable.getQueryStringCursor(filter, MAX_SUGGESTIONS);
        swapCursor(cursor);
        mCurrentFilter = filter;
    }

    /*
     * forces setFilter() to always repopulate by skipping the isCurrentFilter() check
     */
    private void reload() {
        String newFilter = mCurrentFilter;
        mCurrentFilter = null;
        setFilter(newFilter);
    }

    private boolean isCurrentFilter(String filter) {
        if (TextUtils.isEmpty(filter) && TextUtils.isEmpty(mCurrentFilter)) {
            return true;
        }
        return filter != null && filter.equalsIgnoreCase(mCurrentFilter);
    }

    public String getSuggestion(int position) {
        Cursor cursor = (Cursor) getItem(position);
        if (cursor != null) {
            return cursor.getString(cursor.getColumnIndex(ReaderSearchTable.COL_QUERY));
        } else {
            return null;
        }
    }

    private class SuggestionViewHolder {
        private final TextView txtSuggestion;
        private final ImageView imgDelete;

        SuggestionViewHolder(View view) {
            txtSuggestion = (TextView) view.findViewById(R.id.text_suggestion);
            imgDelete = (ImageView) view.findViewById(R.id.image_delete);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.reader_listitem_suggestion, parent, false);
        view.setTag(new SuggestionViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        SuggestionViewHolder holder = (SuggestionViewHolder) view.getTag();
        final String query = cursor.getString(cursor.getColumnIndex(ReaderSearchTable.COL_QUERY));

        holder.txtSuggestion.setText(query);
        holder.imgDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReaderSearchTable.deleteQueryString(query);
                reload();
            }
        });
    }
}
