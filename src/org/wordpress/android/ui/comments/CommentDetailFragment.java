package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Comment;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.ReaderAniUtils;
import org.wordpress.android.util.StringUtils;

/**
 * Created by nbradbury on 11/11/13.
 * comment detail displayed from both the notification list and the comment list
 */
public class CommentDetailFragment extends Fragment {
    private Comment mComment;

    private boolean mIsCommentBoxShowing = false;

    private static final String KEY_INTERNAL_BLOG_ID = "internal_blog_id";
    private static final String KEY_POST_ID = "post_id";
    private static final String KEY_COMMENT_ID = "comment_id";

    private OnCommentModifiedListener mModifiedListener;
    protected interface OnCommentModifiedListener {
        public void onCommentModified();
    }

    protected static CommentDetailFragment newInstance(final int internalBlogId,
                                                       final String postId,
                                                       final int commentId) {
        Bundle args = new Bundle();
        args.putInt(KEY_INTERNAL_BLOG_ID, internalBlogId);
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
            int internalBlogId = args.getInt(KEY_INTERNAL_BLOG_ID);
            String postId = args.getString(KEY_POST_ID);
            int commentId = args.getInt(KEY_COMMENT_ID);
            mComment = WordPress.wpDB.getSingleComment(internalBlogId, postId, commentId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.comment_detail_fragment, container, false);
        return view;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        /*try {
            // container activity must implement this callback
            mModifiedListener = (OnCommentModifiedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement CommentModifiedListener");
        }*/
    }

    @Override
    public void onStart() {
        super.onStart();
        showComment();
    }

    private void showCommentBox() {
        if (mIsCommentBoxShowing)
            return;
        if (!hasActivity())
            return;
        ViewGroup layoutComment = (ViewGroup) getActivity().findViewById(R.id.layout_comment_box);
        ReaderAniUtils.flyIn(layoutComment);
        mIsCommentBoxShowing = true;
    }

    private void hideCommentBox() {
        if (!mIsCommentBoxShowing)
            return;
        if (!hasActivity())
            return;
        ViewGroup layoutComment = (ViewGroup) getActivity().findViewById(R.id.layout_comment_box);
        ReaderAniUtils.flyOut(layoutComment);
        mIsCommentBoxShowing = false;
    }

    private boolean hasActivity() {
        return (getActivity() != null);
    }

    /*
     * called from container when user selects a comment from the list
     */
    protected void loadComment(Comment comment) {
        mComment = comment;
        showComment();
    }

    /*
     * display the current comment
     */
    private void showComment() {
        if (mComment == null || !hasActivity())
            return;

        final NetworkImageView imgAvatar = (NetworkImageView) getActivity().findViewById(R.id.image_avatar);
        final TextView txtName = (TextView) getActivity().findViewById(R.id.text_name);
        final TextView txtDate = (TextView) getActivity().findViewById(R.id.text_date);
        final TextView txtContent = (TextView) getActivity().findViewById(R.id.text_content);
        final TextView txtBtnApprove = (TextView) getActivity().findViewById(R.id.text_approve);

        txtName.setText(TextUtils.isEmpty(mComment.name) ? getString(R.string.anonymous) : mComment.name);
        txtDate.setText(mComment.dateCreatedFormatted);
        txtContent.setText(mComment.comment);

        imgAvatar.setDefaultImageResId(R.drawable.placeholder);
        if (!TextUtils.isEmpty(mComment.authorEmail)) {
            String profileImageUrl = GravatarUtils.gravatarUrlFromEmail(mComment.authorEmail);
            imgAvatar.setImageUrl(profileImageUrl, WordPress.imageLoader);
        }

        if (mComment.getStatusEnum() == Comment.CommentStatus.APPROVED) {
            txtBtnApprove.setVisibility(View.GONE);
            showCommentBox();
        } else {
            txtBtnApprove.setVisibility(View.VISIBLE);
            txtBtnApprove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO - approve comment via API
                    ReaderAniUtils.fadeOut(txtBtnApprove, new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) { }
                        @Override
                        public void onAnimationRepeat(Animation animation) { }
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            showCommentBox();
                        }

                    });
                }
            });
        }
    }
}
