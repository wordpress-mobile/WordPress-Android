package org.wordpress.android.ui.plugins;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.helpers.Version;

public class PluginUtils {
    public static boolean isPluginFeatureAvailable(SiteModel site) {
        return SiteUtils.checkMinimalJetpackVersion(site, "5.6");
    }

    static boolean isUpdateAvailable(@Nullable ImmutablePluginModel immutablePlugin) {
        if (immutablePlugin == null
            || TextUtils.isEmpty(immutablePlugin.getInstalledVersion())
            || TextUtils.isEmpty(immutablePlugin.getWPOrgPluginVersion())) {
            return false;
        }
        String installedVersionStr = immutablePlugin.getInstalledVersion();
        String availableVersionStr = immutablePlugin.getWPOrgPluginVersion();
        try {
            Version currentVersion = new Version(installedVersionStr);
            Version availableVersion = new Version(availableVersionStr);
            return currentVersion.compareTo(availableVersion) == -1;
        } catch (IllegalArgumentException e) {
            String errorStr =
                    String.format("An IllegalArgumentException occurred while trying to compare site plugin version: %s"
                                  + " with wporg plugin version: %s", installedVersionStr, availableVersionStr);
            AppLog.e(AppLog.T.PLUGINS, errorStr, e);
            CrashlyticsUtils.logException(e, AppLog.T.PLUGINS, errorStr);
            // If the versions are not in the expected format, we can assume that an update is available if the version
            // values for the site plugin and wporg plugin are not the same
            return !installedVersionStr.equalsIgnoreCase(availableVersionStr);
        }
    }
}
