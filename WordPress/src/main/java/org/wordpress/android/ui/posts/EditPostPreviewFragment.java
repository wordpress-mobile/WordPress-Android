package org.wordpress.android.ui.posts;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.util.StringUtils;

import javax.inject.Inject;

public class EditPostPreviewFragment extends Fragment {
    private static final String ARG_LOCAL_POST_ID = "local_post_id";
    private WebView mWebView;
    private int mLocalPostId;
    private LoadPostPreviewTask mLoadTask;

    @Inject PostStore mPostStore;

    public static EditPostPreviewFragment newInstance(@NonNull PostModel post) {
        EditPostPreviewFragment fragment = new EditPostPreviewFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_LOCAL_POST_ID, post.getId());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        mLocalPostId = args.getInt(ARG_LOCAL_POST_ID);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) requireActivity().getApplication()).component().inject(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.edit_post_preview_fragment, container, false);
        mWebView = rootView.findViewById(R.id.post_preview_web_view);
        mWebView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getActivity() != null) {
                    loadPost();
                }
                mWebView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });

        return rootView;
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
    private class LoadPostPreviewTask extends AsyncTask<Void, Void, Spanned> {
        private PostModel mPost;

        @Override
        protected Spanned doInBackground(Void... params) {
            Spanned contentSpannable;

            if (getActivity() == null) {
                return null;
            }

            mPost = mPostStore.getPostByLocalPostId(mLocalPostId);
            if (mPost == null) {
                return null;
            }

            String postTitle = "<h1>" + mPost.getTitle() + "</h1>";
            String postContent = postTitle + mPost.getContent();

            String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" "
                              + "type=\"text/css\" href=\"webview.css\" /></head><body><div "
                              + "id=\"container\">%s</div></body></html>";
            htmlText = String.format(htmlText, StringUtils.addPTags(postContent));
            contentSpannable = new SpannableString(htmlText);

            return contentSpannable;
        }

        @Override
        protected void onPostExecute(Spanned spanned) {
            if (mPost != null && spanned != null) {
                mWebView.loadDataWithBaseURL("file:///android_asset/", spanned.toString(),
                        "text/html", "utf-8", null);
            }

            mLoadTask = null;
        }
    }
}
