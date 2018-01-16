package org.wordpress.android.ui.accounts.signup;

public interface SiteCreationListener {
    // Site Creation Category callbacks
    void withCategory(String category);
    void helpCategoryScreen();

    // Site Creation Theme Selection callbacks
    void withTheme(String themeId);
    void helpThemeScreen();

    // Site Creation Site details callbacks
    void withSiteDetails(String siteTitle, String siteTagline);
    void helpSiteDetailsScreen();

    // Site Creation Site details callbacks
    void creationSuccess();
    void helpSiteCreatingScreen();

    void setHelpContext(String faqId, String faqSection);
}
