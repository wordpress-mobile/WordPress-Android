package org.wordpress.android.login.util;

import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.UrlUtils;

import java.util.ArrayList;
import java.util.List;

public class SiteUtils {
    public static ArrayList<Integer> getCurrentSiteIds(SiteStore siteStore, boolean selfhostedOnly) {
        ArrayList<Integer> siteIDs = new ArrayList<>();
        List<SiteModel> sites = selfhostedOnly ? siteStore.getSitesAccessedViaXMLRPC() : siteStore.getSites();
        for (SiteModel site : sites) {
            siteIDs.add(site.getId());
        }

        return siteIDs;
    }

    @Nullable
    public static SiteModel getXMLRPCSiteByUrl(SiteStore siteStore, String url) {
        List<SiteModel> selfhostedSites = siteStore.getSitesAccessedViaXMLRPC();
        if (selfhostedSites != null && !selfhostedSites.isEmpty()) {
            for (SiteModel siteModel : selfhostedSites) {
                String storedSiteUrl = UrlUtils.removeScheme(siteModel.getUrl()).replace("/", "");
                String incomingSiteUrl = UrlUtils.removeScheme(url).replace("/", "");
                if (storedSiteUrl.equalsIgnoreCase(incomingSiteUrl)) {
                    return siteModel;
                }
            }
        }
        return null;
    }
}
