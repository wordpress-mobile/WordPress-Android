package org.wordpress.android.ui.reader.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.ui.reader.ReaderInterfaces;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.views.ReaderFollowButton;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class ReaderTagAdapter extends RecyclerView.Adapter<ReaderTagAdapter.TagViewHolder> {
    public interface TagDeletedListener {
        void onTagDeleted(ReaderTag tag);
    }

    public interface TagAddedListener {
        void onTagAdded(@NonNull ReaderTag readerTag);
    }

    @Inject AccountStore mAccountStore;
    private final WeakReference<Context> mWeakContext;
    private final ReaderTagList mTags = new ReaderTagList();
    private TagDeletedListener mTagDeletedListener;
    private TagAddedListener mTagAddedListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;
    private final Map<String, Boolean> mTagSlugIsFollowedMap = new HashMap<>();

    public ReaderTagAdapter(Context context) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);
        setHasStableIds(true);
        mWeakContext = new WeakReference<>(context);
    }

    public void setTagDeletedListener(TagDeletedListener listener) {
        mTagDeletedListener = listener;
    }

    public void setTagAddedListener(@NonNull final TagAddedListener listener) {
        mTagAddedListener = listener;
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

    @SuppressWarnings("deprecation")
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

    @Nullable
    public ReaderTagList getItems() {
        return mTags;
    }

    @Nullable
    public ReaderTagList getSubscribedItems() {
        final ReaderTagList readerSubscribedTagsList = new ReaderTagList();
        for (final ReaderTag readerTag : mTags) {
            if (Boolean.TRUE.equals(mTagSlugIsFollowedMap.get(readerTag.getTagSlug()))) {
                readerSubscribedTagsList.add(readerTag);
            }
        }
        return readerSubscribedTagsList;
    }

    @Override
    public void onBindViewHolder(TagViewHolder holder, int position) {
        final ReaderTag tag = mTags.get(position);
        holder.mTxtTagName.setText(tag.getLabel());
        holder.mRemoveFollowButton.setOnClickListener(view -> performDeleteTag(tag, holder.mRemoveFollowButton));
    }

    private void performDeleteTag(@NonNull ReaderTag tag, @NonNull final ReaderFollowButton readerFollowButton) {
        if (!NetworkUtils.checkConnection(getContext())) {
            return;
        }

        final boolean isFollowingCurrent = Boolean.TRUE.equals(mTagSlugIsFollowedMap.get(tag.getTagSlug()));
        final boolean isFollowingNew = !isFollowingCurrent;
        readerFollowButton.setIsFollowed(isFollowingNew);

        // Disable follow button until API call returns
        readerFollowButton.setEnabled(false);

        ReaderActions.ActionListener actionListener = succeeded -> {
            mTagSlugIsFollowedMap.put(tag.getTagSlug(), isFollowingNew);
            readerFollowButton.setEnabled(true);
            if (!succeeded && hasContext()) {
                ToastUtils.showToast(getContext(), R.string.reader_toast_err_removing_tag);
                refresh();
            }
        };

        if (isFollowingCurrent) {
            boolean success = ReaderTagActions.deleteTag(tag, actionListener, mAccountStore.hasAccessToken());
            if (success && mTagDeletedListener != null) {
                mTagDeletedListener.onTagDeleted(tag);
            }
        } else {
            boolean success = ReaderTagActions.addTag(tag, actionListener, mAccountStore.hasAccessToken());
            if (success && mTagAddedListener != null) {
                mTagAddedListener.onTagAdded(tag);
            }
        }
    }

    class TagViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTxtTagName;
        private final ReaderFollowButton mRemoveFollowButton;

        TagViewHolder(View view) {
            super(view);
            mTxtTagName = (TextView) view.findViewById(R.id.text_topic);
            mRemoveFollowButton = (ReaderFollowButton) view.findViewById(R.id.remove_button);
            mRemoveFollowButton.setIsFollowed(true);
        }
    }

    /*
     * AsyncTask to load tags
     */
    private boolean mIsTaskRunning = false;

    @SuppressWarnings("deprecation")
    @SuppressLint("StaticFieldLeak")
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
                mTagSlugIsFollowedMap.clear();
                for (final ReaderTag tag : mTags) {
                    mTagSlugIsFollowedMap.put(tag.getTagSlug(), true);
                }
                notifyDataSetChanged();
            }
            mIsTaskRunning = false;
            if (mDataLoadedListener != null) {
                mDataLoadedListener.onDataLoaded(isEmpty());
            }
        }
    }
}
