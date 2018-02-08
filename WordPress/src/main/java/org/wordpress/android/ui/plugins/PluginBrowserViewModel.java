package org.wordpress.android.ui.plugins;

import android.arch.lifecycle.ViewModel;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PluginBrowserViewModel extends ViewModel {
    private SiteModel mSite;
    private final HashMap<String, SitePluginModel> mSitePluginsMap = new HashMap<>();
    private List<WPOrgPluginModel> mNewPlugins;
    private List<WPOrgPluginModel> mPopularPlugins;
    private List<SitePluginModel> mSitePlugins;
    private List<WPOrgPluginModel> mSearchPlugins = new ArrayList<>();

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
        mSitePlugins = sitePlugins;
        mSitePluginsMap.clear();
        for (SitePluginModel plugin: sitePlugins) {
            mSitePluginsMap.put(plugin.getSlug(), plugin);
        }
    }

    void setNewPlugins(List<WPOrgPluginModel> newPlugins) {
        mNewPlugins = newPlugins;
    }

    void setPopularPlugins(List<WPOrgPluginModel> popularPlugins) {
        mPopularPlugins = popularPlugins;
    }

    void setSearchPlugins(List<WPOrgPluginModel> searchPlugins) {
        mSearchPlugins = searchPlugins;
    }

    List<?> getPluginsForListType(PluginBrowserActivity.PluginListType listType) {
        switch (listType) {
            case NEW:
                return mNewPlugins;
            case SEARCH:
                if (mSearchPlugins == null) {
                    return new ArrayList<WPOrgPluginModel>();
                }
                return mSearchPlugins;
            case SITE:
                return mSitePlugins;
            case POPULAR:
                return mPopularPlugins;
            default:
                return null;
        }
    }
}
