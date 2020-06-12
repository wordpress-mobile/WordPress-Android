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
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.stats.refresh.utils.DateUtils
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.viewmodel.ResourceProvider

class PostSettingsUtilsTest : BaseUnitTest() {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var dateUtils: DateUtils
    @Mock lateinit var dateProvider: DateProvider
    private lateinit var postSettingsUtils: PostSettingsUtils
    private lateinit var postUtilsWrapper: PostUtilsWrapper

    private val dateCreated = "2019-05-05T14:33:20+0200"
    private val currentDate = "2019-05-05T20:33:20+0200"
    private val formattedDate = "5. 5. 2019"
    private lateinit var postModel: PostModel

    @Before
    fun setUp() {
        postUtilsWrapper = PostUtilsWrapper(dateProvider)
        postSettingsUtils = PostSettingsUtils(resourceProvider, dateUtils, postUtilsWrapper)
        whenever(dateUtils.formatDateTime(any())).thenReturn(formattedDate)
        whenever(dateProvider.getCurrentDate()).thenReturn(DateTimeUtils.dateUTCFromIso8601(currentDate))
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

        // two hours ahead of the currentDate
        val futureDate = "2019-05-05T22:28:20+0200"
        postModel.setDateCreated(futureDate)

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
        postModel.setDateCreated(currentDate)

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Immediately")
    }

    @Test
    fun `returns "immediately" for local private post that should publish immediately`() {
        postModel.setIsLocalDraft(true)
        postModel.setStatus(PostStatus.PRIVATE.toString())
        postModel.setDateCreated(currentDate)

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Immediately")
    }

    @Test
    fun `returns "publish on" for local draft when date within the next 30 minutes`() {
        postModel.setIsLocalDraft(true)

        // This date is 5 minutes before the currentDate
        val dateCreated = "2019-05-05T20:28:20+0200"

        postModel.setDateCreated(dateCreated)

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Publish on 5. 5. 2019")
    }

    @Test
    fun `returns "schedule for" when post published in future`() {
        // two hours ahead of the currentDate
        val futureDate = "2019-05-05T22:28:20+0200"

        postModel.setDateCreated(futureDate)

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
    fun `returns "immediately" in other cases`() {
        postModel.setDateCreated("")

        val publishedDate = postSettingsUtils.getPublishDateLabel(postModel)

        assertThat(publishedDate).isEqualTo("Immediately")
    }
}
