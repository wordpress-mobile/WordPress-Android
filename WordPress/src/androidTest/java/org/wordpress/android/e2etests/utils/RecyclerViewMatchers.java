package org.wordpress.android.e2etests.utils;


import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.registerIdlingResources;
import static android.support.test.espresso.Espresso.unregisterIdlingResources;
import static android.support.test.espresso.IdlingPolicies.setIdlingResourceTimeout;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static org.hamcrest.Matchers.equalTo;

/**
 * Espresso matcher extensions for RecyclerView.
 */
public final class RecyclerViewMatchers {
    private static final int DEFAULT_TIME_OUT = 3;

    private RecyclerViewMatchers() {
        // no instance
    }

    /**
     * Creates an {@link RecyclerViewInteraction} for a RecyclerView object displayed by the application.
     * Use this method to interact with individual items inside the RecyclerView.
     */
    public static RecyclerViewInteraction onRecyclerViewItem() {
        return new RecyclerViewInteraction();
    }

    /**
     * Matches view if mAdapter contains specified count. Use for assertions.
     */
    public static Matcher<View> hasItemCount(final int count) {
        return hasItemCount(equalTo(count));
    }

    /**
     * Matches view if mAdapter item count matches 'countMatcher'. Use for assertions.
     */
    public static Matcher<View> hasItemCount(final Matcher<Integer> countMatcher) {
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override public void describeTo(Description description) {
                description.appendText("item count: ");
                countMatcher.describeTo(description);
            }

            @Override protected boolean matchesSafely(RecyclerView item) {
                return countMatcher.matches(item.getAdapter().getItemCount());
            }
        };
    }

    /**
     * When executed, this action will wait until matched RecyclerView is populated.
     */
    public static ViewAction waitUntilPopulated() {
        return waitUntilPopulated(DEFAULT_TIME_OUT);
    }

    /**
     * When executed, this action will wait for 'timeout' seconds or until matched RecyclerView is populated.
     */
    public static ViewAction waitUntilPopulated(final int timeout) {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() {
                return isAssignableFrom(RecyclerView.class);
            }

            @Override public String getDescription() {
                return "wait until RecyclerView populated";
            }

            @Override public void perform(UiController uiController, View view) {
                setIdlingResourceTimeout(timeout, TimeUnit.SECONDS);

                RecyclerView.Adapter adapter = ((RecyclerView) view).getAdapter();
                IdlingDataObserver observer = new IdlingDataObserver(adapter);
                adapter.registerAdapterDataObserver(observer);

                registerIdlingResources(observer);
                uiController.loopMainThreadUntilIdle();
                unregisterIdlingResources(observer);

                adapter.unregisterAdapterDataObserver(observer);
            }
        };
    }

    private static class IdlingDataObserver extends RecyclerView.AdapterDataObserver implements IdlingResource {
        private ResourceCallback mCallback;
        private RecyclerView.Adapter mAdapter;

        IdlingDataObserver(RecyclerView.Adapter adapter) {
            this.mAdapter = adapter;
        }

        @Override public void onChanged() {
            if (mCallback != null && mAdapter.getItemCount() > 0) {
                mCallback.onTransitionToIdle();
            }
        }

        @Override public String getName() {
            return "waitUntilPopulated";
        }

        @Override public boolean isIdleNow() {
            return mAdapter.getItemCount() > 0;
        }

        @Override public void registerIdleTransitionCallback(ResourceCallback callback) {
            this.mCallback = callback;
        }
    }
}
