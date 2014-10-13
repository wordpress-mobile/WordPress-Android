package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPWebViewClient;

public class ViewPostFragment extends Fragment {
    /** Called when the activity is first created. */

    private OnDetailPostActionListener onDetailPostActionListener;
    PostsActivity parentActivity;

    private ViewGroup mLayoutCommentBox;
    private EditText mEditComment;
    private ImageButton mAddCommentButton, mShareUrlButton, mViewPostButton;
    private TextView mTitleTextView, mContentTextView;
    private boolean mShouldLoadPost = true;

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't load the post until we know the width of mContentTextView
        // GlobalLayoutListener on mContentTextView will load the post once it gets laid out
        if (WordPress.currentPost != null && !getView().isLayoutRequested()) {
            loadPost(WordPress.currentPost);
        }

        parentActivity = (PostsActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.viewpost, container, false);
        v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                loadPost(WordPress.currentPost);
                v.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });

        mTitleTextView = (TextView) v.findViewById(R.id.postTitle);
        mContentTextView = (TextView) v.findViewById(R.id.viewPostTextView);
        mShareUrlButton = (ImageButton) v.findViewById(R.id.sharePostLink);
        mViewPostButton = (ImageButton) v.findViewById(R.id.viewPost);

        // comment views
        mLayoutCommentBox = (ViewGroup) v.findViewById(R.id.layout_comment_box);
        mEditComment = (EditText) mLayoutCommentBox.findViewById(R.id.edit_comment);
        mEditComment.setHint(R.string.reader_hint_comment_on_post);

        // button listeners here
        ImageButton editPostButton = (ImageButton) v.findViewById(R.id.editPost);
        editPostButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {
                if (WordPress.currentPost != null && !parentActivity.isRefreshing()) {
                    onDetailPostActionListener.onDetailPostAction(PostsActivity.POST_EDIT, WordPress.currentPost);
                    Intent i = new Intent(getActivity().getApplicationContext(), EditPostActivity.class);
                    i.putExtra(EditPostActivity.EXTRA_IS_PAGE, WordPress.currentPost.isPage());
                    i.putExtra(EditPostActivity.EXTRA_POSTID, WordPress.currentPost.getLocalTablePostId());
                    getActivity().startActivityForResult(i, PostsActivity.ACTIVITY_EDIT_POST);
                }
            }
        });


        mShareUrlButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {
                if (!parentActivity.isRefreshing()) {
                    onDetailPostActionListener.onDetailPostAction(PostsActivity.POST_SHARE, WordPress.currentPost);
                }
            }
        });

        ImageButton deletePostButton = (ImageButton) v.findViewById(R.id.deletePost);
        deletePostButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {
                if (!parentActivity.isRefreshing()) {
                    onDetailPostActionListener.onDetailPostAction(PostsActivity.POST_DELETE, WordPress.currentPost);
                }
            }
        });

        mViewPostButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {
                onDetailPostActionListener.onDetailPostAction(PostsActivity.POST_VIEW, WordPress.currentPost);
                if (!parentActivity.isRefreshing()) {
                    loadPostPreview();
                }
            }
        });

        mAddCommentButton = (ImageButton) v.findViewById(R.id.addComment);
        // Tint the comment icon to match the other icons in the toolbar
        mAddCommentButton.setColorFilter(Color.argb(255, 132, 132, 132));
        mAddCommentButton.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {
                if (!parentActivity.isRefreshing()) {
                    toggleCommentBox();
                }
            }
        });

        return v;

    }

    /**
     * Load the post preview. If the post is in a non-public state (e.g. draft status, part of a
     * non-public blog, etc), load the preview as an authenticated URL. Otherwise, just load the
     * preview normally.
     */
    protected void loadPostPreview() {
        if (WordPress.currentPost != null && !TextUtils.isEmpty(WordPress.currentPost.getPermaLink())) {
            Post post = WordPress.currentPost;
            String url = post.getPermaLink();
            if ( WordPress.getCurrentBlog().isPrivate() //blog private
                    || post.isLocalDraft()
                    || post.isLocalChange()
                    || post.getStatusEnum() != PostStatus.PUBLISHED) {
                if (-1 == url.indexOf('?')) {
                    url = url.concat("?preview=true");
                } else {
                    url = url.concat("&preview=true");
                }
                WPWebViewActivity.openUrlByUsingBlogCredentials(
                        getActivity(),
                        WordPress.currentBlog,
                        url
                );
            } else {
                WPWebViewActivity.openURL(getActivity(), url);
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

    public void loadPost(final Post post) {
        // Don't load if the Post object or title are null, see #395
        if (post == null || post.getTitle() == null)
            return;
        if (!isAdded() || getView() == null)
            return;

        // create handler on UI thread
        final Handler handler = new Handler();

        // locate views and determine content in the background to avoid ANR - especially
        // important when using WPHtml.fromHtml() for drafts that contain images since
        // thumbnails may take some time to create
        final WebView webView = (WebView) getView().findViewById(R.id.viewPostWebView);
        webView.setWebViewClient(new WPWebViewClient(WordPress.getCurrentBlog()));
        new Thread() {
            @Override
            public void run() {

                final String title = (TextUtils.isEmpty(post.getTitle())
                                        ? "(" + getResources().getText(R.string.untitled) + ")"
                                        : StringUtils.unescapeHTML(post.getTitle()));

                final String postContent = post.getDescription() + "\n\n" + post.getMoreText();

                final Spanned draftContent;
                final String htmlContent;
                if (post.isLocalDraft()) {
                    View view = getView();
                    int maxWidth = Math.min(view.getWidth(), view.getHeight());

                    draftContent = WPHtml.fromHtml(postContent.replaceAll("\uFFFC", ""), getActivity(), post, maxWidth);
                    htmlContent = null;
                } else {
                    draftContent = null;
                    htmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                                + "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head>"
                                + "<body><div id=\"container\">"
                                + StringUtils.addPTags(postContent)
                                + "</div></body></html>";
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // make sure activity is still valid
                        if (!isAdded())
                            return;

                        mTitleTextView.setText(title);

                        if (post.isLocalDraft()) {
                            mContentTextView.setVisibility(View.VISIBLE);
                            webView.setVisibility(View.GONE);
                            mShareUrlButton.setVisibility(View.GONE);
                            mViewPostButton.setVisibility(View.GONE);
                            mAddCommentButton.setVisibility(View.GONE);
                            mContentTextView.setText(draftContent);
                        } else {
                            mContentTextView.setVisibility(View.GONE);
                            webView.setVisibility(View.VISIBLE);
                            mShareUrlButton.setVisibility(View.VISIBLE);
                            mViewPostButton.setVisibility(View.VISIBLE);
                            mAddCommentButton.setVisibility(post.isAllowComments() ? View.VISIBLE : View.GONE);
                            webView.loadDataWithBaseURL("file:///android_asset/",
                                                        htmlContent,
                                                        "text/html",
                                                        "utf-8",
                                                        null);
                        }
                    }
                });
            }
        }.start();
    }

    public interface OnDetailPostActionListener {
        public void onDetailPostAction(int action, Post post);
    }

    public void clearContent() {
        TextView txtTitle = (TextView) getView().findViewById(R.id.postTitle);
        WebView webView = (WebView) getView().findViewById(R.id.viewPostWebView);
        TextView txtContent = (TextView) getView().findViewById(R.id.viewPostTextView);
        txtTitle.setText("");
        txtContent.setText("");
        String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                        + "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head>"
                        + "<body><div id=\"container\"></div></body></html>";
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

    private void showCommentBox() {
        // skip if it's already showing or a comment is being submitted
        if (mIsCommentBoxShowing || mIsSubmittingComment)
            return;
        if (!isAdded())
            return;

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
        EditTextUtils.showSoftInput(mEditComment);
        mIsCommentBoxShowing = true;
    }

    private void hideCommentBox() {
        if (!mIsCommentBoxShowing)
            return;
        if (!isAdded())
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
        if (!isAdded() || mIsSubmittingComment || WordPress.currentPost == null || !NetworkUtils.checkConnection(
                getActivity())) {
            return;
        }
        final String commentText = EditTextUtils.getText(mEditComment);
        if (TextUtils.isEmpty(commentText)) {
            return;
        }

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
                if (!isAdded())
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

        int accountId = WordPress.getCurrentLocalTableBlogId();
        CommentActions.addComment(accountId, WordPress.currentPost.getRemotePostId(), commentText, actionListener);
    }
}
