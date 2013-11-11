package org.wordpress.android.ui.comments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Comment;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;

/**
 * Created by nbradbury on 11/11/13.
 * comment detail displayed from both the notification list and the comment list
 */
public class CommentDetailFragment extends Fragment {
    private int mBlogId;
    private String mPostId;
    private int mCommentId;

    private static final String KEY_BLOG_ID = "blog_id";
    private static final String KEY_POST_ID = "post_id";
    private static final String KEY_COMMENT_ID = "comment_id";

    protected static CommentDetailFragment newInstance(final int blogId,
                                                       final String postId,
                                                       final int commentId) {
        Bundle args = new Bundle();
        args.putInt(KEY_BLOG_ID, blogId);
        args.putString(KEY_POST_ID, StringUtils.notNullStr(postId));
        args.putInt(KEY_COMMENT_ID, commentId);

        CommentDetailFragment fragment = new CommentDetailFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args!=null) {
            mBlogId = args.getInt(KEY_BLOG_ID);
            mPostId = args.getString(KEY_POST_ID);
            mCommentId = args.getInt(KEY_COMMENT_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.comment_detail_fragment, container, false);
        return view;
    }

    private boolean hasActivity() {
        return (getActivity() != null);
    }

    private boolean mIsCommentTaskRunning = false;
    private class ShowCommentTask extends AsyncTask<Void, Void, Boolean> {
        private NetworkImageView imgAvatar;
        private Comment comment;
        private TextView txtName;
        private TextView txtDate;
        private TextView txtContent;

        @Override
        protected void onPreExecute() {
            mIsCommentTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsCommentTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            comment = WordPress.wpDB.getSingleComment(mBlogId, mPostId, mCommentId);
            if (comment==null)
                return false;

            imgAvatar = (NetworkImageView) getActivity().findViewById(R.id.image_avatar);
            txtName = (TextView) getActivity().findViewById(R.id.text_name);
            txtDate = (TextView) getActivity().findViewById(R.id.text_date);
            txtContent = (TextView) getActivity().findViewById(R.id.text_content);

            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                txtName.setText(comment.name);
                txtDate.setText(comment.dateCreatedFormatted);
                txtContent.setText(comment.comment);

                imgAvatar.setDefaultImageResId(R.drawable.reader_avatar_default);
                imgAvatar.setErrorImageResId(R.drawable.reader_avatar_error);
                if (!TextUtils.isEmpty(comment.authorEmail)) {
                    String profileImageUrl = GravatarUtils.gravatarUrlFromEmail(comment.authorEmail);
                    imgAvatar.setImageUrl(profileImageUrl, WordPress.imageLoader);
                }
            }
            mIsCommentTaskRunning = false;
        }
    }
}
