package org.wordpress.android.util;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.DesignateMobileEditorForAllSitesPayload;
import org.wordpress.android.fluxc.store.SiteStore.DesignateMobileEditorPayload;
import org.wordpress.android.ui.plans.PlansConstants;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils.BlockEditorEnabledSource;
import org.wordpress.android.util.helpers.Version;

import java.util.ArrayList;
import java.util.List;

public class SiteUtils {
    public static final String GB_EDITOR_NAME = "gutenberg";
    public static final String AZTEC_EDITOR_NAME = "aztec";

    /**
     * Migrate the old app-wide editor preference value to per-site setting. wpcom sites will make a network call
     * and store the value on the backend. selfHosted sites just store the value in the local DB in FluxC
     * <p>
     * Strategy: Check if there is the old app-wide preference still available (v12.9 and before used it).
     * -- 12.9 ON -> turn all sites ON in 13.0
     * -- 12.9 OPTED OUT (were auto-opted in but turned it OFF) -> turn all sites OFF in 13.0
     *
     */
    public static void migrateAppWideMobileEditorPreferenceToRemote(final Context context,
                                                                    final Dispatcher dispatcher) {
        if (!AppPrefs.isDefaultAppWideEditorPreferenceSet()) {
            return;
        }
        final boolean oldAppWidePreferenceValue = AppPrefs.isGutenbergDefaultForNewPosts();

        if (oldAppWidePreferenceValue) {
            dispatcher.dispatch(SiteActionBuilder.newDesignateMobileEditorForAllSitesAction(
                    new DesignateMobileEditorForAllSitesPayload(SiteUtils.GB_EDITOR_NAME)));
        } else {
            dispatcher.dispatch(SiteActionBuilder.newDesignateMobileEditorForAllSitesAction(
                    new DesignateMobileEditorForAllSitesPayload(SiteUtils.AZTEC_EDITOR_NAME)));
        }
    }

    public static boolean enableBlockEditorOnSiteCreation(Dispatcher dispatcher, SiteStore siteStore,
                                                          int siteLocalSiteID) {
        SiteModel newSiteModel = siteStore.getSiteByLocalId(siteLocalSiteID);
        if (newSiteModel != null) {
            enableBlockEditor(dispatcher, newSiteModel);
            AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_GUTENBERG_ENABLED, newSiteModel,
                    BlockEditorEnabledSource.ON_SITE_CREATION.asPropertyMap());
            return true;
        }
        return false;
    }

    public static void enableBlockEditor(Dispatcher dispatcher, SiteModel siteModel) {
            dispatcher.dispatch(SiteActionBuilder.newDesignateMobileEditorAction(
                    new DesignateMobileEditorPayload(siteModel, SiteUtils.GB_EDITOR_NAME)));
    }

    public static void disableBlockEditor(Dispatcher dispatcher, SiteModel siteModel) {
        dispatcher.dispatch(SiteActionBuilder.newDesignateMobileEditorAction(
                new DesignateMobileEditorPayload(siteModel, SiteUtils.AZTEC_EDITOR_NAME)));
    }

    public static boolean isBlockEditorDefaultForNewPost(SiteModel site) {
        if (site == null) {
            return false;
        }
        if (TextUtils.isEmpty(site.getMobileEditor())) {
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
