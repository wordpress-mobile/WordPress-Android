package org.wordpress.android.ui.accounts.login.nav;

public interface LoginFsmGetter {
    interface FsmGetter {
        LoginFsmGetter get();
    }

    LoginNav.Prologue getLoginNavPrologue();
    LoginNav.InputEmail getLoginNavInputEmail();
    LoginNav.InputSiteAddress getLoginNavInputSiteAddress();
}
