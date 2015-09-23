package org.wordpress.android.ui.themes;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Theme;

public class ThemeWebActivity extends AppCompatActivity {

    private static final String THEME_URL_CUSTOMIZE = "https://wordpress.com/customize/%s?nomuse=1&theme=pub/%s";
    private static String THEME_URL_SUPPORT = "https://theme.wordpress.com/themes/%s/support/";

    public enum ThemeWebActivityType {
        PREVIEW,
        DEMO,
        DETAILS,
        SUPPORT,
        CUSTOMIZE,
    }

    private String mThemeId;
    private Theme mCurrentTheme;
    private String mBlogId;
    private ThemeWebActivityType mType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_web_activity);
        setCurrentThemeAndThemeId();
        getSupportActionBar().setTitle(mCurrentTheme.getName());
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

    private void setCurrentThemeAndThemeId() {
        mThemeId = getIntent().getStringExtra(ThemeBrowserActivity.THEME_ID);

        mBlogId = WordPress.getCurrentBlog().getDotComBlogId();
        mCurrentTheme = WordPress.wpDB.getTheme(mBlogId, mThemeId);

        int typeValue = getIntent().getIntExtra(ThemeBrowserActivity.THEME_WEB_MODE, 0);
        mType = ThemeWebActivityType.values()[typeValue];
    }

    private void loadThemeUrlInWebView() {
        WebView webView = (WebView) findViewById(R.id.webView);
        String url = "";

        switch (mType) {
            case PREVIEW:
                String currentURL = WordPress.getCurrentBlog().getHomeURL();
                currentURL = currentURL.replaceFirst(getString(R.string.theme_https_prefix), "");
                url = String.format(getString(R.string.theme_preview_url), currentURL, mThemeId);
                break;
            case DEMO:
                url = mCurrentTheme.getDemoURI();
                break;
            case DETAILS: // details
                break;
            case SUPPORT:
                url = String.format(THEME_URL_SUPPORT, mThemeId);
                break;
            case CUSTOMIZE:
                url = String.format(THEME_URL_CUSTOMIZE, mBlogId, mThemeId);
                break;
            default:
                break;
        }

        webView.loadUrl(url);
    }

    public String getThemeId() {
        return mThemeId;
    }
}
