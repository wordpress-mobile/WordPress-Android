package org.wordpress.android.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.plans.PlansConstants;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.helpers.Version;

import java.util.ArrayList;
import java.util.List;

public class SiteUtils {
    public static final String GB_EDITOR_NAME = "gutenberg";
    public static final String AZTEC_EDITOR_NAME = "aztec";

    public static boolean isBlockEditorDefaultForNewPost(SiteModel site) {
        if (TextUtils.isEmpty(site.getMobileEditor())) {
            // If the editor preference is still not set, the migration didn't happen yet for some reason.
            // So let's fallback to the old app preference value: On wpcom accounts we need to check if
            // a new account, and enable GB by default, making sure to re-use the same logic of the web.
            if (site.isUsingWpComRestApi()) {
                return true;
            }

            return AppPrefs.isGutenbergDefaultForNewPosts();
        } else {
            return site.getMobileEditor().equals(SiteUtils.GB_EDITOR_NAME);
        }
    }

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
                CrashLoggingUtils.logException(e, AppLog.T.UTILS, errorStr);
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

    public static boolean onFreePlan(@NonNull SiteModel site) {
        return site.getPlanId() == PlansConstants.FREE_PLAN_ID;
    }

    public static boolean onBloggerPlan(@NonNull SiteModel site) {
        return site.getPlanId() == PlansConstants.BLOGGER_PLAN_ONE_YEAR_ID
               || site.getPlanId() == PlansConstants.BLOGGER_PLAN_TWO_YEARS_ID;
    }

    public static boolean hasCustomDomain(@NonNull SiteModel site) {
        return !site.getUrl().contains(".wordpress.com");
    }
}
