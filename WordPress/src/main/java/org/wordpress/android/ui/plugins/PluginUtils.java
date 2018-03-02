package org.wordpress.android.ui.plugins;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.helpers.Version;

public class PluginUtils {
    public static boolean isPluginFeatureAvailable(SiteModel site) {
        return SiteUtils.checkMinimalJetpackVersion(site, "5.6");
    }

    public static WPOrgPluginModel getWPOrgPlugin(@NonNull PluginStore pluginStore, @NonNull SitePluginModel plugin) {
        String slug = plugin.getSlug();
        if (TextUtils.isEmpty(slug)) {
            return null;
        }
        return pluginStore.getWPOrgPluginBySlug(slug);
    }

    static int getAverageStarRating(@NonNull WPOrgPluginModel wpOrgPlugin) {
        int rating = StringUtils.stringToInt(wpOrgPlugin.getRating(), 1);
        return Math.round(rating / 20f);
    }

    static boolean isUpdateAvailable(SitePluginModel plugin, WPOrgPluginModel wpOrgPlugin) {
        if (wpOrgPlugin == null
                || TextUtils.isEmpty(plugin.getVersion())
                || TextUtils.isEmpty(wpOrgPlugin.getVersion())) {
            return false;
        }
        try {
            Version currentVersion = new Version(plugin.getVersion());
            Version availableVersion = new Version(wpOrgPlugin.getVersion());
            return currentVersion.compareTo(availableVersion) == -1;
        } catch (IllegalArgumentException e) {
            String errorStr = String.format("An IllegalArgumentException occurred while trying to compare site" +
                    " plugin version: %s with wporg plugin version: %s", plugin.getVersion(), wpOrgPlugin.getVersion());
            AppLog.e(AppLog.T.PLUGINS, errorStr, e);
            CrashlyticsUtils.logException(e, AppLog.T.PLUGINS, errorStr);
            // If the versions are not in the expected format, we can assume that an update is available if the version
            // values for the site plugin and wporg plugin are not the same
            return !plugin.getVersion().equalsIgnoreCase(wpOrgPlugin.getVersion());
        }
    }
}
