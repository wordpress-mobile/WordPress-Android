package org.wordpress.android.e2e.pages

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils

class EventScreen {
    fun assertEventScreenLoaded(): EventScreen {
        WPSupportUtils.waitForElementToBeDisplayed(eventScreenNavbar)
        return this
    }

    fun assertEventScreenHasActivity(activityTitle: String): EventScreen {
        val activityRow = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withText(activityTitle),
                ViewMatchers.withId(R.id.activityMessage),
            )
        )

        WPSupportUtils.waitForElementToBeDisplayed(activityRow)
        return this
    }

    companion object {
        var eventScreenNavbar: ViewInteraction = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.toolbar_main),
                ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.activity_log_event)),
            )
        )
    }
}
