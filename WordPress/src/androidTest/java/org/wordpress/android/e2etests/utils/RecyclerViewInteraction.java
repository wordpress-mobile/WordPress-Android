package org.wordpress.android.e2etests.utils;

import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.Matchers.allOf;
import static org.wordpress.android.e2etests.utils.RecyclerViewMatchers.waitUntilPopulated;

/**
 * An interface to interact with items displayed in {@link RecyclerView}.
 * <p>
 * This interface builds on top of {@link ViewInteraction} and should be the preferred way to
 * interact with elements displayed inside {@link RecyclerView}.
 * </p>
 * <p>
 * This is necessary because a {@link RecyclerView} may not load all the data held by its Adapter into the
 * view hierarchy until a user interaction makes it necessary. Also it is more fluent / less brittle
 * to match upon the data object being rendered into the display then the rendering itself.
 * </p>
 * <p>
 * By default, a {@link RecyclerViewInteraction} takes place against any {@link RecyclerView} found within
 * the current screen, if you have multiple {@link RecyclerView} objects displayed, you will need to narrow
 * the selection by using the {@link #inRecyclerView(Matcher)} method.
 * </p>
 * <p>
 * The check and perform method operate on the top level child of the {@link RecyclerView}, if you need to
 * operate on a subview (eg: a Button within the item) use the {@link #onChildView(Matcher)} method before
 * calling perform or check.
 * </p>
 */
public class RecyclerViewInteraction {
    private Matcher<View> mRecyclerViewMatcher = allOf(isDisplayed(), isAssignableFrom(RecyclerView.class));
    private Matcher<View> mChildViewMatcher;
    private int mAtPosition;

    RecyclerViewInteraction() {
    }

    /**
     * Selects a particular {@link RecyclerView} to operate on, by default we operate on any
     * displayed on the screen.
     */
    public RecyclerViewInteraction inRecyclerView(Matcher<View> recyclerViewMatcher) {
        this.mRecyclerViewMatcher = recyclerViewMatcher;
        return this;
    }

    /**
     * Selects the view which matches the nth position on the adapter based on the data matcher.
     */
    public RecyclerViewInteraction atPosition(int atPosition) {
        this.mAtPosition = atPosition;
        return this;
    }

    /**
     * Causes perform and check methods to take place on a specific child view of the view returned
     * by Adapter.getView()
     */
    public RecyclerViewInteraction onChildView(Matcher<View> childMatcher) {
        this.mChildViewMatcher = childMatcher;
        return this;
    }

    /**
     * Performs an action on the view after waiting for the data to be loaded.
     *
     * @return an {@link ViewInteraction} for more assertions or actions.
     */
    public ViewInteraction perform(ViewAction... actions) {
        // Wait for data to load
        onView(mRecyclerViewMatcher)
                .perform(waitUntilPopulated())
                .perform(scrollToPosition(mAtPosition));

        // Perform actions
        return onView(makeTargetMatcher())
                .perform(actions);
    }

    /**
     * Performs an assertion on the state of the view after waiting for the data to be loaded.
     *
     * @return an {@link ViewInteraction} for more assertions or actions.
     */
    public ViewInteraction check(ViewAssertion assertion) {
        // Wait for data to load
        onView(mRecyclerViewMatcher)
                .perform(waitUntilPopulated())
                .perform(scrollToPosition(mAtPosition));

        // Perform assertion
        return onView(makeTargetMatcher())
                .check(assertion);
    }

    private Matcher<View> makeTargetMatcher() {
        Matcher<View> targetView = makeListItemMatcher();
        if (mChildViewMatcher != null) {
            targetView = allOf(mChildViewMatcher, isDescendantOfA(targetView));
        }
        return targetView;
    }

    private Matcher<View> makeListItemMatcher() {
        return new BoundedMatcher<View, View>(View.class) {
            @Override public void describeTo(Description description) {
                description.appendText(" is at adapter position: ");
                description.appendValue(mAtPosition);
                description.appendText(" within RecyclerView matching: ");
                mRecyclerViewMatcher.describeTo(description);
            }

            @Override protected boolean matchesSafely(final View view) {
                if (!(mRecyclerViewMatcher.matches(view.getParent()))) {
                    return false;
                }
                RecyclerView parent = (RecyclerView) view.getParent();
                RecyclerView.ViewHolder viewHolder = parent.findViewHolderForAdapterPosition(mAtPosition);
                return viewHolder.itemView == view;
            }
        };
    }
}
