package org.wordpress.android.e2e.pages

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils

class DomainsScreen {
    fun assertDomainsScreenLoaded(): DomainsScreen {
        WPSupportUtils.waitForElementToBeDisplayedWithoutFailure(R.id.toolbar_domain)
        domainsScreenHeader.check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
        return this
    }

    companion object {
        var domainsScreenHeader: ViewInteraction = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.toolbar_domain),
                ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.register_domain)),
            )
        )
    }
}
