package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
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
import org.wordpress.android.ui.reader.views.ReaderCommentsPostHeaderView;
import org.wordpress.android.ui.reader.views.ReaderIconCountView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ReaderCommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ReaderPost mPost;
    private boolean mMoreCommentsExist;

    private static final int MAX_INDENT_LEVEL = 2;
    private final int mIndentPerLevel;
    private final int mAvatarSz;
    private final int mContentWidth;

    private long mHighlightCommentId = 0;
    private boolean mShowProgressForHighlightedComment = false;
    private final boolean mIsPrivatePost;
    private final boolean mIsLoggedOutReader;
    private boolean mIsHeaderClickEnabled;

    private final int mColorAuthor;
    private final int mColorNotAuthor;
    private final int mColorHighlight;
    private final int mColorOldCommentBackground;
    private final int mColorOldCommentText;
    private final int mColorNewCommentText;

    private static final int VIEW_TYPE_HEADER = 1;
    private static final int VIEW_TYPE_COMMENT = 2;

    private static final long ID_HEADER = -1L;

    private static final int NUM_HEADERS = 1;

    public interface RequestReplyListener {
        void onRequestReply(long commentId);
    }

    private ReaderCommentList mComments = new ReaderCommentList();
    private RequestReplyListener mReplyListener;
    private ReaderInterfaces.DataLoadedListener mDataLoadedListener;
    private ReaderActions.DataRequestedListener mDataRequestedListener;

    private ReaderCommentList mOldComments;
    private volatile boolean mOldCommentsSorted;
    private ArrayList<Integer> mNewCommentsIndexes;  // indexes that are not seen by user
    private final boolean mTrackingNewComments;
    private Comparator<ReaderComment> mCommentsComparator = new Comparator<ReaderComment>() {
        @Override
        public int compare(ReaderComment x, ReaderComment y) {
            return (int)( x.commentId - y.commentId );
        }
    };

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

        private final ReaderIconCountView countLikes;

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

            countLikes = (ReaderIconCountView) view.findViewById(R.id.count_likes);

            txtText.setLinksClickable(true);
            txtText.setMovementMethod(ReaderLinkMovementMethod.getInstance(mIsPrivatePost));
        }
    }

    class PostHeaderHolder extends RecyclerView.ViewHolder {
        private final ReaderCommentsPostHeaderView mHeaderView;

        public PostHeaderHolder(View view) {
            super(view);
            mHeaderView = (ReaderCommentsPostHeaderView) view;
        }
    }

    /**
     * <p>
     * Note: oldComments are sorted for binary search
     * If you are using oldComments at other places pass a copy
     * </p>
     * pass oldComments = null if you don't want to track about new comments */
    public ReaderCommentAdapter(Context context,
                                ReaderPost post,
                                ReaderCommentList oldComments) {
        mPost = post;
        mIsPrivatePost = (post != null && post.isPrivate);
        mIsLoggedOutReader = ReaderUtils.isLoggedOutReader();

        mIndentPerLevel = context.getResources().getDimensionPixelSize(R.dimen.reader_comment_indent_per_level);
        mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_extra_small);

        // calculate the max width of comment content
        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        int cardMargin = context.getResources().getDimensionPixelSize(R.dimen.reader_card_margin);
        int contentPadding = context.getResources().getDimensionPixelSize(R.dimen.reader_card_content_padding);
        int mediumMargin = context.getResources().getDimensionPixelSize(R.dimen.margin_medium);
        mContentWidth = displayWidth - (cardMargin * 2) - (contentPadding * 2) - (mediumMargin * 2);

        mColorAuthor = ContextCompat.getColor(context, R.color.blue_medium);
        mColorNotAuthor = ContextCompat.getColor(context, R.color.grey_dark);
        mColorHighlight = ContextCompat.getColor(context, R.color.grey_lighten_30);
        mColorOldCommentBackground = ContextCompat.getColor(context, R.color.grey_light);
        mColorOldCommentText = ContextCompat.getColor(context, R.color.grey_darken_10);
        mColorNewCommentText = ContextCompat.getColor(context, R.color.grey_dark);

        setHasStableIds(true);

        // NOTE: old comments are sorted in background in LoadCommentsTask
        mOldComments = oldComments;
        mOldCommentsSorted = false;
        mTrackingNewComments = ( oldComments != null );
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
                headerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new PostHeaderHolder(headerView);
            default:
                View commentView = LayoutInflater.from(parent.getContext()).inflate(R.layout.reader_listitem_comment, parent, false);
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

        CommentHolder commentHolder = (CommentHolder) holder;
        commentHolder.txtAuthor.setText(comment.getAuthorName());

        java.util.Date dtPublished = DateTimeUtils.iso8601ToJavaDate(comment.getPublished());
        commentHolder.txtDate.setText(DateTimeUtils.javaDateToTimeSpan(dtPublished));

        if (comment.hasAuthorAvatar()) {
            String avatarUrl = GravatarUtils.fixGravatarUrl(comment.getAuthorAvatar(), mAvatarSz);
            commentHolder.imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
        } else {
            commentHolder.imgAvatar.showDefaultGravatarImage();
        }

        // tapping avatar or author name opens blog preview
        if (comment.hasAuthorBlogId()) {
            View.OnClickListener authorListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReaderActivityLauncher.showReaderBlogPreview(
                            view.getContext(),
                            comment.authorBlogId
                    );
                }
            };
            commentHolder.imgAvatar.setOnClickListener(authorListener);
            commentHolder.txtAuthor.setOnClickListener(authorListener);
        } else {
            commentHolder.imgAvatar.setOnClickListener(null);
            commentHolder.txtAuthor.setOnClickListener(null);
        }

        // author name uses different color for comments from the post's author
        if (comment.authorId == mPost.authorId) {
            commentHolder.txtAuthor.setTextColor(mColorAuthor);
        } else {
            commentHolder.txtAuthor.setTextColor(mColorNotAuthor);
        }

        // show indentation spacer for comments with parents and indent it based on comment level
        int indentWidth;
        if (comment.parentId != 0 && comment.level > 0) {
            indentWidth = Math.min(MAX_INDENT_LEVEL, comment.level) * mIndentPerLevel;
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) commentHolder.spacerIndent.getLayoutParams();
            params.width = indentWidth;
            commentHolder.spacerIndent.setVisibility(View.VISIBLE);
        } else {
            indentWidth = 0;
            commentHolder.spacerIndent.setVisibility(View.GONE);
        }

        int maxImageWidth = mContentWidth - indentWidth;
        CommentUtils.displayHtmlComment(commentHolder.txtText, comment.getText(), maxImageWidth);

        // different background for highlighted comment, with optional progress bar
        if (mHighlightCommentId != 0 && mHighlightCommentId == comment.commentId) {
            commentHolder.container.setBackgroundColor(mColorHighlight);
            commentHolder.txtText.setTextColor(mColorNewCommentText);
            commentHolder.progress.setVisibility(mShowProgressForHighlightedComment ? View.VISIBLE : View.GONE);
        }else if( mTrackingNewComments && Collections.binarySearch(mNewCommentsIndexes,position-NUM_HEADERS) < 0 ){
            commentHolder.container.setBackgroundColor(mColorOldCommentBackground);
            commentHolder.txtText.setTextColor(mColorOldCommentText);
            commentHolder.progress.setVisibility(View.GONE);
        }else {
            commentHolder.container.setBackgroundColor(Color.WHITE);
            commentHolder.txtText.setTextColor(mColorNewCommentText);
            commentHolder.progress.setVisibility(View.GONE);
        }

        if (mIsLoggedOutReader) {
            commentHolder.txtReply.setVisibility(View.GONE);
            commentHolder.imgReply.setVisibility(View.GONE);
        } else if (mReplyListener != null) {
            // tapping reply icon tells activity to show reply box
            View.OnClickListener replyClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mReplyListener.onRequestReply(comment.commentId);
                }
            };
            commentHolder.txtReply.setOnClickListener(replyClickListener);
            commentHolder.imgReply.setOnClickListener(replyClickListener);
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

    private void showLikeStatus(final CommentHolder holder, int position) {
        ReaderComment comment = getItem(position);
        if (comment == null) {
            return;
        }

        if (mPost.canLikePost()) {
            holder.countLikes.setVisibility(View.VISIBLE);
            holder.countLikes.setSelected(comment.isLikedByCurrentUser);
            holder.countLikes.setCount(comment.numLikes);

            if (mIsLoggedOutReader) {
                holder.countLikes.setEnabled(false);
            } else {
                holder.countLikes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int clickedPosition = holder.getAdapterPosition();
                        toggleLike(v.getContext(), holder, clickedPosition);
                    }
                });
            }
        } else {
            holder.countLikes.setVisibility(View.GONE);
            holder.countLikes.setOnClickListener(null);
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
        ReaderAnim.animateLikeButton(holder.countLikes.getImageView(), isAskingToLike);

        if (!ReaderCommentActions.performLikeAction(comment, isAskingToLike)) {
            ToastUtils.showToast(context, R.string.reader_toast_err_generic);
            return;
        }

        ReaderComment updatedComment = ReaderCommentTable.getComment(comment.blogId, comment.postId, comment.commentId);
        if (updatedComment != null) {
            mComments.set(position - NUM_HEADERS, updatedComment);
            showLikeStatus(holder, position);
        }
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
            if( mTrackingNewComments ){
                mNewCommentsIndexes.add( mComments.size() - 1 );
            }
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
     * AsyncTask to load comments for this post
     */
    private boolean mIsTaskRunning = false;

    private class LoadCommentsTask extends AsyncTask<Void, Void, ReaderCommentList> {
        private ReaderCommentList tmpComments;
        private boolean tmpMoreCommentsExist;

        private ArrayList<Integer> tmpUnseenCommentsIndexes;

        @Override
        protected void onPreExecute() {
            mIsTaskRunning = true;
        }

        @Override
        protected void onCancelled() {
            mIsTaskRunning = false;
        }

        @Override
        protected ReaderCommentList doInBackground(Void... params) {
            if (mPost == null) {
                return null;
            }

            // determine whether more comments can be downloaded by comparing the number of
            // comments the post says it has with the number of comments actually stored
            // locally for this post
            int numServerComments = ReaderPostTable.getNumCommentsForPost(mPost);
            int numLocalComments = ReaderCommentTable.getNumCommentsForPost(mPost);
            tmpMoreCommentsExist = (numServerComments > numLocalComments);

            tmpComments = ReaderCommentTable.getCommentsForPost(mPost);

            // arrange the comments with children sorted under their parents and indent levels applied
            tmpComments = ReaderCommentList.getLevelList(tmpComments);

            //perform sort and search of unseen in background because these operations takes time
            //you can access mOldCommentsSorted and mOldComments because we are not using them elsewhere
            if( mTrackingNewComments ){
                if( !mOldCommentsSorted ){
                    Collections.sort(mOldComments, mCommentsComparator);
                }
                tmpUnseenCommentsIndexes = findNextUnseenCommentIndexes(mOldComments, tmpComments);
            }

            return tmpComments;
        }

        @Override
        protected void onPostExecute(ReaderCommentList newList) {
            mMoreCommentsExist = tmpMoreCommentsExist;
            mNewCommentsIndexes = tmpUnseenCommentsIndexes;
            mOldCommentsSorted = true;

            if ( newList != null && !mComments.isSameList(tmpComments)) {
                mComments = newList;
                notifyDataSetChanged();
            }
            if (mDataLoadedListener != null) {
                mDataLoadedListener.onDataLoaded(isEmpty());
            }
            mIsTaskRunning = false;
        }
    }

    /*
    *  Set a post to adapter and update relevant information in the post header
    */
    public void setPost(ReaderPost post) {
        if (post != null) {
            mPost = post;
            notifyItemChanged(0); //notify header to update itself
        }

    }

    public boolean isMoreCommentExist(){
        return mMoreCommentsExist;
    }

    public boolean hasNewComments(){
        if( !mTrackingNewComments ){
            throw new UnsupportedOperationException("You are not using New Comments Functionality");
        }
        return mNewCommentsIndexes.size() > 0;
    }

    /**
     * @return new comment index after "afterIndex" else -1 if no new comment exist after "afterIndex"
     */
    private int getNextNewCommentIndex(int afterIndex){
        if( !mTrackingNewComments ){
            throw new UnsupportedOperationException("You are not using New Comments Functionality");
        }

        final ArrayList<Integer>  indexes = mNewCommentsIndexes;
        //no new comments
        if( indexes.size() == 0 ){
            return -1;
        }

        int first = 0;
        int last = indexes.size() - 1;

        if( afterIndex < indexes.get(first) ){
            return indexes.get(first);
        }

        if( afterIndex >= indexes.get(last) ){
            return -1;
        }

        int i = Collections.binarySearch(indexes, afterIndex);

        //if present in new - return the next new comment index
        if( i >= 0 ){
            return indexes.get( i + 1 );
        }

        //if not i = -(insertion-point)-1;
        int insertionPoint = -(i+1);
        return indexes.get(insertionPoint);
    }

    /**
     * @return new comment index before "beforeIndex" else -1 if no new comment exist before "beforeIndex"
     */
    private int getPrevNextCommentIndex(int beforeIndex){
        if( !mTrackingNewComments ){
            throw new UnsupportedOperationException("You are not using New Comments Functionality");
        }

        final ArrayList<Integer>  indexes = mNewCommentsIndexes;
        //now new comments
        if( indexes.size() == 0 ){
            return -1;
        }

        int first = 0;
        int last = indexes.size() - 1;

        if( beforeIndex <= indexes.get(first) ){
            return -1;
        }

        if( beforeIndex > indexes.get(last) ){
            return indexes.get(last);
        }

        int i = Collections.binarySearch(indexes, beforeIndex);

        //if present in new - return the previous new comment index
        if( i >= 0 ){
            return indexes.get( i - 1 );
        }

        //if not i = -(insertion-point)-1;
        int insertionPoint = -(i+1);
        return indexes.get( insertionPoint - 1 );
    }

    /**
     * @return new comment position after "afterItemPosition" else -1 if no new comment exist after "afterItemPosition"
     */
    public int getNextNewCommentPosition(int afterItemPosition){
        if( !mTrackingNewComments ){
            throw new UnsupportedOperationException("You are not using New Comments Functionality");
        }

        int index = getNextNewCommentIndex(afterItemPosition-NUM_HEADERS);
        if(index < 0){
            return index;
        }
        return index + NUM_HEADERS;
    }

    /**
     * @return new comment position before "beforeItemPosition" else -1 if no new comment exist before "beforeItemPosition"
     */
    public int getPrevNewCommentPosition(int beforeItemPosition){
        if( !mTrackingNewComments ){
            throw new UnsupportedOperationException("You are not using New Comments Functionality");
        }

        int index = getPrevNextCommentIndex(beforeItemPosition-NUM_HEADERS);
        if(index < 0){
            return index;
        }
        return index + NUM_HEADERS;
    }

    private ArrayList<Integer> findNextUnseenCommentIndexes(ReaderCommentList seenComments,
                                                            ReaderCommentList newComments ){
        if( !mTrackingNewComments ){
            throw new UnsupportedOperationException("You are not using New Comments Functionality");
        }

        ArrayList<Integer> result = new ArrayList<Integer>();

        for( int i=0 ; i < newComments.size() ; i++ ){
            if( Collections.binarySearch(seenComments, newComments.get(i), mCommentsComparator) < 0 ){
                result.add(i);
            }
        }

        return result;
    }
}
