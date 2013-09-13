
package org.wordpress.android.ui;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.passcodelock.AppLockManager;
import org.wordpress.android.models.Blog;

/**
 * Activity for displaying WordPress content in a webview which may require authentication.
 * Currently, this activity can only load content for the {@link WordPress.currentBlog}.
 */
public class AuthenticatedWebViewActivity extends WebViewActivity {

    /**
     * Blog for which this activity is loading content.
     */
    protected Blog mBlog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBlog = WordPress.getCurrentBlog();
        if (mBlog == null) {
            Toast.makeText(this, getResources().getText(R.string.blog_not_found),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        mWebView.setWebViewClient(new WordPressWebViewClient(mBlog));

        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.getSettings().setSavePassword(false);
    }

    /**
     * Get the URL of the WordPress login page.
     * 
     * @return URL of the login page.
     */
    protected String getLoginUrl() {
        String loginURL = null;
        Gson gson = new Gson();
        Type type = new TypeToken<Map<?, ?>>() {}.getType();
        Map<?, ?> blogOptions = gson.fromJson(mBlog.getBlogOptions(), type);
        if (blogOptions != null) {
            Map<?, ?> homeURLMap = (Map<?, ?>) blogOptions.get("login_url");
            if (homeURLMap != null)
                loginURL = homeURLMap.get("value").toString();
        }
        // Try to guess the login URL if blogOptions is null (blog not added to the app), or WP version is < 3.6
        if( loginURL == null ) {
            if (mBlog.getUrl().lastIndexOf("/") != -1) {
                return mBlog.getUrl().substring(0, mBlog.getUrl().lastIndexOf("/"))
                        + "/wp-login.php";
            } else {
                return mBlog.getUrl().replace("xmlrpc.php", "wp-login.php");
            }
        }
        
        return loginURL;
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
            // Found a bug on some pages where there is an incorrect
            // auto-redirect to file:///android_asset/webkit/.
            if ( !url.equals("file:///android_asset/webkit/") ) {
                view.loadUrl(url);
            }
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
            AuthenticatedWebViewActivity.this.setTitle(
                    context.getResources().getText(R.string.loading));
            AuthenticatedWebViewActivity.this.setSupportProgress(progress * 100);

            if (progress == 100) {
                setTitle(webView.getTitle());
            }
        } 
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.webview, menu);
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
        } else if (itemID == R.id.menu_share) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            startActivity(Intent.createChooser(share, getResources().getText(R.string.share_link)));
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
        }

        return super.onOptionsItemSelected(item);
    }
}
