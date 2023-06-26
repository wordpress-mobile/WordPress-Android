package org.wordpress.android.e2e.pages

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils

class ActivityLogScreen {
    fun assertActivityLogScreenLoaded(): ActivityLogScreen {
        WPSupportUtils.waitForElementToBeDisplayed(activityLogScreenNavbar)
        WPSupportUtils.waitForElementToBeDisplayed(R.id.date_range_picker)
        WPSupportUtils.waitForElementToBeDisplayed(R.id.activity_type_filter)
        return this
    }

    fun assertActivityLogScreenHasActivity(activityPartial: String): ActivityLogScreen {
        val activityRow = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.log_list_view)),
                ViewMatchers.withText(containsString(activityPartial)),
                ViewMatchers.withId(R.id.action_text),
            )
        )

        WPSupportUtils.waitForElementToBeDisplayed(activityRow)
        return this
    }

    companion object {
        var activityLogScreenNavbar: ViewInteraction = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.toolbar_main),
                ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.activity_log)),
            )
        )
    }
}
