package org.wordpress.android.ui.accounts.login.nav;

public interface LoginNavigationFsmGetter {
    LoginEvents.LoginNavPrologue getLoginNavPrologue();
    LoginEvents.LoginNavInputEmail getLoginNavInputEmail();
    LoginEvents.LoginNavInputSiteAddress getLoginNavInputSiteAddress();
}
