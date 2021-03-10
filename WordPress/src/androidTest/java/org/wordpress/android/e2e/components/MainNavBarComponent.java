package org.wordpress.android.e2e.components;

import org.wordpress.android.R;

import static org.wordpress.android.support.WPSupportUtils.clickOn;

public class MainNavBarComponent {
    public MainNavBarComponent() {
    }

    public MainNavBarComponent goToMySitesTab() {
        clickOn(R.id.nav_sites);
        return this;
    }
}
