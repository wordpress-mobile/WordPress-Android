package org.wordpress.android.ui.comments;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

class CommentAdapter  extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    interface OnDataLoadedListener {
        void onDataLoaded(boolean isEmpty);
    }

    interface OnLoadMoreListener {
        void onLoadMore();
    }

    interface OnSelectedItemsChangeListener {
        void onSelectedItemsChanged();
    }

    interface OnCommentPressedListener {
        void onCommentPressed(int position, View view);
        void onCommentLongPressed(int position, View view);
    }

    private final LayoutInflater mInflater;

    private final CommentList mComments = new CommentList();
    private final HashSet<Integer> mSelectedPositions = new HashSet<>();
    private final List<Long> mModeratingCommentsIds = new ArrayList<>();

    private final int mStatusColorSpam;
    private final int mStatusColorUnapproved;

    private final int mLocalBlogId;
    private final int mAvatarSz;
    private final String mStatusTextSpam;
    private final String mStatusTextUnapproved;
    private final int mSelectedColor;
    private final int mUnselectedColor;

    private OnDataLoadedListener mOnDataLoadedListener;
    private OnCommentPressedListener mOnCommentPressedListener;
    private OnLoadMoreListener mOnLoadMoreListener;
    private OnSelectedItemsChangeListener mOnSelectedChangeListener;

    private boolean mEnableSelection;

    class CommentHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {
        private final TextView txtTitle;
        private final TextView txtComment;
        private final TextView txtStatus;
        private final TextView txtDate;
        private final WPNetworkImageView imgAvatar;
        private final ImageView imgCheckmark;
        private final View progressBar;
        private final ViewGroup containerView;

        public CommentHolder(View view) {
            super(view);
            txtTitle = (TextView) view.findViewById(R.id.title);
            txtComment = (TextView) view.findViewById(R.id.comment);
            txtStatus = (TextView) view.findViewById(R.id.status);
            txtDate = (TextView) view.findViewById(R.id.text_date);
            imgCheckmark = (ImageView) view.findViewById(R.id.image_checkmark);
            imgAvatar = (WPNetworkImageView) view.findViewById(R.id.avatar);
            progressBar = view.findViewById(R.id.moderate_progress);
            containerView = (ViewGroup) view.findViewById(R.id.layout_container);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mOnCommentPressedListener != null) {
                mOnCommentPressedListener.onCommentPressed(getAdapterPosition(), v);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mOnCommentPressedListener != null) {
                mOnCommentPressedListener.onCommentLongPressed(getAdapterPosition(), v);
            }
            return true;
        }
    }

    CommentAdapter(Context context, int localBlogId) {
        mInflater = LayoutInflater.from(context);

        mLocalBlogId = localBlogId;

        mStatusColorSpam = context.getResources().getColor(R.color.comment_status_spam);
        mStatusColorUnapproved = context.getResources().getColor(R.color.comment_status_unapproved);

        mUnselectedColor = context.getResources().getColor(R.color.white);
        mSelectedColor = context.getResources().getColor(R.color.translucent_grey_lighten_20);

        mStatusTextSpam = context.getResources().getString(R.string.comment_status_spam);
        mStatusTextUnapproved = context.getResources().getString(R.string.comment_status_unapproved);

        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);

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

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.comment_listitem, null);
        CommentHolder holder = new CommentHolder(view);
        view.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        Comment comment = mComments.get(position);
        CommentHolder holder = (CommentHolder) viewHolder;

        if (isModeratingCommentId(comment.commentID)) {
            holder.progressBar.setVisibility(View.VISIBLE);
        } else {
            holder.progressBar.setVisibility(View.GONE);
        }

        holder.txtTitle.setText(Html.fromHtml(comment.getFormattedTitle()));
        holder.txtComment.setText(comment.getUnescapedCommentText());
        holder.txtDate.setText(DateTimeUtils.javaDateToTimeSpan(comment.getDatePublished()));

        // status is only shown for comments that haven't been approved
        final boolean showStatus;
        switch (comment.getStatusEnum()) {
            case SPAM :
                showStatus = true;
                holder.txtStatus.setText(mStatusTextSpam);
                holder.txtStatus.setTextColor(mStatusColorSpam);
                break;
            case UNAPPROVED:
                showStatus = true;
                holder.txtStatus.setText(mStatusTextUnapproved);
                holder.txtStatus.setTextColor(mStatusColorUnapproved);
                break;
            default :
                showStatus = false;
                break;
        }
        holder.txtStatus.setVisibility(showStatus ? View.VISIBLE : View.GONE);

        int checkmarkVisibility;
        if (mEnableSelection && isItemSelected(position)) {
            checkmarkVisibility = View.VISIBLE;
            holder.containerView.setBackgroundColor(mSelectedColor);
        } else {
            checkmarkVisibility = View.GONE;
            holder.imgAvatar.setImageUrl(comment.getAvatarForDisplay(mAvatarSz), WPNetworkImageView.ImageType.AVATAR);
            holder.containerView.setBackgroundColor(mUnselectedColor);
        }

        if (holder.imgCheckmark.getVisibility() != checkmarkVisibility) {
            holder.imgCheckmark.setVisibility(checkmarkVisibility);
        }

        // comment text needs to be to the left of date/status when the title is a single line and
        // the status is displayed or else the status may overlap the comment text - note that
        // getLineCount() will return 0 if the view hasn't been rendered yet, which is why we
        // check getLineCount() <= 1
        boolean adjustComment = (showStatus && holder.txtTitle.getLineCount() <= 1);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.txtComment.getLayoutParams();
        if (adjustComment) {
            params.addRule(RelativeLayout.LEFT_OF, R.id.layout_date_status);
        } else {
            params.addRule(RelativeLayout.LEFT_OF, 0);
        }

        // request to load more comments when we near the end
        if (mOnLoadMoreListener != null && position >= getItemCount()-1) {
            mOnLoadMoreListener.onLoadMore();
        }
    }

    public Comment getItem(int position) {
        if (isPositionValid(position)) {
            return mComments.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return mComments.get(position).commentID;
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }

    boolean isEmpty() {
        return getItemCount() == 0;
    }

    void setEnableSelection(boolean enable) {
        if (enable == mEnableSelection) return;

        mEnableSelection = enable;
        if (mEnableSelection) {
            notifyDataSetChanged();
        } else {
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

        for (Integer position: mSelectedPositions) {
            if (isPositionValid(position))
                comments.add(mComments.get(position));
        }

        return comments;
    }

    private boolean isItemSelected(int position) {
        return mSelectedPositions.contains(position);
    }

    void setItemSelected(int position, boolean isSelected, View view) {
        if (isItemSelected(position) == isSelected) return;

        if (isSelected) {
            mSelectedPositions.add(position);
        } else {
            mSelectedPositions.remove(position);
        }

        notifyItemChanged(position);

        if (view != null && view.getTag() instanceof CommentHolder) {
            CommentHolder holder = (CommentHolder) view.getTag();
            // animate the selection change
            AniUtils.startAnimation(holder.imgCheckmark, isSelected ? R.anim.cab_select : R.anim.cab_deselect);
            holder.imgCheckmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }

        if (mOnSelectedChangeListener != null) {
            mOnSelectedChangeListener.onSelectedItemsChanged();
        }
    }

    void toggleItemSelected(int position, View view) {
        setItemSelected(position, !isItemSelected(position), view);
    }

    public void addModeratingCommentId(long commentId) {
        mModeratingCommentsIds.add(commentId);
        int position = indexOfCommentId(commentId);
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    public void removeModeratingCommentId(long commentId) {
        mModeratingCommentsIds.remove(commentId);
        int position = indexOfCommentId(commentId);
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    public boolean isModeratingCommentId(long commentId) {
        return mModeratingCommentsIds.size() > 0
                && mModeratingCommentsIds.contains(commentId);
    }

    private int indexOfCommentId(long commentId) {
        return mComments.indexOfCommentId(commentId);
    }

    private boolean isPositionValid(int position) {
        return (position >= 0 && position < mComments.size());
    }

    void replaceComments(CommentList comments) {
        mComments.replaceComments(comments);
        notifyDataSetChanged();
    }

    void deleteComments(CommentList comments) {
        mComments.deleteComments(comments);
        notifyDataSetChanged();
        if (mOnDataLoadedListener != null) {
            mOnDataLoadedListener.onDataLoaded(isEmpty());
        }
    }

    public void removeComment(Comment comment) {
        int position = indexOfCommentId(comment.commentID);
        if (position >= 0) {
            mComments.remove(position);
            notifyItemRemoved(position);
        }
    }

    /*
     * load comments using an AsyncTask
     */
    void loadComments() {
        if (mIsLoadTaskRunning) {
            AppLog.w(AppLog.T.COMMENTS, "load comments task already active");
        } else {
            new LoadCommentsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /*
     * AsyncTask to load comments from SQLite
     */
    private boolean mIsLoadTaskRunning = false;
    private class LoadCommentsTask extends AsyncTask<Void, Void, Boolean> {
        CommentList tmpComments;
        @Override
        protected void onPreExecute() {
            mIsLoadTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsLoadTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            tmpComments = CommentTable.getCommentsForBlog(mLocalBlogId);
            if (mComments.isSameList(tmpComments)) {
                return false;
            }

            // pre-calc transient values so they're cached prior to display
            for (Comment comment: tmpComments) {
                comment.getDatePublished();
                comment.getUnescapedCommentText();
                comment.getUnescapedPostTitle();
                comment.getAvatarForDisplay(mAvatarSz);
                comment.getFormattedTitle();
            }

            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mComments.clear();
                mComments.addAll(tmpComments);
                notifyDataSetChanged();
            }

            if (mOnDataLoadedListener != null) {
                mOnDataLoadedListener.onDataLoaded(isEmpty());
            }

            mIsLoadTaskRunning = false;
        }
    }
}
