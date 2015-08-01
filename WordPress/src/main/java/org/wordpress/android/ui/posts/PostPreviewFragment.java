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
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPWebViewClient;

public class PostPreviewFragment extends Fragment {

    private int mLocalBlogId;
    private long mLocalPostId;
    private boolean mIsPage;

    private WebView mWebView;

    public static PostPreviewFragment newInstance(int localBlogId, long localPostId, boolean isPage) {
        Bundle args = new Bundle();
        args.putInt(PostPreviewActivity.ARG_LOCAL_BLOG_ID, localBlogId);
        args.putLong(PostPreviewActivity.ARG_LOCAL_POST_ID, localPostId);
        args.putBoolean(PostPreviewActivity.ARG_IS_PAGE, isPage);
        PostPreviewFragment fragment = new PostPreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        mLocalBlogId = args.getInt(PostPreviewActivity.ARG_LOCAL_BLOG_ID);
        mLocalPostId = args.getLong(PostPreviewActivity.ARG_LOCAL_POST_ID);
        mIsPage = args.getBoolean(PostPreviewActivity.ARG_IS_PAGE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mLocalPostId = savedInstanceState.getLong(PostPreviewActivity.ARG_LOCAL_POST_ID);
            mLocalBlogId = savedInstanceState.getInt(PostPreviewActivity.ARG_LOCAL_BLOG_ID);
            mIsPage = savedInstanceState.getBoolean(PostPreviewActivity.ARG_IS_PAGE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(PostPreviewActivity.ARG_LOCAL_POST_ID, mLocalPostId);
        outState.putInt(PostPreviewActivity.ARG_LOCAL_BLOG_ID, mLocalBlogId);
        outState.putBoolean(PostPreviewActivity.ARG_IS_PAGE, mIsPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.post_preview_fragment, container, false);
        mWebView = (WebView) view.findViewById(R.id.webView);
        mWebView.setWebViewClient(new WPWebViewClient(WordPress.wpDB.instantiateBlogByLocalId(mLocalBlogId)));
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadPreview();
    }

    void loadPreview() {
        if (!isAdded()) return;

        new Thread() {
            @Override
            public void run() {
                final String htmlContent = formatPostContentForWebView(
                        getActivity(),
                        WordPress.wpDB.getPostForLocalTablePostId(mLocalPostId));

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) return;

                        if (htmlContent != null) {
                            mWebView.loadDataWithBaseURL(
                                    null,
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

        String postContent = post.getDescription();
        if (!TextUtils.isEmpty(post.getMoreText())) {
            postContent += "\n\n" + post.getMoreText();
        }

        // if this is a local draft, remove src="null" from image tags then replace the "android-uri"
        // tag added for local image with a valid "src" tag so local images can be viewed
        if (post.isLocalDraft()) {
            postContent = postContent.replace("src=\"null\"", "").replace("android-uri=", "src=");
        }

        String textColorStr = HtmlUtils.colorResToHtmlColor(context, R.color.grey_dark);
        String linkColorStr = HtmlUtils.colorResToHtmlColor(context, R.color.reader_hyperlink);

        int contentMargin = getResources().getDimensionPixelSize(R.dimen.content_margin);
        String marginStr = Integer.toString(contentMargin) + "px";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8' />"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<link href='file:///android_asset/merriweather.css' rel='stylesheet' type='text/css'>"
                + "<style type='text/css'>"
                + "  html { margin-left: " + marginStr + "; margin-right: " + marginStr + "; }"
                + "  body { font-family: Merriweather, serif; font-weight: 300; padding: 0px; margin: 0px; width: 100%; color: " + textColorStr + "; }"
                + "  body, p, div { max-width: 100% !important; word-wrap: break-word; }"
                + "  p, div { line-height: 1.6em; font-size: 0.95em; }"
                + "  h1 { font-size: 1.2em; font-family: Merriweather, serif; font-weight: 700; }"
                + "  img { max-width: 100%; height: auto; }"
                + "  a { text-decoration: none; color: " + linkColorStr + "; }"
                + "</style></head><body>"
                + "<h1>" + title + "</h1>"
                + StringUtils.addPTags(postContent)
                + "</body></html>";
    }
}
