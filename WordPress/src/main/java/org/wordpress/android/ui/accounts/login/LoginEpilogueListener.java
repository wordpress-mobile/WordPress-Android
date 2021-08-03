package org.wordpress.android.ui.accounts.login;

public interface LoginEpilogueListener {
    void onSiteClick(int localId);

    void onCreateNewSite();

    void onConnectAnotherSite();

    void onContinue();
}
