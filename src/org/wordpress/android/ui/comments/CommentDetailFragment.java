package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
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

    private OnCommentChangeListener mChangeListener;
    protected interface OnCommentChangeListener {
        public void onCommentModified(Comment comment);
    }

    protected static CommentDetailFragment newInstance(final Comment comment) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        fragment.setComment(comment);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.comment_detail_fragment, container, false);
        return view;
    }

    protected void setComment(final Comment comment) {
        mComment = comment;
        if (hasActivity())
            showComment();
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // container activity must implement this callback
            mChangeListener = (OnCommentChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement CommentModifiedListener");
        }
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
     * display the current comment
     */
    private void showComment() {
        if (!hasActivity())
            return;

        final NetworkImageView imgAvatar = (NetworkImageView) getActivity().findViewById(R.id.image_avatar);
        final TextView txtName = (TextView) getActivity().findViewById(R.id.text_name);
        final TextView txtDate = (TextView) getActivity().findViewById(R.id.text_date);
        final TextView txtContent = (TextView) getActivity().findViewById(R.id.text_content);
        final TextView txtBtnApprove = (TextView) getActivity().findViewById(R.id.text_approve);

        // clear all views when comment is null
        if (mComment == null) {
            imgAvatar.setImageDrawable(null);
            txtName.setText(null);
            txtDate.setText(null);
            txtContent.setText(null);
            txtBtnApprove.setVisibility(View.GONE);
            return;
        }

        txtName.setText(TextUtils.isEmpty(mComment.name) ? getString(R.string.anonymous) : StringUtils.unescapeHTML(mComment.name));
        txtDate.setText(mComment.dateCreatedFormatted);
        txtContent.setText(Html.fromHtml(mComment.comment));

        // TODO: anonymous comments will have a blank avatar because the version of Volley
        // we're using as of 11/11/13 doesn't show the default image - latest version of
        // Volley corrects this
        int avatarSz = getResources().getDimensionPixelSize(R.dimen.reader_avatar_sz_large);
        imgAvatar.setDefaultImageResId(R.drawable.placeholder);
        String profileImageUrl = GravatarUtils.gravatarUrlFromEmail(mComment.authorEmail, avatarSz);
        imgAvatar.setImageUrl(profileImageUrl, WordPress.imageLoader);

        // approve button only appears when comment hasn't already been approved, reply box only
        // appears from approved comments
        Comment.CommentStatus status = Comment.CommentStatus.fromString(mComment.status);
        if (status == Comment.CommentStatus.APPROVED) {
            txtBtnApprove.setVisibility(View.GONE);
            showReplyBox();
            mComment.status = Comment.CommentStatus.APPROVED.toString();
            mChangeListener.onCommentModified(mComment);
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

        txtBtnApprove.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.VISIBLE);
        txtBtnApprove.setText(R.string.moderating_comment);

        CommentActions.CommentActionListener actionListener = new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                progress.setVisibility(View.GONE);
                if (succeeded) {
                    txtBtnApprove.setVisibility(View.GONE);
                    showReplyBox();
                    mComment.status = Comment.CommentStatus.APPROVED.toString();
                    mChangeListener.onCommentModified(mComment);
                } else {
                    txtBtnApprove.setVisibility(View.VISIBLE);
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
