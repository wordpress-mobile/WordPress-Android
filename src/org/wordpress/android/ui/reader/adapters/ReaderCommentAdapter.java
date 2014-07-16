package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderCommentActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.widgets.WPNetworkImageView;

public class ReaderCommentAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final ReaderPost mPost;
    private boolean mMoreCommentsExist;

    private static final int MAX_INDENT_LEVEL = 2;
    private final int mIndentPerLevel;
    private final int mAvatarSz;
    private final int mMaxImageSz;

    private long mHighlightCommentId = 0;
    private boolean mShowProgressForHighlightedComment = false;

    private final int mBgColorNormal;
    private final int mBgColorHighlight;
    private final int mLinkColor;
    private final int mNoLinkColor;

    private final String mLike;
    private final String mLiked;
    private final String mLikesSingle;
    private final String mLikesMulti;

    public interface RequestReplyListener {
        void onRequestReply(long commentId);
    }

    private ReaderCommentList mComments = new ReaderCommentList();
    private final RequestReplyListener mReplyListener;
    private final ReaderActions.DataLoadedListener mDataLoadedListener;
    private final ReaderActions.DataRequestedListener mDataRequestedListener;

    public ReaderCommentAdapter(Context context,
                                ReaderPost post,
                                RequestReplyListener replyListener,
                                ReaderActions.DataLoadedListener dataLoadedListener,
                                ReaderActions.DataRequestedListener dataRequestedListener) {
        mPost = post;
        mReplyListener = replyListener;
        mDataLoadedListener = dataLoadedListener;
        mDataRequestedListener = dataRequestedListener;

        mInflater = LayoutInflater.from(context);
        mIndentPerLevel = (context.getResources().getDimensionPixelSize(R.dimen.reader_comment_indent_per_level) / 2);
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);
        mMaxImageSz = context.getResources().getDimensionPixelSize(R.dimen.reader_comment_max_image_size);

        mBgColorNormal = context.getResources().getColor(R.color.grey_extra_light);
        mBgColorHighlight = context.getResources().getColor(R.color.grey_light);

        mLinkColor = context.getResources().getColor(R.color.reader_hyperlink);
        mNoLinkColor = context.getResources().getColor(R.color.grey_medium_dark);

        mLike = context.getString(R.string.reader_label_like);
        mLiked = context.getString(R.string.reader_label_liked);

        // like count ends with an ellipsis to make it more obvious that it's tappable
        mLikesSingle = context.getString(R.string.reader_likes_one_short) + "…";
        mLikesMulti = context.getString(R.string.reader_likes_multi_short) + "…";
    }

    public void refreshComments() {
        if (mIsTaskRunning) {
            AppLog.w(T.READER, "reader comment adapter > Load comments task already running");
        }
        new LoadCommentsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getCount() {
        return mComments.size();
    }

    @Override
    public Object getItem(int position) {
        return mComments.get(position);
    }

    @Override
    public long getItemId(int position) {
        // this MUST return the comment id in order for ReaderPostDetailActivity to enable replying
        // to an individual comment when clicked - note that while the commentId isn't unique in our
        // database, it will be unique here since we're only showing comments on a specific post
        return mComments.get(position).commentId;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ReaderComment comment = mComments.get(position);
        final CommentHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.reader_listitem_comment, parent, false);
            holder = new CommentHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (CommentHolder) convertView.getTag();
        }

        holder.txtAuthor.setText(comment.getAuthorName());
        holder.imgAvatar.setImageUrl(PhotonUtils.fixAvatar(comment.getAuthorAvatar(), mAvatarSz), WPNetworkImageView.ImageType.AVATAR);
        CommentUtils.displayHtmlComment(holder.txtText, comment.getText(), mMaxImageSz);

        java.util.Date dtPublished = DateTimeUtils.iso8601ToJavaDate(comment.getPublished());
        holder.txtDate.setText(DateTimeUtils.javaDateToTimeSpan(dtPublished));

        // tapping avatar or author name opens blog preview
        if (comment.hasAuthorBlogId()) {
            View.OnClickListener authorListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReaderActivityLauncher.showReaderBlogPreview(view.getContext(), comment.authorBlogId, comment.getAuthorUrl());
                }
            };
            holder.imgAvatar.setOnClickListener(authorListener);
            holder.txtAuthor.setOnClickListener(authorListener);
            holder.txtAuthor.setTextColor(mLinkColor);
        } else {
            holder.txtAuthor.setTextColor(mNoLinkColor);
        }

        // show indentation spacer and indent it based on comment level
        holder.spacerIndent.setVisibility(comment.parentId==0 ? View.GONE : View.VISIBLE);
        if (comment.level > 0) {
            int indent = Math.min(MAX_INDENT_LEVEL, comment.level) * mIndentPerLevel;
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.spacerIndent.getLayoutParams();
            if (params.width != indent) {
                params.width = indent;
            }
            holder.spacerIndent.setVisibility(View.VISIBLE);
        }

        if (mHighlightCommentId == comment.commentId) {
            // different background for highlighted comment, with optional progress bar
            convertView.setBackgroundColor(mBgColorHighlight);
            holder.progress.setVisibility(mShowProgressForHighlightedComment ? View.VISIBLE : View.GONE);
        } else if (comment.authorId == mPost.authorId) {
            // different background color for comments from the post's author
            convertView.setBackgroundColor(mBgColorHighlight);
            holder.progress.setVisibility(View.GONE);
        } else {
            convertView.setBackgroundColor(mBgColorNormal);
            holder.progress.setVisibility(View.GONE);
        }

        // tapping reply icon tells activity to show reply box
        if (mReplyListener != null) {
            View.OnClickListener replyClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mReplyListener.onRequestReply(comment.commentId);
                }
            };
            holder.txtReply.setOnClickListener(replyClickListener);
            holder.imgReply.setOnClickListener(replyClickListener);
        }

        showLikeStatus(holder, comment, position);

        // if we're nearing the end of the comments and we know more exist on the server,
        // fire request to load more
        if (mMoreCommentsExist && mDataRequestedListener != null && (position >= getCount()-1)) {
            mDataRequestedListener.onRequestData();
        }

        return convertView;
    }

    private static class CommentHolder {
        private final TextView txtAuthor;
        private final TextView txtText;
        private final TextView txtDate;

        private final WPNetworkImageView imgAvatar;
        private final View spacerIndent;
        private final ProgressBar progress;

        private final TextView txtReply;
        private final ImageView imgReply;

        private final ImageView imgLike;
        private final TextView txtLike;
        private final TextView txtLikeCount;

        CommentHolder(View view) {
            txtAuthor = (TextView) view.findViewById(R.id.text_comment_author);
            txtText = (TextView) view.findViewById(R.id.text_comment_text);
            txtDate = (TextView) view.findViewById(R.id.text_comment_date);

            txtReply = (TextView) view.findViewById(R.id.text_comment_reply);
            imgReply = (ImageView) view.findViewById(R.id.image_comment_reply);

            imgAvatar = (WPNetworkImageView) view.findViewById(R.id.image_comment_avatar);
            spacerIndent = view.findViewById(R.id.spacer_comment_indent);
            progress = (ProgressBar) view.findViewById(R.id.progress_comment);

            imgLike = (ImageView) view.findViewById(R.id.image_comment_like);
            txtLike = (TextView) view.findViewById(R.id.text_comment_like);
            txtLikeCount = (TextView) view.findViewById(R.id.text_comment_like_count);

            // this is necessary in order for anchor tags in the comment text to be clickable
            txtText.setLinksClickable(true);
            txtText.setMovementMethod(WPLinkMovementMethod.getInstance());
        }
    }

    private void showLikeStatus(final CommentHolder holder,
                                final ReaderComment comment,
                                final int position) {
        if (mPost.isLikesEnabled) {
            holder.imgLike.setVisibility(View.VISIBLE);
            holder.imgLike.setSelected(comment.isLikedByCurrentUser);

            holder.txtLike.setVisibility(View.VISIBLE);
            holder.txtLike.setSelected(comment.isLikedByCurrentUser);
            holder.txtLike.setText(comment.isLikedByCurrentUser ? mLiked : mLike);

            switch (comment.numLikes) {
                case 0:
                    holder.txtLikeCount.setVisibility(View.GONE);
                    break;
                case 1:
                    holder.txtLikeCount.setVisibility(View.VISIBLE);
                    holder.txtLikeCount.setText(mLikesSingle);
                    break;
                default:
                    holder.txtLikeCount.setVisibility(View.VISIBLE);
                    holder.txtLikeCount.setText(String.format(mLikesMulti, comment.numLikes));

            }

            // toggle like when like image or caption is tapped
            View.OnClickListener likeListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleLike(holder, comment, position);
                }
            };
            holder.imgLike.setOnClickListener(likeListener);
            holder.txtLike.setOnClickListener(likeListener);

            // show liking users when like count is tapped
            holder.txtLikeCount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.showReaderLikingUsers(v.getContext(), comment);
                }
            });
        } else {
            holder.imgLike.setVisibility(View.GONE);
            holder.txtLike.setVisibility(View.GONE);
        }
    }

    private void toggleLike(final CommentHolder holder,
                            final ReaderComment comment,
                            final int position) {
        boolean isAskingToLike = !comment.isLikedByCurrentUser;
        ReaderAnim.animateLikeButton(holder.imgLike, isAskingToLike);

        if (!ReaderCommentActions.performLikeAction(comment, isAskingToLike)) {
            return;
        }

        ReaderComment updatedComment = ReaderCommentTable.getComment(comment.blogId, comment.postId, comment.commentId);
        mComments.set(position, updatedComment);
        showLikeStatus(holder, comment, position);
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
        mComments.replaceComment(commentId, comment);
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
