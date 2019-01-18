package org.wordpress.android.ui.themes;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;

public class ThemeWebActivity extends WPWebViewActivity {
    public static final String IS_CURRENT_THEME = "is_current_theme";
    public static final String IS_PREMIUM_THEME = "is_premium_theme";
    public static final String THEME_NAME = "theme_name";
    public static final String THEME_HTTP_PREFIX = "http";
    public static final String THEME_HTTPS_PROTOCOL = "https://";

    private static final String THEME_DOMAIN_PUBLIC = "pub";
    private static final String THEME_DOMAIN_PREMIUM = "premium";
    private static final String THEME_URL_PREVIEW = "https://wordpress.com/customize/%s?theme=%s/%s&hide_close=true";
    private static final String THEME_URL_SUPPORT = "https://wordpress.com/theme/%s/support/?preview=true&iframe=true";
    private static final String THEME_URL_DETAILS = "https://wordpress.com/theme/%s/?preview=true&iframe=true";
    private static final String THEME_URL_DEMO_PARAMETER = "demo=true&iframe=true&theme_preview=true";

    enum ThemeWebActivityType {
        PREVIEW,
        DEMO,
        DETAILS,
        SUPPORT
    }

    public static String getSiteLoginUrl(SiteModel site) {
        if (site.isJetpackConnected()) {
            return WPCOM_LOGIN_URL;
        }
        return WPWebViewActivity.getSiteLoginUrl(site);
    }

    public static void openTheme(Activity activity, SiteModel site, ThemeModel theme, ThemeWebActivityType type) {
        String url = getUrl(site, theme, type, !theme.isFree());
        if (TextUtils.isEmpty(url)) {
            ToastUtils.showToast(activity, R.string.could_not_load_theme);
            return;
        }
        if (type == ThemeWebActivityType.PREVIEW || !theme.isWpComTheme()) {
            // Do not open the Customizer with the in-app browser.
            // Customizer may need to access local files (mostly pictures) on the device storage,
            // and our internal webview doesn't handle this feature yet.
            // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/4934
            ActivityLauncher.openUrlExternal(activity, url);
        } else {
            openWPCOMURL(activity, url, theme, site);
        }
    }

    private static void openWPCOMURL(Activity activity, String url, ThemeModel theme, SiteModel site) {
        if (activity == null) {
            AppLog.e(AppLog.T.UTILS, "ThemeWebActivity requires a non-null activity");
            return;
        } else if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "ThemeWebActivity requires non-empty URL");
            ToastUtils.showToast(activity, R.string.invalid_site_url_message, ToastUtils.Duration.SHORT);
            return;
        }

        String authURL = ThemeWebActivity.getSiteLoginUrl(site);
        Intent intent = new Intent(activity, ThemeWebActivity.class);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, authURL);
        intent.putExtra(WPWebViewActivity.LOCAL_BLOG_ID, site.getId());
        intent.putExtra(WPWebViewActivity.USE_GLOBAL_WPCOM_USER, true);
        intent.putExtra(IS_PREMIUM_THEME, !theme.isFree());
        intent.putExtra(IS_CURRENT_THEME, theme.getActive());
        intent.putExtra(THEME_NAME, theme.getName());
        intent.putExtra(ThemeBrowserActivity.THEME_ID, theme.getThemeId());
        activity.startActivityForResult(intent, ThemeBrowserActivity.ACTIVATE_THEME);
    }

    public static String getUrl(SiteModel site, ThemeModel theme, ThemeWebActivityType type, boolean isPremium) {
        if (theme.isWpComTheme()) {
            switch (type) {
                case PREVIEW:
                    String domain = isPremium ? THEME_DOMAIN_PREMIUM : THEME_DOMAIN_PUBLIC;
                    return String
                            .format(THEME_URL_PREVIEW, UrlUtils.getHost(site.getUrl()), domain, theme.getThemeId());
                case DEMO:
                    String url = theme.getDemoUrl();
                    if (url.contains("?")) {
                        return url + "&" + THEME_URL_DEMO_PARAMETER;
                    } else {
                        return url + "?" + THEME_URL_DEMO_PARAMETER;
                    }
                case DETAILS:
                    return String.format(THEME_URL_DETAILS, theme.getThemeId());
                case SUPPORT:
                    return String.format(THEME_URL_SUPPORT, theme.getThemeId());
            }
        } else {
            switch (type) {
                case PREVIEW:
                    return site.getAdminUrl() + "customize.php?theme=" + theme.getThemeId();
                case DEMO:
                    return site.getAdminUrl() + "themes.php?theme=" + theme.getThemeId();
                case DETAILS:
                case SUPPORT:
                    return theme.getThemeUrl();
            }
        }
        return "";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (shouldShowActivateMenuItem()) {
            getMenuInflater().inflate(R.menu.theme_web, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_activate) {
            Intent returnIntent = new Intent();
            setResult(RESULT_OK, returnIntent);
            returnIntent.putExtra(ThemeBrowserActivity.THEME_ID,
                                  getIntent().getStringExtra(ThemeBrowserActivity.THEME_ID));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    /**
     * Show Activate in the Action Bar menu if the theme is free and not the current theme.
     */
    private boolean shouldShowActivateMenuItem() {
        Boolean isPremiumTheme = getIntent().getBooleanExtra(IS_PREMIUM_THEME, false);
        Boolean isCurrentTheme = getIntent().getBooleanExtra(IS_CURRENT_THEME, false);
        return !isCurrentTheme && !isPremiumTheme;
    }
}
