package org.wordpress.android.ui.comments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.DiffResult;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.ui.comments.CommentListItem.Comment;
import org.wordpress.android.ui.comments.CommentListItem.CommentListItemType;
import org.wordpress.android.ui.comments.CommentListItem.SubHeader;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.image.ImageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

/**
 * @deprecated
 * Comments are being refactored as part of Comments Unification project. If you are adding any
 * features or modifying this class, please ping develric or klymyam
 */
@Deprecated
public class CommentAdapter extends RecyclerView.Adapter<CommentListViewHolder> {
    interface OnDataLoadedListener {
        void onDataLoaded(boolean isEmpty);
    }

    interface OnLoadMoreListener {
        void onLoadMore();
    }

    interface OnSelectedItemsChangeListener {
        void onSelectedItemsChanged();
    }

    public interface OnCommentPressedListener {
        void onCommentPressed(int position, View view);

        void onCommentLongPressed(int position, View view);
    }

    private final ArrayList<CommentListItem> mComments = new ArrayList<>();
    private final HashSet<Integer> mSelectedPositions = new HashSet<>();

    private OnDataLoadedListener mOnDataLoadedListener;
    private OnCommentPressedListener mOnCommentPressedListener;
    private OnLoadMoreListener mOnLoadMoreListener;
    private OnSelectedItemsChangeListener mOnSelectedChangeListener;

    private boolean mEnableSelection;

    private SiteModel mSite;
    private Context mContext;

    @Inject CommentStore mCommentStore;
    @Inject ImageManager mImageManager;
    @Inject AccountStore mAccountStore;
    @Inject UiHelpers mUiHelpers;

    CommentAdapter(Context context, SiteModel site) {
        ((WordPress) context.getApplicationContext()).component().inject(this);
        mSite = site;
        mContext = context;
        setHasStableIds(true);
    }

    void setOnDataLoadedListener(OnDataLoadedListener listener) {
        mOnDataLoadedListener = listener;
    }

    void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
    }

    void setOnCommentPressedListener(OnCommentPressedListener listener) {
        mOnCommentPressedListener = listener;
    }

    void setOnSelectedItemsChangeListener(OnSelectedItemsChangeListener listener) {
        mOnSelectedChangeListener = listener;
    }

    @NotNull
    @Override
    public CommentListViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        switch (CommentListItemType.fromOrdinal(viewType)) {
            case HEADER:
                return new CommentSubHeaderViewHolder(parent);
            case COMMENT:
                return new CommentViewHolder(parent, mOnCommentPressedListener, mImageManager, mUiHelpers);
            default:
                throw new IllegalArgumentException("Unexpected view holder in CommentListAdapter");
        }
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType().ordinal();
    }

    private String getFormattedDate(CommentModel comment) {
        if (comment.getDatePublished() != null) {
            return DateTimeUtils
                    .javaDateToTimeSpan(DateTimeUtils.dateFromIso8601(comment.getDatePublished()), mContext);
        }
        return "";
    }

    @Override
    public void onBindViewHolder(@NonNull CommentListViewHolder viewHolder, int position,
                                 @NonNull List<Object> payloads) {
        if (viewHolder instanceof CommentSubHeaderViewHolder) {
            ((CommentSubHeaderViewHolder) viewHolder).bind((SubHeader) mComments.get(position));
        } else if (viewHolder instanceof CommentViewHolder) {
            ((CommentViewHolder) viewHolder).bind(
                    (Comment) mComments.get(position),
                    mEnableSelection && isItemSelected(position));
        }
        // request to load more comments when we near the end
        if (mOnLoadMoreListener != null && position >= getItemCount() - 1
            && position >= CommentsListFragment.COMMENTS_PER_PAGE - 1) {
            mOnLoadMoreListener.onLoadMore();
        }
    }

    @Override
    public void onBindViewHolder(@NotNull CommentListViewHolder viewHolder, int position) {
        onBindViewHolder(viewHolder, position, new ArrayList<>());
    }

    public CommentListItem getItem(int position) {
        if (isPositionValid(position)) {
            return mComments.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }

    public int getNumberOfComments() {
        int counter = 0;
        for (CommentListItem subHeader : mComments) {
            if (subHeader instanceof Comment) {
                counter++;
            }
        }
        return counter;
    }

    private boolean isEmpty() {
        return getItemCount() == 0;
    }

    void setEnableSelection(boolean enable) {
        if (enable == mEnableSelection) {
            return;
        }

        mEnableSelection = enable;
        if (!mEnableSelection) {
            clearSelectedComments();
        }
    }

    void clearSelectedComments() {
        if (mSelectedPositions.size() > 0) {
            mSelectedPositions.clear();
            notifyDataSetChanged();
            if (mOnSelectedChangeListener != null) {
                mOnSelectedChangeListener.onSelectedItemsChanged();
            }
        }
    }

    int getSelectedCommentCount() {
        return mSelectedPositions.size();
    }

    CommentList getSelectedComments() {
        CommentList comments = new CommentList();
        if (!mEnableSelection) {
            return comments;
        }

        for (Integer position : mSelectedPositions) {
            if (isPositionValid(position)) {
                comments.add(((Comment) mComments.get(position)).getComment());
            }
        }

        return comments;
    }

    private boolean isItemSelected(int position) {
        return mSelectedPositions.contains(position);
    }

    void setItemSelected(int position, boolean isSelected, View view) {
        if (isItemSelected(position) == isSelected) {
            return;
        }

        if (isSelected) {
            mSelectedPositions.add(position);
        } else {
            mSelectedPositions.remove(position);
        }

        notifyItemChanged(position);

        if (mOnSelectedChangeListener != null) {
            mOnSelectedChangeListener.onSelectedItemsChanged();
        }
    }

    void toggleItemSelected(int position, View view) {
        setItemSelected(position, !isItemSelected(position), view);
    }

    private int indexOfCommentId(long commentId) {
        for (int i = 0; i < getItemCount(); i++) {
            CommentListItem commentListItem = getItem(i);
            if (commentListItem instanceof Comment) {
                if (((Comment) commentListItem).getComment().getRemoteCommentId() == commentId) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isPositionValid(int position) {
        return (position >= 0 && position < mComments.size());
    }

    public void removeComment(CommentModel comment) {
        int position = indexOfCommentId(comment.getRemoteCommentId());
        if (position >= 0) {
            mComments.remove(position);
            notifyItemRemoved(position);
        }
    }

    void loadInitialCachedComments(CommentStatus statusFilter) {
        loadComments(statusFilter, new LoadCommentsTaskParameters(0, true, false));
    }

    void reloadComments(CommentStatus statusFilter) {
        loadComments(statusFilter, new LoadCommentsTaskParameters(0, false, true));
    }

    void loadMoreComments(CommentStatus statusFilter, int offset) {
        loadComments(statusFilter, new LoadCommentsTaskParameters(offset, false, false));
    }

    /*
     * load comments using an AsyncTask
     */
    private void loadComments(CommentStatus statusFilter, LoadCommentsTaskParameters params) {
        if (mIsLoadTaskRunning) {
            AppLog.w(AppLog.T.COMMENTS, "load comments task already active");
        } else {
            new LoadCommentsTask(statusFilter).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        }
    }

    public void loadData(ArrayList<CommentListItem> commentListItems) {
        DiffResult diffResult = DiffUtil.calculateDiff(
                new CommentListItemDiffCallback(mComments, commentListItems));
        mComments.clear();
        mComments.addAll(commentListItems);
        diffResult.dispatchUpdatesTo(this);
    }

    /*
     * AsyncTask to load comments from SQLite
     */
    private boolean mIsLoadTaskRunning = false;

    @SuppressLint("StaticFieldLeak")
    private class LoadCommentsTask extends AsyncTask<LoadCommentsTaskParameters, Void, Boolean> {
        private ArrayList<CommentListItem> mTmpComments;
        final CommentStatus mStatusFilter;

        LoadCommentsTask(CommentStatus statusFilter) {
            mStatusFilter = statusFilter;
        }

        @Override
        protected void onPreExecute() {
            mIsLoadTaskRunning = true;
        }

        @Override
        protected void onCancelled() {
            mIsLoadTaskRunning = false;
        }

        @Override
        protected Boolean doInBackground(LoadCommentsTaskParameters... params) {
            LoadCommentsTaskParameters parameters = params[0];
            int numOfCommentsToFetch;
            // UNREPLIED filter has no paging, so we always request MAX_COMMENTS_IN_RESPONSE
            if (mStatusFilter == CommentStatus.UNREPLIED) {
                numOfCommentsToFetch = CommentsListFragment.MAX_COMMENTS_IN_RESPONSE;
            } else if (parameters.mIsLoadingCache) {
                numOfCommentsToFetch = CommentsListFragment.COMMENTS_PER_PAGE;
            } else if (parameters.mIsReloadingContent) {
                // round up to nearest page size (eg. 30, 60, 90, etc.)
                numOfCommentsToFetch = ((getNumberOfComments() + CommentsListFragment.COMMENTS_PER_PAGE - 1)
                                        / CommentsListFragment.COMMENTS_PER_PAGE)
                                       * CommentsListFragment.COMMENTS_PER_PAGE;
            } else {
                numOfCommentsToFetch = CommentsListFragment.COMMENTS_PER_PAGE + parameters.mOffset;
            }

            List<CommentModel> comments;
            if (mStatusFilter == null || mStatusFilter == CommentStatus.ALL
                || mStatusFilter == CommentStatus.UNREPLIED) {
                // The "all" filter actually means "approved" + "unapproved" (but not "spam", "trash" or "deleted")
                comments = mCommentStore.getCommentsForSite(mSite, false, numOfCommentsToFetch, CommentStatus.APPROVED,
                        CommentStatus.UNAPPROVED);
            } else {
                comments = mCommentStore.getCommentsForSite(mSite, false, numOfCommentsToFetch,
                        mStatusFilter);
            }

            if (mStatusFilter == CommentStatus.UNREPLIED) {
                comments = getUnrepliedComments(comments);
            }

            // Sort by date
            Collections.sort(comments, (commentModelX, commentModelY) -> {
                Date d0 = DateTimeUtils.dateFromIso8601(commentModelX.getDatePublished());
                Date d1 = DateTimeUtils.dateFromIso8601(commentModelY.getDatePublished());
                if (d0 == null || d1 == null) {
                    return 0;
                }
                return d1.compareTo(d0);
            });

            mTmpComments = new ArrayList<>();
            for (CommentModel comment : comments) {
                Comment commentListItem = new Comment(comment);

                if (!mTmpComments.isEmpty()) {
                    Comment lastItem = (Comment) mTmpComments.get(mTmpComments.size() - 1);
                    if (lastItem == null || !getFormattedDate(lastItem.getComment())
                            .equals(getFormattedDate(comment))) {
                        mTmpComments.add(
                                new SubHeader(getFormattedDate(comment), comment.getPublishedTimestamp() * -10));
                    }
                } else {
                    mTmpComments.add(
                            new SubHeader(getFormattedDate(comment), comment.getPublishedTimestamp() * -10));
                }

                mTmpComments.add(commentListItem);
            }

            return true;
        }

        private ArrayList<CommentModel> getUnrepliedComments(List<CommentModel> comments) {
            CommentLeveler leveler = new CommentLeveler(comments);
            ArrayList<CommentModel> leveledComments = leveler.createLevelList();

            ArrayList<CommentModel> topLevelComments = new ArrayList<>();
            for (CommentModel comment : leveledComments) {
                // only check top level comments
                if (comment.level == 0) {
                    ArrayList<CommentModel> childrenComments = leveler.getChildren(comment.getRemoteCommentId());
                    // comment is not mine and has no replies
                    if (!isMyComment(comment) && childrenComments.isEmpty()) {
                        topLevelComments.add(comment);
                    } else if (!isMyComment(comment)) {  // comment is not mine and has replies
                        boolean hasMyReplies = false;
                        for (CommentModel childrenComment : childrenComments) { // check if any replies are mine
                            if (isMyComment(childrenComment)) {
                                hasMyReplies = true;
                                break;
                            }
                        }

                        if (!hasMyReplies) {
                            topLevelComments.add(comment);
                        }
                    }
                }
            }
            return topLevelComments;
        }

        private boolean isMyComment(CommentModel comment) {
            String myEmail;
            // if site is self hosted, we want to use email associate with it, even if we are logged into wpcom
            if (!mSite.isUsingWpComRestApi()) {
                myEmail = mSite.getEmail();
            } else {
                AccountModel account = mAccountStore.getAccount();
                myEmail = account.getEmail();
            }

            return comment.getAuthorEmail().equals(myEmail);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                loadData(mTmpComments);
            }

            if (mOnDataLoadedListener != null) {
                mOnDataLoadedListener.onDataLoaded(isEmpty());
            }

            mIsLoadTaskRunning = false;
        }
    }

    static class LoadCommentsTaskParameters {
        int mOffset;
        boolean mIsLoadingCache;
        boolean mIsReloadingContent;

        LoadCommentsTaskParameters(int offset, boolean isLoadingCache, boolean isReloadingContent) {
            mOffset = offset;
            mIsLoadingCache = isLoadingCache;
            mIsReloadingContent = isReloadingContent;
        }
    }
}
