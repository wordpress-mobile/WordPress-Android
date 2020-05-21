package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderCommentList;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.comments.CommentUtils;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderAnim;
import org.wordpress.android.ui.reader.ReaderInterfaces;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderCommentActions;
import org.wordpress.android.ui.reader.utils.ReaderCommentLeveler;
import org.wordpress.android.ui.reader.utils.ReaderLinkMovementMethod;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.views.ReaderCommentsPostHeaderView;
import org.wordpress.android.ui.reader.views.ReaderIconCountView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.Date;

import javax.inject.Inject;

public class ReaderCommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ReaderPost mPost;
    private boolean mMoreCommentsExist;

    private static final int MAX_INDENT_LEVEL = 2;
    private final int mIndentPerLevel;
    private final int mAvatarSz;
    private final int mContentWidth;

    private long mHighlightCommentId = 0;
    private long mAnimateLikeCommentId = 0;
    private boolean mShowProgressForHighlightedComment = false;
    private final boolean mIsPrivatePost;
    private boolean mIsHeaderClickEnabled;

    private final int mColorAuthor;
    private final int mColorNotAuthor;
    private final int mColorHighlight;

    private static final int VIEW_TYPE_HEADER = 1;
    private static final int VIEW_TYPE_COMMENT = 2;

    private static final long ID_HEADER = -1L;

    private static final int NUM_HEADERS = 1;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ImageManager mImageManager;

    public interface RequestReplyListener {
        void onRequestReply(long commentId);
    }

    private ReaderCommentList mComments = new ReaderCommentList();
    private RequestReplyListener mReplyListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;
    private ReaderActions.DataRequestedListener mDataRequestedListener;

    class CommentHolder extends RecyclerView.ViewHolder {
        private final ViewGroup mContainer;
        private final TextView mTxtAuthor;
        private final TextView mTxtText;
        private final TextView mTxtDate;

        private final ImageView mImgAvatar;
        private final View mSpacerIndent;
        private final View mAuthorContainer;
        private final ProgressBar mProgress;

        private final ViewGroup mReplyView;
        private final ReaderIconCountView mCountLikes;

        CommentHolder(View view) {
            super(view);

            mContainer = view.findViewById(R.id.layout_container);

            mTxtAuthor = view.findViewById(R.id.text_comment_author);
            mTxtText = view.findViewById(R.id.text_comment_text);
            mTxtDate = view.findViewById(R.id.text_comment_date);

            mImgAvatar = view.findViewById(R.id.image_comment_avatar);
            mSpacerIndent = view.findViewById(R.id.spacer_comment_indent);
            mProgress = view.findViewById(R.id.progress_comment);

            mAuthorContainer = view.findViewById(R.id.layout_author);

            mReplyView = view.findViewById(R.id.reply_container);
            mCountLikes = view.findViewById(R.id.count_likes);

            mTxtText.setLinksClickable(true);
            mTxtText.setMovementMethod(ReaderLinkMovementMethod.getInstance(mIsPrivatePost));
        }
    }

    class PostHeaderHolder extends RecyclerView.ViewHolder {
        private final ReaderCommentsPostHeaderView mHeaderView;

        PostHeaderHolder(View view) {
            super(view);
            mHeaderView = (ReaderCommentsPostHeaderView) view;
        }
    }

    public ReaderCommentAdapter(Context context, ReaderPost post) {
        ((WordPress) context.getApplicationContext()).component().inject(this);
        mPost = post;
        mIsPrivatePost = (post != null && post.isPrivate);

        mIndentPerLevel = context.getResources().getDimensionPixelSize(R.dimen.reader_comment_indent_per_level);
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_extra_small);

        // calculate the max width of comment content
        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int cardMargin = context.getResources().getDimensionPixelSize(R.dimen.reader_card_margin);
        int contentPadding = context.getResources().getDimensionPixelSize(R.dimen.reader_card_content_padding);
        int mediumMargin = context.getResources().getDimensionPixelSize(R.dimen.margin_medium);
        mContentWidth = displayWidth - (cardMargin * 2) - (contentPadding * 2) - (mediumMargin * 2);

        mColorAuthor = ContextExtensionsKt.getColorFromAttribute(context, R.attr.colorPrimary);
        mColorNotAuthor = ContextExtensionsKt.getColorFromAttribute(context, R.attr.colorOnSurface);
        mColorHighlight = ColorUtils
                .setAlphaComponent(ContextExtensionsKt.getColorFromAttribute(context, R.attr.colorOnSurface),
                        context.getResources().getInteger(R.integer.selected_list_item_opacity));

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

    public void enableHeaderClicks() {
        mIsHeaderClickEnabled = true;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_COMMENT;
    }

    public void refreshComments() {
        if (mIsTaskRunning) {
            AppLog.w(T.READER, "reader comment adapter > Load comments task already running");
        }
        new LoadCommentsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public int getItemCount() {
        return mComments.size() + NUM_HEADERS;
    }

    public boolean isEmpty() {
        return mComments.size() == 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                View headerView = new ReaderCommentsPostHeaderView(parent.getContext());
                headerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                return new PostHeaderHolder(headerView);
            default:
                View commentView = LayoutInflater.from(parent.getContext())
                                                 .inflate(R.layout.reader_listitem_comment, parent, false);
                return new CommentHolder(commentView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PostHeaderHolder) {
            PostHeaderHolder headerHolder = (PostHeaderHolder) holder;
            headerHolder.mHeaderView.setPost(mPost);
            if (mIsHeaderClickEnabled) {
                headerHolder.mHeaderView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ReaderActivityLauncher.showReaderPostDetail(view.getContext(), mPost.blogId, mPost.postId);
                    }
                });
            }
            return;
        }

        final ReaderComment comment = getItem(position);
        if (comment == null) {
            return;
        }

        final CommentHolder commentHolder = (CommentHolder) holder;
        commentHolder.mTxtAuthor.setText(comment.getAuthorName());

        java.util.Date dtPublished;
        if (mShowProgressForHighlightedComment && mHighlightCommentId == comment.commentId) {
            dtPublished = new Date();
        } else {
            dtPublished = DateTimeUtils.dateFromIso8601(comment.getPublished());
        }
        commentHolder.mTxtDate.setText(DateTimeUtils.javaDateToTimeSpan(dtPublished, WordPress.getContext()));

        String avatarUrl = GravatarUtils.fixGravatarUrl(comment.getAuthorAvatar(), mAvatarSz);
        mImageManager.loadIntoCircle(commentHolder.mImgAvatar, ImageType.AVATAR, avatarUrl);

        // tapping avatar or author name opens blog preview
        if (comment.hasAuthorBlogId()) {
            View.OnClickListener authorListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReaderActivityLauncher.showReaderBlogPreview(view.getContext(), comment.authorBlogId);
                }
            };
            commentHolder.mAuthorContainer.setOnClickListener(authorListener);
            commentHolder.mAuthorContainer.setOnClickListener(authorListener);
        } else {
            commentHolder.mAuthorContainer.setOnClickListener(null);
            commentHolder.mAuthorContainer.setOnClickListener(null);
        }

        // author name uses different color for comments from the post's author
        if (comment.authorId == mPost.authorId) {
            commentHolder.mTxtAuthor.setTextColor(mColorAuthor);
        } else {
            commentHolder.mTxtAuthor.setTextColor(mColorNotAuthor);
        }

        // show indentation spacer for comments with parents and indent it based on comment level
        int indentWidth;
        if (comment.parentId != 0 && comment.level > 0) {
            indentWidth = Math.min(MAX_INDENT_LEVEL, comment.level) * mIndentPerLevel;
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) commentHolder.mSpacerIndent.getLayoutParams();
            params.width = indentWidth;
            commentHolder.mSpacerIndent.setVisibility(View.VISIBLE);
        } else {
            indentWidth = 0;
            commentHolder.mSpacerIndent.setVisibility(View.GONE);
        }

        int maxImageWidth = mContentWidth - indentWidth;
        CommentUtils.displayHtmlComment(commentHolder.mTxtText, comment.getText(), maxImageWidth);

        // different background for highlighted comment, with optional progress bar
        if (mHighlightCommentId != 0 && mHighlightCommentId == comment.commentId) {
            commentHolder.mContainer.setBackgroundColor(mColorHighlight);
            commentHolder.mProgress.setVisibility(mShowProgressForHighlightedComment ? View.VISIBLE : View.GONE);
        } else {
            commentHolder.mContainer.setBackgroundColor(0);
            commentHolder.mProgress.setVisibility(View.GONE);
        }

        if (!mAccountStore.hasAccessToken()) {
            commentHolder.mReplyView.setVisibility(View.GONE);
        } else {
            // tapping reply tells activity to show reply box
            commentHolder.mReplyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mReplyListener != null) {
                        mReplyListener.onRequestReply(comment.commentId);
                    }
                }
            });

            if (mAnimateLikeCommentId != 0 && mAnimateLikeCommentId == comment.commentId) {
                // simulate tapping on the "Like" button. Add a delay to help the user notice it.
                commentHolder.mCountLikes.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ReaderAnim.animateLikeButton(commentHolder.mCountLikes.getImageView(), true);
                    }
                }, 400);

                // clear the "command" to like a comment
                mAnimateLikeCommentId = 0;
            }
        }

        showLikeStatus(commentHolder, position);

        // if we're nearing the end of the comments and we know more exist on the server,
        // fire request to load more
        if (mMoreCommentsExist && mDataRequestedListener != null && (position >= getItemCount() - NUM_HEADERS)) {
            mDataRequestedListener.onRequestData();
        }
    }

    @Override
    public long getItemId(int position) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_HEADER:
                return ID_HEADER;
            default:
                ReaderComment comment = getItem(position);
                return comment != null ? comment.commentId : 0;
        }
    }

    private ReaderComment getItem(int position) {
        return position == 0 ? null : mComments.get(position - NUM_HEADERS);
    }

    /*
     * refresh the post from the database - used to reflect changes to comment counts, etc.
     */
    public void refreshPost() {
        if (mPost != null) {
            ReaderPost post = ReaderPostTable.getBlogPost(mPost.blogId, mPost.postId, true);
            setPost(post);
        }
    }

    private void showLikeStatus(final CommentHolder holder, int position) {
        ReaderComment comment = getItem(position);
        if (comment == null) {
            return;
        }

        if (mPost.canLikePost()) {
            holder.mCountLikes.setVisibility(View.VISIBLE);
            holder.mCountLikes.setSelected(comment.isLikedByCurrentUser);
            holder.mCountLikes.setCount(comment.numLikes);
            holder.mCountLikes.setContentDescription(ReaderUtils.getLongLikeLabelText(
                    holder.mCountLikes.getContext(), comment.numLikes, comment.isLikedByCurrentUser));

            if (!mAccountStore.hasAccessToken()) {
                holder.mCountLikes.setEnabled(false);
            } else {
                holder.mCountLikes.setEnabled(true);
                holder.mCountLikes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int clickedPosition = holder.getAdapterPosition();
                        toggleLike(v.getContext(), holder, clickedPosition);
                    }
                });
            }
        } else {
            holder.mCountLikes.setVisibility(View.GONE);
            holder.mCountLikes.setOnClickListener(null);
        }
    }

    private void toggleLike(Context context, CommentHolder holder, int position) {
        if (!NetworkUtils.checkConnection(context)) {
            return;
        }

        ReaderComment comment = getItem(position);
        if (comment == null) {
            ToastUtils.showToast(context, R.string.reader_toast_err_generic);
            return;
        }

        boolean isAskingToLike = !comment.isLikedByCurrentUser;
        ReaderAnim.animateLikeButton(holder.mCountLikes.getImageView(), isAskingToLike);

        if (!ReaderCommentActions.performLikeAction(comment, isAskingToLike, mAccountStore.getAccount().getUserId())) {
            ToastUtils.showToast(context, R.string.reader_toast_err_generic);
            return;
        }

        ReaderComment updatedComment = ReaderCommentTable.getComment(comment.blogId, comment.postId, comment.commentId);
        if (updatedComment != null) {
            mComments.set(position - NUM_HEADERS, updatedComment);
            showLikeStatus(holder, position);
        }

        AnalyticsUtils.trackWithReaderPostDetails(isAskingToLike
                ? AnalyticsTracker.Stat.READER_ARTICLE_COMMENT_LIKED
                : AnalyticsTracker.Stat.READER_ARTICLE_COMMENT_UNLIKED, mPost);
    }

    public boolean refreshComment(long commentId) {
        int position = positionOfCommentId(commentId);
        if (position == -1) {
            return false;
        }

        ReaderComment comment = getItem(position);
        if (comment == null) {
            return false;
        }

        ReaderComment updatedComment = ReaderCommentTable.getComment(comment.blogId, comment.postId, comment.commentId);
        if (updatedComment != null) {
            // copy the comment level over since loading from the DB always has it as 0
            updatedComment.level = comment.level;
            mComments.set(position - NUM_HEADERS, updatedComment);
            notifyItemChanged(position);
        }

        return true;
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

        int index = mComments.indexOfCommentId(commentId);
        if (index > -1) {
            mComments.remove(index);
            notifyDataSetChanged();
        }
    }

    /*
     * replace the comment that has the passed commentId with another comment
     */
    public void replaceComment(long commentId, ReaderComment comment) {
        int position = positionOfCommentId(commentId);
        if (position > -1 && mComments.replaceComment(commentId, comment)) {
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

    /*
     * returns the position of the passed comment in the adapter, taking the header into account
     */
    public int positionOfCommentId(long commentId) {
        int index = mComments.indexOfCommentId(commentId);
        return index == -1 ? -1 : index + NUM_HEADERS;
    }

    /*
     * sets the passed comment as the one to perform a "Like" on when the list comment list has completed loading
     */
    public void setAnimateLikeCommentId(long commentId) {
        mAnimateLikeCommentId = commentId;
    }

    /*
     * AsyncTask to load comments for this post
     */
    private boolean mIsTaskRunning = false;

    private class LoadCommentsTask extends AsyncTask<Void, Void, Boolean> {
        private ReaderCommentList mTmpComments;
        private boolean mTmpMoreCommentsExist;

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
            mTmpMoreCommentsExist = (numServerComments > numLocalComments);

            mTmpComments = ReaderCommentTable.getCommentsForPost(mPost);
            return !mComments.isSameList(mTmpComments);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mMoreCommentsExist = mTmpMoreCommentsExist;

            if (result) {
                // assign the comments with children sorted under their parents and indent levels applied
                mComments = new ReaderCommentLeveler(mTmpComments).createLevelList();
                notifyDataSetChanged();
            }
            if (mDataLoadedListener != null) {
                mDataLoadedListener.onDataLoaded(isEmpty());
            }
            mIsTaskRunning = false;
        }
    }

    /*
     * Set a post to adapter and update relevant information in the post header
     */
    public void setPost(ReaderPost post) {
        if (post != null) {
            mPost = post;
            notifyItemChanged(0); // notify header to update itself
        }
    }
}
