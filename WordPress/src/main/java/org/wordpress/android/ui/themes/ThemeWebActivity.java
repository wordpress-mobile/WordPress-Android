package org.wordpress.android.ui.themes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.WPMeShortlinks;

public class ThemeWebActivity extends WPWebViewActivity {
    public static final String IS_CURRENT_THEME = "is_current_theme";
    public static final String IS_PREMIUM_THEME = "is_premium_theme";
    public static final String THEME_NAME = "theme_name";
    private static final String THEME_URL_PREVIEW = "%s/?theme=pub/%s&hide_banners=true";
    private static final String THEME_URL_CUSTOMIZE = "https://wordpress.com/customize/%s?nomuse=1&theme=pub/%s";
    private static final String THEME_URL_SUPPORT = "https://theme.wordpress.com/themes/%s/support";
    private static final String THEME_URL_DETAILS = "https://wordpress.com/themes/%s/%s";

    public enum ThemeWebActivityType {
        PREVIEW,
        DEMO,
        DETAILS,
        SUPPORT,
        CUSTOMIZE,
    }

    public static void openTheme(Context context, String themeId, ThemeWebActivityType type, boolean isCurrentTheme) {
        String blogId = WordPress.getCurrentBlog().getDotComBlogId();
        Theme currentTheme = WordPress.wpDB.getTheme(blogId, themeId);
        String url = getUrl(context, currentTheme, blogId, type);

        openWPCOMURL(context, url, currentTheme, WordPress.getCurrentBlog(), isCurrentTheme);
    }

    private static void openWPCOMURL(Context context, String url, Theme currentTheme, Blog blog, Boolean isCurrentTheme) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL passed to openUrlByUsingMainWPCOMCredentials");
            Toast.makeText(context, context.getResources().getText(R.string.invalid_url_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String authURL = ThemeWebActivity.getBlogLoginUrl(blog);
        Intent intent = new Intent(context, ThemeWebActivity.class);
        intent.putExtra(ThemeWebActivity.AUTHENTICATION_USER, blog.getUsername());
        intent.putExtra(ThemeWebActivity.AUTHENTICATION_PASSWD, blog.getPassword());
        intent.putExtra(ThemeWebActivity.URL_TO_LOAD, url);
        intent.putExtra(ThemeWebActivity.AUTHENTICATION_URL, authURL);
        intent.putExtra(ThemeWebActivity.LOCAL_BLOG_ID, blog.getLocalTableBlogId());
        intent.putExtra(IS_PREMIUM_THEME, currentTheme.isPremium());
        intent.putExtra(IS_CURRENT_THEME, isCurrentTheme);
        intent.putExtra(THEME_NAME, currentTheme.getName());

        context.startActivity(intent);
    }

    public static String getUrl(Context context, Theme theme, String blogId, ThemeWebActivityType type) {
        String url = "";
        String homeURL = WordPress.getCurrentBlog().getHomeURL();

        switch (type) {
            case PREVIEW:
                url = String.format(THEME_URL_PREVIEW, homeURL, theme.getId());
                break;
            case DEMO:
                url = theme.getDemoURI();
                break;
            case DETAILS:
                String currentURL = homeURL.replaceFirst(context.getString(R.string.theme_https_prefix), "");
                url = String.format(THEME_URL_DETAILS, currentURL, theme.getId());
                break;
            case SUPPORT:
                url = String.format(THEME_URL_SUPPORT, theme.getId());
                break;
            case CUSTOMIZE:
                url = String.format(THEME_URL_CUSTOMIZE, blogId, theme.getId());
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
    public void configureView() {
        setContentView(R.layout.theme_web_activity);
        setActionBarTitleToThemeName();
    }

    private void setActionBarTitleToThemeName() {
        Bundle bundle = getIntent().getExtras();
        String themeName = bundle.getString(THEME_NAME);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(themeName);
        }
    }
}
