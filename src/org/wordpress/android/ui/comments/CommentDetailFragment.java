package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Comment;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.ReaderAniUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

/**
 * Created by nbradbury on 11/11/13.
 * comment detail displayed from both the notification list and the comment list
 */
public class CommentDetailFragment extends Fragment {
    private Comment mComment;

    private boolean mIsReplyBoxShowing = false;

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

    /*
     * enable replying to this comment
     */
    private void showReplyBox() {
        if (mIsReplyBoxShowing || !hasActivity())
            return;

        final ViewGroup layoutComment = (ViewGroup) getActivity().findViewById(R.id.layout_comment_box);
        final EditText editReply = (EditText) layoutComment.findViewById(R.id.edit_comment);
        final ImageView imgSubmit = (ImageView) getActivity().findViewById(R.id.image_post_comment);

        editReply.setHint(R.string.reply_to_comment);
        editReply.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId== EditorInfo.IME_ACTION_DONE || actionId==EditorInfo.IME_ACTION_SEND)
                    submitReply();
                return false;
            }
        });

        imgSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReply();
            }
        });

        ReaderAniUtils.flyIn(layoutComment);
        mIsReplyBoxShowing = true;
    }

    private boolean hasActivity() {
        return (getActivity() != null);
    }

    private boolean hasComment() {
        return (mComment != null);
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

        // approve button only appears when comment hasn't already been approved, reply box only
        // appears from approved comments
        Comment.CommentStatus status = Comment.CommentStatus.fromString(mComment.status);
        if (status == Comment.CommentStatus.APPROVED) {
            txtBtnApprove.setVisibility(View.GONE);
            showReplyBox();
        } else {
            txtBtnApprove.setVisibility(View.VISIBLE);
            txtBtnApprove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    approveComment();
                }
            });
        }
    }

    /*
     * approve the current comment
     */
    private void approveComment() {
        if (!hasActivity() || !hasComment())
            return;

        final ProgressBar progress = (ProgressBar) getActivity().findViewById(R.id.progress);
        final TextView txtBtnApprove = (TextView) getActivity().findViewById(R.id.text_approve);

        progress.setVisibility(View.VISIBLE);
        txtBtnApprove.setText(R.string.moderating_comment);

        CommentActions.CommentActionListener actionListener = new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                progress.setVisibility(View.GONE);
                txtBtnApprove.setText(R.string.approve);
                if (succeeded) {
                    txtBtnApprove.setVisibility(View.GONE);
                    showReplyBox();
                } else {
                    ToastUtils.showToast(getActivity(), R.string.error_moderate_comment, ToastUtils.Duration.LONG);
                }
            }
        };
        CommentActions.setCommentStatus(WordPress.currentBlog, mComment, Comment.CommentStatus.APPROVED, actionListener);
    }

    /*
     * post the text typed into the comment box as a reply to the current comment
     */
    private boolean mIsSubmittingReply = false;
    private void submitReply() {
        if (!hasActivity() || mIsSubmittingReply)
            return;

        final EditText editComment = (EditText) getActivity().findViewById(R.id.edit_comment);
        final ProgressBar progress = (ProgressBar) getActivity().findViewById(R.id.progress);
        final ImageView imgSubmit = (ImageView) getActivity().findViewById(R.id.image_post_comment);

        final String replyText = EditTextUtils.getText(editComment);
        if (TextUtils.isEmpty(replyText))
            return;

        // disable editor, hide soft keyboard, hide submit icon, and show progress spinner while submitting
        editComment.setEnabled(false);
        EditTextUtils.hideSoftInput(editComment);
        imgSubmit.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);

        CommentActions.CommentActionListener actionListener = new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                editComment.setEnabled(true);
                imgSubmit.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
                mIsSubmittingReply = false;

                if (succeeded) {
                    ToastUtils.showToast(getActivity(), R.string.note_reply_successful);
                    editComment.setText(null);
                } else {
                    ToastUtils.showToast(getActivity(), R.string.reply_failed, ToastUtils.Duration.LONG);
                    // refocus editor on failure and show soft keyboard
                    editComment.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editComment, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        };

        mIsSubmittingReply = true;
        CommentActions.submitReply(WordPress.currentBlog,
                                   mComment,
                                   replyText,
                                   actionListener);
    }
}
