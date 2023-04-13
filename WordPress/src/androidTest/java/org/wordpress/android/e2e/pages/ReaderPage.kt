package org.wordpress.android.e2e.pages

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import org.wordpress.android.R
import org.wordpress.android.support.WPSupportUtils

class ReaderPage {
    fun go(): ReaderPage {
        WPSupportUtils.clickOn(R.id.nav_reader)
        return this
    }

    fun tapFollowingTab(): ReaderPage {
        WPSupportUtils.clickOn(Espresso.onView(ViewMatchers.withText("FOLLOWING")))
        return this
    }

    fun openBlogOrPost(postTitle: String): ReaderViewPage {
        val post = Espresso.onView(ViewMatchers.withChild(ViewMatchers.withText(postTitle)))
        WPSupportUtils.scrollIntoView(R.id.reader_recycler_view, post, 1f)
        WPSupportUtils.clickOn(postTitle)
        return ReaderViewPage().waitUntilLoaded()
    }
}
