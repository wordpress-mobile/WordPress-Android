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
import org.wordpress.android.models.Comment;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    private ArrayList<Comment> mComments = new ArrayList<Comment>();
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
        }
    }

    protected int getSelectedCommentCount() {
        return mSelectedCommentPositions.size();
    }

    protected ArrayList<Comment> getSelectedComments() {
        ArrayList<Comment> comments = new ArrayList<Comment>();

        Iterator it = mSelectedCommentPositions.iterator();
        while (it.hasNext()) {
            int position = (Integer) it.next();
            comments.add(mComments.get(position));
        }

        return comments;
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
            txtName.setText(!TextUtils.isEmpty(comment.name) ? comment.name : mAnonymous);
            txtPostTitle.setText(comment.postTitle);
            txtComment.setText(StringUtils.unescapeHTML(comment.comment));

            // use the email address if the commenter didn't add a url
            String fEmailURL = (TextUtils.isEmpty(comment.authorURL) ? comment.emailURL : comment.authorURL);
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
                public void onClick(View arg0) {
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
            } else {
                imgAvatar.setImageResource(R.drawable.placeholder);
            }
        }
    }

    /*
     * load comments from local db
     */
    protected boolean loadComments() {
        String author, postID, commentContent, dateCreatedFormatted, status, authorEmail, authorURL, postTitle;
        int commentID;

        int blogId = WordPress.currentBlog.getLocalTableBlogId();
        List<Map<String, Object>> loadedComments = WordPress.wpDB.loadComments(blogId);

        if (loadedComments == null) {
            return false;
        }

        for (int i = 0; i < loadedComments.size(); i++) {
            Map<String, Object> contentHash = loadedComments.get(i);
            commentID = (Integer) contentHash.get("commentID");
            postID = contentHash.get("postID").toString();
            commentContent = contentHash.get("comment").toString();
            dateCreatedFormatted = contentHash.get("commentDateFormatted").toString();
            status = contentHash.get("status").toString();
            author = StringUtils.unescapeHTML(contentHash.get("author").toString());
            authorEmail = StringUtils.unescapeHTML(contentHash.get("email").toString());
            authorURL = StringUtils.unescapeHTML(contentHash.get("url").toString());
            postTitle = StringUtils.unescapeHTML(contentHash.get("postTitle").toString());

            Comment comment = new Comment(postID,
                    commentID,
                    i,
                    author,
                    dateCreatedFormatted,
                    commentContent,
                    status,
                    postTitle,
                    authorURL,
                    authorEmail,
                    GravatarUtils.gravatarUrlFromEmail(authorEmail, 140));
            mComments.add(comment);
        }

        notifyDataSetChanged();
        return true;
    }
}
