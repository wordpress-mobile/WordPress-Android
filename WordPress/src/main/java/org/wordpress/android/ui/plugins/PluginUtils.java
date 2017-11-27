package org.wordpress.android.ui.plugins;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.store.PluginStore;

public class PluginUtils {
    public static PluginInfoModel getOrFetchPluginInfo(@NonNull Dispatcher dispatcher, @NonNull PluginStore pluginStore,
                                                 @NonNull PluginModel plugin) {
        String slug = plugin.getSlug();
        if (TextUtils.isEmpty(slug)) {
            return null;
        }
        PluginInfoModel pluginInfo = pluginStore.getPluginInfoBySlug(slug);
        if (pluginInfo == null) {
            dispatcher.dispatch(PluginActionBuilder.newFetchPluginInfoAction(slug));
        }
        return pluginInfo;
    }
}
