package org.wordpress.android.e2etests.robots;


import org.wordpress.android.R;

import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.wordpress.android.e2etests.utils.RecyclerViewMatchers.onRecyclerViewItem;

public class BlogPostsRobot {
    public BlogPostsRobot tapBlogPostAtPosition(int position) {
        onRecyclerViewItem()
                .atPosition(position)
                .onChildView(withId(R.id.card_view))
                .perform(click());
        return this;
    }

public static class ResultRobot {
    public void hasTitleAndTextAtPosition(String title, String text, int position) {
        onRecyclerViewItem()
                .atPosition(position)
                .onChildView(withId(R.id.text_title))
                .check(matches(withText(title)));

        onRecyclerViewItem()
                .atPosition(position)
                .onChildView(withId(R.id.text_excerpt))
                .check(matches(withText(text)));
        }
    }
}
