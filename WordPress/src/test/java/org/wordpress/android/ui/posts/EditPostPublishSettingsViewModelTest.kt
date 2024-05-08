package org.wordpress.android.ui.posts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.OFF
import org.wordpress.android.fluxc.store.PostSchedulingNotificationStore.SchedulingReminderModel.Period.ONE_HOUR
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.Person
import org.wordpress.android.ui.people.utils.PeopleUtils.FetchUsersCallback
import org.wordpress.android.ui.people.utils.PeopleUtilsWrapper
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.ui.posts.PublishSettingsViewModel.CalendarEvent
import org.wordpress.android.ui.posts.PublishSettingsViewModel.PublishUiModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@ExperimentalCoroutinesApi
class EditPostPublishSettingsViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var postSettingsUtils: PostSettingsUtils

    @Mock
    lateinit var peopleUtilsWrapper: PeopleUtilsWrapper

    @Mock
    lateinit var localeManagerWrapper: LocaleManagerWrapper

    @Mock
    lateinit var postSchedulingNotificationStore: PostSchedulingNotificationStore

    @Mock
    lateinit var siteStore: SiteStore

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var editPostRepository: EditPostRepository
    private lateinit var viewModel: EditPostPublishSettingsViewModel
    private lateinit var post: PostModel

    private val dateCreated = "2019-05-05T14:33:20+0000"
    private val currentCalendar = Calendar.getInstance(Locale.US)
    private val dateLabel = "Updated date"

    @Before
    fun setUp() {
        viewModel = EditPostPublishSettingsViewModel(
            resourceProvider,
            postSettingsUtils,
            peopleUtilsWrapper,
            localeManagerWrapper,
            postSchedulingNotificationStore,
            siteStore,
        )
        currentCalendar.set(2019, 6, 6, 10, 20)
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(currentCalendar)
        whenever(localeManagerWrapper.getTimeZone()).thenReturn(TimeZone.getTimeZone("GMT"))
        whenever(postSettingsUtils.getPublishDateLabel(any())).thenReturn(dateLabel)
        whenever(resourceProvider.getString(R.string.immediately)).thenReturn("Immediately")
        whenever(postSchedulingNotificationStore.getSchedulingReminderPeriod(any())).thenReturn(OFF)
        whenever(editPostRepository.dateCreated).thenReturn("")
        post = PostModel()
        whenever(editPostRepository.updateAsync(any(), any())).then {
            val action: (PostModel) -> Boolean = it.getArgument(0)
            val onCompleted: (PostImmutableModel, UpdatePostResult) -> Unit = it.getArgument(1)
            action(post)
            onCompleted(post, UpdatePostResult.Updated)
            null
        }
        whenever(editPostRepository.getPost()).thenReturn(post)
        whenever(editPostRepository.hasPost()).thenReturn(true)
    }

    @Test
    fun `on start sets values and builds formatted label`() {
        whenever(editPostRepository.dateCreated).thenReturn(dateCreated)

        val expectedLabel = "Scheduled for 2019"
        whenever(postSettingsUtils.getPublishDateLabel(post)).thenReturn(expectedLabel)
        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        viewModel.start(editPostRepository)

        assertThat(viewModel.year).isEqualTo(2019)
        assertThat(viewModel.month).isEqualTo(4)
        assertThat(viewModel.day).isEqualTo(5)
        assertThat(viewModel.hour).isEqualTo(14)
        assertThat(viewModel.minute).isEqualTo(33)

        assertThat(uiModel!!.publishDateLabel).isEqualTo(expectedLabel)
    }

    @Test
    fun `on start sets current date when post not present`() {
        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        viewModel.start(null)

        assertThat(viewModel.year).isEqualTo(2019)
        assertThat(viewModel.month).isEqualTo(6)
        assertThat(viewModel.day).isEqualTo(6)
        assertThat(viewModel.hour).isEqualTo(10)
        assertThat(viewModel.minute).isEqualTo(20)

        assertThat(uiModel!!.publishDateLabel).isEqualTo("Immediately")
    }

    @Test
    fun `on start sets authors`() {
        val localSiteId = 2
        whenever(editPostRepository.localSiteId).thenReturn(localSiteId)

        val site = SiteModel()
        val siteTitle = "Site title"
        site.name = siteTitle
        site.hasCapabilityListUsers = true
        whenever(siteStore.getSiteByLocalId(localSiteId)).thenReturn(site)

        val peopleList = listOf(Person(1, 1), Person(2, 1))
        whenever(peopleUtilsWrapper.fetchAuthors(any(), any(), any()))
            .then { it.getArgument<FetchUsersCallback>(2).onSuccess(peopleList, true) }

        var authors = listOf<Person>()
        viewModel.authors.observeForever { authors = it }

        viewModel.start(editPostRepository)
        assertThat(authors).isEqualTo(peopleList)
    }

    @Test
    fun `on publishNow updates published date`() {
        var publishedDate: Event<Calendar>? = null
        viewModel.onPublishedDateChanged.observeForever {
            publishedDate = it
        }

        viewModel.publishNow()

        assertThat(publishedDate?.peekContent()).isEqualTo(currentCalendar)
    }

    @Test
    fun `onDateSelected updates date and triggers onDatePicked`() {
        var datePicked: Unit? = null
        viewModel.onDatePicked.observeForever {
            datePicked = it?.getContentIfNotHandled()
        }

        val updatedYear = 2018
        val updatedMonth = 1
        val updatedDay = 5
        viewModel.onDateSelected(updatedYear, updatedMonth, updatedDay)

        assertThat(viewModel.year).isEqualTo(updatedYear)
        assertThat(viewModel.month).isEqualTo(updatedMonth)
        assertThat(viewModel.day).isEqualTo(updatedDay)

        assertThat(datePicked).isNotNull
    }

    @Test
    fun `onTimeSelected updates time and triggers onPublishedDateChanged`() {
        viewModel.start(null)

        var publishedDate: Event<Calendar>? = null
        viewModel.onPublishedDateChanged.observeForever {
            publishedDate = it
        }

        val updatedHour = 15
        val updatedMinute = 15

        viewModel.onTimeSelected(updatedHour, updatedMinute)

        assertThat(viewModel.hour).isEqualTo(updatedHour)
        assertThat(viewModel.minute).isEqualTo(updatedMinute)

        assertThat(publishedDate?.peekContent()).isNotNull()
    }

    @Test
    fun `updatePost updates post status from DRAFT to PUBLISHED to published when date in the future`() {
        whenever(editPostRepository.status).thenReturn(PostStatus.DRAFT)
        val futureDate = Calendar.getInstance()
        futureDate.add(Calendar.MINUTE, 15)
        val updatedDate = DateTimeUtils.iso8601FromDate(futureDate.time)

        var updatedStatus: PostStatus? = null
        viewModel.onPostStatusChanged.observeForever {
            updatedStatus = it
        }

        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        viewModel.updatePost(futureDate, editPostRepository)

        assertThat(post.status).isEqualTo(PostStatus.SCHEDULED.toString())
        assertThat(post.dateCreated).isEqualTo(updatedDate)

        assertThat(updatedStatus).isEqualTo(PostStatus.SCHEDULED)
        uiModel?.apply {
            assertThat(this.publishDateLabel).isEqualTo("Updated date")
            assertThat(this.notificationLabel).isEqualTo(R.string.post_notification_off)
            assertThat(this.notificationEnabled).isTrue()
            assertThat(this.notificationVisible).isTrue()
        }
    }

    @Test
    fun `updatePost updates post status from PUBLISHED to DRAFT for local draft`() {
        whenever(editPostRepository.status).thenReturn(PostStatus.PUBLISHED)
        whenever(editPostRepository.isLocalDraft).thenReturn(true)

        var updatedStatus: PostStatus? = null
        viewModel.onPostStatusChanged.observeForever {
            updatedStatus = it
        }

        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        viewModel.updatePost(currentCalendar, editPostRepository)

        assertThat(post.status).isEqualTo(PostStatus.DRAFT.toString())
        assertThat(post.dateCreated).isNotNull()

        assertThat(updatedStatus).isEqualTo(PostStatus.DRAFT)
        uiModel?.apply {
            assertThat(this.publishDateLabel).isEqualTo("Updated date")
            assertThat(this.notificationLabel).isEqualTo(R.string.post_notification_off)
            assertThat(this.notificationEnabled).isFalse()
            assertThat(this.notificationVisible).isTrue()
        }
    }

    @Test
    fun `updatePost updates post status from SCHEDULED to DRAFT when published date in past`() {
        val expectedToastMessage = "Message"
        whenever(resourceProvider.getString(R.string.editor_post_converted_back_to_draft)).thenReturn(
            expectedToastMessage
        )
        whenever(editPostRepository.status).thenReturn(PostStatus.SCHEDULED)
        val pastDate = Calendar.getInstance()
        pastDate.add(Calendar.MINUTE, -100)

        var updatedStatus: PostStatus? = null
        viewModel.onPostStatusChanged.observeForever {
            updatedStatus = it
        }

        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        var toastMessage: String? = null
        viewModel.onToast.observeForever {
            toastMessage = it?.getContentIfNotHandled()
        }

        viewModel.updatePost(currentCalendar, editPostRepository)

        assertThat(post.status).isEqualTo(PostStatus.DRAFT.toString())
        assertThat(post.dateCreated).isNotNull()

        assertThat(updatedStatus).isEqualTo(PostStatus.DRAFT)
        uiModel?.apply {
            assertThat(this.publishDateLabel).isEqualTo("Updated date")
            assertThat(this.notificationLabel).isEqualTo(R.string.post_notification_off)
            assertThat(this.notificationEnabled).isFalse()
            assertThat(this.notificationVisible).isTrue()
        }

        assertThat(toastMessage).isEqualTo(expectedToastMessage)
    }

    @Test
    fun `hides notification when publish date in the past`() {
        val postId = 1
        post.setId(postId)
        post.setDateCreated("2019-05-05T14:33:20+0000")
        val pastDate = Calendar.getInstance()
        pastDate.set(2019, 6, 6, 10, 10, 10)
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(pastDate)

        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        whenever(postSchedulingNotificationStore.getSchedulingReminderPeriod(postId)).thenReturn(ONE_HOUR)

        viewModel.updateUiModel(post)

        uiModel?.apply {
            assertThat(this.publishDateLabel).isEqualTo("Updated date")
            assertThat(this.notificationLabel).isEqualTo(R.string.post_notification_off)
            assertThat(this.notificationEnabled).isFalse()
            assertThat(this.notificationVisible).isFalse()
        }
    }

    @Test
    fun `DISABLES notification when publish date in NOW`() {
        val postId = 1
        post.setId(postId)
        post.setDateCreated("2019-05-05T14:33:20+0000")
        val pastDate = Calendar.getInstance(Locale.US)
        pastDate.timeZone = TimeZone.getTimeZone("GMT")
        pastDate.set(2019, 4, 5, 14, 33, 20)
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(pastDate)

        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        whenever(postSchedulingNotificationStore.getSchedulingReminderPeriod(postId)).thenReturn(ONE_HOUR)

        viewModel.updateUiModel(post)

        uiModel?.apply {
            assertThat(this.publishDateLabel).isEqualTo("Updated date")
            assertThat(this.notificationEnabled).isFalse()
            assertThat(this.notificationVisible).isTrue()
            assertThat(this.notificationLabel).isEqualTo(R.string.post_notification_off)
        }
    }

    @Test
    fun `DISABLES notification when publish date in missing`() {
        val postId = 1
        post.setId(postId)
        post.setDateCreated("2019-05-05T14:33:20+0000")
        val pastDate = Calendar.getInstance(Locale.US)
        pastDate.timeZone = TimeZone.getTimeZone("GMT")
        pastDate.set(2019, 4, 5, 14, 33, 20)
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(pastDate)

        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        whenever(postSchedulingNotificationStore.getSchedulingReminderPeriod(postId)).thenReturn(ONE_HOUR)

        viewModel.updateUiModel(post)

        uiModel?.apply {
            assertThat(this.publishDateLabel).isEqualTo("Updated date")
            assertThat(this.notificationEnabled).isFalse()
            assertThat(this.notificationVisible).isTrue()
            assertThat(this.notificationLabel).isEqualTo(R.string.post_notification_off)
        }
    }

    @Test
    fun `onAddToCalendar adds a calendar event`() {
        val postTitle = "Post title"
        val localSiteId = 2
        val postLink = "link.com"
        whenever(editPostRepository.dateCreated).thenReturn("2019-05-05T14:33:20+0000")
        whenever(editPostRepository.title).thenReturn(postTitle)
        whenever(editPostRepository.localSiteId).thenReturn(localSiteId)
        whenever(editPostRepository.link).thenReturn(postLink)

        val site = SiteModel()
        val siteTitle = "Site title"
        site.name = siteTitle
        whenever(siteStore.getSiteByLocalId(localSiteId)).thenReturn(site)

        var calendarEvent: CalendarEvent? = null
        viewModel.onAddToCalendar.observeForever {
            calendarEvent = it?.getContentIfNotHandled()
        }

        val appName = "App name"
        whenever(resourceProvider.getString(R.string.app_name)).thenReturn(appName)

        val eventTitle = "Event title"
        val eventDescription = "Event description"
        whenever(
            resourceProvider.getString(
                R.string.calendar_scheduled_post_title,
                postTitle
            )
        ).thenReturn(eventTitle)
        whenever(
            resourceProvider.getString(
                R.string.calendar_scheduled_post_description,
                postTitle,
                siteTitle,
                appName,
                postLink
            )
        ).thenReturn(eventDescription)

        viewModel.onAddToCalendar(editPostRepository)

        assertThat(calendarEvent!!.startTime).isEqualTo(1557066800000L)
        assertThat(calendarEvent!!.title).isEqualTo(eventTitle)
        assertThat(calendarEvent!!.description).isEqualTo(eventDescription)
    }

    @Test
    fun `onNotificationCreated updates notification`() {
        var schedulingReminderPeriod: SchedulingReminderModel.Period? = null
        viewModel.onNotificationTime.observeForever {
            schedulingReminderPeriod = it
        }

        viewModel.onNotificationCreated(ONE_HOUR)

        assertThat(schedulingReminderPeriod).isEqualTo(ONE_HOUR)
    }

    @Test
    fun `on start sets current date when post not present in the repository`() {
        var uiModel: PublishUiModel? = null
        viewModel.onUiModel.observeForever {
            uiModel = it
        }

        whenever(editPostRepository.hasPost()).thenReturn(false)
        whenever(editPostRepository.getPost()).thenReturn(null)

        viewModel.start(editPostRepository)

        assertThat(viewModel.year).isEqualTo(2019)
        assertThat(viewModel.month).isEqualTo(6)
        assertThat(viewModel.day).isEqualTo(6)
        assertThat(viewModel.hour).isEqualTo(10)
        assertThat(viewModel.minute).isEqualTo(20)

        assertThat(uiModel!!.publishDateLabel).isEqualTo("Immediately")
    }

    @Test
    fun `given dateCreated is empty, when onAddToCalendar, then a toast is shown`() {
        whenever(editPostRepository.dateCreated).thenReturn("")
        val expectedToastMessage = ""
        whenever(resourceProvider.getString(R.string.post_settings_add_to_calendar_error)).thenReturn(
            expectedToastMessage
        )

        var toastMessage: String? = null
        viewModel.onToast.observeForever {
            toastMessage = it?.getContentIfNotHandled()
        }

        viewModel.onAddToCalendar(editPostRepository)

        assertThat(toastMessage).isEqualTo(expectedToastMessage)
    }
}
