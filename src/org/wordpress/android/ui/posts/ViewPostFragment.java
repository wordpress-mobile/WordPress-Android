package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPMobileStatsUtil;

public class ViewPostFragment extends Fragment {
    /** Called when the activity is first created. */

    private OnDetailPostActionListener onDetailPostActionListener;
    PostsActivity parentActivity;

    private ViewGroup mLayoutCommentBox;
    private EditText mEditComment;
    private ImageButton mAddCommentButton;

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

    }

    @Override
    public void onResume() {
        super.onResume();

        if (WordPress.currentPost != null)
            loadPost(WordPress.currentPost);

        parentActivity = (PostsActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.viewpost, container, false);

        // comment views
        mLayoutCommentBox = (ViewGroup) v.findViewById(R.id.layout_comment_box);
        mEditComment = (EditText) mLayoutCommentBox.findViewById(R.id.edit_comment);
        mEditComment.setHint(R.string.reader_hint_comment_on_post);

        // button listeners here
        ImageButton editPostButton = (ImageButton) v
                .findViewById(R.id.editPost);
        editPostButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {
                if (WordPress.currentPost != null && !parentActivity.isRefreshing) {
                    onDetailPostActionListener.onDetailPostAction(
                            PostsActivity.POST_EDIT, WordPress.currentPost);
                    Intent i = new Intent(
                            getActivity().getApplicationContext(),
                            EditPostActivity.class);
                    i.putExtra(EditPostActivity.EXTRA_IS_PAGE, WordPress.currentPost.isPage());
                    i.putExtra(EditPostActivity.EXTRA_POSTID, WordPress.currentPost.getId());
                    getActivity().startActivityForResult(i, PostsActivity.ACTIVITY_EDIT_POST);
                }

            }
        });

        ImageButton shareURLButton = (ImageButton) v
                .findViewById(R.id.sharePostLink);
        shareURLButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {

                if (!parentActivity.isRefreshing)
                    onDetailPostActionListener.onDetailPostAction(PostsActivity.POST_SHARE, WordPress.currentPost);

            }
        });

        ImageButton deletePostButton = (ImageButton) v
                .findViewById(R.id.deletePost);
        deletePostButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {

                if (!parentActivity.isRefreshing)
                    onDetailPostActionListener.onDetailPostAction(PostsActivity.POST_DELETE, WordPress.currentPost);

            }
        });

        ImageButton viewPostButton = (ImageButton) v
                .findViewById(R.id.viewPost);
        viewPostButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {
                onDetailPostActionListener.onDetailPostAction(PostsActivity.POST_VIEW, WordPress.currentPost);
                if (!parentActivity.isRefreshing)
                    loadPostPreview();

            }
        });

        mAddCommentButton = (ImageButton) v.findViewById(R.id.addComment);
        mAddCommentButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {

                if (!parentActivity.isRefreshing)
                    toggleCommentBox();

            }
        });

        return v;

    }

    protected void loadPostPreview() {

        if (WordPress.currentPost != null) {
            if (WordPress.currentPost.getPermaLink() != null && !WordPress.currentPost.getPermaLink().equals("")) {
                Intent i = new Intent(getActivity(), PreviewPostActivity.class);
                startActivity(i);
            }
        }

    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            onDetailPostActionListener = (OnDetailPostActionListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    public void loadPost(Post post) {

        // Don't load if the Post object of title are null, see #395
        if (post == null || post.getTitle() == null)
            return;

        TextView title = (TextView) getActivity().findViewById(R.id.postTitle);
        if (post.getTitle().equals(""))
            title.setText("(" + getResources().getText(R.string.untitled) + ")");
        else
            title.setText(StringUtils.unescapeHTML(post.getTitle()));

        WebView webView = (WebView) getActivity().findViewById(
                R.id.viewPostWebView);
        TextView tv = (TextView) getActivity().findViewById(
                R.id.viewPostTextView);
        ImageButton shareURLButton = (ImageButton) getActivity().findViewById(
                R.id.sharePostLink);
        ImageButton viewPostButton = (ImageButton) getActivity().findViewById(
                R.id.viewPost);
        ImageButton addCommentButton = (ImageButton) getActivity().findViewById(
                R.id.addComment);

        String postContent = post.getDescription() + "\n\n" + post.getMt_text_more();

        if (post.isLocalDraft()) {
            tv.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
            shareURLButton.setVisibility(View.GONE);
            viewPostButton.setVisibility(View.GONE);
            addCommentButton.setVisibility(View.GONE);

            tv.setText(WPHtml.fromHtml(postContent.replaceAll("\uFFFC", ""), getActivity(), post));
        } else {
            tv.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            shareURLButton.setVisibility(View.VISIBLE);
            viewPostButton.setVisibility(View.VISIBLE);
            if (post.isMt_allow_comments()) {
                addCommentButton.setVisibility(View.VISIBLE);
            } else {
                addCommentButton.setVisibility(View.GONE);
            }

            String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head><body><div id=\"container\">"
                    + StringUtils.addPTags(postContent) + "</div></body></html>";
            webView.loadDataWithBaseURL("file:///android_asset/", htmlText,
                    "text/html", "utf-8", null);
        }

    }

    public interface OnDetailPostActionListener {
        public void onDetailPostAction(int action, Post post);
    }

    public void clearContent() {
        TextView title = (TextView) getActivity().findViewById(R.id.postTitle);
        title.setText("");
        WebView webView = (WebView) getActivity().findViewById(
                R.id.viewPostWebView);
        TextView tv = (TextView) getActivity().findViewById(
                R.id.viewPostTextView);
        tv.setText("");
        String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head><body><div id=\"container\"></div></body></html>";
        webView.loadDataWithBaseURL("file:///android_asset/", htmlText,
                "text/html", "utf-8", null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    boolean mIsCommentBoxShowing = false;
    boolean mIsSubmittingComment = false;

    private boolean hasActivity() {
        return (getActivity() != null && !isRemoving());
    }

    private void showCommentBox() {
        // skip if it's already showing or a comment is being submitted
        if (mIsCommentBoxShowing || mIsSubmittingComment)
            return;
        if (!hasActivity())
            return;

        WPMobileStatsUtil.flagProperty(WPMobileStatsUtil.StatsEventPostsClosed,
                WPMobileStatsUtil.StatsPropertyPostDetailClickedComment);

        // show the comment box in, force keyboard to appear and highlight the comment button
        mLayoutCommentBox.setVisibility(View.VISIBLE);
        mEditComment.requestFocus();

        // submit comment when done/send tapped on the keyboard
        mEditComment.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND)
                    submitComment();
                return false;
            }
        });

        // submit comment when send icon tapped
        final ImageView imgPostComment = (ImageView) mLayoutCommentBox.findViewById(R.id.image_post_comment);
        imgPostComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitComment();
            }
        });

        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditComment, InputMethodManager.SHOW_IMPLICIT);
        mIsCommentBoxShowing = true;
    }

    private void hideCommentBox() {
        if (!mIsCommentBoxShowing)
            return;
        if (!hasActivity())
            return;

        EditTextUtils.hideSoftInput(mEditComment);
        mLayoutCommentBox.setVisibility(View.GONE);

        mIsCommentBoxShowing = false;
    }

    private void toggleCommentBox() {
        if (mIsCommentBoxShowing) {
            hideCommentBox();
        } else {
            showCommentBox();
        }
    }

    private void submitComment() {
        if (!hasActivity() || mIsSubmittingComment)
            return;

        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        final String commentText = EditTextUtils.getText(mEditComment);
        if (TextUtils.isEmpty(commentText))
            return;

        final ImageView imgPostComment = (ImageView) mLayoutCommentBox.findViewById(R.id.image_post_comment);
        final ProgressBar progress = (ProgressBar) mLayoutCommentBox.findViewById(R.id.progress_submit_comment);

        // disable editor & comment button, hide soft keyboard, hide submit icon, and show progress spinner while submitting
        mEditComment.setEnabled(false);
        mAddCommentButton.setEnabled(false);
        EditTextUtils.hideSoftInput(mEditComment);
        imgPostComment.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);

        CommentActions.CommentActionListener actionListener = new CommentActions.CommentActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                mIsSubmittingComment = false;
                if (!hasActivity())
                    return;

                parentActivity.attemptToSelectPost();

                mEditComment.setEnabled(true);
                mAddCommentButton.setEnabled(true);
                imgPostComment.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);

                if (succeeded) {
                    ToastUtils.showToast(getActivity(), R.string.comment_added);
                    hideCommentBox();
                    mEditComment.setText(null);
                    parentActivity.refreshComments();
                } else {
                    ToastUtils.showToast(getActivity(), R.string.reader_toast_err_comment_failed, ToastUtils.Duration.LONG);
                }
            }
        };

        int accountId = WordPress.getCurrentBlogAccountId();
        CommentActions.addComment(accountId, WordPress.currentPost.getPostid(), commentText, actionListener);
    }

}
