package org.wordpress.android.ui.themes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Theme;

public class ThemeWebActivity extends AppCompatActivity {
    private String mThemeId;
    private String mBlogId;
    private int mThemeMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_web_activity);
        loadThemeUrlInWebView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.theme_web, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_activate) {
            Intent intent = new Intent();
            intent.putExtra(ThemeBrowserActivity.THEME_ID, mThemeId);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void loadThemeUrlInWebView() {
        WebView webView = (WebView) findViewById(R.id.webView);
        Intent intent = getIntent();
        mThemeId = intent.getStringExtra(ThemeBrowserActivity.THEME_ID);
        String currentBlog = intent.getStringExtra(ThemeBrowserActivity.BLOG_ID);
        int type = intent.getIntExtra(ThemeBrowserActivity.THEME_WEB_MODE, 0);
        String url = String.format("https://theme.wordpress.com/themes/%s/", mThemeId);
        if (type == 0) {
            url = url + "support/";
        } else if (type == 2) {
            url = String.format("https://wordpress.com/customize/%s?nomuse=1&theme=pub/%s", currentBlog, mThemeId);
        } else if (type == 3) {
            Theme currentTheme = WordPress.wpDB.getTheme(currentBlog, mThemeId);
            url = currentTheme.getDemoURI();
        }

        webView.loadUrl(url);
    }

    public String getThemeId() {
        return mThemeId;
    }
}
