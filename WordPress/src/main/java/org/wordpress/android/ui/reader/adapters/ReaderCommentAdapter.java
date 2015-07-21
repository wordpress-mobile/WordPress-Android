package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderCommentList;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.comments.CommentUtils;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderAnim;
import org.wordpress.android.ui.reader.ReaderInterfaces;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderCommentActions;
import org.wordpress.android.ui.reader.utils.ReaderLinkMovementMethod;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class ReaderCommentAdapter extends RecyclerView.Adapter<ReaderCommentAdapter.CommentHolder> {
    private final ReaderPost mPost;
    private boolean mMoreCommentsExist;

    private static final int MAX_INDENT_LEVEL = 2;
    private final int mIndentPerLevel;
    private final int mAvatarSz;

    private long mHighlightCommentId = 0;
    private boolean mShowProgressForHighlightedComment = false;
    private final boolean mIsPrivatePost;
    private final boolean mIsLoggedOutReader;

    private final int mLinkColor;
    private final int mNoLinkColor;

    private final String mLike;
    private final String mLikedBy;
    private final String mLikesSingle;
    private final String mLikedByYou;
    private final String mLikesMulti;

    public interface RequestReplyListener {
        void onRequestReply(long commentId);
    }

    private ReaderCommentList mComments = new ReaderCommentList();
    private RequestReplyListener mReplyListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;
    private ReaderActions.DataRequestedListener mDataRequestedListener;

    class CommentHolder extends RecyclerView.ViewHolder {
        private final ViewGroup container;
        private final TextView txtAuthor;
        private final TextView txtText;
        private final TextView txtDate;

        private final WPNetworkImageView imgAvatar;
        private final View spacerIndent;
        private final ProgressBar progress;

        private final TextView txtReply;
        private final ImageView imgReply;

        private final ViewGroup layoutLikes;
        private final ImageView imgLike;
        private final TextView txtLike;
        private final TextView txtLikeCount;

        public CommentHolder(View view) {
            super(view);

            container = (ViewGroup) view.findViewById(R.id.layout_container);

            txtAuthor = (TextView) view.findViewById(R.id.text_comment_author);
            txtText = (TextView) view.findViewById(R.id.text_comment_text);
            txtDate = (TextView) view.findViewById(R.id.text_comment_date);

            txtReply = (TextView) view.findViewById(R.id.text_comment_reply);
            imgReply = (ImageView) view.findViewById(R.id.image_comment_reply);

            imgAvatar = (WPNetworkImageView) view.findViewById(R.id.image_comment_avatar);
            spacerIndent = view.findViewById(R.id.spacer_comment_indent);
            progress = (ProgressBar) view.findViewById(R.id.progress_comment);

            layoutLikes = (ViewGroup) view.findViewById(R.id.layout_likes);
            imgLike = (ImageView) layoutLikes.findViewById(R.id.image_comment_like);
            txtLike = (TextView) layoutLikes.findViewById(R.id.text_comment_like);
            txtLikeCount = (TextView) view.findViewById(R.id.text_comment_like_count);

            txtText.setLinksClickable(true);
            txtText.setMovementMethod(ReaderLinkMovementMethod.getInstance(mIsPrivatePost));
        }
    }

    /**
     *
     */
    public ReaderCommentAdapter(Context context, ReaderPost post) {
        mPost = post;
        mIsPrivatePost = (post != null && post.isPrivate);
        mIsLoggedOutReader = ReaderUtils.isLoggedOutReader();

        mIndentPerLevel = (context.getResources().getDimensionPixelSize(R.dimen.reader_comment_indent_per_level) / 2);
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);

        mLinkColor = context.getResources().getColor(R.color.reader_hyperlink);
        mNoLinkColor = context.getResources().getColor(R.color.grey_darken_10);

        mLike = context.getString(R.string.reader_label_like);
        mLikedBy = context.getString(R.string.reader_label_liked_by);
        mLikedByYou = context.getString(R.string.reader_label_liked_by_you);
        mLikesSingle = context.getString(R.string.reader_likes_one_short);
        mLikesMulti = context.getString(R.string.reader_likes_multi_short);

        setHasStableIds(true);
    }

    public void setReplyListener(RequestReplyListener replyListener) {
        mReplyListener = replyListener;
    }

    public void setDataLoadedListener(ReaderInterfaces.DataLoadedListener dataLoadedListener) {
        mDataLoadedListener = dataLoadedListener;
    }

    public void setDataRequestedListener(ReaderActions.DataRequestedListener dataRequestedListener) {
        mDataRequestedListener = dataRequestedListener;
    }

    public void refreshComments() {
        if (mIsTaskRunning) {
            AppLog.w(T.READER, "reader comment adapter > Load comments task already running");
        }
        new LoadCommentsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }

    public boolean isEmpty() {
        return (getItemCount() == 0);
    }

    @Override
    public CommentHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reader_listitem_comment, parent, false);
        return new CommentHolder(view);
    }

    @Override
    public void onBindViewHolder(CommentHolder holder, int position) {
        final ReaderComment comment = mComments.get(position);

        holder.txtAuthor.setText(comment.getAuthorName());
        holder.imgAvatar.setImageUrl(GravatarUtils.fixGravatarUrl(comment.getAuthorAvatar(), mAvatarSz), WPNetworkImageView.ImageType.AVATAR);
        CommentUtils.displayHtmlComment(holder.txtText, comment.getText(), holder.itemView.getWidth());

        java.util.Date dtPublished = DateTimeUtils.iso8601ToJavaDate(comment.getPublished());
        holder.txtDate.setText(DateTimeUtils.javaDateToTimeSpan(dtPublished));

        // tapping avatar or author name opens blog preview
        if (comment.hasAuthorBlogId()) {
            View.OnClickListener authorListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReaderActivityLauncher.showReaderBlogPreview(view.getContext(), comment.authorBlogId);
                }
            };
            holder.imgAvatar.setOnClickListener(authorListener);
            holder.txtAuthor.setOnClickListener(authorListener);
            holder.txtAuthor.setTextColor(mLinkColor);
        } else {
            holder.txtAuthor.setTextColor(mNoLinkColor);
        }

        // show indentation spacer for comments with parents and indent it based on comment level
        if (comment.parentId != 0 && comment.level > 0) {
            int indent = Math.min(MAX_INDENT_LEVEL, comment.level) * mIndentPerLevel;
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.spacerIndent.getLayoutParams();
            params.width = indent;
            holder.spacerIndent.setVisibility(View.VISIBLE);
        } else {
            holder.spacerIndent.setVisibility(View.GONE);
        }

        if (mHighlightCommentId != 0 && mHighlightCommentId == comment.commentId) {
            // different background for highlighted comment, with optional progress bar
            holder.container.setSelected(true);
            holder.progress.setVisibility(mShowProgressForHighlightedComment ? View.VISIBLE : View.GONE);
        } else {
            // different background for comments from the post's author
            holder.container.setSelected(comment.authorId == mPost.authorId);
            holder.progress.setVisibility(View.GONE);
        }

        if (mIsLoggedOutReader) {
            holder.txtReply.setVisibility(View.GONE);
            holder.imgReply.setVisibility(View.GONE);
        } else if (mReplyListener != null) {
            // tapping reply icon tells activity to show reply box
            View.OnClickListener replyClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mReplyListener.onRequestReply(comment.commentId);
                }
            };
            holder.txtReply.setOnClickListener(replyClickListener);
            holder.imgReply.setOnClickListener(replyClickListener);
        }

        showLikeStatus(holder, position);

        // if we're nearing the end of the comments and we know more exist on the server,
        // fire request to load more
        if (mMoreCommentsExist && mDataRequestedListener != null && (position >= getItemCount()-1)) {
            mDataRequestedListener.onRequestData();
        }
    }

    @Override
    public long getItemId(int position) {
        if (isValidPosition(position)) {
            return mComments.get(position).commentId;
        } else {
            return 0;
        }
    }

    private boolean isValidPosition(int position) {
        return (position >= 0 && position < mComments.size());
    }

    private void showLikeStatus(final CommentHolder holder, final int position) {
        if (!isValidPosition(position)) {
            return;
        }

        final ReaderComment comment = mComments.get(position);
        if (mPost.isLikesEnabled) {
            holder.layoutLikes.setVisibility(View.VISIBLE);
            holder.imgLike.setSelected(comment.isLikedByCurrentUser);

            if (comment.numLikes == 0) {
                // no likes, so show "Like" as the caption with no count
                holder.txtLike.setText(mLike);
                holder.txtLike.setTextColor(mLinkColor);
                holder.txtLikeCount.setVisibility(View.GONE);
            } else if (comment.numLikes == 1 && comment.isLikedByCurrentUser) {
                // comment is liked only by the current user, so show "Liked by you" with no count
                holder.txtLike.setText(mLikedByYou);
                holder.txtLike.setTextColor(mLinkColor);
                holder.txtLikeCount.setVisibility(View.GONE);
            } else {
                // otherwise show "Liked by" followed by the like count
                holder.txtLike.setText(mLikedBy);
                holder.txtLike.setTextColor(mNoLinkColor);
                holder.txtLikeCount.setText(comment.numLikes == 1 ? mLikesSingle : String.format(mLikesMulti, comment.numLikes));
                holder.txtLikeCount.setSelected(comment.isLikedByCurrentUser);
                holder.txtLikeCount.setVisibility(View.VISIBLE);
            }

            if (mIsLoggedOutReader) {
                holder.imgLike.setEnabled(false);
                holder.txtLike.setEnabled(false);
                holder.txtLikeCount.setEnabled(false);
            } else {
                // toggle like when layout containing like image and caption is tapped
                holder.layoutLikes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleLike(v.getContext(), holder, position);
                    }
                });
            }

            // show liking users when like count is tapped
            holder.txtLikeCount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.showReaderLikingUsers(v.getContext(), comment);
                }
            });
        } else {
            holder.layoutLikes.setVisibility(View.GONE);
            holder.layoutLikes.setOnClickListener(null);
        }
    }

    private void toggleLike(Context context, CommentHolder holder, int position) {
        if (!NetworkUtils.checkConnection(context)) {
            return;
        }

        if (!isValidPosition(position)) {
            ToastUtils.showToast(context, R.string.reader_toast_err_generic);
            return;
        }

        ReaderComment comment = mComments.get(position);
        boolean isAskingToLike = !comment.isLikedByCurrentUser;
        ReaderAnim.animateLikeButton(holder.imgLike, isAskingToLike);

        if (!ReaderCommentActions.performLikeAction(comment, isAskingToLike)) {
            ToastUtils.showToast(context, R.string.reader_toast_err_generic);
            return;
        }

        ReaderComment updatedComment = ReaderCommentTable.getComment(comment.blogId, comment.postId, comment.commentId);
        mComments.set(position, updatedComment);
        showLikeStatus(holder, position);
    }

    /*
     * called from post detail activity when user submits a comment
     */
    public void addComment(ReaderComment comment) {
        if (comment == null) {
            return;
        }

        // if the comment doesn't have a parent we can just add it to the list of existing
        // comments - but if it does have a parent, we need to reload the list so that it
        // appears under its parent and is correctly indented
        if (comment.parentId == 0) {
            mComments.add(comment);
            notifyDataSetChanged();
        } else {
            refreshComments();
        }
    }

    /*
     * called from post detail when submitted a comment fails - this removes the "fake" comment
     * that was inserted while the API call was still being processed
     */
    public void removeComment(long commentId) {
        if (commentId == mHighlightCommentId) {
            setHighlightCommentId(0, false);
        }

        int position = indexOfCommentId(commentId);
        if (position == -1) {
            return;
        }

        mComments.remove(position);
        notifyDataSetChanged();
    }

    /*
     * replace the comment that has the passed commentId with another comment - used
     * after a comment is submitted to replace the "fake" comment with the real one
     */
    public void replaceComment(long commentId, ReaderComment comment) {
        int position = mComments.replaceComment(commentId, comment);
        if (position > -1) {
            notifyItemChanged(position);
        }
    }

    /*
     * sets the passed comment as highlighted with a different background color and an optional
     * progress bar (used when posting new comments) - note that we don't call notifyDataSetChanged()
     * here since in most cases it's unnecessary, so we leave it up to the caller to do that
     */
    public void setHighlightCommentId(long commentId, boolean showProgress) {
        mHighlightCommentId = commentId;
        mShowProgressForHighlightedComment = showProgress;
    }

    public int indexOfCommentId(long commentId) {
        return mComments.indexOfCommentId(commentId);
    }

    /*
     * AsyncTask to load comments for this post
     */
    private boolean mIsTaskRunning = false;
    private class LoadCommentsTask extends AsyncTask<Void, Void, Boolean> {
        private ReaderCommentList tmpComments;
        private boolean tmpMoreCommentsExist;

        @Override
        protected void onPreExecute() {
            mIsTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            if (mPost == null) {
                return false;
            }

            // determine whether more comments can be downloaded by comparing the number of
            // comments the post says it has with the number of comments actually stored
            // locally for this post
            int numServerComments = ReaderPostTable.getNumCommentsForPost(mPost);
            int numLocalComments = ReaderCommentTable.getNumCommentsForPost(mPost);
            tmpMoreCommentsExist = (numServerComments > numLocalComments);

            tmpComments = ReaderCommentTable.getCommentsForPost(mPost);
            return !mComments.isSameList(tmpComments);
        }
        @Override
        protected void onPostExecute(Boolean result) {
            mMoreCommentsExist = tmpMoreCommentsExist;

            if (result) {
                // assign the comments with children sorted under their parents and indent levels applied
                mComments = ReaderCommentList.getLevelList(tmpComments);
                notifyDataSetChanged();
            }
            if (mDataLoadedListener != null) {
                mDataLoadedListener.onDataLoaded(isEmpty());
            }
            mIsTaskRunning = false;
        }
    }
}
