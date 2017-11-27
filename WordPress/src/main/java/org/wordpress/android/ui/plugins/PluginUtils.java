package org.wordpress.android.ui.plugins;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.store.PluginStore;

class PluginUtils {
    static PluginInfoModel getPluginInfo(@NonNull PluginStore pluginStore, @NonNull PluginModel plugin) {
        String slug = plugin.getSlug();
        if (TextUtils.isEmpty(slug)) {
            return null;
        }
        return pluginStore.getPluginInfoBySlug(slug);
    }
}
