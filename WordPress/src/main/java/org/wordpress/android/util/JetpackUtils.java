package org.wordpress.android.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.WPWebViewActivity;

public class JetpackUtils {
    public static void showJetpackStatsModuleAlert(final Activity activity, final SiteModel site) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (site.isSelfHostedAdmin()) {
            builder.setMessage(activity.getString(R.string.jetpack_stats_module_disabled_message))
                    .setTitle(activity.getString(R.string.jetpack_info));
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    String stringToLoad = site.getAdminUrl();
                    String jetpackConnectPageAdminPath = "admin.php?page=jetpack#/engagement";
                    stringToLoad = stringToLoad.endsWith("/") ? stringToLoad + jetpackConnectPageAdminPath :
                            stringToLoad + "/" + jetpackConnectPageAdminPath;
                    String authURL = WPWebViewActivity.getSiteLoginUrl(site);
                    Intent jetpackIntent = new Intent(activity, WPWebViewActivity.class);
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, site.getUsername());
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_PASSWD, site.getPassword());
                    jetpackIntent.putExtra(WPWebViewActivity.URL_TO_LOAD, stringToLoad);
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, authURL);
                    activity.startActivityForResult(jetpackIntent, RequestCodes.REQUEST_JETPACK);
                    // TODO: track analytics here?
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog. Hide current activity.
                    activity.finish();
                }
            });
        } else {
            builder.setMessage(activity.getString(R.string.jetpack_stats_module_disabled_message_not_admin))
                    .setTitle(activity.getString(R.string.jetpack_info));
            builder.setPositiveButton(R.string.yes, null);
        }

        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // User pressed the back key. Hide current activity.
                activity.finish();
            }
        });
        dialog.show();
    }

    public static void showJetpackNonConnectedAlert(final Activity activity, final SiteModel site) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (site.isSelfHostedAdmin()) {
            builder.setMessage(activity.getString(R.string.jetpack_not_connected_message))
                    .setTitle(activity.getString(R.string.jetpack_not_connected));
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    String stringToLoad = site.getAdminUrl();
                    String jetpackConnectPageAdminPath = "admin.php?page=jetpack";
                    stringToLoad = stringToLoad.endsWith("/") ? stringToLoad + jetpackConnectPageAdminPath :
                            stringToLoad + "/" + jetpackConnectPageAdminPath;
                    String authURL = WPWebViewActivity.getSiteLoginUrl(site);
                    Intent jetpackIntent = new Intent(activity, WPWebViewActivity.class);
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, site.getUsername());
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_PASSWD, site.getPassword());
                    jetpackIntent.putExtra(WPWebViewActivity.URL_TO_LOAD, stringToLoad);
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, authURL);
                    activity.startActivityForResult(jetpackIntent, RequestCodes.REQUEST_JETPACK);
                    // TODO: rename STATS_SELECTED_CONNECT_JETPACK to something more generic
                    AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_SELECTED_CONNECT_JETPACK);
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog. Hide current activity.
                    activity.finish();
                }
            });
        } else {
            builder.setMessage(activity.getString(R.string.jetpack_message_not_admin))
                    .setTitle(activity.getString(R.string.jetpack_not_found));
            builder.setPositiveButton(R.string.yes, null);
        }

        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // User pressed the back key. Hide current activity.
                activity.finish();
            }
        });
        dialog.show();
    }

    public static void showInstallJetpackAlert(final Activity activity, final SiteModel site) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (site.isSelfHostedAdmin()) {
            builder.setMessage(activity.getString(R.string.jetpack_message))
                    .setTitle(activity.getString(R.string.jetpack_not_found));
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    String stringToLoad = site.getAdminUrl()
                                          + "plugin-install.php?tab=search&s=jetpack+by+wordpress.com"
                                          + "&plugin-search-input=Search+Plugins";
                    String authURL = WPWebViewActivity.getSiteLoginUrl(site);
                    Intent jetpackIntent = new Intent(activity, WPWebViewActivity.class);
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, site.getUsername());
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_PASSWD, site.getPassword());
                    jetpackIntent.putExtra(WPWebViewActivity.URL_TO_LOAD, stringToLoad);
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, authURL);
                    activity.startActivityForResult(jetpackIntent, RequestCodes.REQUEST_JETPACK);
                    // TODO: rename STATS_SELECTED_INSTALL_JETPACK to something more generic
                    AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_SELECTED_INSTALL_JETPACK);
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog. Hide current activity.
                    activity.finish();
                }
            });
        } else {
            builder.setMessage(activity.getString(R.string.jetpack_message_not_admin))
                    .setTitle(activity.getString(R.string.jetpack_not_found));
            builder.setPositiveButton(R.string.yes, null);
        }

        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // User pressed the back key. Hide current activity.
                activity.finish();
            }
        });
        dialog.show();
    }
}
