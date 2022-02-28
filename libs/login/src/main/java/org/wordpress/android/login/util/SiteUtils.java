package org.wordpress.android.login.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteFilter;
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
        return getSiteByMatchingUrl(siteStore.getSitesAccessedViaXMLRPC(), url);
    }

    @Nullable
    public static SiteModel getSiteByMatchingUrl(SiteStore siteStore, String url) {
        return getSiteByMatchingUrl(siteStore.getSites(), url);
    }

    @Nullable
    private static SiteModel getSiteByMatchingUrl(List<SiteModel> siteModelList, String url) {
        if (siteModelList != null && !siteModelList.isEmpty()) {
            for (SiteModel siteModel : siteModelList) {
                String storedSiteUrl = UrlUtils.removeScheme(siteModel.getUrl()).replace("/", "");
                String incomingSiteUrl = UrlUtils.removeScheme(url).replace("/", "");
                if (storedSiteUrl.equalsIgnoreCase(incomingSiteUrl)) {
                    return siteModel;
                }
            }
        }
        return null;
    }

    @NonNull
    public static FetchSitesPayload getFetchSitesPayload(boolean isJetpackAppLogin, boolean isWooAppLogin) {
        ArrayList<SiteFilter> siteFilters = new ArrayList<>();
        return new FetchSitesPayload(siteFilters, !isJetpackAppLogin && !isWooAppLogin);
    }
}
