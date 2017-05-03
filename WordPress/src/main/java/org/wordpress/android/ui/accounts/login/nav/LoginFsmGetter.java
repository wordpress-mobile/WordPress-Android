package org.wordpress.android.ui.accounts.login.nav;

public interface LoginFsmGetter {
    LoginNav.Prologue getLoginNavPrologue();
    LoginNav.InputEmail getLoginNavInputEmail();
    LoginNav.InputSiteAddress getLoginNavInputSiteAddress();
}
