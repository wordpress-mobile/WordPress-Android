package org.wordpress.android.e2e.pages;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.wordpress.android.support.WPSupportUtils.clickOn;

public class SitePickerPage {
    public SitePickerPage() {
    }

    public void chooseSiteWithURL(String url) {
        clickOn(onView(withText(url)));
    }
}
