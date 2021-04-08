package org.wordpress.android.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JetpackSettingsModel {
    public final ArrayList<String> jetpackProtectAllowlist = new ArrayList<>();

    public long localTableId;
    public boolean monitorActive;
    public boolean emailNotifications;
    public boolean wpNotifications;
    public boolean jetpackProtectEnabled;
    public boolean ssoActive;
    public boolean ssoMatchEmail;
    public boolean ssoRequireTwoFactor;
    // Modules
    public boolean serveImagesFromOurServers;
    public boolean serveStaticFilesFromOurServers;
    public boolean lazyLoadImages;
    public boolean commentLikes;
    public boolean sharingEnabled = true;
    public boolean improvedSearch;
    public boolean adFreeVideoHosting;

    public JetpackSettingsModel() {
        super();
    }

    public JetpackSettingsModel(final JetpackSettingsModel other) {
        super();

        if (other == null) {
            return;
        }

        monitorActive = other.monitorActive;
        emailNotifications = other.emailNotifications;
        wpNotifications = other.wpNotifications;
        jetpackProtectEnabled = other.jetpackProtectEnabled;
        ssoActive = other.ssoActive;
        ssoMatchEmail = other.ssoMatchEmail;
        ssoRequireTwoFactor = other.ssoRequireTwoFactor;
        commentLikes = other.commentLikes;
        jetpackProtectAllowlist.addAll(other.jetpackProtectAllowlist);
        serveImagesFromOurServers = other.serveImagesFromOurServers;
        serveStaticFilesFromOurServers = other.serveStaticFilesFromOurServers;
        lazyLoadImages = other.lazyLoadImages;
        sharingEnabled = other.sharingEnabled;
        improvedSearch = other.improvedSearch;
        adFreeVideoHosting = other.adFreeVideoHosting;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JetpackSettingsModel)) return false;
        JetpackSettingsModel otherModel = (JetpackSettingsModel) other;
        return monitorActive == otherModel.monitorActive
               && emailNotifications == otherModel.emailNotifications
               && wpNotifications == otherModel.wpNotifications
               && jetpackProtectEnabled == otherModel.jetpackProtectEnabled
               && ssoActive == otherModel.ssoActive
               && ssoMatchEmail == otherModel.ssoMatchEmail
               && ssoRequireTwoFactor == otherModel.ssoRequireTwoFactor
               && serveImagesFromOurServers == otherModel.serveImagesFromOurServers
               && serveStaticFilesFromOurServers == otherModel.serveStaticFilesFromOurServers
               && lazyLoadImages == otherModel.lazyLoadImages
               && commentLikes == otherModel.commentLikes
               && sharingEnabled == otherModel.sharingEnabled
               && improvedSearch == otherModel.improvedSearch
               && adFreeVideoHosting == otherModel.adFreeVideoHosting
               && allowlistMatches(otherModel.jetpackProtectAllowlist);
    }

    public boolean allowlistMatches(List<String> otherAllowlist) {
        if (otherAllowlist == null) {
            return false;
        }

        Set<String> allowlistSet = new HashSet<>(jetpackProtectAllowlist);
        Set<String> otherSet = new HashSet<>(otherAllowlist);
        return allowlistSet.equals(otherSet);
    }
}
