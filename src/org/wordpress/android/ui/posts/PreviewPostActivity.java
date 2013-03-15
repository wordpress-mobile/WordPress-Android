package org.wordpress.android.ui.posts;

import java.lang.reflect.Type;
import java.util.Map;

import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.google.gson.reflect.TypeToken;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.AuthenticatedWebViewActivity;
import org.wordpress.android.util.EscapeUtils;

/**
 * Activity for previewing a post or page in a webview. Currently this activity can only preview the
 * {@link WordPress.currentPost}.
 */
public class PreviewPostActivity extends AuthenticatedWebViewActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isPage = getIntent().getBooleanExtra("isPage", false);
        if (isPage) {
            this.setTitle(EscapeUtils.unescapeHtml(WordPress.currentBlog.getBlogName())
                    + " - " + getResources().getText(R.string.preview_page));
        } else {
            this.setTitle(EscapeUtils.unescapeHtml(WordPress.currentBlog.getBlogName())
                    + " - " + getResources().getText(R.string.preview_post));
        }

        mWebView.setWebChromeClient(new WordPressWebChromeClient(this));
        mWebView.getSettings().setJavaScriptEnabled(true);

        loadPostPreview(WordPress.currentPost);
    }

    /**
     * Load the post preview. If the post is in a non-public state (e.g. draft status, part of a
     * non-public blog, etc), load the preview as an authenticated URL. Otherwise, just load the
     * preview normally.
     * 
     * @param post Post to load the preview for.
     */
    private void loadPostPreview(Post post) {
        if (post != null) {
            String url = post.getPermaLink();
            boolean isPrivate = false;
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Object>>() {
                }.getType();
                Map<String, Object> blogOptions = gson.fromJson(
                        mBlog.getBlogOptions(), type);
                StringMap<?> blogPublicOption = (StringMap<?>) blogOptions.get("blog_public");
                String blogPublicOptionValue = blogPublicOption.get("value").toString();
                if (blogPublicOptionValue.equals("-1")) {
                    isPrivate = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (isPrivate || post.isLocalDraft() || post.isLocalChange()
                    || !post.getPost_status().equals("publish")) {
                if (-1 == url.indexOf('?')) {
                    url = url.concat("?preview=true");
                } else {
                    url = url.concat("&preview=true");
                }
                loadAuthenticatedUrl(url);
            } else {
                loadUrl(url);
            }
        }
    }
}
