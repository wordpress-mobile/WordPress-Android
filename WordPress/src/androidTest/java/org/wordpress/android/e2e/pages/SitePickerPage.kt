package org.wordpress.android.e2e.pages

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import org.wordpress.android.support.WPSupportUtils

class SitePickerPage {
    fun chooseSiteWithURL(url: String?) {
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withText(url)))
    }
}
