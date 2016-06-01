package org.wordpress.android.ui.reader.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderSearchTable;

public class ReaderSearchSuggestionAdapter extends CursorAdapter {
    private static final int MAX_SUGGESTIONS = 5;
    private static final int CLEAR_ALL_ROW_ID = -1;

    private String mCurrentFilter;

    private final String mClearAllText;
    private final int mClearAllBgColor;

    public ReaderSearchSuggestionAdapter(Context context) {
        super(context, null, false);
        mClearAllText = context.getString(R.string.label_clear_saved_searches);
        mClearAllBgColor = context.getResources().getColor(R.color.grey_lighten_30);
    }

    public void setFilter(String filter) {
        // skip if unchanged
        if (isCurrentFilter(filter) && getCursor() != null) {
            return;
        }

        // get db cursor containing matching query strings
        Cursor sqlCursor = ReaderSearchTable.getQueryStringCursor(filter, MAX_SUGGESTIONS);

        // create a MatrixCursor which will be the actual cursor behind this adapter
        MatrixCursor matrixCursor = new MatrixCursor(
                new String[]{
                        ReaderSearchTable.COL_ID,
                        ReaderSearchTable.COL_QUERY});

        if (sqlCursor.moveToFirst()) {
            // first populate the matrix from the db cursor...
            do {
                long id = sqlCursor.getLong(sqlCursor.getColumnIndex(ReaderSearchTable.COL_ID));
                String query = sqlCursor.getString(sqlCursor.getColumnIndex(ReaderSearchTable.COL_QUERY));
                matrixCursor.addRow(new Object[]{id, query});
            } while (sqlCursor.moveToNext());

            // ...then add our custom item
            matrixCursor.addRow(new Object[]{CLEAR_ALL_ROW_ID, mClearAllText});
        }

        mCurrentFilter = filter;
        swapCursor(matrixCursor);
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

        SuggestionViewHolder(View view) {
            txtSuggestion = (TextView) view.findViewById(R.id.text_suggestion);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.reader_listitem_suggestion, parent, false);
        view.setTag(new SuggestionViewHolder(view));

        long id = cursor.getLong(cursor.getColumnIndex(ReaderSearchTable.COL_ID));
        if (id == CLEAR_ALL_ROW_ID) {
            view.setBackgroundColor(mClearAllBgColor);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmClearSavedSearches(v.getContext());
                }
            });
        }

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        SuggestionViewHolder holder = (SuggestionViewHolder) view.getTag();

        String query = cursor.getString(cursor.getColumnIndex(ReaderSearchTable.COL_QUERY));
        holder.txtSuggestion.setText(query);
    }

    private void confirmClearSavedSearches(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.dlg_title_confirm_clear_saved_searches)
               .setMessage(R.string.dlg_text_confirm_clear_saved_searches)
               .setCancelable(true)
               .setNegativeButton(R.string.no, null)
               .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       clearSavedSearches();
                   }
               });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void clearSavedSearches() {
        ReaderSearchTable.deleteAllQueries();
        swapCursor(null);
    }
}
