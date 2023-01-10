package org.wordpress.android.support

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ListView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ScrollToAction
import androidx.test.espresso.action.ViewActions.actionWithAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf

/**
 * ScrollToAction that supports scrolling of all kinds of scrollable containers, including nested ones.
 */
class BetterScrollToAction(
    private val original: ScrollToAction = ScrollToAction()
) : ViewAction by original {
    override fun getConstraints(): Matcher<View> {
        return allOf(
            ViewMatchers.withEffectiveVisibility(VISIBLE), ViewMatchers.isDescendantOfA(
                Matchers.anyOf(
                    isAssignableFrom(ScrollView::class.java),
                    isAssignableFrom(HorizontalScrollView::class.java),
                    isAssignableFrom(NestedScrollView::class.java),
                    isAssignableFrom(ListView::class.java)
                )
            )
        )
    }

    companion object {
        @JvmStatic
        fun scrollTo(): ViewAction {
            return actionWithAssertions(BetterScrollToAction())
        }
    }
}
