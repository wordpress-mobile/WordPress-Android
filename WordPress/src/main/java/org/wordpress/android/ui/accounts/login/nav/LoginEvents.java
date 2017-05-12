package org.wordpress.android.ui.accounts.login.nav;

public class LoginEvents {
    public interface LoginNavPrologue {
        void doStartLogin();
        void doStartSignup();
    }

    public interface LoginNavInputEmail {
        void gotEmail(String email);
    }

    public interface LoginNavInputSiteAddress {
        void gotSiteAddress(String siteAddress);
    }
}
