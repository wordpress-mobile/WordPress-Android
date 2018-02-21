package org.wordpress.android.fluxc.model.plugin;

import android.support.annotation.Nullable;

import org.wordpress.android.util.StringUtils;

@SuppressWarnings("unused")
public class ImmutablePluginModel {
    private final SitePluginModel mSitePlugin;
    private final WPOrgPluginModel mWPOrgPlugin;

    public static @Nullable ImmutablePluginModel newInstance(@Nullable SitePluginModel sitePlugin,
                                                             @Nullable WPOrgPluginModel wpOrgPlugin) {
        if (sitePlugin == null && wpOrgPlugin == null) {
            return null;
        }
        return new ImmutablePluginModel(sitePlugin, wpOrgPlugin);
    }

    private ImmutablePluginModel(@Nullable SitePluginModel sitePlugin, @Nullable WPOrgPluginModel wpOrgPlugin) {
        mSitePlugin = sitePlugin;
        mWPOrgPlugin = wpOrgPlugin;
    }

    public boolean isInstalled() {
        return mSitePlugin != null;
    }

    public boolean doesHaveWPOrgPluginDetails() {
        return mWPOrgPlugin != null;
    }

    public @Nullable String getSlug() {
        if (mSitePlugin != null) {
            return mSitePlugin.getSlug();
        }
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getSlug();
        }
        return null;
    }

    public int getAverageStarRating() {
        if (mWPOrgPlugin == null) {
            return 0;
        }
        int rating = StringUtils.stringToInt(mWPOrgPlugin.getRating(), 1);
        return Math.round(rating / 20f);
    }

    public @Nullable String getAuthorAsHtml() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getAuthorAsHtml();
        }
        return null;
    }

    public @Nullable String getAuthorName() {
        if (mSitePlugin != null) {
            return mSitePlugin.getAuthorName();
        }
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getAuthorName();
        }
        return null;
    }

    public @Nullable String getAuthorUrl() {
        if (mSitePlugin != null) {
            return mSitePlugin.getAuthorUrl();
        }
        return null;
    }

    public @Nullable String getBanner() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getBanner();
        }
        return null;
    }

    public @Nullable String getDescription() {
        if (mSitePlugin != null) {
            return mSitePlugin.getDescription();
        }
        return null;
    }

    public @Nullable String getDescriptionAsHtml() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getDescriptionAsHtml();
        }
        return null;
    }

    public @Nullable String getDisplayName() {
        if (mSitePlugin != null) {
            return mSitePlugin.getDisplayName();
        } else if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getDisplayName();
        }
        return null;
    }

    public @Nullable String getFaqAsHtml() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getFaqAsHtml();
        }
        return null;
    }

    public @Nullable String getHomepageUrl() {
        if (mSitePlugin != null) {
            return mSitePlugin.getPluginUrl();
        } else if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getHomepageUrl();
        }
        return null;
    }

    public @Nullable String getIcon() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getIcon();
        }
        return null;
    }

    public @Nullable String getInstallationInstructionsAsHtml() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getInstallationInstructionsAsHtml();
        }
        return null;
    }

    public @Nullable String getInstalledVersion() {
        if (mSitePlugin != null) {
            return mSitePlugin.getVersion();
        }
        return null;
    }

    public @Nullable String getLastUpdatedForWPOrgPlugin() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getLastUpdated();
        }
        return null;
    }

    public @Nullable String getName() {
        if (mSitePlugin != null) {
            return mSitePlugin.getName();
        }
        return null;
    }

    public @Nullable String getRating() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getRating();
        }
        return null;
    }

    public @Nullable String getRequiredWordPressVersion() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getRequiredWordPressVersion();
        }
        return null;
    }

    public @Nullable String getSettingsUrl() {
        if (mSitePlugin != null) {
            return mSitePlugin.getSettingsUrl();
        }
        return null;
    }

    public @Nullable String getWhatsNewAsHtml() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getWhatsNewAsHtml();
        }
        return null;
    }

    public @Nullable String getWPOrgPluginVersion() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getVersion();
        }
        return null;
    }

    public int getDownloadCount() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getDownloadCount();
        }
        return 0;
    }

    public int getNumberOfRatings() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getNumberOfRatings();
        }
        return 0;
    }

    public int getNumberOfRatingsOfOne() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getNumberOfRatingsOfOne();
        }
        return 0;
    }

    public int getNumberOfRatingsOfTwo() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getNumberOfRatingsOfTwo();
        }
        return 0;
    }

    public int getNumberOfRatingsOfThree() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getNumberOfRatingsOfThree();
        }
        return 0;
    }

    public int getNumberOfRatingsOfFour() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getNumberOfRatingsOfFour();
        }
        return 0;
    }

    public int getNumberOfRatingsOfFive() {
        if (mWPOrgPlugin != null) {
            return mWPOrgPlugin.getNumberOfRatingsOfFive();
        }
        return 0;
    }

    public boolean isActive() {
        return mSitePlugin != null && mSitePlugin.isActive();
    }

    public boolean isAutoUpdateEnabled() {
        return mSitePlugin != null && mSitePlugin.isAutoUpdateEnabled();
    }
}
