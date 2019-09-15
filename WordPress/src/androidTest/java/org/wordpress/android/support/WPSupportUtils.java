package org.wordpress.android.support;

import android.app.Activity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.AmbiguousViewMatcherException;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.wordpress.android.R;
import org.wordpress.android.util.image.ImageType;

import java.util.Collection;
import java.util.function.Supplier;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.core.internal.deps.guava.base.Preconditions.checkNotNull;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static androidx.test.runner.lifecycle.Stage.RESUMED;
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
                .perform(scrollTo());
        clickOn(elementID);
    }

    public static void scrollToThenClickOn(ViewInteraction element) {
        waitForElementToBeDisplayed(element);
        element.perform(scrollTo());
        clickOn(element);
    }

    public static void clickOn(Integer elementID) {
        waitForElementToBeDisplayed(elementID);
        clickOn(onView(withId(elementID)));
        idleFor(500); // allow for transitions
    }

    public static void clickOn(ViewInteraction viewInteraction) {
        waitForElementToBeDisplayed(viewInteraction);
        idleFor(2000); // allow for transitions
        viewInteraction.perform(click(closeSoftKeyboard())); // attempt to close the soft keyboard as the rollback
        idleFor(500); // allow for transitions
    }

    /**
     * Returns an action that performs a single click on the view.
     *
     * If the click takes longer than the 'long press' duration
     * (which is possible because of
     * https://android.googlesource.com/platform/frameworks/testing/+/android-support-test/espresso/core/src/main/
     * java/android/support/test/espresso/action/GeneralClickAction.java#75)
     * the provided rollback action is invoked on the view and a click is attempted again.
     *
     * In our case it triggers a toast so the rollback action will be
     *
     */
    public static ViewAction click(ViewAction rollbackAction) {
        checkNotNull(rollbackAction);
        return new GeneralClickAction(Tap.SINGLE,
                GeneralLocation.CENTER,
                Press.PINPOINT,
                InputDevice.SOURCE_MOUSE,
                MotionEvent.ACTION_DOWN,
                rollbackAction);
    }



    private static boolean isResourceId(String text) {
        return text.startsWith("id");
    }

    /**
     * Uses UIAutomator to click on an element using either the resource ID or text in
     * the cases of flakiness in Espresso click performing a long click
     * @param locator - String resource ID(preceded with 'id/') or text
     */
    public static void clickOn(String locator) {
        try {
            if (isResourceId(locator)) {
                UiDevice.getInstance(getInstrumentation()).findObject(new UiSelector().resourceId(
                        "org.wordpress.android:" + locator)).click();
            } else {
                UiDevice.getInstance(getInstrumentation()).findObject(new UiSelector().text(locator)).click();
            }
        } catch (UiObjectNotFoundException e) {
            System.out.println("Could not find button with Resource ID:" + locator + " to click");
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
        clickOn(childElement);
    }

    public static void clickOnSpinnerItemAtIndex(int index) {
        final ViewInteraction spinnerItem = onView(
                allOf(
                        withId(R.id.text),
                        childAtPosition(withClassName(is("android.widget.DropDownListView")), index)
                )
        );
        waitForElementToBeDisplayed(spinnerItem);
        clickOn(spinnerItem);
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
        scrollToThenClickOn(postTitle);
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
        clickOn(onView(
                allOf(
                        withText(string),
                        isDescendantOfA(withId(R.id.tabLayout))
                     )
              ));
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
        getInstrumentation().runOnMainSync(new Runnable() {
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
