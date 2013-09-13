
package org.wordpress.android.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.passcodelock.AppLockManager;
import org.wordpress.android.models.Blog;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Basic activity for displaying a WebView.
 */
public class DashboardActivity extends SherlockActivity {
    /** Primary webview used to display content. */
    protected WebView mWebView;

    /**
     * Blog for which this activity is loading content.
     */
    protected Blog mBlog;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_PROGRESS);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.webview);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(R.string.view_admin);

        ActionBar ab = getSupportActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ab.setDisplayShowTitleEnabled(true);

        mWebView = (WebView) findViewById(R.id.webView);
        mWebView.getSettings().setUserAgentString(Constants.USER_AGENT);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        Bundle extras = getIntent().getExtras();
        if (extras.containsKey("blogID")) {
            try {
                mBlog = new Blog(extras.getInt("blogID", -1));
            } catch (Exception e) {
                mBlog = WordPress.getCurrentBlog();
            }
        }

        if (mBlog == null) {
            Toast.makeText(this, getResources().getText(R.string.blog_not_found),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        mWebView.setWebViewClient(new WordPressWebViewClient(mBlog));
        mWebView.setWebChromeClient(new WordPressWebChromeClient(this));

        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.getSettings().setSavePassword(false);

        loadDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Friendly reminder that they are in the dashboard :)
        Toast.makeText(this, getString(R.string.dashboard_tip), Toast.LENGTH_LONG).show();
    }

    
    private void loadDashboard() {
        String dashboardUrl = null;
        Gson gson = new Gson();
        Type type = new TypeToken<Map<?, ?>>() {}.getType();
        Map<?, ?> blogOptions = gson.fromJson(mBlog.getBlogOptions(), type);
        if (blogOptions != null) {
            Map<?, ?> homeURLMap = (Map<?, ?>) blogOptions.get("admin_url");
            if (homeURLMap != null)
                dashboardUrl = homeURLMap.get("value").toString();
        }
        // Try to guess the URL of the dashboard if blogOptions is null (blog not added to the app), or WP version is < 3.6 
        if (dashboardUrl == null) {
            if (mBlog.getUrl().lastIndexOf("/") != -1) {
                dashboardUrl = mBlog.getUrl().substring(0, mBlog.getUrl().lastIndexOf("/"))
                + "/wp-admin";
            } else {
                dashboardUrl = mBlog.getUrl().replace("xmlrpc.php", "wp-admin");
            }
        }
        loadAuthenticatedUrl(dashboardUrl);
    }
    
    /**
     * Load the specified URL in the webview.
     *
     * @param url URL to load in the webview.
     */
    protected void loadUrl(String url) {
        mWebView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {

        if (mWebView != null && mWebView.canGoBack())
            mWebView.goBack();
        else
            super.onBackPressed();
    }

    /**
     * Get the URL of the WordPress login page.
     *
     * @return URL of the login page.
     */
    protected String getLoginUrl() {
        if (mBlog.getUrl().lastIndexOf("/") != -1) {
            return mBlog.getUrl().substring(0, mBlog.getUrl().lastIndexOf("/"))
                    + "/wp-login.php";
        } else {
            return mBlog.getUrl().replace("xmlrpc.php", "wp-login.php");
        }
    }

    /**
     * Login to the WordPress blog and load the specified URL.
     *
     * @param url URL to be loaded in the webview.
     */
    protected void loadAuthenticatedUrl(String url) {
        try {
            String postData = String.format("log=%s&pwd=%s&redirect_to=%s",
                    mBlog.getUsername(), mBlog.getPassword(),
                    URLEncoder.encode(url, "UTF-8"));
            mWebView.postUrl(getLoginUrl(), postData.getBytes());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * WebChromeClient that displays "Loading..." title until the content of the webview is fully
     * loaded.
     */
    protected class WordPressWebChromeClient extends WebChromeClient {
        private Context context;

        public WordPressWebChromeClient(Context context) {
            this.context = context;
        }

        public void onProgressChanged(WebView webView, int progress) {
            setTitle(
                    context.getResources().getText(R.string.loading));
            setSupportProgress(progress * 100);

            if (progress == 100) {
                setTitle(webView.getTitle());
            }
        }
    }

    /**
     * WebViewClient that is capable of handling HTTP authentication requests using the HTTP
     * username and password of the blog configured for this activity.
     */
    private class WordPressWebViewClient extends WebViewClient {
        private Blog blog;

        WordPressWebViewClient(Blog blog) {
            super();
            this.blog = blog;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {

        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view,
                                              HttpAuthHandler handler, String host, String realm) {
            handler.proceed(blog.getHttpuser(), blog.getHttppassword());
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mWebView == null)
            return false;

        int itemID = item.getItemId();
        if (itemID == R.id.menu_refresh) {
            mWebView.reload();
            return true;
        } else if (itemID == R.id.menu_browser) {
            String url = mWebView.getUrl();
            if (url != null) {
                Uri uri = Uri.parse(url);
                if (uri != null) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(uri);
                    startActivity(i);
                    AppLockManager.getInstance().setExtendedTimeout();
                }
            }
            return true;
        } else if (itemID == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

}
