package org.wordpress.android.ui.accounts.signup;

public interface SiteCreationListener {
    // Site Creation Category callbacks
    void withCategory(String category);
    void helpCategoryScreen();

    // Site Creation Theme Selection callbacks
    void withTheme(String themeId);
    void helpThemeScreen();

    void setHelpContext(String faqId, String faqSection);
}
