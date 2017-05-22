package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.ui.reader.ReaderInterfaces;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.lang.ref.WeakReference;

public class ReaderTagAdapter extends RecyclerView.Adapter<ReaderTagAdapter.TagViewHolder> {

    public interface TagDeletedListener {
        void onTagDeleted(ReaderTag tag);
    }

    private final WeakReference<Context> mWeakContext;
    private final ReaderTagList mTags = new ReaderTagList();
    private TagDeletedListener mTagDeletedListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;

    public ReaderTagAdapter(Context context) {
        super();
        setHasStableIds(true);
        mWeakContext = new WeakReference<>(context);
    }

    public void setTagDeletedListener(TagDeletedListener listener) {
        mTagDeletedListener = listener;
    }

    public void setDataLoadedListener(ReaderInterfaces.DataLoadedListener listener) {
        mDataLoadedListener = listener;
    }

    private boolean hasContext() {
        return (getContext() != null);
    }

    private Context getContext() {
        return mWeakContext.get();
    }

    public void refresh() {
        if (mIsTaskRunning) {
            AppLog.w(T.READER, "tag task is already running");
            return;
        }
        new LoadTagsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        return mTags.size();
    }

    public boolean isEmpty() {
        return (getItemCount() == 0);
    }

    @Override
    public long getItemId(int position) {
        return mTags.get(position).getTagSlug().hashCode();
    }

    @Override
    public TagViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reader_listitem_tag, parent, false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TagViewHolder holder, int position) {
        final ReaderTag tag = mTags.get(position);
        holder.txtTagName.setText(tag.getLabel());
        holder.btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performDeleteTag(tag);

            }
        });
    }

    private void performDeleteTag(@NonNull ReaderTag tag) {
        if (!NetworkUtils.checkConnection(getContext())) {
            return;
        }

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded && hasContext()) {
                    ToastUtils.showToast(getContext(), R.string.reader_toast_err_remove_tag);
                    refresh();
                }
            }
        };

        boolean success = ReaderTagActions.deleteTag(tag, actionListener);

        if (success) {
            int index = mTags.indexOfTagName(tag.getTagSlug());
            if (index > -1) {
                mTags.remove(index);
                notifyItemRemoved(index);
            }
            if (mTagDeletedListener != null) {
                mTagDeletedListener.onTagDeleted(tag);
            }
        }
    }

    class TagViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtTagName;
        private final ImageButton btnRemove;

        public TagViewHolder(View view) {
            super(view);
            txtTagName = (TextView) view.findViewById(R.id.text_topic);
            btnRemove = (ImageButton) view.findViewById(R.id.btn_remove);
            ReaderUtils.setBackgroundToRoundRipple(btnRemove);
        }
    }

    /*
     * AsyncTask to load tags
     */
    private boolean mIsTaskRunning = false;
    private class LoadTagsTask extends AsyncTask<Void, Void, ReaderTagList> {
        @Override
        protected void onPreExecute() {
            mIsTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsTaskRunning = false;
        }
        @Override
        protected ReaderTagList doInBackground(Void... params) {
            return ReaderTagTable.getFollowedTags();
        }
        @Override
        protected void onPostExecute(ReaderTagList tagList) {
            if (tagList != null && !tagList.isSameList(mTags)) {
                mTags.clear();
                mTags.addAll(tagList);
                notifyDataSetChanged();
            }
            mIsTaskRunning = false;
            if (mDataLoadedListener != null) {
                mDataLoadedListener.onDataLoaded(isEmpty());
            }
        }
    }

}
