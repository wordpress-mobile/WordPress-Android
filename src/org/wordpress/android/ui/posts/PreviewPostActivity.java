package org.wordpress.android.ui.posts;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

import android.os.Bundle;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;
import com.google.gson.reflect.TypeToken;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.AuthenticatedWebViewActivity;
import org.wordpress.android.util.StringUtils;

/**
 * Activity for previewing a post or page in a webview.
 */
public class PreviewPostActivity extends AuthenticatedWebViewActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        
        boolean isPage = getIntent().getBooleanExtra("isPage", false);
        if (isPage) {
            this.setTitle(StringUtils.unescapeHTML(WordPress.currentBlog.getBlogName())
                    + " - " + getResources().getText(R.string.preview_page));
        } else {
            this.setTitle(StringUtils.unescapeHTML(WordPress.currentBlog.getBlogName())
                    + " - " + getResources().getText(R.string.preview_post));
        }

        mWebView.setWebChromeClient(new WordPressWebChromeClient(this));
        mWebView.getSettings().setJavaScriptEnabled(true);
        
        if (extras != null) {
            long mPostID = extras.getLong("postID");
            int mBlogID = extras.getInt("blogID");

            Post post = new Post(mBlogID, mPostID, isPage);

            if (post.getId() < 0)
                Toast.makeText(this, R.string.post_not_found, Toast.LENGTH_SHORT).show();
            else
                loadPostPreview(post);
        } else if (WordPress.currentPost != null) {
            loadPostPreview(WordPress.currentPost);
        }
        else {
            Toast.makeText(this, R.string.post_not_found, Toast.LENGTH_SHORT).show();
        }
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
            
            Date d = new Date();           
            if ( isPrivate //blog private 
                    || post.isLocalDraft() 
                    || post.isLocalChange()
                    || post.getDate_created_gmt() > d.getTime() //Scheduled
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
