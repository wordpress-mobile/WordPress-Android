package org.wordpress.android.ui.themes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.util.AppLog;

public class ThemeWebActivity extends WPWebViewActivity {
    private static final String THEME_URL_CUSTOMIZE = "https://wordpress.com/customize/%s?nomuse=1&theme=pub/%s";
    private static String THEME_URL_SUPPORT = "https://theme.wordpress.com/themes/%s/support/";

    public enum ThemeWebActivityType {
        PREVIEW,
        DEMO,
        DETAILS,
        SUPPORT,
        CUSTOMIZE,
    }

    public static void openTheme(Context context, String themeId, ThemeWebActivityType type) {
        String blogId = WordPress.getCurrentBlog().getDotComBlogId();
        Theme currentTheme = WordPress.wpDB.getTheme(blogId, themeId);
        String url = getUrl(context, currentTheme, blogId, type);

        openURL(context, url, currentTheme);
    }

    private static void openURL(Context context, String url, Theme currentTheme) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL");
            Toast.makeText(context, context.getResources().getText(R.string.invalid_url_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, ThemeWebActivity.class);
        intent.putExtra(ThemeWebActivity.URL_TO_LOAD, url);
        intent.putExtra("isPremium", currentTheme.isPremium());
        context.startActivity(intent);
    }

    public static String getUrl(Context context, Theme theme, String blogId, ThemeWebActivityType type) {
        String url = "";

        switch (type) {
            case PREVIEW:
                String currentURL = WordPress.getCurrentBlog().getHomeURL();
                currentURL = currentURL.replaceFirst(context.getString(R.string.theme_https_prefix), "");
                url = String.format(context.getString(R.string.theme_preview_url), currentURL, theme.getId());
                break;
            case DEMO:
                url = theme.getDemoURI();
                break;
            case DETAILS: // details
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
        Boolean isPremiumTheme = bundle.getBoolean("isPremium", false);

        if (isPremiumTheme) {
            menu.findItem(R.id.action_settings).setVisible(false);
        }

        return true;
    }

    @Override
    public void configureView() {
        setContentView(R.layout.theme_web_activity);
    }
}
