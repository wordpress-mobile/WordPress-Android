package org.wordpress.android.util;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.plans.PlansConstants;
import org.wordpress.android.util.helpers.Version;

import java.util.ArrayList;
import java.util.List;

public class SiteUtils {
    public static String getSiteNameOrHomeURL(SiteModel site) {
        String siteName = site.getName();
        if (siteName == null) {
            return "";
        }
        if (siteName.trim().length() == 0) {
            siteName = getHomeURLOrHostName(site);
        }
        return siteName;
    }

    public static String getHomeURLOrHostName(SiteModel site) {
        String homeURL = UrlUtils.removeScheme(site.getUrl());
        homeURL = StringUtils.removeTrailingSlash(homeURL);
        if (TextUtils.isEmpty(homeURL)) {
            return UrlUtils.getHost(site.getXmlRpcUrl());
        }
        return homeURL;
    }

    /**
     * @return true if the site is WPCom or Jetpack and is not private
     */
    public static boolean isPhotonCapable(SiteModel site) {
        return SiteUtils.isAccessedViaWPComRest(site) && !site.isPrivate();
    }

    public static boolean isAccessedViaWPComRest(SiteModel site) {
        return site.getOrigin() == SiteModel.ORIGIN_WPCOM_REST;
    }

    public static String getSiteIconUrl(SiteModel site, int size) {
        return PhotonUtils.getPhotonImageUrl(site.getIconUrl(), size, size, PhotonUtils.Quality.HIGH);
    }

    public static ArrayList<Integer> getCurrentSiteIds(SiteStore siteStore, boolean selfhostedOnly) {
        ArrayList<Integer> siteIDs = new ArrayList<>();
        List<SiteModel> sites = selfhostedOnly ? siteStore.getSitesAccessedViaXMLRPC() : siteStore.getSites();
        for (SiteModel site : sites) {
            siteIDs.add(site.getId());
        }

        return siteIDs;
    }

    /**
     * Checks if site Jetpack version is higher than limit version
     *
     * @param site
     * @param limitVersion minimal acceptable Jetpack version
     * @return
     */
    public static boolean checkMinimalJetpackVersion(SiteModel site, String limitVersion) {
        String jetpackVersion = site.getJetpackVersion();
        if (site.isUsingWpComRestApi() && site.isJetpackConnected() && !TextUtils.isEmpty(jetpackVersion)) {
            try {
                // strip any trailing "-beta" or "-alpha" from the version
                int index = jetpackVersion.lastIndexOf("-");
                if (index > 0) {
                    jetpackVersion = jetpackVersion.substring(0, index);
                }
                Version siteJetpackVersion = new Version(jetpackVersion);
                Version minVersion = new Version(limitVersion);
                return siteJetpackVersion.compareTo(minVersion) >= 0;
            } catch (IllegalArgumentException e) {
                String errorStr = "Invalid site jetpack version " + jetpackVersion + ", expected " + limitVersion;
                AppLog.e(AppLog.T.UTILS, errorStr, e);
                CrashlyticsUtils.logException(e, AppLog.T.UTILS, errorStr);
                return true;
            }
        }
        return false;
    }

    public static boolean isNonAtomicBusinessPlanSite(@Nullable SiteModel site) {
        return site != null && !site.isAutomatedTransfer() && SiteUtils.hasNonJetpackBusinessPlan(site);
    }

    public static boolean hasNonJetpackBusinessPlan(SiteModel site) {
        return site.getPlanId() == PlansConstants.BUSINESS_PLAN_ID;
    }
}
