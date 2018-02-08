package org.wordpress.android.ui.plugins;

import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ViewModel needs to be public to work with lower level APIs
@SuppressWarnings("WeakerAccess")
public class PluginBrowserViewModel extends ViewModel {
    private String mSearchQuery;
    private SiteModel mSite;

    private boolean mCanLoadMoreNewPlugins = true;
    private boolean mCanLoadMorePopularPlugins = true;

    private Map<PluginBrowserActivity.PluginListType, Boolean> mLoadingMorePlugins = new HashMap<>();

    private final HashMap<String, SitePluginModel> mSitePluginsMap = new HashMap<>();
    private final HashMap<String, WPOrgPluginModel> mWPOrgPluginsForSitePluginsMap = new HashMap<>();
    private List<WPOrgPluginModel> mNewPlugins;
    private List<WPOrgPluginModel> mPopularPlugins;
    private List<SitePluginModel> mSitePlugins;
    private final List<WPOrgPluginModel> mSearchResults = new ArrayList<>();

    String getSearchQuery() {
        return mSearchQuery;
    }

    void setSearchQuery(String searchQuery) {
        mSearchQuery = searchQuery;
    }

    SiteModel getSite() {
        return mSite;
    }

    void setSite(SiteModel site) {
        mSite = site;
    }

    boolean canLoadMorePlugins(PluginBrowserActivity.PluginListType listType) {
        if (listType == PluginBrowserActivity.PluginListType.NEW) {
            return mCanLoadMoreNewPlugins;
        } else if (listType == PluginBrowserActivity.PluginListType.POPULAR) {
            return mCanLoadMorePopularPlugins;
        }
        // site plugins are retrieved all at once so "load more" isn't necessary, search returns
        // the first 50 best matches which we've decided is enough
        return false;
    }

    void setCanLoadMorePlugins(PluginBrowserActivity.PluginListType listType, boolean canLoadMore) {
        if (listType == PluginBrowserActivity.PluginListType.NEW) {
            mCanLoadMoreNewPlugins = canLoadMore;
        } else if (listType == PluginBrowserActivity.PluginListType.POPULAR) {
            mCanLoadMorePopularPlugins = canLoadMore;
        }
    }

    boolean isLoadingMorePlugins(PluginBrowserActivity.PluginListType listType) {
        return mLoadingMorePlugins.get(listType);
    }

    void setLoadingMorePlugins(PluginBrowserActivity.PluginListType listType, boolean isLoadingMore) {
        mLoadingMorePlugins.put(listType, isLoadingMore);
    }

    // Site & WPOrg plugin management

    SitePluginModel getSitePluginFromSlug(String slug) {
        return mSitePluginsMap.get(slug);
    }

    // This method is specifically taking SitePluginModel as parameter, so it's understood that not all plugins
    // will be cached here
    @Nullable WPOrgPluginModel getCachedWPOrgPluginForSitePlugin(@NonNull SitePluginModel sitePlugin) {
        return mWPOrgPluginsForSitePluginsMap.get(sitePlugin.getSlug());
    }

    // In order to avoid hitting the DB in bindViewHolder multiple times for site plugins, we attempt to cache them here
    void cacheWPOrgPluginIfNecessary(WPOrgPluginModel wpOrgPlugin) {
        if (wpOrgPlugin == null) {
            return;
        }
        String slug = wpOrgPlugin.getSlug();
        if (mSitePluginsMap.containsKey(slug)) {
            mWPOrgPluginsForSitePluginsMap.put(slug, wpOrgPlugin);
        }
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

    void clearSearchResults() {
        mSearchResults.clear();
    }

    List<WPOrgPluginModel> getSearchResults() {
        return mSearchResults;
    }

    void setSearchResults(String searchQuery, List<WPOrgPluginModel> searchResults) {
        if (mSearchQuery.equalsIgnoreCase(searchQuery)) {
            mSearchResults.clear();
            mSearchResults.addAll(searchResults);
        }
    }

    @NonNull List<?> getPluginsForListType(PluginBrowserActivity.PluginListType listType) {
        switch (listType) {
            case NEW:
                return mNewPlugins;
            case SEARCH:
                return getSearchResults();
            case SITE:
                return mSitePlugins;
            case POPULAR:
                return mPopularPlugins;
            default:
                return null;
        }
    }
}
