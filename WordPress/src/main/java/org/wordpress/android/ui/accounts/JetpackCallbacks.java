package org.wordpress.android.ui.accounts;

import org.wordpress.android.fluxc.model.SiteModel;

public interface JetpackCallbacks {
    boolean jetpackAuthEh();
    SiteModel getJetpackSite();
}
