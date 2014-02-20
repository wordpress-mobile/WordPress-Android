package org.wordpress.android.ui.reader.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.SysUtils;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.widgets.WPNetworkImageView;

/**
 * Created by nbradbury on 6/27/13.
 */
public class ReaderCommentAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private ReaderPost mPost;
    private boolean mMoreCommentsExist;

    private static final int MAX_INDENT_LEVEL = 2;
    private int mIndentPerLevel;
    private int mAvatarSz;
    private int mMaxImageSz;

    private long mHighlightCommentId = 0;
    private boolean mShowProgressForHighlightedComment = false;

    private int mBgColorNormal;
    private int mBgColorHighlight;
    private int mLinkColor;
    private int mNoLinkColor;

    public interface RequestReplyListener {
        void onRequestReply(long commentId);
    }

    private ReaderCommentList mComments = new ReaderCommentList();
    private RequestReplyListener mReplyListener;
    private ReaderActions.DataLoadedListener mDataLoadedListener;
    private ReaderActions.DataRequestedListener mDataRequestedListener;

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
    }

    @SuppressLint("NewApi")
    public void refreshComments() {
        if (mIsTaskRunning)
            AppLog.w(T.READER, "Load comments task already running");

        if (SysUtils.canUseExecuteOnExecutor()) {
            new LoadCommentsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new LoadCommentsTask().execute();
        }
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
        CommentViewHolder holder;
        if (convertView==null) {
            convertView = mInflater.inflate(R.layout.reader_listitem_comment, parent, false);
            holder = new CommentViewHolder();
            holder.txtAuthor = (TextView) convertView.findViewById(R.id.text_comment_author);
            holder.txtText = (TextView) convertView.findViewById(R.id.text_comment_text);
            holder.txtDate = (TextView) convertView.findViewById(R.id.text_comment_date);
            holder.txtReply = (TextView) convertView.findViewById(R.id.text_reply);
            holder.imgAvatar = (WPNetworkImageView) convertView.findViewById(R.id.image_avatar);
            holder.spacerIndent = convertView.findViewById(R.id.spacer_indent);
            holder.spacerTop = convertView.findViewById(R.id.spacer_top);
            holder.progress = (ProgressBar) convertView.findViewById(R.id.progress);
            convertView.setTag(holder);

            // this is necessary in order for anchor tags in the comment text to be clickable
            holder.txtText.setLinksClickable(true);
            holder.txtText.setMovementMethod(WPLinkMovementMethod.getInstance());
        } else {
            holder = (CommentViewHolder) convertView.getTag();
        }

        holder.txtAuthor.setText(comment.getAuthorName());
        holder.imgAvatar.setImageUrl(PhotonUtils.fixAvatar(comment.getAuthorAvatar(), mAvatarSz), WPNetworkImageView.ImageType.AVATAR);
        CommentUtils.displayHtmlComment(holder.txtText, comment.getText(), mMaxImageSz);

        java.util.Date dtPublished = DateTimeUtils.iso8601ToJavaDate(comment.getPublished());
        holder.txtDate.setText(DateTimeUtils.javaDateToTimeSpan(dtPublished));

        // tapping avatar or author name opens blog in browser
        if (comment.hasAuthorUrl()) {
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ReaderActivityLauncher.openUrl(view.getContext(), comment.getAuthorUrl());
                }
            };
            holder.imgAvatar.setOnClickListener(listener);
            holder.txtAuthor.setOnClickListener(listener);
            holder.txtAuthor.setTextColor(mLinkColor);
        } else {
            holder.txtAuthor.setTextColor(mNoLinkColor);
        }

        // show top spacer for first comment (adds extra space between comments and post)
        holder.spacerTop.setVisibility(position == 0 ? View.VISIBLE : View.GONE);

        // show indentation spacer and indent it based on comment level
        holder.spacerIndent.setVisibility(comment.parentId==0 ? View.GONE : View.VISIBLE);
        if (comment.level > 0) {
            int indent = Math.min(MAX_INDENT_LEVEL, comment.level) * mIndentPerLevel;
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.spacerIndent.getLayoutParams();
            if (params.width!=indent)
                params.width = indent;
            holder.spacerIndent.setVisibility(View.VISIBLE);
        }

        // different background for highlighted comment, with optional progress bar
        if (mHighlightCommentId==comment.commentId) {
            convertView.setBackgroundColor(mBgColorHighlight);
            holder.progress.setVisibility(mShowProgressForHighlightedComment ? View.VISIBLE : View.GONE);
        } else {
            convertView.setBackgroundColor(mBgColorNormal);
            holder.progress.setVisibility(View.GONE);
        }

        // tapping reply icon tells activity to show reply box
        if (mReplyListener != null) {
            holder.txtReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mReplyListener.onRequestReply(comment.commentId);
                }
            });
        }

        // if we're nearing the end of the comments and we know more exist on the server,
        // fire request to load more
        if (mMoreCommentsExist && mDataRequestedListener!=null && (position >= getCount()-1))
            mDataRequestedListener.onRequestData(ReaderActions.RequestDataAction.LOAD_NEWER);

        return convertView;
    }



    private static class CommentViewHolder {
        private TextView txtAuthor;
        private TextView txtText;
        private TextView txtDate;
        private TextView txtReply;
        private WPNetworkImageView imgAvatar;
        private View spacerIndent;
        private View spacerTop;
        private ProgressBar progress;
    }

    /*
     * called from post detail activity when user submits a comment
     */
    public void addComment(ReaderComment comment) {
        if (comment==null)
            return;

        // if the comment doesn't have a parent we can just add it to the list of existing
        // comments - but if it does have a parent, we need to reload the list so that it
        // appears under its parent and is correctly indented
        if (comment.parentId==0) {
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
        if (commentId==mHighlightCommentId)
            setHighlightCommentId(0, false);

        int position = indexOfCommentId(commentId);
        if (position == -1)
            return;

        mComments.remove(position);
        notifyDataSetChanged();
    }

    /*
     * replace the comment that has the passed commentId with another comment - used
     * after a comment is submitted to replace the "fake" comment with the real one
     */
    public boolean replaceComment(long commentId, ReaderComment comment) {
        return mComments.replaceComment(commentId, comment);
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
        private boolean moreCommentsExist;

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
            if (mPost==null)
                return false;

            // determine whether more comments can be downloaded by comparing the number of
            // comments the post says it has with the number of comments actually stored
            // locally for this post
            int numServerComments = ReaderPostTable.getNumCommentsForPost(mPost);
            int numLocalComments = ReaderCommentTable.getNumCommentsForPost(mPost);
            moreCommentsExist = (numServerComments > numLocalComments);

            tmpComments = ReaderCommentTable.getCommentsForPost(mPost);
            if (mComments.isSameList(tmpComments))
                return false;

            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            mMoreCommentsExist = moreCommentsExist;
            if (result) {
                // assign the comments with children sorted under their parents and indent levels applied
                mComments = ReaderCommentList.getLevelList(tmpComments);
                notifyDataSetChanged();
            }
            if (mDataLoadedListener!=null)
                mDataLoadedListener.onDataLoaded(isEmpty());
            mIsTaskRunning = false;
        }
    }
}
