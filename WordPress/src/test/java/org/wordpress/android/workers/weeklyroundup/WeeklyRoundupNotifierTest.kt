package org.wordpress.android.workers.weeklyroundup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.stubbing.Answer
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.NotificationPushIds.WEEKLY_ROUNDUP_NOTIFICATION_ID
import org.wordpress.android.push.NotificationType.WEEKLY_ROUNDUP
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WeeklyRoundupNotifierTest : BaseUnitTest() {
    private lateinit var weeklyRoundupNotifier: WeeklyRoundupNotifier

    private val accountStore: AccountStore = mock()
    private val siteStore: SiteStore = mock {
        on { sitesAccessedViaWPComRest }.thenReturn(buildMockSites())
    }
    private val contextProvider: ContextProvider = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any(), anyOrNull()) }.thenReturn("mock_string")
    }
    private val weeklyRoundupScheduler: WeeklyRoundupScheduler = mock()
    private val notificationsTracker: SystemNotificationsTracker = mock()
    private val siteUtils: SiteUtilsWrapper = mock()
    private val weeklyRoundupRepository: WeeklyRoundupRepository = mock {
        onBlocking { fetchWeeklyRoundupData(any()) }.then(buildMockData())
    }
    private val appPrefs: AppPrefsWrapper = mock {
        on { shouldShowWeeklyRoundupNotification(any()) }.thenReturn(true)
    }
    private val statsUtils: StatsUtils = mock()

    @Before
    fun setUp() {
        weeklyRoundupNotifier = WeeklyRoundupNotifier(
            accountStore,
            siteStore,
            contextProvider,
            resourceProvider,
            weeklyRoundupScheduler,
            notificationsTracker,
            siteUtils,
            weeklyRoundupRepository,
            appPrefs,
            statsUtils
        )
    }

    @Test
    fun `should not show notification when the user is logged out`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        assertThat(weeklyRoundupNotifier.shouldShowNotifications()).isFalse
    }

    @Test
    fun `should not show notification when the user has no sites`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(siteStore.hasSitesAccessedViaWPComRest()).thenReturn(false)

        assertThat(weeklyRoundupNotifier.shouldShowNotifications()).isFalse
    }

    @Test
    fun `should show notification when the user is logged in and has sites`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(siteStore.hasSitesAccessedViaWPComRest()).thenReturn(true)

        assertThat(weeklyRoundupNotifier.shouldShowNotifications()).isTrue
    }

    @Test
    fun `should track notification shown once for each notification`() {
        val numberOfNotifications = 5

        val notification: WeeklyRoundupNotification = mock()
        val notifications = (1..numberOfNotifications).map { notification }

        weeklyRoundupNotifier.onNotificationsShown(notifications)

        verify(notificationsTracker, times(numberOfNotifications)).trackShownNotification(WEEKLY_ROUNDUP)
    }

    @Test
    fun `buildNotifications should not have more than 5 sites`() = test {
        whenever(siteStore.sitesAccessedViaWPComRest).thenReturn(buildMockSites(10))

        val list = weeklyRoundupNotifier.buildNotifications()

        assertThat(list).hasSize(5)
    }

    @Test
    fun `buildNotifications should filter out null data`() = test {
        whenever(weeklyRoundupRepository.fetchWeeklyRoundupData(any())).then(buildMockData(isNull = true))

        val list = weeklyRoundupNotifier.buildNotifications()

        assertThat(list).isEmpty()
    }

    @Test
    fun `buildNotifications should filter out disabled preference`() = test {
        whenever(appPrefs.shouldShowWeeklyRoundupNotification(any())).thenReturn(false)

        val list = weeklyRoundupNotifier.buildNotifications()

        assertThat(list).isEmpty()
    }

    @Test
    fun `buildNotifications should filter out sites with less than 5 views`() = test {
        whenever(weeklyRoundupRepository.fetchWeeklyRoundupData(any())).then(buildMockData(views = 1))

        val list = weeklyRoundupNotifier.buildNotifications()

        assertThat(list).isEmpty()
    }

    @Test
    fun `buildNotifications should sort sites by ascending score`() = test {
        val mockSites = buildMockSites()
        val data1 = buildMockData(mockSites[0], views = 10, comments = 0, likes = 0)
        val data2 = buildMockData(mockSites[1], views = 9, comments = 8, likes = 8)
        val data3 = buildMockData(mockSites[2], views = 10, comments = 1, likes = 1)
        val unsortedData = listOf(data1, data2, data3)
        val sortedData = listOf(data1, data3, data2)

        whenever(siteStore.sitesAccessedViaWPComRest).thenReturn(mockSites)
        whenever(weeklyRoundupRepository.fetchWeeklyRoundupData(any())).then {
            unsortedData[(it.arguments.first() as SiteModel).id - 1]
        }

        val list = weeklyRoundupNotifier.buildNotifications().map { it.id }

        assertThat(list).isEqualTo(sortedData.map { WEEKLY_ROUNDUP_NOTIFICATION_ID + (it?.site?.id ?: 0) })
    }

    @Test
    fun `buildNotifications should not include likes and comments with 0 count`() = test {
        val mockSites = buildMockSites()
        val data = buildMockData(mockSites[0], views = 10, comments = 0, likes = 0)

        whenever(siteStore.sitesAccessedViaWPComRest).thenReturn(mockSites)
        whenever(weeklyRoundupRepository.fetchWeeklyRoundupData(any())).then { data }

        val list = weeklyRoundupNotifier.buildNotifications()

        assertThat(list.first().contentTitle).isEqualTo(
            resourceProvider.getString(
                R.string.weekly_roundup_notification_text_views_only,
                statsUtils.toFormattedString(data!!.views)
            )
        )
    }

    @Test
    fun `buildNotifications should not include likes with 0 count`() = test {
        val mockSites = buildMockSites()
        val data = buildMockData(mockSites[2], views = 10, comments = 1, likes = 0)

        whenever(siteStore.sitesAccessedViaWPComRest).thenReturn(mockSites)
        whenever(weeklyRoundupRepository.fetchWeeklyRoundupData(any())).then { data }

        val list = weeklyRoundupNotifier.buildNotifications()

        assertThat(list.first().contentTitle).isEqualTo(
            resourceProvider.getString(
                R.string.weekly_roundup_notification_text_views_and_comments,
                statsUtils.toFormattedString(data!!.views),
                statsUtils.toFormattedString(data.comments)
            )
        )
    }

    @Test
    fun `buildNotifications should not include comments with 0 count`() = test {
        val mockSites = buildMockSites()
        val data = buildMockData(mockSites[1], views = 9, comments = 0, likes = 8)

        whenever(siteStore.sitesAccessedViaWPComRest).thenReturn(mockSites)
        whenever(weeklyRoundupRepository.fetchWeeklyRoundupData(any())).then { data }

        val list = weeklyRoundupNotifier.buildNotifications()

        assertThat(list.first().contentTitle).isEqualTo(
            resourceProvider.getString(
                R.string.weekly_roundup_notification_text_views_and_likes,
                statsUtils.toFormattedString(data!!.views),
                statsUtils.toFormattedString(data.likes)
            )
        )
    }

    @Test
    fun `buildNotifications should include views, likes, and comments greater than zero`() = test {
        val mockSites = buildMockSites()
        val data = buildMockData(mockSites[1], views = 9, comments = 8, likes = 8)

        whenever(siteStore.sitesAccessedViaWPComRest).thenReturn(mockSites)
        whenever(weeklyRoundupRepository.fetchWeeklyRoundupData(any())).then { data }

        val list = weeklyRoundupNotifier.buildNotifications()

        assertThat(list.first().contentTitle).isEqualTo(
            resourceProvider.getString(
                R.string.weekly_roundup_notification_text_all,
                statsUtils.toFormattedString(data!!.views),
                statsUtils.toFormattedString(data.likes),
                statsUtils.toFormattedString(data.comments)
            )
        )
    }

    private companion object {
        fun buildMockSites(quantity: Int = 3) = (1..quantity).map {
            SiteModel().apply {
                id = it
                siteId = 1000L + it
            }
        }

        fun buildMockData(
            site: SiteModel,
            period: String = "2021W09W01",
            views: Long = 10,
            likes: Long = 10,
            comments: Long = 10,
            isNull: Boolean = false
        ) = if (isNull) null else WeeklyRoundupData(site, period, views, likes, comments)

        fun buildMockData(
            period: String = "2021W09W01",
            views: Long = 10,
            likes: Long = 10,
            comments: Long = 10,
            isNull: Boolean = false
        ) = Answer { buildMockData(it.arguments.first() as SiteModel, period, views, likes, comments, isNull) }
    }
}
