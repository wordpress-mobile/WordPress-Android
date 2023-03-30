package org.wordpress.android.fluxc.model;

import androidx.annotation.NonNull;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

import java.util.ArrayList;
import java.util.List;

public class SitesModel extends Payload<BaseNetworkError> {
    private List<SiteModel> mSites;

    private List<SiteModel> mJetpackCPSites;

    public SitesModel() {
        mSites = new ArrayList<>();
        mJetpackCPSites = new ArrayList<>();
    }

    public SitesModel(@NonNull List<SiteModel> sites) {
        mSites = sites;
        mJetpackCPSites = new ArrayList<>();
    }

    public SitesModel(@NonNull List<SiteModel> sites, @NonNull List<SiteModel> jetpackCPSites) {
        mSites = sites;
        mJetpackCPSites = jetpackCPSites;
    }

    public List<SiteModel> getSites() {
        return mSites;
    }

    public List<SiteModel> getJetpackCPSites() {
        return mJetpackCPSites;
    }

    public void setSites(List<SiteModel> sites) {
        mSites = sites;
    }

    public void setJetpackCPSites(List<SiteModel> jetpackCPSites) {
        mJetpackCPSites = jetpackCPSites;
    }
}
