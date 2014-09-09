package org.wordpress.android.ui.comments;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
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

class CommentAdapter extends BaseAdapter {
    static interface DataLoadedListener {
        public void onDataLoaded(boolean isEmpty);
    }

    static interface OnLoadMoreListener {
        public void onLoadMore();
    }

    static interface OnSelectedItemsChangeListener {
        public void onSelectedItemsChanged();
    }

    private final LayoutInflater mInflater;
    private final DataLoadedListener mDataLoadedListener;
    private final OnLoadMoreListener mOnLoadMoreListener;
    private final OnSelectedItemsChangeListener mOnSelectedChangeListener;

    private CommentList mComments = new CommentList();
    private final HashSet<Integer> mSelectedPositions = new HashSet<Integer>();
    private final List<Long> mModeratingCommentsIds = new ArrayList<Long>();

    private final int mStatusColorSpam;
    private final int mStatusColorUnapproved;

    private final int mAvatarSz;

    private final String mStatusTextSpam;
    private final String mStatusTextUnapproved;
    private final int mSelectionColor;

    private boolean mEnableSelection;

    CommentAdapter(Context context,
                   DataLoadedListener onDataLoadedListener,
                   OnLoadMoreListener onLoadMoreListener,
                   OnSelectedItemsChangeListener onChangeListener) {
        mInflater = LayoutInflater.from(context);

        mDataLoadedListener = onDataLoadedListener;
        mOnLoadMoreListener = onLoadMoreListener;
        mOnSelectedChangeListener = onChangeListener;

        mStatusColorSpam = context.getResources().getColor(R.color.comment_status_spam);
        mStatusColorUnapproved = context.getResources().getColor(R.color.comment_status_unapproved);
        mSelectionColor = context.getResources().getColor(R.color.blue_extra_light);

        mStatusTextSpam = context.getResources().getString(R.string.comment_status_spam);
        mStatusTextUnapproved = context.getResources().getString(R.string.comment_status_unapproved);

        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
    }

    @Override
    public int getCount() {
        return (mComments != null ? mComments.size() : 0);
    }

    @Override
    public Object getItem(int position) {
        return mComments.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    void clear() {
        if (mComments.size() > 0) {
            mComments.clear();
            notifyDataSetChanged();
        }
    }

    void setEnableSelection(boolean enable) {
        if (enable == mEnableSelection)
            return;

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
            if (mOnSelectedChangeListener != null)
                mOnSelectedChangeListener.onSelectedItemsChanged();
        }
    }

    int getSelectedCommentCount() {
        return mSelectedPositions.size();
    }

    CommentList getSelectedComments() {
        CommentList comments = new CommentList();
        if (!mEnableSelection)
            return comments;

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
        if (isItemSelected(position) == isSelected)
            return;

        if (isSelected) {
            mSelectedPositions.add(position);
        } else {
            mSelectedPositions.remove(position);
        }

        notifyDataSetChanged();

        if (view != null && view.getTag() instanceof CommentHolder) {
            CommentHolder holder = (CommentHolder) view.getTag();
            // animate the selection change on ICS or later (looks wonky on Gingerbread)
            holder.imgCheckmark.clearAnimation();
            AniUtils.startAnimation(holder.imgCheckmark, isSelected ? R.anim.cab_select : R.anim.cab_deselect);
            holder.imgCheckmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }

        if (mOnSelectedChangeListener != null)
            mOnSelectedChangeListener.onSelectedItemsChanged();
    }

    void toggleItemSelected(int position, View view) {
        setItemSelected(position, !isItemSelected(position), view);
    }

    public void addModeratingCommentId(long commentId) {
        mModeratingCommentsIds.add(commentId);
        notifyDataSetChanged();
    }

    public void removeModeratingCommentId(long commentId) {
        mModeratingCommentsIds.remove(commentId);
        notifyDataSetChanged();
    }

    public boolean isModeratingCommentId(long commentId) {
        return mModeratingCommentsIds.size() > 0 && mModeratingCommentsIds.contains(commentId);
    }

    public int indexOfCommentId(long commentId) {
        return mComments.indexOfCommentId(commentId);
    }

    private boolean isPositionValid(int position) {
        return (position >= 0 && position < mComments.size());
    }

    void replaceComments(final CommentList comments) {
        mComments.replaceComments(comments);
        notifyDataSetChanged();
    }

    void deleteComments(final CommentList comments) {
        mComments.deleteComments(comments);
        notifyDataSetChanged();
    }

    public void removeComment(Comment comment) {
        int commentIndex = indexOfCommentId(comment.commentID);
        if (commentIndex >= 0) {
            mComments.remove(commentIndex);
            notifyDataSetChanged();
        }
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        final Comment comment = mComments.get(position);
        final CommentHolder holder;

        if (convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.comment_listitem, null);
            holder = new CommentHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (CommentHolder) convertView.getTag();
        }

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

        boolean useSelectionBackground = false;
        if (mEnableSelection && isItemSelected(position)) {
            useSelectionBackground = true;
            if (holder.imgCheckmark.getVisibility() != View.VISIBLE)
                holder.imgCheckmark.setVisibility(View.VISIBLE);
        } else {
            if (holder.imgCheckmark.getVisibility() == View.VISIBLE)
                holder.imgCheckmark.setVisibility(View.GONE);
            holder.imgAvatar.setImageUrl(comment.getAvatarForDisplay(mAvatarSz), WPNetworkImageView.ImageType.AVATAR);
        }

        if (useSelectionBackground) {
            convertView.setBackgroundColor(mSelectionColor);
        } else {
            convertView.setBackgroundDrawable(null);
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
        if (mOnLoadMoreListener != null && position >= getCount()-1)
            mOnLoadMoreListener.onLoadMore();

        return convertView;
    }

    private class CommentHolder {
        private final TextView txtTitle;
        private final TextView txtComment;
        private final TextView txtStatus;
        private final TextView txtDate;
        private final WPNetworkImageView imgAvatar;
        private final ImageView imgCheckmark;
        private final View progressBar;

        private CommentHolder(View row) {
            txtTitle = (TextView) row.findViewById(R.id.title);
            txtComment = (TextView) row.findViewById(R.id.comment);
            txtStatus = (TextView) row.findViewById(R.id.status);
            txtDate = (TextView) row.findViewById(R.id.text_date);
            imgCheckmark = (ImageView) row.findViewById(R.id.image_checkmark);
            imgAvatar = (WPNetworkImageView) row.findViewById(R.id.avatar);
            progressBar = row.findViewById(R.id.moderate_progress);
        }
    }

    /*
     * load comments using an AsyncTask
     */
    void loadComments() {
        if (mIsLoadTaskRunning) {
            AppLog.w(AppLog.T.COMMENTS, "load comments task already active");
        }
        new LoadCommentsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
            int localBlogId = WordPress.getCurrentLocalTableBlogId();
            tmpComments = CommentTable.getCommentsForBlog(localBlogId);
            if (mComments.isSameList(tmpComments))
                return false;

            // pre-calc transient values so they're cached when used by getView()
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
                mComments = (CommentList)(tmpComments.clone());
                notifyDataSetChanged();
            }

            if (mDataLoadedListener != null)
                mDataLoadedListener.onDataLoaded(isEmpty());

            mIsLoadTaskRunning = false;
        }
    }
}
