package org.wordpress.android.ui.prefs;

import android.content.Context;

import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.fluxc.model.SiteModel;

class SelfHostedSiteSettings extends SiteSettingsInterface {
    /**
     * Only instantiated by {@link SiteSettingsInterface}.
     */
    SelfHostedSiteSettings(Context host, SiteModel site, SiteSettingsListener listener) {
        super(host, site, listener);
    }

    @Override
    protected void fetchRemoteData() {
        // TODO - Call the XML-RPC endpoint
        SiteSettingsTable.saveSettings(mSettings);
    }

    @Override
    public void saveSettings() {
        super.saveSettings();
        mSite.setUsername(mSettings.username);
        mSite.setPassword(mSettings.password);
    }
}
