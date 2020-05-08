package org.wordpress.android.ui.posts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.stats.refresh.utils.DateUtils
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar
import java.util.Date

class PostSettingsUtilsTest : BaseUnitTest() {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var dateUtils: DateUtils
    private lateinit var postSettingsUtils: PostSettingsUtils

    private val dateCreated = "2019-05-05T14:33:20+0200"
    private val formattedDate = "5. 5. 2019"
    private lateinit var postModel: PostModel
    @Before
    fun setUp() {
        postSettingsUtils = PostSettingsUtils(resourceProvider, dateUtils)
        whenever(dateUtils.formatDateTime(any())).thenReturn(formattedDate)
        whenever(
                resourceProvider.getString(
                        R.string.scheduled_for,
                        formattedDate
                )
        ).thenReturn("Scheduled for $formattedDate")
        whenever(
                resourceProvider.getString(
                        R.string.published_on,
                        formattedDate
                )
        ).thenReturn("Published on $formattedDate")
        whenever(
                resourceProvider.getString(
                        R.string.backdated_for,
                        formattedDate
                )
        ).thenReturn("Backdated for $formattedDate")
        whenever(
                resourceProvider.getString(R.string.immediately)
        ).thenReturn("Immediately")
        whenever(
                resourceProvider.getString(
                        eq(R.string.publish_on),
                        any()
                )
        ).thenReturn("Publish on $formattedDate")
        whenever(
                resourceProvider.getString(
                        eq(R.string.schedule_for),
                        any()
                )
        ).thenReturn("Schedule for $formattedDate")
        postModel = PostModel()
    }

    @Test
    fun `returns "scheduled for" for scheduled post`() {
        postModel.setStatus(PostStatus.SCHEDULED.toString())
        postModel.setDateCreated(dateCreated)

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Scheduled for 5. 5. 2019")
    }

    @Test
    fun `returns "published on" for published post`() {
        postModel.setStatus(PostStatus.PUBLISHED.toString())
        postModel.setDateCreated(dateCreated)

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Published on 5. 5. 2019")
    }

    @Test
    fun `returns "published on" for private post`() {
        postModel.setStatus(PostStatus.PRIVATE.toString())
        postModel.setDateCreated(dateCreated)

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Published on 5. 5. 2019")
    }

    @Test
    fun `returns "scheduled for" for private post that is local and scheduled`() {
        postModel.setStatus(PostStatus.PRIVATE.toString())
        postModel.setIsLocalDraft(true)

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 100)
        postModel.setDateCreated(DateTimeUtils.iso8601FromDate(calendar.time))

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Schedule for 5. 5. 2019")
    }

    @Test
    fun `returns "backdated for" for local draft when publish date in the past`() {
        postModel.setIsLocalDraft(true)
        postModel.setDateCreated(dateCreated)

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Backdated for 5. 5. 2019")
    }

    @Test
    fun `returns "immediately" for local draft when should publish immediately`() {
        postModel.setIsLocalDraft(true)
        postModel.setStatus(PostStatus.DRAFT.toString())
        postModel.setDateCreated(DateTimeUtils.iso8601FromDate(Date()))

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Immediately")
    }

    @Test
    fun `returns "immediately" for local private post that should publish immediately`() {
        postModel.setIsLocalDraft(true)
        postModel.setStatus(PostStatus.PRIVATE.toString())
        postModel.setDateCreated(DateTimeUtils.iso8601FromDate(Date()))

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Immediately")
    }

    @Test
    fun `returns "published on" for local draft when date is not set`() {
        postModel.setIsLocalDraft(true)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -5)
        postModel.setDateCreated(DateTimeUtils.iso8601FromDate(calendar.time))

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Publish on 5. 5. 2019")
    }

    @Test
    fun `returns "schedule for" when post published in future`() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 100)
        postModel.setDateCreated(DateTimeUtils.iso8601FromDate(calendar.time))

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Schedule for 5. 5. 2019")
    }

    @Test
    fun `returns "immediately" when post does not have the date and is draft`() {
        postModel.setStatus(PostStatus.DRAFT.toString())
        postModel.setDateCreated("")

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Immediately")
    }

    @Test
    fun `returns empty string in other cases`() {
        postModel.setDateCreated("")

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("")
    }
}
