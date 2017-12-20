package org.wordpress.android.ui.accounts.signup;

public interface SiteCreationListener {
    // Site Creation Category callbacks
    void startWithBlog();
    void startWithWebsite();
    void startWithPortfolio();
    void helpCategoryScreen();

    // Site Creation Theme Selection callbacks
    void helpThemeScreen();

    void setHelpContext(String faqId, String faqSection);
}
