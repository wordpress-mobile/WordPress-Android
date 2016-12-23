package org.wordpress.android.fluxc.model;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.Payload;

import java.util.ArrayList;
import java.util.List;

public class SitesModel extends Payload {
    private List<SiteModel> mSites;

    public SitesModel() {
        mSites = new ArrayList<>();
    }

    public SitesModel(@NonNull List<SiteModel> sites) {
        mSites = sites;
    }

    public List<SiteModel> getSites() {
        return mSites;
    }

    public List<Long> getSiteIds() {
        List<Long> siteIds = new ArrayList<>(mSites.size());
        for (SiteModel siteModel : mSites) {
            siteIds.add(siteModel.getSiteId());
        }
        return siteIds;
    }

    public void setSites(List<SiteModel> sites) {
        this.mSites = sites;
    }
}
