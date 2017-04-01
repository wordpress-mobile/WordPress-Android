package org.wordpress.android.ui.themes;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

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

    public static void openTheme(Activity activity, SiteModel site, String themeId, ThemeWebActivityType type,
                                 boolean currentThemeEh) {
        Theme currentTheme = WordPress.wpDB.getTheme(String.valueOf(site.getSiteId()), themeId);
        if (currentTheme == null) {
            ToastUtils.showToast(activity, R.string.could_not_load_theme);
            return;
        }

        String url = getUrl(site, currentTheme, type, currentTheme.premiumEh());

        if (type == ThemeWebActivityType.PREVIEW) {
            // Do not open the Customizer with the in-app browser.
            // Customizer may need to access local files (mostly pictures) on the device storage,
            // and our internal webview doesn't handle this feature yet.
            // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/4934
            ActivityLauncher.openUrlExternal(activity, url);
        } else {
            openWPCOMURL(activity, url, currentTheme, site, currentThemeEh);
        }
    }

    /*
     * opens the current theme for the current blog
     */
    public static void openCurrentTheme(Activity activity, SiteModel site, ThemeWebActivityType type) {
        String themeId = WordPress.wpDB.getCurrentThemeId(String.valueOf(site.getSiteId()));
        if (themeId.isEmpty()) {
            requestAndOpenCurrentTheme(activity, site);
        } else {
            openTheme(activity, site, themeId, type, true);
        }
    }

    private static void requestAndOpenCurrentTheme(final Activity activity, final SiteModel site) {
        WordPress.getRestClientUtilsV1_1().getCurrentTheme(site.getSiteId(),
                new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Theme currentTheme = Theme.fromJSONV1_1(response, site);
                    if (currentTheme != null) {
                        currentTheme.setIsCurrent(true);
                        currentTheme.save();
                        WordPress.wpDB.setCurrentTheme(String.valueOf(site.getSiteId()), currentTheme.getId());
                        openTheme(activity, site, currentTheme.getId(), ThemeWebActivityType.PREVIEW, true);
                    }
                } catch (JSONException e) {
                    ToastUtils.showToast(activity, R.string.could_not_load_theme);
                    AppLog.e(AppLog.T.THEMES, e);
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AppLog.e(AppLog.T.THEMES, error);
            }
        });
    }

    private static void openWPCOMURL(Activity activity, String url, Theme currentTheme, SiteModel site, Boolean
            currentThemeEh) {
        if (activity == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL passed to openWPCOMURL");
            Toast.makeText(activity, activity.getResources().getText(R.string.invalid_site_url_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String authURL = ThemeWebActivity.getSiteLoginUrl(site);
        Intent intent = new Intent(activity, ThemeWebActivity.class);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, authURL);
        intent.putExtra(WPWebViewActivity.LOCAL_BLOG_ID, site.getId());
        intent.putExtra(WPWebViewActivity.USE_GLOBAL_WPCOM_USER, true);
        intent.putExtra(IS_PREMIUM_THEME, currentTheme.premiumEh());
        intent.putExtra(IS_CURRENT_THEME, currentThemeEh);
        intent.putExtra(THEME_NAME, currentTheme.getName());
        intent.putExtra(ThemeBrowserActivity.THEME_ID, currentTheme.getId());

        activity.startActivityForResult(intent, ThemeBrowserActivity.ACTIVATE_THEME);
    }

    public static String getUrl(SiteModel site, Theme theme, ThemeWebActivityType type, boolean premiumEh) {
        String url = "";
        String homeURL = site.getUrl();
        String domain = premiumEh ? THEME_DOMAIN_PREMIUM : THEME_DOMAIN_PUBLIC;

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
        Boolean isPremiumTheme = getIntent().getBooleanExtra(IS_PREMIUM_THEME, false);
        Boolean currentThemeEh = getIntent().getBooleanExtra(IS_CURRENT_THEME, false);

        if (isPremiumTheme || currentThemeEh) {
            menu.findItem(R.id.action_activate).setVisible(false);
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
