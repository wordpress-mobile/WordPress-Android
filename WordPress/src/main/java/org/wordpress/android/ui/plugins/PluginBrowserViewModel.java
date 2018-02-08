package org.wordpress.android.ui.plugins;

import android.arch.lifecycle.ViewModel;

import org.wordpress.android.fluxc.model.SiteModel;

public class PluginBrowserViewModel extends ViewModel {
    private SiteModel mSite;

    public void setSite(SiteModel site) {
        mSite = site;
    }

    public SiteModel getSite() {
        return mSite;
    }
}
