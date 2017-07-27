package org.wordpress.android.models;

import java.util.ArrayList;

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

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JetpackSettingsModel)) return false;
        JetpackSettingsModel otherModel = (JetpackSettingsModel) other;
        return monitorActive == otherModel.monitorActive &&
                emailNotifications == otherModel.emailNotifications &&
                wpNotifications == otherModel.wpNotifications &&
                jetpackProtectEnabled == otherModel.jetpackProtectEnabled &&
                ssoActive == otherModel.ssoActive &&
                ssoMatchEmail == otherModel.ssoMatchEmail &&
                ssoRequireTwoFactor == otherModel.ssoRequireTwoFactor &&
                whitelistMatches(otherModel.jetpackProtectWhitelist);
    }

    public boolean whitelistMatches(ArrayList<String> otherWhitelist) {
        return otherWhitelist != null && jetpackProtectWhitelist.equals(otherWhitelist);
    }
}
