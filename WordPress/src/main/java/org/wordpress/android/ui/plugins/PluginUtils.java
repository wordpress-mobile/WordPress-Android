package org.wordpress.android.ui.plugins;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.helpers.Version;

import java.util.Arrays;
import java.util.List;

public class PluginUtils {
    private static final String JETPACK_PLUGIN_NAME = "jetpack/jetpack";
    private static final List<String> AUTO_MANAGED_PLUGINS = Arrays.asList(JETPACK_PLUGIN_NAME,
            "akismet/akismet", "vaultpress/vaultpress");

    public static boolean isPluginFeatureAvailable(SiteModel site) {
        if (site.isUsingWpComRestApi() && site.isJetpackConnected()) {
            return SiteUtils.checkMinimalJetpackVersion(site, "5.6");
        }

        // If the site has business plan we can do an Automated Transfer
        return site.isWPCom()
               && SiteUtils.hasNonJetpackBusinessPlan(site)
               && site.getHasCapabilityManageOptions() // Automated Transfer require admin capabilities
               && !site.isPrivate(); // Private sites are not eligible for Automated Transfer
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
            return currentVersion.compareTo(availableVersion) < 0;
        } catch (IllegalArgumentException e) {
            String errorStr =
                    String.format("An IllegalArgumentException occurred while trying to compare site plugin version: %s"
                                  + " with wporg plugin version: %s", installedVersionStr, availableVersionStr);
            AppLog.e(AppLog.T.PLUGINS, errorStr, e);
            // If the versions are not in the expected format, we can assume that an update is available if the version
            // values for the site plugin and wporg plugin are not the same
            return !installedVersionStr.equalsIgnoreCase(availableVersionStr);
        }
    }

    static boolean isJetpack(@NonNull ImmutablePluginModel plugin) {
        return StringUtils.equals(plugin.getName(), JETPACK_PLUGIN_NAME);
    }

    static boolean isAutoManaged(@NonNull SiteModel site, @NonNull ImmutablePluginModel plugin) {
        if (!site.isAutomatedTransfer()) {
            return false;
        }
        if (!plugin.isInstalled()) {
            return false;
        }
        boolean isAutoManaged = false;
        for (String pluginName : AUTO_MANAGED_PLUGINS) {
            isAutoManaged = isAutoManaged || StringUtils.equals(plugin.getName(), pluginName);
        }
        return isAutoManaged;
    }
}
