package org.wordpress.android.ui.comments;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by nbradbury on 1/29/14.
 */
public class CommentAdapter extends BaseAdapter {
    protected static interface OnLoadMoreListener {
        public void onLoadMore();
    }

    protected static interface OnSelectionChangeListener {
        public void onSelectionChanged();
    }

    private LayoutInflater mInflater;
    private OnLoadMoreListener mOnLoadMoreListener;
    private OnSelectionChangeListener mOnSelectionChangeListener;
    private CommentList mComments = new CommentList();
    private HashSet<Integer> mSelectedCommentPositions = new HashSet<Integer>();

    private int mStatusColorSpam;
    private int mStatusColorUnapproved;

    private String mStatusTextSpam;
    private String mStatusTextUnapproved;
    private String mAnonymous;

    protected CommentAdapter(Context context,
                             OnLoadMoreListener onLoadMoreListener,
                             OnSelectionChangeListener onChangeListener) {
        mInflater = LayoutInflater.from(context);

        mOnLoadMoreListener = onLoadMoreListener;
        mOnSelectionChangeListener = onChangeListener;

        mStatusColorSpam = Color.parseColor("#FF0000");
        mStatusColorUnapproved = Color.parseColor("#D54E21");
        mStatusTextSpam = context.getResources().getString(R.string.spam);
        mStatusTextUnapproved = context.getResources().getString(R.string.unapproved);
        mAnonymous = context.getString(R.string.anonymous);
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

    protected void clear() {
        if (mComments.size() > 0) {
            mComments.clear();
            notifyDataSetChanged();
        }
    }

    protected void clearSelectedComments() {
        if (mSelectedCommentPositions.size() > 0) {
            mSelectedCommentPositions.clear();
            notifyDataSetChanged();
            if (mOnSelectionChangeListener != null)
                mOnSelectionChangeListener.onSelectionChanged();
        }
    }

    protected int getSelectedCommentCount() {
        return mSelectedCommentPositions.size();
    }

    protected CommentList getSelectedComments() {
        CommentList comments = new CommentList();

        Iterator it = mSelectedCommentPositions.iterator();
        while (it.hasNext()) {
            int position = (Integer) it.next();
            comments.add(mComments.get(position));
        }

        return comments;
    }

    protected void replaceComments(final CommentList comments) {
        mComments.replaceComments(comments);
        notifyDataSetChanged();
    }

    protected void deleteComments(final CommentList comments) {
        mComments.deleteComments(comments);
        notifyDataSetChanged();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final Comment comment = mComments.get(position);
        final CommentEntryWrapper wrapper;

        if (convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.comment_row, null);
            wrapper = new CommentEntryWrapper(convertView);
            convertView.setTag(wrapper);
        } else {
            wrapper = (CommentEntryWrapper) convertView.getTag();
        }

        wrapper.populateFrom(comment, position);

        // request to load more comments when we near the end
        if (mOnLoadMoreListener != null && position >= getCount()-1)
            mOnLoadMoreListener.onLoadMore();

        return convertView;
    }

    class CommentEntryWrapper {
        private TextView txtName;
        private TextView txtEmailURL;
        private TextView txtComment;
        private TextView txtStatus;
        private TextView txtPostTitle;
        private NetworkImageView imgAvatar;
        private View row;
        private CheckBox bulkCheck;

        CommentEntryWrapper(View row) {
            this.row = row;

            txtName = (TextView) row.findViewById(R.id.name);
            txtEmailURL = (TextView) row.findViewById(R.id.email_url);
            txtComment = (TextView) row.findViewById(R.id.comment);
            txtStatus = (TextView) row.findViewById(R.id.status);
            txtPostTitle = (TextView) row.findViewById(R.id.postTitle);
            bulkCheck = (CheckBox) row.findViewById(R.id.bulkCheck);
            imgAvatar = (NetworkImageView) row.findViewById(R.id.avatar);
        }

        void populateFrom(Comment comment, final int position) {
            txtName.setText(comment.hasAuthorName() ? comment.getAuthorName() : mAnonymous);
            txtPostTitle.setText(comment.getPostTitle());
            txtComment.setText(StringUtils.unescapeHTML(comment.getCommentText()));

            // use the email address if the commenter didn't add a url
            String fEmailURL = (comment.hasAuthorUrl() ? comment.getAuthorUrl() : comment.getAuthorEmail());
            txtEmailURL.setVisibility(TextUtils.isEmpty(fEmailURL) ? View.GONE : View.VISIBLE);
            txtEmailURL.setText(fEmailURL);

            row.setId(Integer.valueOf(comment.commentID));

            // status is only shown for comments that haven't been approved
            switch (comment.getStatusEnum()) {
                case SPAM :
                    txtStatus.setText(mStatusTextSpam);
                    txtStatus.setTextColor(mStatusColorSpam);
                    txtStatus.setVisibility(View.VISIBLE);
                    break;
                case UNAPPROVED:
                    txtStatus.setText(mStatusTextUnapproved);
                    txtStatus.setTextColor(mStatusColorUnapproved);
                    txtStatus.setVisibility(View.VISIBLE);
                    break;
                default :
                    txtStatus.setVisibility(View.GONE);
                    break;
            }

            bulkCheck.setChecked(mSelectedCommentPositions.contains(position));
            bulkCheck.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (bulkCheck.isChecked()) {
                        mSelectedCommentPositions.add(position);
                    } else {
                        mSelectedCommentPositions.remove(position);
                    }
                    if (mOnSelectionChangeListener != null)
                        mOnSelectionChangeListener.onSelectionChanged();
                }
            });

            imgAvatar.setDefaultImageResId(R.drawable.placeholder);
            if (comment.hasProfileImageUrl()) {
                imgAvatar.setImageUrl(GravatarUtils.fixGravatarUrl(comment.getProfileImageUrl()), WordPress.imageLoader);
            } else if (comment.hasAuthorEmail()) {
                imgAvatar.setImageUrl(GravatarUtils.gravatarUrlFromEmail(comment.getAuthorEmail()), WordPress.imageLoader);
            } else {
                imgAvatar.setImageResource(R.drawable.placeholder);
            }
        }
    }

    /*
     * load comments from local db
     */
    protected boolean loadComments() {
        int localBlogId = WordPress.currentBlog.getLocalTableBlogId();
        mComments = CommentTable.loadComments(localBlogId);
        notifyDataSetChanged();

        return true;
    }
}
