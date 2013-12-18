package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
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
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.notifications.NotificationFragment;
import org.wordpress.android.ui.reader_native.ReaderActivityLauncher;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.Emoticons;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.MessageBarUtils;
import org.wordpress.android.util.MessageBarUtils.MessageBarType;
import org.wordpress.android.util.ReaderAniUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.VolleyUtils;
import org.wordpress.android.util.WPImageGetter;

import java.util.Map;

/**
 * Created by nbradbury on 11/11/13.
 * comment detail displayed from both the notification list and the comment list
 * prior to this there were separate comment detail screens for each list
 */
public class CommentDetailFragment extends Fragment implements NotificationFragment {
    private int mAccountId;
    private int mBlogId;

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

    private CommentActions.OnCommentChangeListener mChangeListener;

    /*
     * used when called from comment list
     */
    protected static CommentDetailFragment newInstance(int blogId, final Comment comment) {
        CommentDetailFragment fragment = new CommentDetailFragment();
        fragment.setComment(blogId, comment);
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

    protected void setComment(int blogId, final Comment comment) {
        mComment = comment;
        mBlogId = blogId;
        mAccountId = WordPress.wpDB.getAccountIdForBlogId(blogId);
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
        return (getActivity() != null);
    }

    private boolean hasComment() {
        return (mComment != null);
    }

    protected int getCommentId() {
        return (mComment != null ? mComment.commentID : 0);
    }

    protected int getBlogId() {
        return mBlogId;
    }

    /*
     * reload the current comment from the local database
     */
    protected void refreshComment() {
        if (!hasComment())
            return;
        Comment updatedComment = WordPress.wpDB.getComment(mAccountId, getCommentId());
        setComment(mBlogId, updatedComment);
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

        // hide all views when comment is null
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

        txtName.setText(TextUtils.isEmpty(mComment.name) ? getString(R.string.anonymous) : StringUtils.unescapeHTML(mComment.name));
        txtDate.setText(mComment.dateCreatedFormatted);

        // this is necessary in order for anchor tags in the comment text to be clickable
        txtContent.setLinksClickable(true);
        txtContent.setMovementMethod(LinkMovementMethod.getInstance());

        // convert emoticons in content first so their images won't be downloaded, then convert to HTML
        String content = StringUtils.notNullStr(mComment.comment);
        if (content.contains("icon_"))
            content = Emoticons.replaceEmoticonsWithEmoji((SpannableStringBuilder) Html.fromHtml(content)).toString().trim();
        final SpannableStringBuilder html;
        if (content.contains("<img")) {
            int maxImageSz = getResources().getDimensionPixelSize(R.dimen.reader_comment_max_image_size);
            html = (SpannableStringBuilder) Html.fromHtml(content, new WPImageGetter(getActivity(), txtContent, maxImageSz), null);
        } else {
            html = (SpannableStringBuilder) Html.fromHtml(content);
        }
        txtContent.setText(html);

        imgAvatar.setDefaultImageResId(R.drawable.placeholder);
        if (mComment.hasProfileImageUrl()) {
            imgAvatar.setImageUrl(mComment.getProfileImageUrl(), WordPress.imageLoader);
        } else {
            int avatarSz = getResources().getDimensionPixelSize(R.dimen.reader_avatar_sz_large);
            String avatarUrl = GravatarUtils.gravatarUrlFromEmail(mComment.authorEmail, avatarSz);
            imgAvatar.setImageUrl(avatarUrl, WordPress.imageLoader);
        }

        updateStatusViews();

        // navigate to author's home page when avatar or name clicked
        if (!TextUtils.isEmpty(mComment.authorURL)) {
            View.OnClickListener authorListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(getActivity(), mComment.authorURL);
                }
            };
            imgAvatar.setOnClickListener(authorListener);
            txtName.setOnClickListener(authorListener);
        }

        // make sure reply box is showing
        if (mLayoutReply.getVisibility() != View.VISIBLE)
            ReaderAniUtils.flyIn(mLayoutReply);
    }

    /*
     * returns true if there's an active network connection, otherwise displays a toast error
     * and returns false
     */
    // TODO: move this routine to NetworkUtils once that class is merged into develop
    private boolean checkConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected())
            return true;

        ToastUtils.showToast(context, R.string.no_network_message);
        return false;
    }

    /*
     * approve or unapprove the current comment
     */
    private void moderateComment(final CommentStatus newStatus) {
        if (!hasActivity() || !hasComment() || mIsModeratingComment)
            return;
        if (!checkConnection(getActivity()))
            return;

        // disable buttons during request
        mBtnModerate.setEnabled(false);
        mBtnSpam.setEnabled(false);

        // animate the buttons out (updateStatusViews will re-display them when request completes)
        mLayoutButtons.clearAnimation();
        ReaderAniUtils.flyOut(mLayoutButtons);

        // hide status (updateStatusViews will un-hide it)
        ReaderAniUtils.fadeOut(mTxtStatus);

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
                        mChangeListener.onCommentModerated();
                } else {
                    ToastUtils.showToast(getActivity(), R.string.error_moderate_comment, ToastUtils.Duration.LONG);
                }
                // note this MUST come after mComment.setStatus
                updateStatusViews();
            }
        };
        mIsModeratingComment = true;
        CommentActions.moderateComment(mAccountId, mComment, newStatus, actionListener);
    }

    /*
     * post comment box text as a reply to the current comment
     */
    private void submitReply() {
        if (!hasActivity() || mIsSubmittingReply)
            return;

        if (!checkConnection(getActivity()))
            return;

        final ProgressBar progress = (ProgressBar) getActivity().findViewById(R.id.progress_submit_comment);
        final String replyText = EditTextUtils.getText(mEditReply);
        if (TextUtils.isEmpty(replyText))
            return;

        // disable editor, hide soft keyboard, hide submit icon, and show progress spinner while submitting
        mEditReply.setEnabled(false);
        EditTextUtils.hideSoftInput(mEditReply);
        mImgSubmitReply.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);

        CommentActions.CommentActionListener actionListener = new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                mIsSubmittingReply = false;

                if (hasActivity()) {
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
            }
        };

        mIsSubmittingReply = true;
        CommentActions.submitReplyToComment(mAccountId,
                mComment,
                replyText,
                actionListener);
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

        mTxtStatus.setText(getString(statusTextResId).toUpperCase());
        mTxtStatus.setTextColor(statusColor);
        if (mTxtStatus.getVisibility() != View.VISIBLE) {
            mTxtStatus.clearAnimation();
            ReaderAniUtils.fadeIn(mTxtStatus);
        }

        mBtnModerate.setText(btnTextResId);
        mBtnModerate.setCompoundDrawablesWithIntrinsicBounds(btnDrawResId, 0, 0, 0);
        mBtnModerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moderateComment(newStatus);
            }
        });

        mBtnSpam.setVisibility(showSpamButton ? View.VISIBLE : View.GONE);

        // animate the buttons in if they're not visible
        if (mLayoutButtons.getVisibility() != View.VISIBLE) {
            mLayoutButtons.clearAnimation();
            ReaderAniUtils.flyIn(mLayoutButtons);
        }
    }

    /*
     * display the comment associated with the passed notification
     */
    private void showCommentForNote(Note note) {
        /*
         * in order to get the actual comment from a notification we need to extract the
         * blogId/postId/commentId from the notification, and this info is buried in the
         * "actions" array of the note's JSON. each action entry contains a "params"
         * array which contains these IDs, so find the first action then extract the IDs
         * from its params
         */
        Map<String,JSONObject> actions = note.getActions();
        if (actions.size() > 0) {
            String firstKey = actions.keySet().iterator().next();
            JSONObject jsonAction = actions.get(firstKey);
            JSONObject jsonParams = jsonAction.optJSONObject("params");
            if (jsonParams != null) {
                int blogId = jsonParams.optInt("blog_id");
                int commentId = jsonParams.optInt("comment_id");
                int accountId = WordPress.wpDB.getAccountIdForBlogId(blogId);

                // first try to get from local db, if that fails request it from the server
                Comment comment = WordPress.wpDB.getComment(accountId, commentId);
                if (comment != null) {
                    comment.setProfileImageUrl(note.getIconURL());
                    setComment(blogId, comment);
                } else {
                    requestComment(blogId, commentId, note.getIconURL());
                }
            }
        }
    }

    /*
     * request a comment - note that this uses the REST API rather than XMLRPC, which means the user must
     * either be wp.com or have Jetpack, but it's safe to do this since this method is only called when
     * displayed from a notification (and notifications require wp.com/Jetpack)
     */
    private void requestComment(final int blogId, final int commentId, final String profileImageUrl) {
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
                    Comment comment = new Comment(jsonObject);
                    if (comment != null) {
                        if (profileImageUrl != null)
                            comment.setProfileImageUrl(profileImageUrl);
                        WordPress.wpDB.addComment(mAccountId, comment);
                        setComment(blogId, comment);
                    }
                }
            }
        };
        RestRequest.ErrorListener restErrListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                mIsRequestingComment = false;
                Log.e(WordPress.TAG, VolleyUtils.errStringFromVolleyError(volleyError), volleyError);
                if (hasActivity()) {
                    if (progress != null)
                        progress.setVisibility(View.GONE);
                    ToastUtils.showToast(getActivity(), R.string.connection_error, ToastUtils.Duration.LONG);
                }
            }
        };

        final String path = String.format("/sites/%s/comments/%s", blogId, commentId);
        mIsRequestingComment = true;
        WordPress.restClient.get(path, restListener, restErrListener);
    }
}
