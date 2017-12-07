package org.wordpress.android.ui.plugins;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PluginStore;

public class PluginUtils {
    public static boolean isPluginsFeatureAvailable(SiteModel site) {
        return site.isUsingWpComRestApi() && site.isJetpackConnected();
    }

    static PluginInfoModel getPluginInfo(@NonNull PluginStore pluginStore, @NonNull PluginModel plugin) {
        String slug = plugin.getSlug();
        if (TextUtils.isEmpty(slug)) {
            return null;
        }
        return pluginStore.getPluginInfoBySlug(slug);
    }

    static boolean isUpdateAvailable(PluginModel plugin, PluginInfoModel pluginInfo) {
        return pluginInfo != null && !TextUtils.isEmpty(pluginInfo.getVersion())
                && !plugin.getVersion().equals(pluginInfo.getVersion());
    }
}
