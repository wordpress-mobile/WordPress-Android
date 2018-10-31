package org.wordpress.android.ui.posts;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPWebViewClient;

import javax.inject.Inject;

import static org.wordpress.android.ui.posts.EditPostActivity.EXTRA_POST_LOCAL_ID;

public class PostPreviewFragment extends Fragment {
    private SiteModel mSite;
    private PostModel mPost;
    private WebView mWebView;

    @Inject AccountStore mAccountStore;
    @Inject PostStore mPostStore;

    public static PostPreviewFragment newInstance(SiteModel site, PostModel post) {
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);
        args.putInt(EXTRA_POST_LOCAL_ID, post.getId());
        PostPreviewFragment fragment = new PostPreviewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        int localPostId;
        if (savedInstanceState == null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
            localPostId = getArguments().getInt(EXTRA_POST_LOCAL_ID);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            localPostId = savedInstanceState.getInt(EXTRA_POST_LOCAL_ID);
        }
        mPost = mPostStore.getPostByLocalPostId(localPostId);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(EXTRA_POST_LOCAL_ID, mPost.getId());
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.post_preview_fragment, container, false);

        mWebView = (WebView) view.findViewById(R.id.webView);
        WPWebViewClient client = new WPWebViewClient(mSite, mAccountStore.getAccessToken());
        mWebView.setWebViewClient(client);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshPreview();
    }

    public void setPost(PostModel post) {
        mPost = post;
    }

    void refreshPreview() {
        if (!isAdded()) {
            return;
        }

        new Thread() {
            @Override
            public void run() {
                final String htmlContent = formatPostContentForWebView(getActivity(), mPost);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) {
                            return;
                        }

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

    private String formatPostContentForWebView(Context context, PostModel post) {
        if (context == null || post == null) {
            return null;
        }

        String title = (TextUtils.isEmpty(post.getTitle())
                ? "(" + getResources().getText(R.string.untitled) + ")"
                : StringEscapeUtils.unescapeHtml4(post.getTitle()));

        String postContent = PostUtils.collapseShortcodes(post.getContent());

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
