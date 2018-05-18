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

    // Site Creation Domain Selection callbacks
    void withDomain(String domain);

    void helpDomainScreen();

    // Site Creation Creating and epilogue screen callbacks
    void helpSiteCreatingScreen();

    void doConfigureSite(int siteLocalId);

    void doWriteFirstPost(int siteLocalId);
}
