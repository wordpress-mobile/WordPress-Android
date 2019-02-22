package org.wordpress.android.e2e.components;

import android.support.test.espresso.ViewInteraction;

import org.wordpress.android.R;
import org.wordpress.android.e2e.pages.MePage;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.hamcrest.CoreMatchers.allOf;

public class MasterbarComponent {

    private static ViewInteraction mainNavBar = onView(withId(R.id.tab_layout));
    private static ViewInteraction mySitesButton = onView(withId(R.id.nav_sites));
    private static ViewInteraction meButton = onView(withId(R.id.nav_me));
    private static ViewInteraction blogPostButton = onView(withId(R.id.my_site_blog_posts_text_view));

    public MasterbarComponent() {
//        mainNavBar.check(matches(isDisplayed()));
    }

    public MasterbarComponent goToMySitesTab() {
        clickOn(R.id.nav_sites);
        return this;
    }

    public MasterbarComponent clickBlogPosts() {
        clickOn(R.id.row_blog_posts);
        return this;
    }

    public void goToMeTab() {
        new MePage().go();
    }
}
