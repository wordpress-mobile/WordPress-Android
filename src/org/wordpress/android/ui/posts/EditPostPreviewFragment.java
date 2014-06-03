package org.wordpress.android.ui.posts;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPHtml;

public class EditPostPreviewFragment extends Fragment {
    EditPostActivity mActivity;
    WebView mWebView;
    TextView mTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActivity = (EditPostActivity)getActivity();

        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_edit_post_preview, container, false);
        mWebView = (WebView) rootView.findViewById(R.id.post_preview_webview);
        mTextView = (TextView) rootView.findViewById(R.id.post_preview_textview);

        return rootView;
    }

    public void loadPost(Post post) {
        // Don't load if the Post object is null, see #395
        if (post == null)
            return;

        String postTitle = "<h1>" + post.getTitle() + "</h1>";
        String postContent = postTitle + post.getDescription() + "\n\n" + post.getMoreText();

        if (post.isLocalDraft()) {
            mTextView.setVisibility(View.VISIBLE);
            mWebView.setVisibility(View.GONE);
            mTextView.setText(
                    WPHtml.fromHtml(
                            postContent.replaceAll("\uFFFC", ""),
                            getActivity(),
                            post,
                            mTextView.getWidth()
                    )
            );
        } else {
            mTextView.setVisibility(View.GONE);
            mWebView.setVisibility(View.VISIBLE);

            String htmlText = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head><body><div id=\"container\">%s</div></body></html>";
            htmlText = String.format(htmlText, StringUtils.addPTags(postContent));
            mWebView.loadDataWithBaseURL("file:///android_asset/", htmlText,
                    "text/html", "utf-8", null);
        }

    }
}
