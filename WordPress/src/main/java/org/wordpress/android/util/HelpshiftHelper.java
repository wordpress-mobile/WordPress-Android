package org.wordpress.android.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.helpshift.Core;
import com.helpshift.InstallConfig;
import com.helpshift.exceptions.InstallException;
import com.helpshift.support.Support;
import com.helpshift.support.Support.Delegate;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.accounts.HelpActivity.Origin;
import org.wordpress.android.util.AppLog.T;

import java.io.File;
import java.util.HashMap;

public class HelpshiftHelper {
    private static final HashMap<String, Object> METADATA = new HashMap<String, Object>();

    private static HelpshiftHelper mInstance = null;

    private HelpshiftHelper() {
    }

    public static synchronized HelpshiftHelper getInstance() {
        if (mInstance == null) {
            mInstance = new HelpshiftHelper();
        }
        return mInstance;
    }

    public static void init(Application application) {
        InstallConfig installConfig = new InstallConfig.Builder()
                .setEnableInAppNotification(true)
                .setEnableDefaultFallbackLanguage(true)
                .build();
        Core.init(Support.getInstance());
        try {
            Core.install(application, BuildConfig.HELPSHIFT_API_KEY, BuildConfig.HELPSHIFT_API_DOMAIN,
                         BuildConfig.HELPSHIFT_API_ID, installConfig);
        } catch (InstallException e) {
            AppLog.e(T.UTILS, e);
        }
        Support.setDelegate(new Delegate() {
            @Override
            public void conversationEnded() {
            }

            @Override
            public void sessionBegan() {
            }

            @Override
            public void sessionEnded() {
            }

            @Override
            public void newConversationStarted(String newMessage) {
            }

            @Override
            public void userRepliedToConversation(String newMessage) {
                switch (newMessage) {
                    case Support.UserAcceptedTheSolution:
                        AnalyticsTracker.track(Stat.SUPPORT_USER_ACCEPTED_THE_SOLUTION);
                        break;
                    case Support.UserRejectedTheSolution:
                        AnalyticsTracker.track(Stat.SUPPORT_USER_REJECTED_THE_SOLUTION);
                        break;
                    case Support.UserSentScreenShot:
                        AnalyticsTracker.track(Stat.SUPPORT_USER_SENT_SCREENSHOT);
                        break;
                    case Support.UserReviewedTheApp:
                        AnalyticsTracker.track(Stat.SUPPORT_USER_REVIEWED_THE_APP);
                        break;
                    default:
                        AnalyticsTracker.track(Stat.SUPPORT_USER_REPLIED_TO_HELPSHIFT);
                        break;
                }
            }

            @Override
            public void userCompletedCustomerSatisfactionSurvey(int i, String s) {
            }

            @Override
            public void displayAttachmentFile(File file) {
            }

            @Override
            public void didReceiveNotification(int i) {
            }
        });
    }

    /**
     * Show conversation activity
     * Automatically add default metadata to this conversation
     */
    public void showConversation(Activity activity, SiteStore siteStore, Origin origin, String wpComUsername) {
        HashMap config = getHelpshiftConfig(activity, siteStore, wpComUsername);
        Support.showConversation(activity, config);
    }

    /**
     * Register a GCM device token to Helpshift servers
     *
     * @param regId registration id
     */
    public void registerDeviceToken(Context context, String regId) {
        if (!TextUtils.isEmpty(regId)) {
            Core.registerDeviceToken(context, regId);
        }
    }

    /**
     * Handle push notification
     */
    public void handlePush(Context context, Intent intent) {
        Core.handlePush(context, intent);
    }

    private String getJetpackMetadataString(SiteModel site) {
        StringBuffer sb = new StringBuffer();
        if (site.isJetpackConnected()) {
            sb.append("🚀✅ Jetpack connected with site ID: ");
            sb.append(site.getSiteId());
        } else {
            sb.append("🚀❌ Jetpack not connected");
            if (site.isJetpackInstalled()) {
                sb.append(" but ✅ installed");
            } else {
                sb.append(" and ❌ not installed");
            }
        }
        return sb.toString();
    }

    private void addDefaultMetaData(Context context, SiteStore siteStore, String wpComUsername) {
        // Use plain text log (unfortunately Helpshift can't display this correctly)
        METADATA.put("log", AppLog.toPlainText(context));

        // List blogs name and url
        int counter = 1;
        for (SiteModel site : siteStore.getSites()) {
            METADATA.put("blog-name-" + counter, site.getName());
            METADATA.put("blog-url-" + counter, site.getUrl());
            METADATA.put("blog-plan-" + counter, site.getPlanId());
            if (site.isAutomatedTransfer()) {
                METADATA.put("is-automated-transfer-" + counter, "true");
            }
            if (!site.isWPCom()) {
                METADATA.put("blog-jetpack-infos-" + counter, getJetpackMetadataString(site));
            }
            counter += 1;
        }

        if (AnalyticsUtils.isJetpackUser(siteStore)) {
            METADATA.put("jetpack-user", true);
        } else {
            METADATA.put("jetpack-user", false);
        }

        // wpcom user
        METADATA.put("wpcom-username", wpComUsername);
    }

    private HashMap getHelpshiftConfig(Context context, SiteStore siteStore, String wpComUsername) {
        addDefaultMetaData(context, siteStore, wpComUsername);
        HashMap config = new HashMap();
        config.put(Support.CustomMetadataKey, METADATA);
        config.put("showSearchOnNewConversation", true);
        return config;
    }

    // Deprecated. Used by the old login activities and may be removed with the old code.
    // 2017.07.30 - Aerych
    public static Origin chooseHelpshiftLoginTag(boolean isJetpackAuth, boolean isWPComMode) {
        // Origin assignment:
        // LOGIN_SCREEN_JETPACK when trying to view stats on a Jetpack site and need to login with WPCOM
        // LOGIN_SCREEN_WPCOM for when trying to log into a WPCOM site and UI not in forced self-hosted mode
        // LOGIN_SCREEN_SELF_HOSTED when logging in a self hosted site
        return isJetpackAuth ? Origin.LOGIN_SCREEN_JETPACK
                : (isWPComMode ? Origin.LOGIN_SCREEN_WPCOM : Origin.LOGIN_SCREEN_SELF_HOSTED);
    }
}
