package org.wordpress.android.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JetpackSettingsModel {
    public final ArrayList<String> jetpackProtectWhitelist = new ArrayList<>();

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
    public boolean lazyLoadImages;
    public boolean commentLikes;
    public boolean sharingEnabled = true;

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
        jetpackProtectWhitelist.addAll(other.jetpackProtectWhitelist);
        serveImagesFromOurServers = other.serveImagesFromOurServers;
        lazyLoadImages = other.lazyLoadImages;
        sharingEnabled = other.sharingEnabled;
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
                && lazyLoadImages == otherModel.lazyLoadImages
                && commentLikes == otherModel.commentLikes
                && sharingEnabled == otherModel.sharingEnabled
                && whitelistMatches(otherModel.jetpackProtectWhitelist);
    }

    public boolean whitelistMatches(List<String> otherWhitelist) {
        if (otherWhitelist == null) {
            return false;
        }

        Set<String> whitelistSet = new HashSet<>(jetpackProtectWhitelist);
        Set<String> otherSet = new HashSet<>(otherWhitelist);
        return whitelistSet.equals(otherSet);
    }
}
