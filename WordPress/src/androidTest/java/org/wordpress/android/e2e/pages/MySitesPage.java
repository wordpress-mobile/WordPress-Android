package org.wordpress.android.e2e.pages;

import org.wordpress.android.R;

import static org.wordpress.android.support.WPSupportUtils.clickOn;

public class MySitesPage {
    public MySitesPage() {
    }

    public void startNewPost(String siteAddress) {
        clickOn(R.id.fab_button);
    }
}
