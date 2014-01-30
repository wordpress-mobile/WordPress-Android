package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.Note.EnabledActions;
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
import org.wordpress.android.util.MessageBarUtils;
import org.wordpress.android.util.MessageBarUtils.MessageBarType;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.VolleyUtils;
import org.wordpress.android.util.WPLinkMovementMethod;

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

    private Button mBtnModerate;
    private Button mBtnSpam;
    private TextView mTxtStatus;
    private EditText mEditReply;
    private ImageView mImgSubmitReply;
    private ViewGroup mLayoutReply;
    private ViewGroup mLayoutButtons;

    private boolean mIsSubmittingReply = false;
    private boolean mIsModeratingComment = false;
    private boolean mIsRequestingComment = false;
    private boolean mIsUsersBlog = false;

    /*
     * these determine which actions (moderation, replying, marking as spam) to enable
     * for this comment - all actions are enabled when opened from the comment list, only
     * changed when opened from a notification
     */
    private EnumSet<EnabledActions> mEnabledActions = EnumSet.allOf(EnabledActions.class);

    private CommentActions.OnCommentChangeListener mChangeListener;

    /*
     * used when called from comment list
     */
    protected static CommentDetailFragment newInstance(int localBlogId, final Comment comment) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        fragment.setComment(localBlogId, comment);
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

        mLayoutReply = (ViewGroup) view.findViewById(R.id.layout_comment_box);
        mEditReply = (EditText) mLayoutReply.findViewById(R.id.edit_comment);
        mImgSubmitReply = (ImageView) mLayoutReply.findViewById(R.id.image_post_comment);

        mLayoutButtons = (ViewGroup) view.findViewById(R.id.layout_buttons);
        mBtnModerate = (Button) mLayoutButtons.findViewById(R.id.text_btn_moderate);
        mBtnSpam = (Button) mLayoutButtons.findViewById(R.id.text_btn_spam);

        // hide moderation buttons until updateModerationButtons() is called
        mLayoutButtons.setVisibility(View.GONE);

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

        mBtnSpam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moderateComment(CommentStatus.SPAM);
            }
        });

        return view;
    }

    protected void setComment(int localBlogId, final Comment comment) {
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
        if (hasActivity())
            showComment();
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mChangeListener = (CommentActions.OnCommentChangeListener) activity;
        } catch (ClassCastException e) {
            mChangeListener = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        showComment();
    }

    private boolean hasActivity() {
        return (getActivity() != null && !isRemoving());
    }

    private boolean hasComment() {
        return (mComment != null);
    }

    protected int getCommentId() {
        return (mComment != null ? mComment.commentID : 0);
    }

    protected int getLocalBlogId() {
        return mLocalBlogId;
    }

    protected int getRemoteBlogId() {
        return mRemoteBlogId;
    }

    /*
     * reload the current comment from the local database
     */
    protected void refreshComment() {
        if (!hasComment())
            return;
        Comment updatedComment = CommentTable.getComment(mLocalBlogId, getCommentId());
        setComment(mLocalBlogId, updatedComment);
    }

    /*
     * clear the currently displayed comment
     */
    protected void clearComment() {
        setComment(0, null);
    }

    /*
     * display the current comment
     */
    private void showComment() {
        if (!hasActivity())
            return;

        // locate detail view, which contains all the other views - important to search for other
        // views within this view rather than the activity itself, since the activity may contain
        // fragments which use the same view names (ex: text_date in both the notification list
        // and comment detail fragments)
        final ViewGroup viewDetail = (ViewGroup) getActivity().findViewById(R.id.layout_detail);

        final NetworkImageView imgAvatar = (NetworkImageView) viewDetail.findViewById(R.id.image_avatar);
        final TextView txtName = (TextView) viewDetail.findViewById(R.id.text_name);
        final TextView txtDate = (TextView) viewDetail.findViewById(R.id.text_date);
        final TextView txtContent = (TextView) viewDetail.findViewById(R.id.text_content);

        // hide all views when comment is null (will happen when opened from a notification)
        if (mComment == null) {
            imgAvatar.setImageDrawable(null);
            txtName.setText(null);
            txtDate.setText(null);
            txtContent.setText(null);
            mTxtStatus.setText(null);
            mLayoutButtons.setVisibility(View.GONE);
            mLayoutReply.setVisibility(View.GONE);

            // if a notification was passed, request its associated comment
            if (mNote != null && !mIsRequestingComment)
                showCommentForNote(mNote);

            return;
        }

        txtName.setText(mComment.hasAuthorName() ? mComment.getAuthorName() : getString(R.string.anonymous));
        txtDate.setText(DateTimeUtils.javaDateToTimeSpan(mComment.getDatePublished()));

        // this is necessary in order for anchor tags in the comment text to be clickable
        txtContent.setLinksClickable(true);
        txtContent.setMovementMethod(WPLinkMovementMethod.getInstance());

        int maxImageSz = getResources().getDimensionPixelSize(R.dimen.reader_comment_max_image_size);
        CommentUtils.displayHtmlComment(txtContent, mComment.getCommentText(), maxImageSz);

        imgAvatar.setDefaultImageResId(R.drawable.placeholder);
        if (mComment.hasProfileImageUrl()) {
            imgAvatar.setImageUrl(mComment.getProfileImageUrl(), WordPress.imageLoader);
        } else if (mComment.hasAuthorEmail()) {
            int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
            String avatarUrl = GravatarUtils.gravatarUrlFromEmail(mComment.getAuthorEmail(), avatarSz);
            imgAvatar.setImageUrl(avatarUrl, WordPress.imageLoader);
        } else {
            imgAvatar.setImageResource(R.drawable.placeholder);
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

        int blogId = getRemoteBlogId();
        int postId = Integer.valueOf(mComment.postID);
        showPostTitle(blogId, postId);

        // make sure reply box is showing
        if (mLayoutReply.getVisibility() != View.VISIBLE && isReplyingEnabled())
            AniUtils.flyIn(mLayoutReply);
    }

    /*
     * ensure the post associated with this comment is available to the reader and show its
     * title above the comment
     */
    private void showPostTitle(final int blogId, final int postId) {
        if (!hasActivity())
            return;

        final TextView txtPostTitle = (TextView) getActivity().findViewById(R.id.text_post_title);
        boolean postExists = ReaderPostTable.postExists(blogId, postId);

        // use notification subject as the title if this was shown from a notification, otherwise
        // user the title of the associated post
        final boolean hasTitle;
        if (getNote() != null) {
            txtPostTitle.setText(getNote().getSubject());
            hasTitle = true;
        } else if (postExists) {
            txtPostTitle.setText(ReaderPostTable.getPostTitle(blogId, postId));
            hasTitle = true;
        } else {
            hasTitle = false;
        }

        // make sure this post is available to the reader, and show progress bar in title view
        // if the title wasn't set above so user knows something is happening
        if (!postExists) {
            final ProgressBar progress = (ProgressBar) getActivity().findViewById(R.id.progress_post_title);
            if (!hasTitle)
                progress.setVisibility(View.VISIBLE);
            ReaderPostActions.requestPost(blogId, postId, new ReaderActions.ActionListener() {
                @Override
                public void onActionResult(boolean succeeded) {
                    if (!hasActivity())
                        return;
                    progress.setVisibility(View.INVISIBLE);
                    // update title if it wasn't set above
                    if (succeeded && !hasTitle)
                        txtPostTitle.setText(ReaderPostTable.getPostTitle(blogId, postId));
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
        long postId = Long.valueOf(mComment.postID);
        ReaderActivityLauncher.showReaderPostDetail(getActivity(), mRemoteBlogId, postId);
    }

    /*
     * approve or unapprove the current comment
     */
    private void moderateComment(final CommentStatus newStatus) {
        if (!hasActivity() || !hasComment() || mIsModeratingComment)
            return;
        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        // disable buttons during request
        mBtnModerate.setEnabled(false);
        mBtnSpam.setEnabled(false);

        // animate the buttons out (updateStatusViews will re-display them when request completes)
        mLayoutButtons.clearAnimation();
        AniUtils.flyOut(mLayoutButtons);

        // hide status (updateStatusViews will un-hide it)
        if (mTxtStatus.getVisibility() == View.VISIBLE)
            AniUtils.fadeOut(mTxtStatus);

        // immediately show message bar displaying new status
        final int msgResId;
        final MessageBarType msgType;
        switch (newStatus) {
            case APPROVED:
                msgResId = R.string.comment_approved;
                msgType = MessageBarType.INFO;
                break;
            case UNAPPROVED:
                msgResId = R.string.comment_unapproved;
                msgType = MessageBarType.ALERT;
                break;
            case SPAM:
                msgResId = R.string.comment_spammed;
                msgType = MessageBarType.ALERT;
                break;
            default :
                msgResId = R.string.comment_moderated;
                msgType = MessageBarType.INFO;
                break;
        }
        MessageBarUtils.showMessageBar(getActivity(), getString(msgResId), msgType, null);

        CommentActions.CommentActionListener actionListener = new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                mIsModeratingComment = false;
                if (!hasActivity())
                    return;
                mBtnModerate.setEnabled(true);
                mBtnSpam.setEnabled(true);
                if (succeeded) {
                    mComment.setStatus(CommentStatus.toString(newStatus));
                    if (mChangeListener != null)
                        mChangeListener.onCommentModerated(mComment, mNote);
                } else {
                    ToastUtils.showToast(getActivity(), R.string.error_moderate_comment, ToastUtils.Duration.LONG);
                }
                // note this MUST come after mComment.setStatus
                updateStatusViews();
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
        final ProgressBar progress = (ProgressBar) getActivity().findViewById(R.id.progress_submit_comment);
        progress.setVisibility(View.VISIBLE);

        CommentActions.CommentActionListener actionListener = new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                mIsSubmittingReply = false;
                if (!hasActivity())
                    return;

                mEditReply.setEnabled(true);
                mImgSubmitReply.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);

                if (succeeded) {
                    if (mChangeListener != null)
                        mChangeListener.onCommentAdded();
                    MessageBarUtils.showMessageBar(getActivity(), getString(R.string.note_reply_successful));
                    mEditReply.setText(null);
                } else {
                    ToastUtils.showToast(getActivity(), R.string.reply_failed, ToastUtils.Duration.LONG);
                    // refocus editor on failure and show soft keyboard
                    mEditReply.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(mEditReply, InputMethodManager.SHOW_IMPLICIT);
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
     * update the text, drawable & click listener for mBtnModerate based on
     * the current status of the comment, show mBtnSpam if the comment isn't
     * already marked as spam, and show the current status of the comment
     */
    private void updateStatusViews() {
        if (!hasActivity() || !hasComment())
            return;

        final int btnTextResId;         // string resource id for moderation button
        final int btnDrawResId;         // drawable resource id for moderation button
        final CommentStatus newStatus;  // status to apply when moderation button is tapped
        final int statusTextResId;      // string resource id for status text
        final int statusColor;          // color for status text
        final boolean showSpamButton;

        switch (mComment.getStatusEnum()) {
            case APPROVED:
                btnTextResId = R.string.unapprove;
                btnDrawResId = R.drawable.moderate_unapprove;
                newStatus = CommentStatus.UNAPPROVED;
                showSpamButton = true;
                statusTextResId = R.string.approved;
                statusColor = getActivity().getResources().getColor(R.color.blue_extra_dark);
                break;
            case UNAPPROVED:
                btnTextResId = R.string.approve;
                btnDrawResId = R.drawable.moderate_approve;
                newStatus = CommentStatus.APPROVED;
                showSpamButton = true;
                statusTextResId = R.string.unapproved;
                statusColor = getActivity().getResources().getColor(R.color.orange_medium);
                break;
            case SPAM:
                btnTextResId = R.string.approve;
                btnDrawResId = R.drawable.moderate_approve;
                newStatus = CommentStatus.APPROVED;
                showSpamButton = false;
                statusTextResId = R.string.spam;
                statusColor = Color.RED;
                break;
            default:
                return;
        }

        // comment status is only shown if this comment is from one of this user's blogs
        if (mIsUsersBlog) {
            mTxtStatus.setText(getString(statusTextResId).toUpperCase());
            mTxtStatus.setTextColor(statusColor);
            if (mTxtStatus.getVisibility() != View.VISIBLE) {
                mTxtStatus.clearAnimation();
                AniUtils.fadeIn(mTxtStatus);
            }
        } else {
            mTxtStatus.setVisibility(View.GONE);
        }

        if (isModerationEnabled()) {
            mBtnModerate.setText(btnTextResId);
            mBtnModerate.setCompoundDrawablesWithIntrinsicBounds(btnDrawResId, 0, 0, 0);
            mBtnModerate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    moderateComment(newStatus);
                }
            });
            mBtnModerate.setVisibility(View.VISIBLE);
        } else {
            mBtnModerate.setVisibility(View.GONE);
        }

        mBtnSpam.setVisibility(isMarkSpamEnabled() && showSpamButton ? View.VISIBLE : View.GONE);

        // animate the buttons in if they're not visible
        if (mLayoutButtons.getVisibility() != View.VISIBLE && (isMarkSpamEnabled() || isModerationEnabled())) {
            mLayoutButtons.clearAnimation();
            AniUtils.flyIn(mLayoutButtons);
        }
    }

    private boolean isModerationEnabled() {
        if (mEnabledActions == null)
            return false;
        return (mEnabledActions.contains(EnabledActions.ACTION_APPROVE)
             || mEnabledActions.contains(EnabledActions.ACTION_UNAPPROVE));
    }

    private boolean isMarkSpamEnabled() {
        return (mEnabledActions != null && mEnabledActions.contains(EnabledActions.ACTION_SPAM));
    }

    private boolean isReplyingEnabled() {
        return (mEnabledActions != null && mEnabledActions.contains(EnabledActions.ACTION_REPLY));
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
                    comment.setProfileImageUrl(note.getIconURL());
                    setComment(localBlogId, comment);
                } else {
                    requestComment(localBlogId, mRemoteBlogId, commentId, note.getIconURL());
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
                                final int commentId,
                                final String profileImageUrl) {
        final ProgressBar progress = (hasActivity() ? (ProgressBar) getActivity().findViewById(R.id.progress_loading) : null);
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
                        if (profileImageUrl != null)
                            comment.setProfileImageUrl(profileImageUrl);
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
