package org.wordpress.android.ui.posts;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPWebViewClient;

public class PostPreviewFragment extends Fragment {

    private int mLocalBlogId;
    private long mLocalPostId;
    private WebView mWebView;

    public static PostPreviewFragment newInstance(int localBlogId, long localPostId) {
        Bundle args = new Bundle();
        args.putInt(PostPreviewActivity.ARG_LOCAL_BLOG_ID, localBlogId);
        args.putLong(PostPreviewActivity.ARG_LOCAL_POST_ID, localPostId);
        PostPreviewFragment fragment = new PostPreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        mLocalBlogId = args.getInt(PostPreviewActivity.ARG_LOCAL_BLOG_ID);
        mLocalPostId = args.getLong(PostPreviewActivity.ARG_LOCAL_POST_ID);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mLocalBlogId = savedInstanceState.getInt(PostPreviewActivity.ARG_LOCAL_BLOG_ID);
            mLocalPostId = savedInstanceState.getLong(PostPreviewActivity.ARG_LOCAL_POST_ID);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(PostPreviewActivity.ARG_LOCAL_BLOG_ID, mLocalBlogId);
        outState.putLong(PostPreviewActivity.ARG_LOCAL_POST_ID, mLocalPostId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.post_preview_fragment, container, false);

        mWebView = (WebView) view.findViewById(R.id.webView);
        WPWebViewClient client = new WPWebViewClient(WordPress.wpDB.instantiateBlogByLocalId(mLocalBlogId));
        mWebView.setWebViewClient(client);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshPreview();
    }

    void refreshPreview() {
        if (!isAdded()) return;

        new Thread() {
            @Override
            public void run() {
                Post post = WordPress.wpDB.getPostForLocalTablePostId(mLocalPostId);
                final String htmlContent = formatPostContentForWebView(getActivity(), post);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) return;

                        if (htmlContent != null) {
                            mWebView.loadDataWithBaseURL(
                                    "file:///android_asset/",
                                    htmlContent,
                                    "text/html",
                                    "utf-8",
                                    null);
                        } else {
                            ToastUtils.showToast(getActivity(), R.string.post_not_found);
                        }
                    }
                });
            }
        }.start();
    }

    private String formatPostContentForWebView(Context context, Post post) {
        if (context == null || post == null) {
            return null;
        }

        String title = (TextUtils.isEmpty(post.getTitle())
                ? "(" + getResources().getText(R.string.untitled) + ")"
                : StringUtils.unescapeHTML(post.getTitle()));

        String postContent = PostUtils.collapseShortcodes(post.getDescription());
        if (!TextUtils.isEmpty(post.getMoreText())) {
            postContent += "\n\n" + post.getMoreText();
        }

        // if this is a local draft, remove src="null" from image tags then replace the "android-uri"
        // tag added for local image with a valid "src" tag so local images can be viewed
        if (post.isLocalDraft()) {
            postContent = postContent.replace("src=\"null\"", "").replace("android-uri=", "src=");
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8' />"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<link rel='stylesheet' href='editor.css'>"
                + "<link rel='stylesheet' href='editor-android.css'>"
                + "</head><body>"
                + "<h1>" + title + "</h1>"
                + StringUtils.addPTags(postContent)
                + "</body></html>";
    }
}
