package org.wordpress.android.fluxc.plugin;

import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.PluginModel;

import java.util.ArrayList;
import java.util.List;

class PluginTestUtils {
    static List<PluginModel> generatePlugins(String... names) {
        List<PluginModel> res = new ArrayList<>();
        for (String name : names) {
            res.add(generatePlugin(name));
        }
        return res;
    }

    static PluginInfoModel generatePluginInfo(String slug) {
        PluginInfoModel pluginInfo = new PluginInfoModel();
        pluginInfo.setSlug(slug);
        return pluginInfo;
    }

    static PluginModel generatePlugin(String name) {
        PluginModel pluginModel = new PluginModel();
        pluginModel.setName(name.toLowerCase());
        return pluginModel;
    }
}
