package org.wordpress.android.e2e.components;

import org.wordpress.android.R;

import static androidx.test.espresso.Espresso.onView;
import static org.wordpress.android.support.BetterScrollToAction.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.wordpress.android.support.WPSupportUtils.clickOn;

public class MainNavBarComponent {
    public MainNavBarComponent() {
    }

    public MainNavBarComponent goToMySitesTab() {
        clickOn(R.id.nav_sites);
        return this;
    }

    public MainNavBarComponent clickBlogPosts() {
        onView(withId(R.id.quick_action_posts_button))
                .perform(scrollTo());
        clickOn(R.id.quick_action_posts_button);
        return this;
    }
}
