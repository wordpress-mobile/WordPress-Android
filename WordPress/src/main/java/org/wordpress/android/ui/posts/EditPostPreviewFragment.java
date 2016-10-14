package org.wordpress.android.ui.posts;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPHtml;

public class EditPostPreviewFragment extends Fragment {
    private WebView mWebView;
    private TextView mTextView;
    private LoadPostPreviewTask mLoadTask;

    private SiteModel mSite;
    private PostModel mPost;

    public static EditPostPreviewFragment newInstance(SiteModel site, PostModel post) {
        EditPostPreviewFragment fragment = new EditPostPreviewFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordPress.SITE, site);
        bundle.putSerializable(EditPostActivity.EXTRA_POST, post);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(EditPostActivity.EXTRA_POST, mPost);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateSiteOrFinishActivity(savedInstanceState);
    }

    private void updateSiteOrFinishActivity(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            if (getArguments() != null) {
                mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
                mPost = (PostModel) getArguments().getSerializable(EditPostActivity.EXTRA_POST);
            } else {
                mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
                mPost = (PostModel) getActivity().getIntent().getSerializableExtra(EditPostActivity.EXTRA_POST);
            }
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mPost = (PostModel) savedInstanceState.getSerializable(EditPostActivity.EXTRA_POST);
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.edit_post_preview_fragment, container, false);
        mWebView = (WebView) rootView.findViewById(R.id.post_preview_webview);
        mTextView = (TextView) rootView.findViewById(R.id.post_preview_textview);
        mTextView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getActivity() != null) {
                    loadPost();
                }
                mTextView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null && !mTextView.isLayoutRequested()) {
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

    public void loadPost() {
        if (mLoadTask == null) {
            mLoadTask = new LoadPostPreviewTask();
            mLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    // Load post content in the background
    private class LoadPostPreviewTask extends AsyncTask<Void, Void, Spanned> {
        @Override
        protected Spanned doInBackground(Void... params) {
            Spanned contentSpannable;

            if (getActivity() == null) {
                return null;
            }

            if (mPost == null) {
                return null;
            }

            String postTitle = "<h1>" + mPost.getTitle() + "</h1>";
            String postContent = postTitle + mPost.getContent();

            if (mPost.isLocalDraft()) {
                contentSpannable = WPHtml.fromHtml(
                        postContent.replaceAll("\uFFFC", ""),
                        getActivity(),
                        mPost,
                        Math.min(mTextView.getWidth(), mTextView.getHeight())
                );
            } else {
                String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" " +
                        "type=\"text/css\" href=\"webview.css\" /></head><body><div " +
                        "id=\"container\">%s</div></body></html>";
                htmlText = String.format(htmlText, StringUtils.addPTags(postContent));
                contentSpannable = new SpannableString(htmlText);
            }

            return contentSpannable;
        }

        @Override
        protected void onPostExecute(Spanned spanned) {
            if (mPost != null && spanned != null) {
                if (mPost.isLocalDraft()) {
                    mTextView.setVisibility(View.VISIBLE);
                    mWebView.setVisibility(View.GONE);
                    mTextView.setText(spanned);
                } else {
                    mTextView.setVisibility(View.GONE);
                    mWebView.setVisibility(View.VISIBLE);

                    mWebView.loadDataWithBaseURL("file:///android_asset/", spanned.toString(),
                            "text/html", "utf-8", null);
                }
            }

            mLoadTask = null;
        }
    }
}
