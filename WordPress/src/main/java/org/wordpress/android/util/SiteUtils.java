package org.wordpress.android.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
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
    private static final int GB_ROLLOUT_PERCENTAGE = 10;

    /**
     * Migrate the old app-wide editor preference value to per-site setting. wpcom sites will make a network call
     * and store the value on the backend. selfHosted sites just store the value in the local DB in FluxC
     * <p>
     * Strategy: Check if there is the old app-wide preference still available (v12.9 and before used it).
     * -- 12.9 ON -> turn all sites ON in 13.0
     * -- 12.9 OPTED OUT (were auto-opted in but turned it OFF) -> turn all sites OFF in 13.0
     *
     */
    public static void migrateAppWideMobileEditorPreferenceToRemote(final AccountStore accountStore,
                                                                    final SiteStore siteStore,
                                                                    final Dispatcher dispatcher) {
        // Skip if the user is not signed in
        if (!FluxCUtils.isSignedInWPComOrHasWPOrgSite(accountStore, siteStore)) {
            return;
        }

        // In a later version we might override mobile_editor setting if it's set to `aztec` and show a specific notice
        // for these users ("We made a lot of progress on the block editor and we think it's now better than
        // the classic editor, we switched it on, but you can change the configuration in your Site Settings").
        // ^ This code should be here.

        // If the user is already in the rollout group, we can skip this the migration.
        if (AppPrefs.isUserInGutenbergRolloutGroup()) {
            return;
        }

        // Check if the user has been "randomly" selected to enter the rollout group.
        //
        // For self hosted sites, there are often one or two users, and the user id is probably 0, 1 in these cases.
        // If we exclude low ids, we won't get an not an homogeneous distribution over self hosted and WordPress.com
        // users, but the purpose of this is to do a progressive rollout, not an necessarily an homogeneous rollout.
        //
        // To exclude ids 0 and 1, to rollout for 10% users,
        // we'll use a test like `id % 100 >= 90` instead of `id % 100 < 10`.
        if (accountStore.getAccount().getUserId() % 100 >= (100 - GB_ROLLOUT_PERCENTAGE)) {
            if (atLeastOneSiteHasAztecEnabled(siteStore)) {
                // If the user has opt-ed out from at least one of their site, then exclude them from the cohort
                return;
            }

            if (!NetworkUtils.isNetworkAvailable(WordPress.getContext())) {
                // If the network is not available, abort. We can't update the remote setting.
                return;
            }

            // Force the dialog to be shown on updated sites
            for (SiteModel site : siteStore.getSites()) {
                if (TextUtils.isEmpty(site.getMobileEditor())) {
                    AppPrefs.setShowGutenbergInfoPopupForTheNewPosts(site.getUrl(), true);
                }
            }

            // Enable Gutenberg for all sites using a single network call
            dispatcher.dispatch(SiteActionBuilder.newDesignateMobileEditorForAllSitesAction(
                    new DesignateMobileEditorForAllSitesPayload(SiteUtils.GB_EDITOR_NAME)));

            // After enabling Gutenberg on these sites, we consider the user entered the rollout group
            AppPrefs.setUserInGutenbergRolloutGroup();
        }

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

    private static boolean atLeastOneSiteHasAztecEnabled(final SiteStore siteStore) {
        // We want to make sure to enable Gutenberg only on the sites they didn't opt-out.
        for (SiteModel site : siteStore.getSites()) {
            if (TextUtils.equals(site.getMobileEditor(), AZTEC_EDITOR_NAME)) {
                return true;
            }
        }
        return false;
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
