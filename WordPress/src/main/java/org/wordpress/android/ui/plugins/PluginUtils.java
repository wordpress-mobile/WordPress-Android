package org.wordpress.android.ui.plugins;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.helpers.Version;

public class PluginUtils {
    public static boolean isPluginFeatureAvailable(SiteModel site) {
        String jetpackVersion = site.getJetpackVersion();
        if (site.isUsingWpComRestApi() && site.isJetpackConnected() && !TextUtils.isEmpty(jetpackVersion)) {
            try {
                // strip any trailing "-beta" or "-alpha" from the version
                int index = jetpackVersion.lastIndexOf("-");
                if (index > 0) {
                    jetpackVersion = jetpackVersion.substring(0, index);
                }
                Version siteJetpackVersion = new Version(jetpackVersion);
                Version minVersion = new Version("5.6");
                return siteJetpackVersion.compareTo(minVersion) >= 0; // if the site has Jetpack 5.6 or newer installed
            } catch (IllegalArgumentException e) {
                AppLog.e(AppLog.T.UTILS, "Invalid site jetpack version " + jetpackVersion, e);
                return true;
            }
        }
        return false;
    }

    static PluginInfoModel getPluginInfo(@NonNull PluginStore pluginStore, @NonNull PluginModel plugin) {
        String slug = plugin.getSlug();
        if (TextUtils.isEmpty(slug)) {
            return null;
        }
        return pluginStore.getPluginInfoBySlug(slug);
    }

    static boolean isUpdateAvailable(PluginModel plugin, PluginInfoModel pluginInfo) {
        if (pluginInfo == null
                || TextUtils.isEmpty(plugin.getVersion())
                || TextUtils.isEmpty(pluginInfo.getVersion())) {
            return false;
        }
        try {
            Version currentVersion = new Version(plugin.getVersion());
            Version availableVersion = new Version(pluginInfo.getVersion());
            return currentVersion.compareTo(availableVersion) == -1;
        } catch (IllegalArgumentException e) {
            AppLog.e(AppLog.T.UTILS, String.format("An IllegalArgumentException occurred while trying to compare site" +
                    " plugin version: %s with wporg plugin version: %s", plugin.getVersion(), pluginInfo.getVersion()));
            // If the versions are not in the expected format, we can assume that an update is available if the version
            // values for the site plugin and wporg plugin are not the same
            return !plugin.getVersion().equalsIgnoreCase(pluginInfo.getVersion());
        }
    }
}
