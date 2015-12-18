package org.wordpress.android.ui.themes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.util.AppLog;

public class ThemeWebActivity extends WPWebViewActivity {
    public static final String IS_CURRENT_THEME = "is_current_theme";
    public static final String IS_PREMIUM_THEME = "is_premium_theme";
    public static final String THEME_NAME = "theme_name";
    private static final String THEME_DOMAIN_PUBLIC = "pub";
    private static final String THEME_DOMAIN_PREMIUM = "premium";
    private static final String THEME_URL_PREVIEW = "%s/wp-admin/customize.php?theme=%s/%s&hide_close=true";
    private static final String THEME_URL_SUPPORT = "https://wordpress.com/themes/%s/support/?preview=true&iframe=true";
    private static final String THEME_URL_DETAILS = "https://wordpress.com/themes/%s/%s/?preview=true&iframe=true";
    private static final String THEME_URL_DEMO_PARAMETER = "demo=true&iframe=true&theme_preview=true";
    private static final String THEME_HTTPS_PREFIX = "https://";

    public enum ThemeWebActivityType {
        PREVIEW,
        DEMO,
        DETAILS,
        SUPPORT
    }

    public static void openTheme(Activity activity, String themeId, ThemeWebActivityType type, boolean isCurrentTheme) {
        String blogId = WordPress.getCurrentBlog().getDotComBlogId();
        Theme currentTheme = WordPress.wpDB.getTheme(blogId, themeId);
        String url = getUrl(currentTheme, type, currentTheme.isPremium());

        openWPCOMURL(activity, url, currentTheme, WordPress.getCurrentBlog(), isCurrentTheme);
    }

    private static void openWPCOMURL(Activity activity, String url, Theme currentTheme, Blog blog, Boolean isCurrentTheme) {
        if (activity == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL passed to openWPCOMURL");
            Toast.makeText(activity, activity.getResources().getText(R.string.invalid_url_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String authURL = ThemeWebActivity.getBlogLoginUrl(blog);
        Intent intent = new Intent(activity, ThemeWebActivity.class);
        intent.putExtra(ThemeWebActivity.AUTHENTICATION_USER, blog.getUsername());
        intent.putExtra(ThemeWebActivity.AUTHENTICATION_PASSWD, blog.getPassword());
        intent.putExtra(ThemeWebActivity.URL_TO_LOAD, url);
        intent.putExtra(ThemeWebActivity.AUTHENTICATION_URL, authURL);
        intent.putExtra(ThemeWebActivity.LOCAL_BLOG_ID, blog.getLocalTableBlogId());
        intent.putExtra(IS_PREMIUM_THEME, currentTheme.isPremium());
        intent.putExtra(IS_CURRENT_THEME, isCurrentTheme);
        intent.putExtra(THEME_NAME, currentTheme.getName());
        intent.putExtra(ThemeBrowserActivity.THEME_ID, currentTheme.getId());

        activity.startActivityForResult(intent, ThemeBrowserActivity.ACTIVATE_THEME);
    }

    public static String getUrl(Theme theme, ThemeWebActivityType type, boolean isPremium) {
        String url = "";
        String homeURL = WordPress.getCurrentBlog().getHomeURL();
        String domain = isPremium ? THEME_DOMAIN_PREMIUM : THEME_DOMAIN_PUBLIC;

        switch (type) {
            case PREVIEW:
                url = String.format(THEME_URL_PREVIEW, homeURL, domain, theme.getId());
                break;
            case DEMO:
                url = theme.getDemoURI();
                if (url.contains("?")) {
                    url = url + "&" + THEME_URL_DEMO_PARAMETER;
                } else {
                    url = url + "?" + THEME_URL_DEMO_PARAMETER;
                }
                break;
            case DETAILS:
                String currentURL = homeURL.replaceFirst(THEME_HTTPS_PREFIX, "");
                url = String.format(THEME_URL_DETAILS, currentURL, theme.getId());
                break;
            case SUPPORT:
                url = String.format(THEME_URL_SUPPORT, theme.getId());
                break;
            default:
                break;
        }

        return url;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.theme_web, menu);

        Bundle bundle = getIntent().getExtras();
        Boolean isPremiumTheme = bundle.getBoolean(IS_PREMIUM_THEME, false);
        Boolean isCurrentTheme = bundle.getBoolean(IS_CURRENT_THEME, false);

        if (isPremiumTheme || isCurrentTheme) {
            menu.findItem(R.id.action_activate).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_activate) {
            Bundle bundle = getIntent().getExtras();
            Intent returnIntent = new Intent();
            setResult(RESULT_OK, returnIntent);
            returnIntent.putExtra(ThemeBrowserActivity.THEME_ID, bundle.getString(ThemeBrowserActivity.THEME_ID));
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void configureView() {
        setContentView(R.layout.theme_web_activity);
        setActionBarTitleToThemeName();
    }

    private void setActionBarTitleToThemeName() {
        String themeName = getIntent().getStringExtra(THEME_NAME);
        if (getSupportActionBar() != null && themeName != null) {
            getSupportActionBar().setTitle(themeName);
        }
    }
}
