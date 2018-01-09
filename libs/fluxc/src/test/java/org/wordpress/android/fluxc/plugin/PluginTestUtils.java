package org.wordpress.android.fluxc.plugin;

import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.SitePluginModel;

import java.util.ArrayList;
import java.util.List;

class PluginTestUtils {
    static List<SitePluginModel> generatePlugins(String... names) {
        List<SitePluginModel> res = new ArrayList<>();
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

    static SitePluginModel generatePlugin(String name) {
        SitePluginModel sitePluginModel = new SitePluginModel();
        sitePluginModel.setName(name.toLowerCase());
        return sitePluginModel;
    }
}
