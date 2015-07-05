package org.wordpress.android.ui.posts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPWebViewClient;

public class PostPreviewActivity extends AppCompatActivity {

    public static final String ARG_LOCAL_POST_ID = "local_post_id";
    public static final String ARG_LOCAL_BLOG_ID = "local_blog_id";
    public static final String ARG_IS_PAGE = "is_page";

    private WebView mWebView;
    private long mLocalPostId;
    private int mLocalBlogId;
    private boolean mIsPage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_preview_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        mWebView = (WebView) findViewById(R.id.webView);

        if (savedInstanceState != null) {
            mLocalPostId = savedInstanceState.getLong(ARG_LOCAL_POST_ID);
            mLocalBlogId = savedInstanceState.getInt(ARG_LOCAL_BLOG_ID);
            mIsPage = savedInstanceState.getBoolean(ARG_IS_PAGE);
        } else {
            mLocalPostId = getIntent().getLongExtra(ARG_LOCAL_POST_ID, 0);
            mLocalBlogId = getIntent().getIntExtra(ARG_LOCAL_BLOG_ID, 0);
            mIsPage = getIntent().getBooleanExtra(ARG_IS_PAGE, false);
        }

        Blog blog = WordPress.wpDB.instantiateBlogByLocalId(mLocalBlogId);
        mWebView.setWebViewClient(new WPWebViewClient(blog));

        loadPost();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(ARG_LOCAL_POST_ID, mLocalPostId);
        outState.putInt(ARG_LOCAL_BLOG_ID, mLocalBlogId);
        outState.putBoolean(ARG_IS_PAGE, mIsPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.post_preview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_edit) {
            ActivityLauncher.editBlogPostOrPageForResult(this, mLocalPostId, mIsPage);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.EDIT_POST) {
            loadPost();
        }
    }

    private void loadPost() {
        if (isFinishing()) {
            return;
        }

        final Post post = WordPress.wpDB.getPostForLocalTablePostId(mLocalPostId);
        if (post == null) {
            ToastUtils.showToast(this, R.string.post_not_found);
            finish();
            return;
        }

        mWebView.loadDataWithBaseURL(
                null,
                formatPostContentForWebView(post),
                "text/html",
                "utf-8",
                null);
    }

    private String formatPostContentForWebView(Post post) {
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

        String textColorStr = HtmlUtils.colorResToHtmlColor(this, R.color.grey_dark);
        String linkColorStr = HtmlUtils.colorResToHtmlColor(this, R.color.reader_hyperlink);

        int contentMargin = getResources().getDimensionPixelSize(R.dimen.content_margin);
        String marginStr =  Integer.toString(contentMargin) + "px";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8' />"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<link rel='stylesheet' type='text/css' href='http://fonts.googleapis.com/css?family=Merriweather:300,700' />"
                + "<style type='text/css'>"
                + "  html { margin-left: " + marginStr + "; margin-right: " + marginStr + "; }"
                + "  body { font-family: Merriweather, serif; font-weight: 300; padding: 0px; width: 100%; color: " + textColorStr + "; }"
                + "  body, p, div { max-width: 100% !important; word-wrap: break-word; }"
                + "  p, div { line-height: 1.6em; font-size: 0.95em; }"
                + "  h1 { font-size: 1.2em; font-family: Merriweather, serif; font-weight: 700; }"
                + "  img { max-width: 100%; }"
                + "  a { text-decoration: none; color: " + linkColorStr + "; }"
                + "</style></head><body>"
                + "<h1>" + title + "</h1>"
                + StringUtils.addPTags(postContent)
                + "</body></html>";
    }
}
