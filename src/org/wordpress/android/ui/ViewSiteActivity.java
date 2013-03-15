
package org.wordpress.android.ui;

import java.lang.reflect.Type;
import java.util.Map;

import android.os.Bundle;
import android.view.Window;

import com.actionbarsherlock.app.ActionBar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

/**
 * Activity to view the WordPress blog in a WebView
 */
public class ViewSiteActivity extends AuthenticatedWebViewActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createMenuDrawer(this.findViewById(R.id.webview_wrapper));

        this.setTitle(getResources().getText(R.string.view_site));

        // configure webview
        mWebView.setWebChromeClient(new WordPressWebChromeClient(this));
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setPluginsEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);

        String siteURL = null;
        Gson gson = new Gson();
        Type type = new TypeToken<Map<?, ?>>() {}.getType();
        Map<?, ?> blogOptions = gson.fromJson(mBlog.getBlogOptions(), type);
        if (blogOptions != null) {
            Map<?, ?> homeURLMap = (Map<?, ?>) blogOptions.get("home_url");
            if (homeURLMap != null)
                siteURL = homeURLMap.get("value").toString();
        }
        // load dashboard
        if (siteURL == null) {
            siteURL = mBlog.getUrl().replace("/xmlrpc.php", "");
        }
        loadAuthenticatedUrl(siteURL);
    }

}
