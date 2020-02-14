package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderSearchTable;
import org.wordpress.android.ui.reader.adapters.ReaderSearchSuggestionRecyclerAdapter.SearchSuggestionHolder;
import org.wordpress.android.util.SqlUtils;

public class ReaderSearchSuggestionRecyclerAdapter extends RecyclerView.Adapter<SearchSuggestionHolder> {
    private static final int MAX_SUGGESTIONS = 5;
    private static final int MAX_SUGGESTIONS_WHEN_EMPTY = 10;
    private static final int CLEAR_ALL_ROW_ID = -1;

    private Cursor mCursor;
    private String mCurrentQuery;
    private OnSuggestionClickListener mOnSuggestionClickListener;
    private OnSuggestionDeleteClickListener mOnSuggestionDeleteClickListener;
    private OnSuggestionClearClickListener mOnSuggestionClearClickListener;

    public ReaderSearchSuggestionRecyclerAdapter() {
        setHasStableIds(true);
        swapCursor(null);
    }

    @Override
    @NotNull
    public SearchSuggestionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        final View view =
                LayoutInflater.from(context).inflate(R.layout.reader_listitem_suggestion_recycler, parent, false);
        return new SearchSuggestionHolder(view);
    }

    @Override
    public void onBindViewHolder(@NotNull SearchSuggestionHolder holder, int position) {
        if (isLast(position)) {
            onBindClearAllViewHolder(holder);
        } else if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        } else {
            onBindSuggestionViewHolder(holder);
        }
    }

    private void onBindClearAllViewHolder(final SearchSuggestionHolder holder) {
        final Context context = holder.itemView.getContext();
        final String text = context.getString(R.string.label_clear_search_history);
        holder.mHistoryImageView.setVisibility(View.INVISIBLE);
        holder.mSuggestionTextView.setText(text);
        holder.mDeleteImageView.setVisibility(View.INVISIBLE);
        holder.itemView.setOnClickListener(v -> {
            if (mOnSuggestionClearClickListener != null) {
                mOnSuggestionClearClickListener.onClearClicked();
            }
        });
    }

    private void onBindSuggestionViewHolder(final SearchSuggestionHolder holder) {
        final String query = mCursor.getString(mCursor.getColumnIndex(ReaderSearchTable.COL_QUERY));
        holder.mHistoryImageView.setVisibility(View.VISIBLE);
        holder.mSuggestionTextView.setText(query);
        holder.mDeleteImageView.setVisibility(View.VISIBLE);
        holder.mDeleteImageView.setOnClickListener(v -> {
            if (mOnSuggestionDeleteClickListener != null) {
                mOnSuggestionDeleteClickListener.onDeleteClicked(query);
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (mOnSuggestionClickListener != null) {
                mOnSuggestionClickListener.onSuggestionClicked(query);
            }
        });
    }

    @Override
    public int getItemCount() {
        final int count = mCursor == null ? 0 : mCursor.getCount();
        return count > 0 ? count + 1 : 0; // we add an extra row at the end to show the "Clear search history" button
    }

    @Override
    public long getItemId(int position) {
        if (isLast(position)) {
            return CLEAR_ALL_ROW_ID;
        } else if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        return mCursor.getLong(mCursor.getColumnIndex(ReaderSearchTable.COL_ID));
    }

    private boolean isLast(final int position) {
        return position == mCursor.getCount();
    }

    public void swapCursor(final Cursor newCursor) {
        if (newCursor == mCursor) return;
        SqlUtils.closeCursor(mCursor);
        mCursor = newCursor;
        notifyDataSetChanged();
    }

    public void setOnSuggestionClickListener(OnSuggestionClickListener onSuggestionClickListener) {
        mOnSuggestionClickListener = onSuggestionClickListener;
    }

    public void setOnSuggestionDeleteClickListener(OnSuggestionDeleteClickListener onSuggestionDeleteClickListener) {
        mOnSuggestionDeleteClickListener = onSuggestionDeleteClickListener;
    }

    public void setOnSuggestionClearClickListener(OnSuggestionClearClickListener onSuggestionClearClickListener) {
        mOnSuggestionClearClickListener = onSuggestionClearClickListener;
    }

    public void reload() {
        setQuery(mCurrentQuery, true);
    }

    public void setQuery(final String newQuery) {
        setQuery(newQuery, false);
    }

    private void setQuery(final String newQuery, final boolean forceUpdate) {
        if (!forceUpdate && newQuery != null && newQuery.equalsIgnoreCase(mCurrentQuery) && mCursor != null) {
            return;
        }
        mCurrentQuery = newQuery;
        final int maxSuggestions = newQuery == null ? MAX_SUGGESTIONS_WHEN_EMPTY : MAX_SUGGESTIONS;
        swapCursor(ReaderSearchTable.getQueryStringCursor(newQuery, maxSuggestions));
    }

    class SearchSuggestionHolder extends RecyclerView.ViewHolder {
        private final ImageView mHistoryImageView;
        private final TextView mSuggestionTextView;
        private final ImageView mDeleteImageView;

        SearchSuggestionHolder(final View itemView) {
            super(itemView);
            mHistoryImageView = itemView.findViewById(R.id.image_history);
            mSuggestionTextView = itemView.findViewById(R.id.text_suggestion);
            mDeleteImageView = itemView.findViewById(R.id.image_delete);
        }
    }
}
