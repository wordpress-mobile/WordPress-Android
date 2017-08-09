package org.wordpress.android.fluxc.plugin;

import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.PluginModel;

import java.util.ArrayList;
import java.util.List;

class PluginTestUtils {
    static List<PluginModel> generatePlugins(String... names) {
        List<PluginModel> res = new ArrayList<>();
        for (String name : names) {
            PluginModel pluginModel = new PluginModel();
            pluginModel.setName(name.toLowerCase());
            res.add(pluginModel);
        }
        return res;
    }

    static PluginInfoModel generatePluginInfo(String slug) {
        PluginInfoModel pluginInfo = new PluginInfoModel();
        pluginInfo.setSlug(slug);
        return pluginInfo;
    }
}
