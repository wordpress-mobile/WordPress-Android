package org.wordpress.android.ui.plugins;

import android.arch.lifecycle.ViewModel;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;

import java.util.HashMap;
import java.util.List;

public class PluginBrowserViewModel extends ViewModel {
    private SiteModel mSite;
    private final HashMap<String, SitePluginModel> mSitePluginsMap = new HashMap<>();

    public SiteModel getSite() {
        return mSite;
    }

    public void setSite(SiteModel site) {
        mSite = site;
    }

    SitePluginModel getSitePluginFromSlug(String slug) {
        return mSitePluginsMap.get(slug);
    }

    void setSitePlugins(List<SitePluginModel> sitePlugins) {
        mSitePluginsMap.clear();
        for (SitePluginModel plugin: sitePlugins) {
            mSitePluginsMap.put(plugin.getSlug(), plugin);
        }
    }
}
