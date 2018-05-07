package org.wordpress.android.ui.reader;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.reader.views.ReaderWebView;
import org.wordpress.android.util.NetworkUtils;

/**
 * Fragment responsible for caching post content into WebView.
 * Caching happens on UI thread, so any configuration change will restart it from scratch.
 */
public class ReaderPostWebViewCachingFragment extends Fragment {
    private static final String ARG_BLOG_ID = "blog_id";
    private static final String ARG_POST_ID = "post_id";

    private long mBlogId;
    private long mPostId;

    public static ReaderPostWebViewCachingFragment newInstance(long blogId, long postId) {
        ReaderPostWebViewCachingFragment fragment = new ReaderPostWebViewCachingFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_BLOG_ID, blogId);
        args.putLong(ARG_POST_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        mBlogId = getArguments().getLong(ARG_BLOG_ID);
        mPostId = getArguments().getLong(ARG_POST_ID);
    }

    @Nullable @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return new ReaderWebView(getActivity());
    }

    @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // check network again to detect disconnects during loading + configuration change
        if (NetworkUtils.isNetworkAvailable(view.getContext())) {
            ReaderPost post = ReaderPostTable.getBlogPost(mBlogId, mPostId, false);

            ReaderPostRenderer rendered = new ReaderPostRenderer((ReaderWebView) view, post);

            ((ReaderWebView) view).setWebViewClient(new WebViewClient() {
                // onPageFinished will be called either when page is done loading or resources loading timed out
                public void onPageFinished(WebView view, String url) {
                    selfRemoveFragment();
                }
            });

            rendered.beginRender(); // rendering will cache post content using native WebView implementation.


        } else {
            // abort mission if no network is available
            selfRemoveFragment();
        }
    }

    private void selfRemoveFragment() {
        if (isAdded()) {
            getActivity().getFragmentManager()
                         .beginTransaction()
                         .remove(ReaderPostWebViewCachingFragment.this)
                         .commitAllowingStateLoss(); // we don't care about state here
        }
    }
}
