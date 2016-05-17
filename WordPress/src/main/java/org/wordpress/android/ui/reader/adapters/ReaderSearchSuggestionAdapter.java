package org.wordpress.android.ui.reader.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
        mClearAllText = context.getString(R.string.reader_label_clear_suggestions);
        mClearAllBgColor = context.getResources().getColor(R.color.grey_lighten_30);
    }

    public void populate(String filter) {
        Cursor sqlCursor = ReaderSearchTable.getQueryStringCursor(filter, MAX_SUGGESTIONS);
        MatrixCursor matrixCursor = new MatrixCursor(
                new String[]{
                        ReaderSearchTable.COL_ID,
                        ReaderSearchTable.COL_QUERY});
        if (sqlCursor.moveToFirst()) {
            do {
                long id = sqlCursor.getLong(sqlCursor.getColumnIndex(ReaderSearchTable.COL_ID));
                String query = sqlCursor.getString(sqlCursor.getColumnIndex(ReaderSearchTable.COL_QUERY));
                matrixCursor.addRow(new Object[]{id, query});
            } while (sqlCursor.moveToNext());
            matrixCursor.addRow(new Object[]{CLEAR_ALL_ROW_ID, mClearAllText});
        }

        swapCursor(matrixCursor);
        mCurrentFilter = filter;
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
        private final ViewGroup container;
        private final ImageView imgSuggestion;
        private final TextView txtSuggestion;
        private final ImageView imgDelete;

        SuggestionViewHolder(View view) {
            container = (ViewGroup) view.findViewById(R.id.layout_container);
            imgSuggestion = (ImageView) view.findViewById(R.id.image_suggestion);
            txtSuggestion = (TextView) view.findViewById(R.id.text_suggestion);
            imgDelete = (ImageView) view.findViewById(R.id.image_delete);
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
                    confirmClearAllSuggestions(v.getContext());
                }
            });
        }

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        SuggestionViewHolder holder = (SuggestionViewHolder) view.getTag();

        long id = cursor.getLong(cursor.getColumnIndex(ReaderSearchTable.COL_ID));
        final String query = cursor.getString(cursor.getColumnIndex(ReaderSearchTable.COL_QUERY));

        if (id == CLEAR_ALL_ROW_ID) {
            holder.imgSuggestion.setVisibility(View.GONE);
            holder.txtSuggestion.setText(query);
            holder.imgDelete.setVisibility(View.GONE);
            holder.imgDelete.setOnClickListener(null);
        } else {
            holder.imgSuggestion.setVisibility(View.VISIBLE);
            holder.txtSuggestion.setText(query);
            holder.imgDelete.setVisibility(View.VISIBLE);
            holder.imgDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderSearchTable.deleteQueryString(query);
                    populate(mCurrentFilter);
                }
            });
        }
    }

    private void confirmClearAllSuggestions(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.dlg_confirm_trash_comments);
        builder.setTitle(R.string.trash);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.trash_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                clearAllSuggestions();
            }
        });
        builder.setNegativeButton(R.string.trash_no, null);
        AlertDialog alert = builder.create();
        alert.show();
        clearAllSuggestions();
    }

    private void clearAllSuggestions() {
        ReaderSearchTable.deleteAllQueries();
    }
}
