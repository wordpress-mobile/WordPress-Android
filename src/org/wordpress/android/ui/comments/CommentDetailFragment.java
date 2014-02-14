package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.Note.EnabledActions;
import org.wordpress.android.ui.comments.CommentActions.ChangedFrom;
import org.wordpress.android.ui.comments.CommentActions.OnCommentChangeListener;
import org.wordpress.android.ui.notifications.NotificationFragment;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.VolleyUtils;
import org.wordpress.android.util.WPLinkMovementMethod;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.EnumSet;
import java.util.Map;

/**
 * Created by nbradbury on 11/11/13.
 * comment detail displayed from both the notification list and the comment list
 * prior to this there were separate comment detail screens for each list
 */
public class CommentDetailFragment extends Fragment implements NotificationFragment {
    private int mLocalBlogId;
    private int mRemoteBlogId;

    private Comment mComment;
    private Note mNote;

    private TextView mTxtStatus;
    private TextView mTxtContent;
    private ImageView mImgSubmitReply;
    private EditText mEditReply;
    private ViewGroup mLayoutReply;
    private ViewGroup mLayoutButtons;

    private TextView mBtnModerateComment;
    private TextView mBtnSpamComment;
    private TextView mBtnEditComment;
    private TextView mBtnTrashComment;

    private boolean mIsSubmittingReply = false;
    private boolean mIsModeratingComment = false;
    private boolean mIsRequestingComment = false;
    private boolean mIsUsersBlog = false;

    private OnCommentChangeListener mOnCommentChangeListener;

    /*
     * these determine which actions (moderation, replying, marking as spam) to enable
     * for this comment - all actions are enabled when opened from the comment list, only
     * changed when opened from a notification
     */
    private EnumSet<EnabledActions> mEnabledActions = EnumSet.allOf(EnabledActions.class);

    /*
     * used when called from comment list
     */
    protected static CommentDetailFragment newInstance(int localBlogId, int commentId) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        fragment.setComment(localBlogId, commentId);
        return fragment;
    }

    /*
     * used when called from notification list for a comment notification
     */
    public static CommentDetailFragment newInstance(final Note note) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        fragment.setNote(note);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.comment_detail_fragment, container, false);

        mTxtStatus = (TextView) view.findViewById(R.id.text_status);
        mTxtContent = (TextView) view.findViewById(R.id.text_content);

        mLayoutButtons = (ViewGroup) view.findViewById(R.id.layout_buttons);
        mBtnModerateComment = (TextView) mLayoutButtons.findViewById(R.id.text_btn_moderate);
        mBtnSpamComment = (TextView) mLayoutButtons.findViewById(R.id.text_btn_spam);
        mBtnEditComment = (TextView) mLayoutButtons.findViewById(R.id.image_edit_comment);
        mBtnTrashComment = (TextView) mLayoutButtons.findViewById(R.id.image_trash_comment);

        setTextDrawable(mBtnSpamComment, R.drawable.ic_cab_spam);
        setTextDrawable(mBtnEditComment, R.drawable.ab_icon_edit);
        setTextDrawable(mBtnTrashComment, R.drawable.ic_cab_trash);

        mLayoutReply = (ViewGroup) view.findViewById(R.id.layout_comment_box);
        mEditReply = (EditText) mLayoutReply.findViewById(R.id.edit_comment);
        mImgSubmitReply = (ImageView) mLayoutReply.findViewById(R.id.image_post_comment);

        // hide moderation buttons until updateModerationButtons() is called
        mLayoutButtons.setVisibility(View.GONE);
        mBtnEditComment.setVisibility(View.GONE);

        // this is necessary in order for anchor tags in the comment text to be clickable
        mTxtContent.setLinksClickable(true);
        mTxtContent.setMovementMethod(WPLinkMovementMethod.getInstance());

        mEditReply.setHint(R.string.reader_hint_comment_on_comment);
        mEditReply.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND)
                    submitReply();
                return false;
            }
        });

        mImgSubmitReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReply();
            }
        });

        mBtnSpamComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mComment.getStatusEnum() == CommentStatus.SPAM) {
                    moderateComment(CommentStatus.APPROVED);
                } else {
                    moderateComment(CommentStatus.SPAM);
                }
            }
        });

        mBtnEditComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editComment();
            }
        });

        mBtnTrashComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeleteComment();
            }
        });

        return view;
    }

    protected void setComment(int localBlogId, int commentId) {
        setComment(localBlogId, CommentTable.getComment(localBlogId, commentId));
    }

    private void setComment(int localBlogId, final Comment comment) {
        mComment = comment;
        mLocalBlogId = localBlogId;

        // is this comment on one of the user's blogs? it won't be if this was displayed from a
        // notification about a reply to a comment this user posted on someone else's blog
        mIsUsersBlog = (comment != null && WordPress.wpDB.isLocalBlogIdInDatabase(mLocalBlogId));

        if (mIsUsersBlog)
            mRemoteBlogId = WordPress.wpDB.getRemoteBlogIdForLocalTableBlogId(mLocalBlogId);

        if (hasActivity())
            showComment();
    }

    @Override
    public Note getNote() {
        return mNote;
    }

    @Override
    public void setNote(Note note) {
        mNote = note;
        if (hasActivity() && mNote != null)
            showComment();
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnCommentChangeListener = (OnCommentChangeListener) activity;
        } catch (ClassCastException e) {
            mOnCommentChangeListener = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        showComment();
    }

    @Override
    public void onPause() {
        super.onPause();
        EditTextUtils.hideSoftInput(mEditReply);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.INTENT_COMMENT_EDITOR && resultCode == Activity.RESULT_OK) {
            reloadComment();
            // tell the host to reload the comment list
            if (mOnCommentChangeListener != null)
                mOnCommentChangeListener.onCommentChanged(ChangedFrom.COMMENT_DETAIL);
        }
    }

    private boolean hasActivity() {
        return (getActivity() != null && !isRemoving());
    }

    private boolean hasComment() {
        return (mComment != null);
    }

    private int getCommentId() {
        return (mComment != null ? mComment.commentID : 0);
    }

    private int getLocalBlogId() {
        return mLocalBlogId;
    }

    private int getRemoteBlogId() {
        return mRemoteBlogId;
    }

    /*
     * reload the current comment from the local database
     */
    protected void reloadComment() {
        if (!hasComment())
            return;
        Comment updatedComment = CommentTable.getComment(mLocalBlogId, getCommentId());
        setComment(mLocalBlogId, updatedComment);
    }

    private void clearComment() {
        setNote(null);
        setComment(0, null);
    }

    /*
     * called after comment trashed to remove this fragment
     */
    private void closeThisFragment() {
        if (!hasActivity())
            return;
        if (getActivity() instanceof CommentsActivity)
            ((CommentsActivity)getActivity()).popCommentDetail();
    }

    /*
     * open the comment for editing
     */
    private void editComment() {
        if (!hasActivity() || !hasComment())
            return;
        // IMPORTANT: don't use getActivity().startActivityForResult() or else onActivityResult()
        // won't be called in this fragment
        // https://code.google.com/p/android/issues/detail?id=15394#c45
        Intent intent = new Intent(getActivity(), EditCommentActivity.class);
        intent.putExtra(EditCommentActivity.ARG_LOCAL_BLOG_ID, getLocalBlogId());
        intent.putExtra(EditCommentActivity.ARG_COMMENT_ID, getCommentId());
        startActivityForResult(intent, Constants.INTENT_COMMENT_EDITOR);
    }

    /*
     * display the current comment
     */
    private void showComment() {
        if (!hasActivity())
            return;

        // these two views contain all the other views except the progress bar
        final ScrollView scrollView = (ScrollView) getView().findViewById(R.id.scroll_view);
        final View layoutBottom = getView().findViewById(R.id.layout_bottom);

        // hide container views when comment is null (will happen when opened from a notification)
        if (mComment == null) {
            scrollView.setVisibility(View.GONE);
            layoutBottom.setVisibility(View.GONE);

            // if a notification was passed, request its associated comment
            if (mNote != null && !mIsRequestingComment)
                showCommentForNote(mNote);

            return;
        }

        scrollView.setVisibility(View.VISIBLE);
        layoutBottom.setVisibility(View.VISIBLE);

        final WPNetworkImageView imgAvatar = (WPNetworkImageView) getView().findViewById(R.id.image_avatar);
        final TextView txtName = (TextView) getView().findViewById(R.id.text_name);
        final TextView txtDate = (TextView) getView().findViewById(R.id.text_date);

        txtName.setText(mComment.hasAuthorName() ? mComment.getAuthorName() : getString(R.string.anonymous));
        txtDate.setText(DateTimeUtils.javaDateToTimeSpan(mComment.getDatePublished()));

        int maxImageSz = getResources().getDimensionPixelSize(R.dimen.reader_comment_max_image_size);
        CommentUtils.displayHtmlComment(mTxtContent, mComment.getCommentText(), maxImageSz);

        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        if (mComment.hasProfileImageUrl()) {
            imgAvatar.setImageUrl(PhotonUtils.fixAvatar(mComment.getProfileImageUrl(), avatarSz), WPNetworkImageView.ImageType.AVATAR);
        } else if (mComment.hasAuthorEmail()) {
            String avatarUrl = GravatarUtils.gravatarUrlFromEmail(mComment.getAuthorEmail(), avatarSz);
            imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
        } else {
            imgAvatar.setImageUrl(null, WPNetworkImageView.ImageType.AVATAR);
        }

        updateStatusViews();

        // navigate to author's home page when avatar or name clicked
        if (mComment.hasAuthorUrl()) {
            View.OnClickListener authorListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(getActivity(), mComment.getAuthorUrl());
                }
            };
            imgAvatar.setOnClickListener(authorListener);
            txtName.setOnClickListener(authorListener);
            txtName.setTextColor(getResources().getColor(R.color.reader_hyperlink));
        } else {
            txtName.setTextColor(getResources().getColor(R.color.grey_medium_dark));
        }

        showPostTitle(getRemoteBlogId(), mComment.postID);

        // make sure reply box is showing
        if (mLayoutReply.getVisibility() != View.VISIBLE && canReply())
            AniUtils.flyIn(mLayoutReply);
    }

    /*
     * displays the passed post title for the current comment, updates stored title if one doesn't exist
     */
    private void setPostTitle(TextView txtTitle, final String postTitle) {
        if (txtTitle == null || !hasActivity())
            return;
        if (TextUtils.isEmpty(postTitle)) {
            txtTitle.setText(R.string.untitled);
            return;
        }

        // if comment doesn't have a post title, set it to the passed one and save to comment table
        if (hasComment() && !mComment.hasPostTitle()) {
            mComment.setPostTitle(postTitle);
            CommentTable.updateCommentPostTitle(getLocalBlogId(), getCommentId(), postTitle);
        }

        // display "on [Post Title]..."
        String html = getString(R.string.on)
                    + " <font color=" + HtmlUtils.colorResToHtmlColor(getActivity(), R.color.reader_hyperlink) + ">"
                    + postTitle.trim()
                    + "</font>";
        txtTitle.setText(Html.fromHtml(html));
    }

    /*
     * ensure the post associated with this comment is available to the reader and show its
     * title above the comment
     */
    private void showPostTitle(final int blogId, final int postId) {
        if (!hasActivity())
            return;

        final TextView txtPostTitle = (TextView) getView().findViewById(R.id.text_post_title);
        boolean postExists = ReaderPostTable.postExists(blogId, postId);

        final String title;
        final boolean hasTitle;
        if (mComment.hasPostTitle()) {
            // use comment's stored post title if available
            title = mComment.getPostTitle();
            hasTitle = true;
        } else if (postExists) {
            // use title from post if available
            title = ReaderPostTable.getPostTitle(blogId, postId);
            hasTitle = !TextUtils.isEmpty(title);
        } else {
            title = null;
            hasTitle = false;
        }
        if (hasTitle) {
            setPostTitle(txtPostTitle, title);
        } else {
            txtPostTitle.setText(postExists? R.string.untitled : R.string.loading);
        }

        // make sure this post is available to the reader, and once it's retrieved set the title
        // if it wasn't already set
        if (!postExists) {
            AppLog.d(T.COMMENTS, "comment detail > retrieving post");
            ReaderPostActions.requestPost(blogId, postId, new ReaderActions.ActionListener() {
                @Override
                public void onActionResult(boolean succeeded) {
                    if (!hasActivity())
                        return;
                    // update title if it wasn't set above
                    if (!hasTitle) {
                        String postTitle = ReaderPostTable.getPostTitle(blogId, postId);
                        if (!TextUtils.isEmpty(postTitle)) {
                            setPostTitle(txtPostTitle, postTitle);
                        } else {
                            txtPostTitle.setText(R.string.untitled);
                        }
                    }
                }
            });
        }

        // tapping this view should open the associated post in the reader
        txtPostTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPostInReader();
            }
        });
    }

    /*
     * open the post associated with this comment in the reader
     */
    private void viewPostInReader() {
        if (!hasActivity() || !hasComment())
            return;
        ReaderActivityLauncher.showReaderPostDetail(getActivity(), mRemoteBlogId, mComment.postID);
    }

    private void confirmDeleteComment() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dlg_confirm_trash_comments);
        builder.setTitle(R.string.trash);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.trash_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                moderateComment(CommentStatus.TRASH);
            }
        });
        builder.setNegativeButton(R.string.trash_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void dismissDialog(int id) {
        if (!hasActivity())
            return;
        try {
            getActivity().dismissDialog(id);
        } catch (IllegalArgumentException e) {
            // raised when dialog wasn't created
        }
    }

    /*
     * approve, unapprove, spam, or trash the current comment
     */
    private void moderateComment(final CommentStatus newStatus) {
        if (!hasActivity() || !hasComment() || mIsModeratingComment)
            return;
        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        // show dialog while moderating
        final int dlgId;
        switch (newStatus) {
            case APPROVED:
                dlgId = CommentDialogs.ID_COMMENT_DLG_APPROVING;
                break;
            case UNAPPROVED:
                dlgId = CommentDialogs.ID_COMMENT_DLG_UNAPPROVING;
                break;
            case SPAM:
                dlgId = CommentDialogs.ID_COMMENT_DLG_SPAMMING;
                break;
            case TRASH:
                dlgId = CommentDialogs.ID_COMMENT_DLG_TRASHING;
                break;
            default :
                return;
        }
        getActivity().showDialog(dlgId);

        // disable buttons during request
        mLayoutButtons.setEnabled(false);

        // animate the buttons out (updateStatusViews will re-display them when request completes)
        mLayoutButtons.clearAnimation();
        AniUtils.flyOut(mLayoutButtons);

        // hide status (updateStatusViews will un-hide it)
        if (mTxtStatus.getVisibility() == View.VISIBLE) {
            mTxtStatus.clearAnimation();
            AniUtils.startAnimation(mTxtStatus, R.anim.fade_out);
            mTxtStatus.setVisibility(View.INVISIBLE);
        }

        CommentActions.CommentActionListener actionListener = new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                mIsModeratingComment = false;
                if (succeeded && mOnCommentChangeListener != null)
                    mOnCommentChangeListener.onCommentChanged(ChangedFrom.COMMENT_DETAIL);
                if (hasActivity()) {
                    dismissDialog(dlgId);
                    mLayoutButtons.setEnabled(true);
                    if (succeeded) {
                        mComment.setStatus(CommentStatus.toString(newStatus));
                    } else {
                        ToastUtils.showToast(getActivity(), R.string.error_moderate_comment, ToastUtils.Duration.LONG);
                    }
                    if (newStatus == CommentStatus.TRASH) {
                        // clear the comment and remove this detail fragment if comment was trashed
                        clearComment();
                        closeThisFragment();
                    } else {
                        // reflect the new status - note this MUST come after mComment.setStatus
                        updateStatusViews();
                    }
                }
            }
        };
        mIsModeratingComment = true;
        CommentActions.moderateComment(mLocalBlogId, mComment, newStatus, actionListener);
    }

    /*
     * post comment box text as a reply to the current comment
     */
    private void submitReply() {
        if (!hasActivity() || mIsSubmittingReply)
            return;

        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        final String replyText = EditTextUtils.getText(mEditReply);
        if (TextUtils.isEmpty(replyText))
            return;

        // disable editor, hide soft keyboard, hide submit icon, and show progress spinner while submitting
        mEditReply.setEnabled(false);
        EditTextUtils.hideSoftInput(mEditReply);
        mImgSubmitReply.setVisibility(View.GONE);
        final ProgressBar progress = (ProgressBar) getView().findViewById(R.id.progress_submit_comment);
        progress.setVisibility(View.VISIBLE);

        CommentActions.CommentActionListener actionListener = new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                mIsSubmittingReply = false;
                if (succeeded && mOnCommentChangeListener != null)
                    mOnCommentChangeListener.onCommentChanged(ChangedFrom.COMMENT_DETAIL);
                if (hasActivity()) {
                    mEditReply.setEnabled(true);
                    mImgSubmitReply.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                    if (succeeded) {
                        ToastUtils.showToast(getActivity(), getString(R.string.note_reply_successful));
                        mEditReply.setText(null);
                    } else {
                        ToastUtils.showToast(getActivity(), R.string.reply_failed, ToastUtils.Duration.LONG);
                        // refocus editor on failure and show soft keyboard
                        mEditReply.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(mEditReply, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }
        };

        mIsSubmittingReply = true;

        if (mNote != null) {
            CommentActions.submitReplyToCommentNote(mNote, replyText, actionListener);
        } else {
            CommentActions.submitReplyToComment(mLocalBlogId, mComment, replyText, actionListener);
        }
    }

    /*
     * sets the drawable for moderation buttons
     */
    private void setTextDrawable(final TextView view, int resId) {
        view.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(resId), null, null);
    }

    /*
     * update the text, drawable & click listener for mBtnModerate based on
     * the current status of the comment, show mBtnSpam if the comment isn't
     * already marked as spam, and show the current status of the comment
     */
    private void updateStatusViews() {
        if (!hasActivity() || !hasComment())
            return;

        final int moderationDrawResId;  // drawable resource id for moderation button
        final int moderationTextResId;  // string resource id for moderation button
        final CommentStatus newStatus;  // status to apply when moderation button is tapped
        final int statusTextResId;      // string resource id for status text
        final int statusColor;          // color for status text

        switch (mComment.getStatusEnum()) {
            case APPROVED:
                moderationDrawResId = R.drawable.ic_cab_unapprove;
                moderationTextResId = R.string.mnu_comment_unapprove;
                newStatus = CommentStatus.UNAPPROVED;
                statusTextResId = R.string.comment_status_approved;
                statusColor = getActivity().getResources().getColor(R.color.comment_status_approved);
                break;
            case UNAPPROVED:
                moderationDrawResId = R.drawable.ic_cab_approve;
                moderationTextResId = R.string.mnu_comment_approve;
                newStatus = CommentStatus.APPROVED;
                statusTextResId = R.string.comment_status_unapproved;
                statusColor = getActivity().getResources().getColor(R.color.comment_status_unapproved);
                break;
            case SPAM:
                moderationDrawResId = R.drawable.ic_cab_approve;
                moderationTextResId = R.string.mnu_comment_approve;
                newStatus = CommentStatus.APPROVED;
                statusTextResId = R.string.comment_status_spam;
                statusColor = getActivity().getResources().getColor(R.color.comment_status_spam);
                break;
            case TRASH:
                // should never get here
                moderationDrawResId = R.drawable.ic_cab_approve;
                moderationTextResId = R.string.mnu_comment_approve;
                newStatus = CommentStatus.APPROVED;
                statusTextResId = R.string.comment_status_trash;
                statusColor = getActivity().getResources().getColor(R.color.comment_status_spam);
                break;
            default:
                return;
        }

        // comment status is only shown if this comment is from one of this user's blogs and the
        // comment hasn't been approved
        if (mIsUsersBlog && mComment.getStatusEnum() != CommentStatus.APPROVED) {
            mTxtStatus.setText(getString(statusTextResId).toUpperCase());
            mTxtStatus.setTextColor(statusColor);
            if (mTxtStatus.getVisibility() != View.VISIBLE) {
                mTxtStatus.clearAnimation();
                AniUtils.fadeIn(mTxtStatus);
            }
        } else {
            mTxtStatus.setVisibility(View.GONE);
        }

        if (canModerate()) {
            setTextDrawable(mBtnModerateComment, moderationDrawResId);
            mBtnModerateComment.setText(moderationTextResId);
            mBtnModerateComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    moderateComment(newStatus);
                }
            });
            mBtnModerateComment.setVisibility(View.VISIBLE);
        } else {
            mBtnModerateComment.setVisibility(View.GONE);
        }

        if (canMarkAsSpam()) {
            mBtnSpamComment.setVisibility(View.VISIBLE);
            if (mComment.getStatusEnum() == CommentStatus.SPAM) {
                mBtnSpamComment.setText(R.string.mnu_comment_unspam);
            } else {
                mBtnSpamComment.setText(R.string.mnu_comment_spam);
            }
        } else {
            mBtnSpamComment.setVisibility(View.GONE);
        }

        mBtnTrashComment.setVisibility(canTrash() ? View.VISIBLE : View.GONE);
        mBtnEditComment.setVisibility(canEdit() ? View.VISIBLE : View.GONE);

        // animate the buttons in if they're not visible
        if (mLayoutButtons.getVisibility() != View.VISIBLE && (canMarkAsSpam() || canModerate())) {
            mLayoutButtons.clearAnimation();
            AniUtils.flyIn(mLayoutButtons);
        }
    }

    /*
     * does user have permission to moderate/reply/spam this comment?
     */
    private boolean canModerate() {
        if (mEnabledActions == null)
            return false;
        return (mEnabledActions.contains(EnabledActions.ACTION_APPROVE)
             || mEnabledActions.contains(EnabledActions.ACTION_UNAPPROVE));
    }
    private boolean canMarkAsSpam() {
        return (mEnabledActions != null && mEnabledActions.contains(EnabledActions.ACTION_SPAM));
    }
    private boolean canReply() {
        return (mEnabledActions != null && mEnabledActions.contains(EnabledActions.ACTION_REPLY));
    }
    private boolean canTrash() {
        return canModerate();
    }
    private boolean canEdit() {
        return canModerate();
    }

    /*
     * display the comment associated with the passed notification
     */
    private void showCommentForNote(Note note) {
        /*
         * determine which actions to enable for this comment - if the comment is from this user's
         * blog then all actions will be enabled, but they won't be if it's a reply to a comment
         * this user made on someone else's blog
         */
        mEnabledActions = note.getEnabledActions();

        /*
         * in order to get the actual comment from a notification we need to extract the
         * blogId/postId/commentId from the notification, and this info is buried in the
         * actions array of the note's JSON. each action entry contains a "params"
         * array which contains these IDs, so find the first action then extract the IDs
         * from its params
         */
        Map<String,JSONObject> actions = note.getActions();
        if (actions.size() > 0) {
            String firstKey = actions.keySet().iterator().next();
            JSONObject jsonAction = actions.get(firstKey);
            JSONObject jsonParams = jsonAction.optJSONObject("params");
            if (jsonParams != null) {
                mRemoteBlogId = jsonParams.optInt("blog_id");
                int commentId = jsonParams.optInt("comment_id");

                // note that the local blog id won't be found if the comment is from someone else's blog
                int localBlogId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogId(mRemoteBlogId);

                // first try to get from local db, if that fails request it from the server
                Comment comment = CommentTable.getComment(localBlogId, commentId);
                if (comment != null) {
                    setComment(localBlogId, comment);
                } else {
                    requestComment(localBlogId, mRemoteBlogId, commentId);
                }
            }
        } else {
            if (hasActivity())
                ToastUtils.showToast(getActivity(), R.string.reader_toast_err_get_comment, ToastUtils.Duration.LONG);
        }
    }

    /*
     * request a comment - note that this uses the REST API rather than XMLRPC, which means the user must
     * either be wp.com or have Jetpack, but it's safe to do this since this method is only called when
     * displayed from a notification (and notifications require wp.com/Jetpack)
     */
    private void requestComment(final int localBlogId,
                                final int remoteBlogId,
                                final int commentId) {
        final ProgressBar progress = (hasActivity() ? (ProgressBar) getView().findViewById(R.id.progress_loading) : null);
        if (progress != null)
            progress.setVisibility(View.VISIBLE);

        RestRequest.Listener restListener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                mIsRequestingComment = false;
                if (hasActivity()) {
                    if (progress != null)
                        progress.setVisibility(View.GONE);
                    Comment comment = Comment.fromJSON(jsonObject);
                    if (comment != null) {
                        CommentTable.addComment(localBlogId, comment);
                        setComment(localBlogId, comment);
                    }
                }
            }
        };
        RestRequest.ErrorListener restErrListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                mIsRequestingComment = false;
                AppLog.e(T.COMMENTS, VolleyUtils.errStringFromVolleyError(volleyError), volleyError);
                if (hasActivity()) {
                    if (progress != null)
                        progress.setVisibility(View.GONE);
                    ToastUtils.showToast(getActivity(), R.string.reader_toast_err_get_comment, ToastUtils.Duration.LONG);
                }
            }
        };

        final String path = String.format("/sites/%s/comments/%s", remoteBlogId, commentId);
        mIsRequestingComment = true;
        WordPress.restClient.get(path, restListener, restErrListener);
    }
}
