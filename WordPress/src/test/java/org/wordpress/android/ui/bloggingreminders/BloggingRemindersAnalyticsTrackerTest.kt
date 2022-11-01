package org.wordpress.android.ui.bloggingreminders

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_BUTTON_PRESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_CANCELLED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_FLOW_COMPLETED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_FLOW_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_FLOW_START
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_INCLUDE_PROMPT_HELP_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_INCLUDE_PROMPT_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_NOTIFICATION_RECEIVED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_SCHEDULED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_REMINDERS_SCREEN_SHOWN
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.Source.BLOG_SETTINGS
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.Source.PUBLISH_FLOW
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.EPILOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.PROLOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.SELECTION
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY

@RunWith(MockitoJUnitRunner::class)
class BloggingRemindersAnalyticsTrackerTest {
    lateinit var bloggingRemindersAnalyticsTracker: BloggingRemindersAnalyticsTracker
    lateinit var bloggingRemindersUiModel: BloggingRemindersUiModel

    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val siteStore: SiteStore = mock {
        on { getSiteByLocalId(eq(WPCOM_SITE_ID)) } doReturn WPCOM_SITE
        on { getSiteByLocalId(eq(SELF_HOSTED_SITE_ID)) } doReturn SELF_HOSTED_SITE
    }

    @Before
    fun setUp() {
        bloggingRemindersAnalyticsTracker = BloggingRemindersAnalyticsTracker(analyticsTracker, siteStore)
    }

    @Test
    fun `setId adds correct site type to event properties`() {
        bloggingRemindersAnalyticsTracker.setSite(WPCOM_SITE_ID)
        bloggingRemindersAnalyticsTracker.trackFlowCompleted()

        verify(analyticsTracker).track(BLOGGING_REMINDERS_FLOW_COMPLETED, mapOf("blog_type" to "wpcom"))

        bloggingRemindersAnalyticsTracker.setSite(SELF_HOSTED_SITE_ID)
        bloggingRemindersAnalyticsTracker.trackFlowCompleted()

        verify(analyticsTracker).track(BLOGGING_REMINDERS_FLOW_COMPLETED, mapOf("blog_type" to "self_hosted"))
    }

    @Test
    fun `trackScreenShown tracks correct event and properties`() {
        bloggingRemindersAnalyticsTracker.trackScreenShown(PROLOGUE)
        bloggingRemindersAnalyticsTracker.trackScreenShown(SELECTION)
        bloggingRemindersAnalyticsTracker.trackScreenShown(EPILOGUE)

        mapCaptor().apply {
            verify(analyticsTracker, times(3)).track(eq(BLOGGING_REMINDERS_SCREEN_SHOWN), capture())

            assertThat(firstValue).containsEntry("screen", "main")
            assertThat(secondValue).containsEntry("screen", "day_picker")
            assertThat(thirdValue).containsEntry("screen", "all_set")
            assertThat(allValues).allMatch { it.containsKey("blog_type") }
        }
    }

    @Test
    fun `trackPrimaryButtonPressed tracks correct event and properties`() {
        bloggingRemindersAnalyticsTracker.trackPrimaryButtonPressed(PROLOGUE)
        bloggingRemindersAnalyticsTracker.trackPrimaryButtonPressed(SELECTION)
        bloggingRemindersAnalyticsTracker.trackPrimaryButtonPressed(EPILOGUE)

        mapCaptor().apply {
            verify(analyticsTracker, times(3)).track(eq(BLOGGING_REMINDERS_BUTTON_PRESSED), capture())

            assertThat(firstValue).containsEntry("screen", "main")
            assertThat(secondValue).containsEntry("screen", "day_picker")
            assertThat(thirdValue).containsEntry("screen", "all_set")
            assertThat(allValues).allMatch { it["button"] == "continue" }
            assertThat(allValues).allMatch { it.containsKey("blog_type") }
        }
    }

    @Test
    fun `trackFlowStart tracks correct event and properties`() {
        bloggingRemindersAnalyticsTracker.trackFlowStart(PUBLISH_FLOW)
        bloggingRemindersAnalyticsTracker.trackFlowStart(BLOG_SETTINGS)

        mapCaptor().apply {
            verify(analyticsTracker, times(2)).track(eq(BLOGGING_REMINDERS_FLOW_START), capture())

            assertThat(firstValue).containsEntry("source", "publish_flow")
            assertThat(secondValue).containsEntry("source", "blog_settings")
            assertThat(allValues).allMatch { it.containsKey("blog_type") }
        }
    }

    @Test
    fun `trackFlowDismissed tracks correct event and properties`() {
        bloggingRemindersAnalyticsTracker.trackFlowDismissed(PROLOGUE)
        bloggingRemindersAnalyticsTracker.trackFlowDismissed(SELECTION)
        bloggingRemindersAnalyticsTracker.trackFlowDismissed(EPILOGUE)

        mapCaptor().apply {
            verify(analyticsTracker, times(3)).track(eq(BLOGGING_REMINDERS_FLOW_DISMISSED), capture())

            assertThat(firstValue).containsEntry("source", "main")
            assertThat(secondValue).containsEntry("source", "day_picker")
            assertThat(thirdValue).containsEntry("source", "all_set")
            assertThat(allValues).allMatch { it.containsKey("blog_type") }
        }
    }

    @Test
    fun `trackFlowCompleted tracks correct event and properties`() {
        bloggingRemindersAnalyticsTracker.trackFlowCompleted()
        verify(analyticsTracker).track(eq(BLOGGING_REMINDERS_FLOW_COMPLETED), checkMap {
            assertThat(it).containsKey("blog_type")
        })
    }

    @Test
    fun `trackRemindersScheduled tracks correct event and properties`() {
        bloggingRemindersUiModel = BloggingRemindersUiModel(
                1, setOf(MONDAY, THURSDAY, FRIDAY), 14, 30, true)
        bloggingRemindersAnalyticsTracker.trackRemindersScheduled(
                bloggingRemindersUiModel.enabledDays.size, bloggingRemindersUiModel.getNotificationTime24hour())
        verify(analyticsTracker).track(eq(BLOGGING_REMINDERS_SCHEDULED), checkMap {
            assertThat(it).containsEntry("days_of_week_count", 3)
            assertThat(it).containsEntry("selected_time", "14:30")
            assertThat(it).containsKey("blog_type")
        })
    }

    @Test
    fun `trackRemindersCancelled tracks correct event and properties`() {
        bloggingRemindersAnalyticsTracker.trackRemindersCancelled()
        verify(analyticsTracker).track(eq(BLOGGING_REMINDERS_CANCELLED), checkMap {
            assertThat(it).containsKey("blog_type")
        })
    }

    @Test
    fun `trackNotificationReceived tracks correct event and properties`() {
        bloggingRemindersAnalyticsTracker.trackNotificationReceived(promptIncluded = false)
        verify(analyticsTracker).track(eq(BLOGGING_REMINDERS_NOTIFICATION_RECEIVED), checkMap {
            assertThat(it).containsKey("blog_type")
        })
    }

    @Test
    fun `trackRemindersIncludePromptPressed tracks correct event and properties`() {
        bloggingRemindersAnalyticsTracker.trackRemindersIncludePromptPressed(true)
        verify(analyticsTracker).track(
                BLOGGING_REMINDERS_INCLUDE_PROMPT_TAPPED,
                mapOf("enabled" to "true", "blog_type" to null)
        )
    }

    @Test
    fun `trackRemindersIncludePromptHelpPressed tracks correct event`() {
        bloggingRemindersAnalyticsTracker.trackRemindersIncludePromptHelpPressed()
        verify(analyticsTracker).track(eq(BLOGGING_REMINDERS_INCLUDE_PROMPT_HELP_TAPPED), checkMap {
            assertThat(it).containsKey("blog_type")
        })
    }

    private fun mapCaptor() = argumentCaptor<Map<String, Any?>>()

    private fun checkMap(predicate: (Map<String, Any?>) -> Unit) = check(predicate)

    companion object {
        private val WPCOM_SITE = SiteModel().apply {
            id = WPCOM_SITE_ID
            setIsWPCom(true)
        }

        private val SELF_HOSTED_SITE = SiteModel().apply {
            id = SELF_HOSTED_SITE_ID
            setIsWPCom(false)
        }

        private const val WPCOM_SITE_ID = 1
        private const val SELF_HOSTED_SITE_ID = 2
    }
}
