package org.wordpress.android.e2etests.robots;


import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class MySitesMenuRobot {
    public MySitesMenuRobot tapToWriteNewPost() {
        onView(withId(R.id.fab_button))
                .perform(click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }

    public MySitesMenuRobot tapBlogPosts() {
        onView(withId(R.id.my_site_blog_posts_text_view))
                .perform(click());
        return this;
    }
}
