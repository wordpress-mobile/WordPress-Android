package org.wordpress.android.fluxc.model;

import org.wordpress.android.fluxc.Payload;

import java.util.List;

public class SitesModel extends Payload {
    private List<SiteModel> mSites;

    public SitesModel(List<SiteModel> sites) {
        mSites = sites;
    }

    public List<SiteModel> getSites() {
        return mSites;
    }

    public void setSites(List<SiteModel> sites) {
        this.mSites = sites;
    }
}
