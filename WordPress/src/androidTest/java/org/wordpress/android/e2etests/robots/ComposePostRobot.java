package org.wordpress.android.e2etests.robots;


import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class ComposePostRobot {
    public ComposePostRobot writeTitle(String title) {
        onView(withId(R.id.title))
                .perform(replaceText("Hello"), closeSoftKeyboard());
        return this;
    }

    public ComposePostRobot writePost(String post) {
        onView(withId(R.id.aztec))
                .perform(replaceText("World"), closeSoftKeyboard());
        return this;
    }

    public ComposePostRobot tapToPublish() {
        onView(withId(R.id.menu_save_post))
                .perform(click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.promo_dialog_button_positive))
                .perform(click());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }
}
