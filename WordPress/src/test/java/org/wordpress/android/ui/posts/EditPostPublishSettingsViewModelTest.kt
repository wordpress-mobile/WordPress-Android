package org.wordpress.android.ui.posts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class EditPostPublishSettingsViewModelTest : BaseUnitTest() {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var postSettingsUtils: PostSettingsUtils
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var postModelProvider: EditPostModelProvider
    private lateinit var viewModel: EditPostPublishSettingsViewModel

    private val dateCreated = "2019-05-05T14:33:20+0000"
    private val currentCalendar = Calendar.getInstance(Locale.US)
    private val dateLabel = "Updated date"

    @Before
    fun setUp() {
        viewModel = EditPostPublishSettingsViewModel(
                resourceProvider,
                postSettingsUtils,
                localeManagerWrapper,
                postModelProvider
        )
        currentCalendar.set(2019, 6, 6, 10, 20, 0)
        currentCalendar.timeZone = TimeZone.getTimeZone("GMT")
        whenever(localeManagerWrapper.getCurrentCalendar()).thenAnswer {
            val calendarInstance = Calendar.getInstance(Locale.US)
            calendarInstance.time = currentCalendar.time
            calendarInstance.timeZone = currentCalendar.timeZone
            calendarInstance
        }
        whenever(localeManagerWrapper.getTimeZone()).thenReturn(TimeZone.getTimeZone("GMT"))
        whenever(postSettingsUtils.getPublishDateLabel(any())).thenReturn(dateLabel)
    }

    @Test
    fun `on start sets values and builds formatted label`() {
        val post = PostModel()
        post.dateCreated = dateCreated

        val expectedLabel = "Scheduled for 2019"
        whenever(postSettingsUtils.getPublishDateLabel(post)).thenReturn(expectedLabel)
        var label: String? = null
        viewModel.onPublishedLabelChanged.observeForever {
            label = it
        }

        viewModel.onPostChanged(post)

        assertThat(viewModel.year).isEqualTo(2019)
        assertThat(viewModel.month).isEqualTo(6)
        assertThat(viewModel.day).isEqualTo(6)
        assertThat(viewModel.hour).isEqualTo(10)
        assertThat(viewModel.minute).isEqualTo(20)

        assertThat(label).isEqualTo(expectedLabel)
    }

    @Test
    fun `on start sets current date when post not present`() {
        var label: String? = null
        viewModel.onPublishedLabelChanged.observeForever {
            label = it
        }

        viewModel.onPostChanged(null)

        assertThat(viewModel.year).isEqualTo(2019)
        assertThat(viewModel.month).isEqualTo(6)
        assertThat(viewModel.day).isEqualTo(6)
        assertThat(viewModel.hour).isEqualTo(10)
        assertThat(viewModel.minute).isEqualTo(20)

        assertThat(label).isNull()
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
    fun `onTimeSelected updates time and triggers updatePost`() {
        viewModel.onPostChanged(null)

        val updatedHour = 15
        val updatedMinute = 15

        viewModel.onTimeSelected(updatedHour, updatedMinute)

        assertThat(viewModel.hour).isEqualTo(updatedHour)
        assertThat(viewModel.minute).isEqualTo(updatedMinute)
    }

    @Test
    fun `updatePost updates post status from DRAFT to SCHEDULED to published when date in the future`() {
        val post = PostModel()
        whenever(postModelProvider.getStatus()).thenReturn(DRAFT)
        post.dateCreated = "2019-09-08T15:45:00+0000"

        var updatedStatus: PostStatus? = null
        viewModel.onPostStatusChanged.observeForever {
            updatedStatus = it
        }

        var dateLabel: String? = null
        viewModel.onPublishedLabelChanged.observeForever {
            dateLabel = it
        }
        whenever(postModelProvider.postModel).thenReturn(post)

        viewModel.onPostChanged(post)
        viewModel.onTimeSelected(15, 45)

        verify(postModelProvider).setDateCreated("2019-09-08T15:45:00+0000")
        verify(postModelProvider).setStatus(SCHEDULED)

        assertThat(updatedStatus).isEqualTo(SCHEDULED)
        assertThat(dateLabel).isEqualTo(dateLabel)
    }

    @Test
    fun `updatePost updates post status from PUBLISHED to DRAFT for local draft`() {
        val post = PostModel()
        whenever(postModelProvider.getStatus()).thenReturn(PostStatus.PUBLISHED)
        post.setIsLocalDraft(true)

        var updatedStatus: PostStatus? = null
        viewModel.onPostStatusChanged.observeForever {
            updatedStatus = it
        }

        var dateLabel: String? = null
        viewModel.onPublishedLabelChanged.observeForever {
            dateLabel = it
        }
        whenever(postModelProvider.postModel).thenReturn(post)

        viewModel.publishNow()

        verify(postModelProvider).setDateCreated("2019-07-06T10:20:00+0000")
        verify(postModelProvider).setStatus(DRAFT)

        assertThat(updatedStatus).isEqualTo(DRAFT)
        assertThat(dateLabel).isEqualTo(dateLabel)
    }

    @Test
    fun `updatePost updates post status from SCHEDULED to DRAFT when published date in past`() {
        val expectedToastMessage = "Message"
        whenever(resourceProvider.getString(R.string.editor_post_converted_back_to_draft)).thenReturn(
                expectedToastMessage
        )
        whenever(postModelProvider.getStatus()).thenReturn(SCHEDULED)
        val post = PostModel()
        post.setIsLocalDraft(true)
        post.dateCreated = "2019-06-05T12:10:00+0200"

        var updatedStatus: PostStatus? = null
        viewModel.onPostStatusChanged.observeForever {
            updatedStatus = it
        }

        var dateLabel: String? = null
        viewModel.onPublishedLabelChanged.observeForever {
            dateLabel = it
        }

        var toastMessage: String? = null
        viewModel.onToast.observeForever {
            toastMessage = it?.getContentIfNotHandled()
        }
        whenever(postModelProvider.postModel).thenReturn(post)

        viewModel.onPostChanged(post)
        viewModel.onTimeSelected(12, 10)

        verify(postModelProvider).setDateCreated("2019-06-05T12:10:00+0000")
        verify(postModelProvider).setStatus(DRAFT)

        assertThat(updatedStatus).isEqualTo(DRAFT)
        assertThat(dateLabel).isEqualTo(dateLabel)

        assertThat(toastMessage).isEqualTo(expectedToastMessage)
    }
}
