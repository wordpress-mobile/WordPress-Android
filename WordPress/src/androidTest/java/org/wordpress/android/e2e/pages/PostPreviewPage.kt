package org.wordpress.android.e2e.pages

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.wordpress.android.R

class PostPreviewPage {
    private val mPreview: ViewInteraction

    init {
        mPreview = Espresso.onView(ViewMatchers.withId(R.id.preview_container))
        mPreview.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}
