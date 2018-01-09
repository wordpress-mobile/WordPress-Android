package org.wordpress.android.ui.plugins;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.fluxc.model.WPOrgPluginModel;
import org.wordpress.android.fluxc.model.SitePluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.util.helpers.Version;

public class PluginUtils {
    public static boolean isPluginFeatureAvailable(SiteModel site) {
        String jetpackVersion = site.getJetpackVersion();
        if (site.isUsingWpComRestApi() && site.isJetpackConnected() && !TextUtils.isEmpty(jetpackVersion)) {
            Version siteJetpackVersion = new Version(jetpackVersion);
            Version minVersion = new Version("5.6");
            return siteJetpackVersion.compareTo(minVersion) >= 0; // if the site has Jetpack 5.6 or newer installed
        }
        return false;
    }

    static WPOrgPluginModel getWPOrgPlugin(@NonNull PluginStore pluginStore, @NonNull SitePluginModel plugin) {
        String slug = plugin.getSlug();
        if (TextUtils.isEmpty(slug)) {
            return null;
        }
        return pluginStore.getWPOrgPluginBySlug(slug);
    }

    static boolean isUpdateAvailable(SitePluginModel plugin, WPOrgPluginModel wpOrgPlugin) {
        if (wpOrgPlugin == null
                || TextUtils.isEmpty(plugin.getVersion())
                || TextUtils.isEmpty(wpOrgPlugin.getVersion())) {
            return false;
        }
        Version currentVersion = new Version(plugin.getVersion());
        Version availableVersion = new Version(wpOrgPlugin.getVersion());
        return currentVersion.compareTo(availableVersion) == -1;
    }
}
