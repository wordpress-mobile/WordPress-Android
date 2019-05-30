package org.wordpress.android.support;

import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.AmbiguousViewMatcherException;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.wordpress.android.R;
import org.wordpress.android.util.image.ImageType;

import java.util.Collection;
import java.util.function.Supplier;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.runner.lifecycle.Stage.RESUMED;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;


public class WPSupportUtils {
    // HIGH-LEVEL METHODS

    public static boolean isElementDisplayed(Integer elementID) {
        return isElementDisplayed(onView(withId(elementID)));
    }

    public static boolean isElementDisplayed(ViewInteraction element) {
        try {
            element.check(matches(isDisplayed()));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean isElementCompletelyDisplayed(ViewInteraction element) {
        try {
            element.check(matches(isCompletelyDisplayed()));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void scrollToThenClickOn(Integer elementID) {
        waitForElementToBeDisplayed(elementID);
        onView(withId(elementID))
                .perform(scrollTo())
                .perform(click());
    }

    public static void scrollToThenClickOn(ViewInteraction element) {
        waitForElementToBeDisplayed(element);
        element.perform(scrollTo())
                .perform(click());
    }

    public static void clickOn(Integer elementID) {
        waitForElementToBeDisplayed(elementID);
        clickOn(onView(withId(elementID)));
    }

    public static void clickOn(ViewInteraction viewInteraction) {
        waitForElementToBeDisplayed(viewInteraction);
        viewInteraction.perform(click());
        idleFor(500);   // allow for transitions
    }

    /**
     * Uses UIAutomator to click on an element using the resource ID in the cases of flakiness in Espresso click
     * performing a long click
     * @param resourceID - String resource ID
     */
    public static void clickOn(String resourceID) {
        try {
            UiDevice.getInstance(getInstrumentation()).findObject(new UiSelector().resourceId(
                    "org.wordpress.android:id/" + resourceID)).click();
        } catch (UiObjectNotFoundException e) {
            System.out.println("Could not find button with Resource ID:" + resourceID + " to click");
        }
    }

    public static void longClickOn(Integer elementID) {
        waitForElementToBeDisplayed(elementID);
        onView(withId(elementID)).perform(longClick());
    }

    public static void longClickOn(ViewInteraction element) {
        waitForElementToBeDisplayed(element);
        element.perform(longClick());
    }

    public static void clickOnChildAtIndex(int index, int parentElementID, int childElementID) {
        final ViewInteraction childElement = onView(
                allOf(
                        withId(childElementID),
                        childAtPosition(withId(parentElementID), index)
                )
        );
        waitForElementToBeDisplayed(childElement);
        childElement.perform(click());
    }

    public static void clickOnSpinnerItemAtIndex(int index) {
        final ViewInteraction spinnerItem = onView(
                allOf(
                        withId(R.id.text),
                        childAtPosition(withClassName(is("android.widget.DropDownListView")), index)
                )
        );
        waitForElementToBeDisplayed(spinnerItem);
        spinnerItem.perform(click());
    }

    public static void populateTextField(Integer elementID, String text) {
        waitForElementToBeDisplayed(elementID);
        onView(withId(elementID))
                .perform(replaceText(text))
                .perform(closeSoftKeyboard());
    }

    public static void populateTextField(ViewInteraction element, String text) {
        waitForElementToBeDisplayed(element);
        element.perform(replaceText(text))
               .perform(closeSoftKeyboard());
    }

    public static void checkViewHasText(ViewInteraction element, String text) {
        waitForElementToBeDisplayed(element);
        element.check(matches(withText(text)));
    }

    public static void focusEditPostTitle() {
        ViewInteraction postTitle = onView(
                allOf(
                        withId(R.id.title),
                        childAtPosition(withClassName(is("android.widget.RelativeLayout")), 0)
                     )
                                          );
        postTitle.perform(scrollTo(), click());
        moveCaretToEndAndDisplayIn(postTitle);
    }

    private static void moveCaretToEndAndDisplayIn(ViewInteraction element) {
        element.perform(new FlashCaretViewAction());

        // To sync between the test target and the app target
        waitOneFrame();
        waitOneFrame();
    }

    public static void scrollToAndClickOnTextInRecyclerView(String text, final RecyclerView recyclerView) {
        ViewInteraction view = onView(withText(text));

        // Prevent java.lang.IllegalStateException:
        // Cannot call this method while RecyclerView is computing a layout or scrolling
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override public Boolean get() {
                return !recyclerView.isComputingLayout();
            }
        });

        // Let the layout settle down before attempting to scroll
        idleFor(100);
        
        while (recyclerView.getLayoutManager().canScrollVertically() && !isElementCompletelyDisplayed(view)) {
            getCurrentActivity().runOnUiThread(new Runnable() {
                @Override public void run() {
                    // 40 pts is the minimum suggested tappable view size, so it makes a good minimum
                    recyclerView.scrollBy(0, 40);
                }
            });
        }

        clickOn(view);
    }

    public static void selectItemAtIndexInSpinner(Integer index, Integer spinnerElementID) {
        clickOn(spinnerElementID);
        clickOnSpinnerItemAtIndex(index);
    }

    public static void selectItemWithResourceIDInTabLayout(Integer stringResourceID, Integer elementID) {
        String localizedString = getCurrentActivity().getString(stringResourceID);
        selectItemWithTitleInTabLayout(localizedString, elementID);
    }

    public static void selectItemWithTitleInTabLayout(String string, Integer elementID) {
        onView(
                allOf(
                        withText(string),
                        isDescendantOfA(withId(R.id.tabLayout))
                     )
              ).perform(click());
    }

    // WAITERS
    public static void waitForElementToBeDisplayed(final Integer elementID) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return isElementDisplayed(elementID);
            }
        });
    }

    public static void waitForElementToBeDisplayed(final ViewInteraction element) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return isElementDisplayed(element);
            }
        });
    }

    public static void waitForElementToNotBeDisplayed(final Integer elementID) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return !isElementDisplayed(elementID);
            }
        });
    }

    public static void waitForElementToNotBeDisplayed(final ViewInteraction element) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return !isElementDisplayed(element);
            }
        });
    }

    public static boolean waitForElementToBeDisplayedWithoutFailure(final Integer elementID) {
        try {
            waitForConditionToBeTrueWithoutFailure(new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return isElementDisplayed(elementID);
                }
            });
        } catch (Exception e) {
            // ignore the failure
        }
        return isElementDisplayed(elementID);
    }

    public static void waitForConditionToBeTrue(Supplier<Boolean> supplier) {
        if (supplier.get()) {
            return;
        }

        new SupplierIdler(supplier).idleUntilReady();
    }

    public static void waitForConditionToBeTrueWithoutFailure(Supplier<Boolean> supplier) {
        if (supplier.get()) {
            return;
        }

        new SupplierIdler(supplier).idleUntilReady(false);
    }

    public static void waitForImagesOfTypeWithPlaceholder(final Integer elementID, final ImageType imageType) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return hasLoadedAllImagesOfTypeWithPlaceholder(elementID, imageType);
            }
        });

        // sometimes the result of `getDrawable()` isn't the placeholder, but the placeholder is still displayed
        waitOneFrame();
    }

    public static void waitForAtLeastOneElementOfTypeToBeDisplayed(final Class c) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return atLeastOneElementOfTypeIsDisplayed(c);
            }
        });
    }

    public static void waitForAtLeastOneElementWithIdToBeDisplayed(final int elementID) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return atLeastOneElementWithIdIsDisplayed(elementID);
            }
        });
    }

    public static void waitForRecyclerViewToStopReloading() {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return hasLoadedRecyclerView();
            }
        });
    }

    public static void waitForSwipeRefreshLayoutToStopReloading() {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return hasLoadedSwipeRefreshLayout();
            }
        });
    }

    public static void pressBackUntilElementIsDisplayed(int elementID) {
        while (!isElementDisplayed(elementID)) {
            Espresso.pressBack();
        }
    }

    // Used by some methods that access the view layer directly. Because the screenshot generation code runs in
    // a different thread than the UI, the UI sometimes reports completion of an operation before repainting the
    // screen to reflect the change. Delaying by one frame ensures we're not taking a screenshot of a stale UI.
    public static void waitOneFrame() {
        idleFor(17);
    }

    public static void idleFor(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception ex) {
            // do nothing
        }
    }

    public static void sleep(int timeout) {
        // TODO: The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        // https://developer.android.com/training/testing/espresso/idling-resource
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sleep() {
        sleep(2000);
    }

    // MATCHERS

    /**
     * Returns a matcher that ensures only a single match is returned. It is best combined with
     * other matchers to prevent an {@link AmbiguousViewMatcherException}.
     */
    public static FirstMatcher first() {
        return new FirstMatcher();
    }

    public static EmptyImageMatcher isEmptyImage() {
        return new EmptyImageMatcher();
    }

    public static PlaceholderImageMatcher isPlaceholderImage(ImageType imageType) {
        return new PlaceholderImageMatcher(imageType);
    }

    // HELPERS

    public static Boolean atLeastOneElementOfTypeIsDisplayed(Class c) {
        try {
            onView(
                    allOf(
                            Matchers.<View>instanceOf(c),
                            first()
                    )
            ).check(matches(isDisplayed()));

            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static Boolean atLeastOneElementWithIdIsDisplayed(int elementID) {
        try {
            onView(
                    allOf(
                            withId(elementID),
                            first()
                    )
            ).check(matches(isDisplayed()));

            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static Boolean hasLoadedAllImagesOfTypeWithPlaceholder(Integer elementID, ImageType imageType) {
        try {
            onView(
                    allOf(
                            withId(elementID),
                            isDisplayed(),
                            anyOf(isEmptyImage(), isPlaceholderImage(imageType)),
                            first()
                    )
            ).check(doesNotExist());

            return true;
        } catch (Throwable e) {
            return false; // There are still unloaded images
        }
    }

    public static boolean hasLoadedRecyclerView() {
        try {
            onView(
                    allOf(
                            new RefreshingRecyclerViewMatcher(false),
                            first()
                    )
            ).check(matches(isDisplayed()));

            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean hasLoadedSwipeRefreshLayout() {
        try {
            onView(
                    allOf(
                            new SwipeRefreshLayoutMatcher(false),
                            first()
                    )
            ).check(matches(isDisplayed()));

            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    public static Matcher<View> withIndex(final Matcher<View> matcher, final int index) {
        return new TypeSafeMatcher<View>() {
            int mCurrentIndex = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("with index: ");
                description.appendValue(index);
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                return matcher.matches(view) && mCurrentIndex++ == index;
            }
        };
    }

    private static Activity mCurrentActivity;
    public static Activity getCurrentActivity() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Collection resumedActivities = ActivityLifecycleMonitorRegistry
                        .getInstance()
                        .getActivitiesInStage(RESUMED);

                if (resumedActivities.iterator().hasNext()) {
                    mCurrentActivity = (Activity) resumedActivities.iterator().next();
                } else {
                    mCurrentActivity = (Activity) resumedActivities.toArray()[0];
                }
            }
        });

        return mCurrentActivity;
    }
}
