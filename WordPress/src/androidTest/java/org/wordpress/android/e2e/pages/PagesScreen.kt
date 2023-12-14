package org.wordpress.android.e2e.pages

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils

class PagesScreen {
    fun assertPagesScreenLoaded(): PagesScreen {
        WPSupportUtils.waitForElementToBeDisplayed(pagesListScreenNavbar)
        return this
    }

    fun assertPagesScreenHasPage(pageTitle: String): PagesScreen {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.page_list_layout),

                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withText(pageTitle),
                        ViewMatchers.withId(R.id.page_title),
                    )
                )
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))

        return this
    }

    companion object {
        var pagesListScreenNavbar: ViewInteraction = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.toolbar),
                ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.pages)),
            )
        )
    }
}
