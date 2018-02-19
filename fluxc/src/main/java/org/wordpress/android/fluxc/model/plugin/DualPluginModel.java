package org.wordpress.android.fluxc.model.plugin;

import android.support.annotation.Nullable;

public class DualPluginModel {
    private final SitePluginModel mSitePlugin;
    private final WPOrgPluginModel mWPOrgPlugin;

    public DualPluginModel(@Nullable SitePluginModel sitePlugin, @Nullable WPOrgPluginModel wpOrgPlugin) {
        mSitePlugin = sitePlugin;
        mWPOrgPlugin = wpOrgPlugin;
    }

    public @Nullable SitePluginModel getSitePlugin() {
        return mSitePlugin;
    }

    public @Nullable WPOrgPluginModel getWPOrgPlugin() {
        return mWPOrgPlugin;
    }
}
