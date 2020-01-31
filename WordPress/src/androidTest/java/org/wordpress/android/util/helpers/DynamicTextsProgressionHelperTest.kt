package org.wordpress.android.util.helpers

import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.TextSwitcher
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.testing.TestActivity
import org.wordpress.android.ui.utils.UiString.UiStringRes
import java.lang.Thread.sleep
import java.lang.ref.WeakReference

private const val TEST_DISPLAY_DURATION: Long = 100

class DynamicTextsProgressionHelperTest {
    @Rule @JvmField val rule = ActivityTestRule(TestActivity::class.java)

    lateinit var textSwitcher: TextSwitcher
    @Before
    fun setUp() {
        // create a text switcher as hte activity's view
        rule.runOnUiThread {
            TextSwitcher(rule.activity).also {
                it.layoutParams = FrameLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                )
                rule.activity.setContentView(it)
                it.id = R.id.progress_text
                textSwitcher = it
            }
        }
    }

    @Test
    fun that_helper_animates_texts() {
        // setup
        // (none)

        rule.runOnUiThread {
            DynamicTextsProgressionHelper(
                    WeakReference(textSwitcher), listOf(
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_1),
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_2),
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_3),
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_4),
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_5),
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_6)
            ),
                    TEST_DISPLAY_DURATION,
                    layout.site_creation_progress_text
            ).also {
                // execute

                textSwitcher.postDelayed(it, TEST_DISPLAY_DURATION)
            }
        }
        // verify
        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_1))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )

        sleep(TEST_DISPLAY_DURATION)

        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_2))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )

        sleep(TEST_DISPLAY_DURATION)

        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_3))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )

        sleep(TEST_DISPLAY_DURATION)

        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_4))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )

        sleep(TEST_DISPLAY_DURATION)

        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_5))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )
        sleep(TEST_DISPLAY_DURATION)

        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_6))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )
    }

    @Test
    fun that_helper_cycles_through_texts() {
        // setup
        // (none)

        rule.runOnUiThread {
            DynamicTextsProgressionHelper(
                    WeakReference(textSwitcher), listOf(
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_1),
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_2),
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_3)
            ),
                    TEST_DISPLAY_DURATION,
                    layout.site_creation_progress_text
            ).also {
                // execute

                textSwitcher.postDelayed(it, TEST_DISPLAY_DURATION)
            }
        }
        // verify
        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_1))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )

        sleep(TEST_DISPLAY_DURATION)

        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_2))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )

        sleep(TEST_DISPLAY_DURATION)

        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_3))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )

        sleep(TEST_DISPLAY_DURATION)

        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_1))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )

        sleep(TEST_DISPLAY_DURATION)

        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_2))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )
        sleep(TEST_DISPLAY_DURATION)

        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_3))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )
    }

    @Test
    fun that_helper_stops_animation_when_canceled() {
        // setup
        // (none)

        var helper: DynamicTextsProgressionHelper? = null

        rule.runOnUiThread {
            DynamicTextsProgressionHelper(
                    WeakReference(textSwitcher), listOf(
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_1),
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_2),
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_3),
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_4),
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_5),
                    UiStringRes(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_6)
            ),
                    TEST_DISPLAY_DURATION,
                    layout.site_creation_progress_text
            ).also {
                // execute

                textSwitcher.postDelayed(it, TEST_DISPLAY_DURATION)

                helper = it
            }
        }
        // verify
        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_1))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )

        sleep(TEST_DISPLAY_DURATION)

        onView(withText(R.string.notification_new_site_creation_creating_site_dynamic_subtitle_2))
                .check(
                        matches(
                                isCompletelyDisplayed()
                        )
                )

        // CANCEL IT:
        rule.runOnUiThread {
            helper!!.cancel()
        }

        sleep(TEST_DISPLAY_DURATION)

        onView(withId(textSwitcher.id))
                .check(
                        matches(
                                not(
                                        isDisplayed()
                                )
                        )
                )
    }
}
