package org.wordpress.android.ui.posts;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.util.ErrorManagedWebViewClient.ErrorManagedWebViewClientListener;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPWebViewClient;

import javax.inject.Inject;

import static org.wordpress.android.ui.posts.EditPostActivity.EXTRA_POST_LOCAL_ID;

public class PostPreviewFragment extends Fragment {
    private LoadPostPreviewTask mLoadTask;
    private SiteModel mSite;
    private int mLocalPostId;
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

        if (savedInstanceState == null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
            mLocalPostId = getArguments().getInt(EXTRA_POST_LOCAL_ID);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mLocalPostId = savedInstanceState.getInt(EXTRA_POST_LOCAL_ID);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(EXTRA_POST_LOCAL_ID, mLocalPostId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.post_preview_fragment, container, false);

        mWebView = view.findViewById(R.id.webView);
        // Listener is not used, this preview is only used to display local data and will be dropped soon.
        WPWebViewClient client = new WPWebViewClient(mSite, mAccountStore.getAccessToken(),
                new ErrorManagedWebViewClientListener() {
                    @Override public void onWebViewPageLoaded() {
                    }

                    @Override public void onWebViewReceivedError() {
                    }
                });
        mWebView.setWebViewClient(client);
        mWebView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getActivity() != null) {
                    loadPost();
                }
                mWebView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null && !mWebView.isLayoutRequested()) {
            loadPost();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mLoadTask != null) {
            mLoadTask.cancel(true);
            mLoadTask = null;
        }
    }

    void loadPost() {
        // cancel the previous load so we can load the new post
        if (mLoadTask != null) {
            mLoadTask.cancel(true);
        }

        mLoadTask = new LoadPostPreviewTask();
        mLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // Load post content in the background
    private class LoadPostPreviewTask extends AsyncTask<Void, Void, String> {
        private PostModel mPost;

        @Override
        protected String doInBackground(Void... params) {
            if (getActivity() == null) {
                return null;
            }

            mPost = mPostStore.getPostByLocalPostId(mLocalPostId);
            if (mPost == null) {
                return null;
            }


            return formatPostContentForWebView(getActivity(), mPost);
        }

        @Override
        protected void onPostExecute(String content) {
            if (mPost != null && content != null) {
                mWebView.loadDataWithBaseURL("file:///android_asset/", content,
                        "text/html", "utf-8", null);
            }

            mLoadTask = null;
        }
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
