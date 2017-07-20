package org.wordpress.android.models;

public class JetpackSettingsModel {
    public long localTableId;
    public boolean monitorActive;
    public boolean emailNotifications;
    public boolean wpNotifications;
    public boolean jetpackProtectEnabled;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JetpackSettingsModel)) return false;
        JetpackSettingsModel otherModel = (JetpackSettingsModel) other;
        return monitorActive == otherModel.monitorActive &&
                emailNotifications == otherModel.emailNotifications &&
                wpNotifications == otherModel.wpNotifications &&
                jetpackProtectEnabled == otherModel.jetpackProtectEnabled;
    }
}
