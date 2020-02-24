package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cursoradapter.widget.CursorAdapter;

import com.google.android.material.elevation.ElevationOverlayProvider;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderSearchTable;

public class ReaderSearchSuggestionAdapter extends CursorAdapter {
    private static final int MAX_SUGGESTIONS = 5;
    private static final int CLEAR_ALL_ROW_ID = -1;

    private static final int NUM_VIEW_TYPES = 2;
    private static final int VIEW_TYPE_QUERY = 0;
    private static final int VIEW_TYPE_CLEAR = 1;

    private String mCurrentFilter;
    private final Object[] mClearAllRow;
    private final int mClearAllBgColor;
    private final int mSuggestionBgColor;

    private OnSuggestionDeleteClickListener mOnSuggestionDeleteClickListener;
    private OnSuggestionClearClickListener mOnSuggestionClearClickListener;

    public ReaderSearchSuggestionAdapter(Context context) {
        super(context, null, false);
        String clearAllText = context.getString(R.string.label_clear_search_history);
        mClearAllRow = new Object[]{CLEAR_ALL_ROW_ID, clearAllText};

        ElevationOverlayProvider elevationOverlayProvider = new ElevationOverlayProvider(context);
        float appbarElevation = context.getResources().getDimension(R.dimen.appbar_elevation);
        int elevatedSurfaceColor =
                elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(appbarElevation);

        mClearAllBgColor = elevatedSurfaceColor;
        mSuggestionBgColor = elevatedSurfaceColor;
    }

    public synchronized void setFilter(String filter) {
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
            matrixCursor.addRow(mClearAllRow);
        }

        mCurrentFilter = filter;
        swapCursor(matrixCursor);
    }

    /*
     * forces setFilter() to always repopulate by skipping the isCurrentFilter() check
     */
    public synchronized void reload() {
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

    @Override
    public int getItemViewType(int position) {
        // use a different view type for the "clear" row so it doesn't get recycled and used
        // as a query row
        if (getItemId(position) == CLEAR_ALL_ROW_ID) {
            return VIEW_TYPE_CLEAR;
        }
        return VIEW_TYPE_QUERY;
    }

    @Override
    public int getViewTypeCount() {
        return NUM_VIEW_TYPES;
    }

    private class SuggestionViewHolder {
        private final TextView mTxtSuggestion;
        private final ImageView mImgDelete;

        SuggestionViewHolder(View view) {
            mTxtSuggestion = view.findViewById(R.id.text_suggestion);
            mImgDelete = view.findViewById(R.id.image_delete);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.reader_listitem_suggestion, parent, false);

        SuggestionViewHolder holder = new SuggestionViewHolder(view);
        view.setTag(holder);

        long id = cursor.getLong(cursor.getColumnIndex(ReaderSearchTable.COL_ID));
        if (id == CLEAR_ALL_ROW_ID) {
            view.setBackgroundColor(mClearAllBgColor);
            view.setOnClickListener(v -> {
                if (mOnSuggestionClearClickListener != null) {
                    mOnSuggestionClearClickListener.onClearClicked();
                }
            });
            holder.mImgDelete.setVisibility(View.GONE);
        } else {
            view.setBackgroundColor(mSuggestionBgColor);
        }

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        SuggestionViewHolder holder = (SuggestionViewHolder) view.getTag();

        final String query = cursor.getString(cursor.getColumnIndex(ReaderSearchTable.COL_QUERY));
        holder.mTxtSuggestion.setText(query);

        long id = cursor.getLong(cursor.getColumnIndex(ReaderSearchTable.COL_ID));
        if (id != CLEAR_ALL_ROW_ID) {
            holder.mImgDelete.setOnClickListener(v -> {
                if (mOnSuggestionDeleteClickListener != null) {
                    mOnSuggestionDeleteClickListener.onDeleteClicked(query);
                }
            });
        }
    }

    public void setOnSuggestionDeleteClickListener(OnSuggestionDeleteClickListener onSuggestionDeleteClickListener) {
        mOnSuggestionDeleteClickListener = onSuggestionDeleteClickListener;
    }

    public void setOnSuggestionClearClickListener(OnSuggestionClearClickListener onSuggestionClearClickListener) {
        mOnSuggestionClearClickListener = onSuggestionClearClickListener;
    }
}
