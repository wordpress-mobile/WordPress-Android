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
                Version siteJetpackVersion = new Version(jetpackVersion);
                Version minVersion = new Version("5.6");
                return siteJetpackVersion.compareTo(minVersion) >= 0; // if the site has Jetpack 5.6 or newer installed
            } catch (IllegalArgumentException e) {
                AppLog.e(AppLog.T.UTILS, "Invalid site jetpack version " + jetpackVersion, e);
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
        Version currentVersion = new Version(plugin.getVersion());
        Version availableVersion = new Version(pluginInfo.getVersion());
        return currentVersion.compareTo(availableVersion) == -1;
    }
}
