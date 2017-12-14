package org.wordpress.android.ui.accounts.signup;

public interface SiteCreationListener {
    // Login Prologue callbacks
    void startWithBlog();
    void startWithWebsite();
    void startWithPortfolio();
    void helpCategoryScreen();

    void setHelpContext(String faqId, String faqSection);
}
