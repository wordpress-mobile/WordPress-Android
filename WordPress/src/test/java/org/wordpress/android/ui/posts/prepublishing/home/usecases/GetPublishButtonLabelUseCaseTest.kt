package org.wordpress.android.ui.posts.prepublishing.home.usecases

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.util.DateTimeUtils
import java.util.Calendar
import java.util.Date

class GetPublishButtonLabelUseCaseTest : BaseUnitTest() {
    lateinit var editPostRepository: EditPostRepository
    lateinit var getPublishButtonLabelUseCase: GetPublishButtonLabelUseCase
    private lateinit var postModel: PostModel

    private val dateCreated = "2019-05-05T14:33:20+0200"

    @InternalCoroutinesApi
    @Before
    fun setup() {
        getPublishButtonLabelUseCase = GetPublishButtonLabelUseCase()
        postModel = PostModel()
        editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
        editPostRepository.set { postModel }
    }

    @Test
    fun `returns "scheduled now" for scheduled post`() {
        // arrange
        val expectedTextResource = R.string.prepublishing_nudges_home_schedule_button

        postModel.setStatus(PostStatus.SCHEDULED.toString())
        postModel.setDateCreated(dateCreated)

        // act
        val textResource = getPublishButtonLabelUseCase.getLabel(editPostRepository)

        // assert
        assertThat(textResource).isEqualTo(expectedTextResource)
    }

    @Test
    fun `returns "publish now" for published post`() {
        // arrange
        val expectedTextResource = R.string.prepublishing_nudges_home_publish_button

        postModel.setStatus(PostStatus.PUBLISHED.toString())
        postModel.setDateCreated(dateCreated)

        val textResource = getPublishButtonLabelUseCase.getLabel(editPostRepository)

        assertThat(textResource).isEqualTo(expectedTextResource)
    }

    @Test
    fun `returns "publish now" for private post`() {
        // arrange
        val expectedTextResource = R.string.prepublishing_nudges_home_publish_button

        postModel.setStatus(PostStatus.PRIVATE.toString())
        postModel.setDateCreated(dateCreated)

        val textResource = getPublishButtonLabelUseCase.getLabel(editPostRepository)

        assertThat(textResource).isEqualTo(expectedTextResource)
    }

    @Test
    fun `returns "publish now" for local draft when publish date in the past`() {
        // arrange
        val expectedTextResource = R.string.prepublishing_nudges_home_publish_button

        postModel.setIsLocalDraft(true)
        postModel.setDateCreated(dateCreated)

        // act
        val textResource = getPublishButtonLabelUseCase.getLabel(editPostRepository)

        // assert
        assertThat(textResource).isEqualTo(expectedTextResource)
    }

    @Test
    fun `returns "publish now" for local draft when should publish immediately`() {
        // arrange
        val expectedTextResource = R.string.prepublishing_nudges_home_publish_button

        postModel.setIsLocalDraft(true)
        postModel.setStatus(PostStatus.DRAFT.toString())
        postModel.setDateCreated(DateTimeUtils.iso8601FromDate(Date()))

        // act
        val textResource = getPublishButtonLabelUseCase.getLabel(editPostRepository)

        // assert
        assertThat(textResource).isEqualTo(expectedTextResource)
    }

    @Test
    fun `returns "publish now" for local draft when date is not set`() {
        // arrange
        val expectedTextResource = R.string.prepublishing_nudges_home_publish_button

        postModel.setIsLocalDraft(true)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -5)
        postModel.setDateCreated(DateTimeUtils.iso8601FromDate(calendar.time))

        // act
        val textResource = getPublishButtonLabelUseCase.getLabel(editPostRepository)

        // assert
        assertThat(textResource).isEqualTo(expectedTextResource)
    }

    @Test
    fun `returns "scheduled now" when post published in future`() {
        // arrange
        val expectedTextResource = R.string.prepublishing_nudges_home_schedule_button

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 100)
        postModel.setDateCreated(DateTimeUtils.iso8601FromDate(calendar.time))

        // act
        val textResource = getPublishButtonLabelUseCase.getLabel(editPostRepository)

        // assert
        assertThat(textResource).isEqualTo(expectedTextResource)
    }

    @Test
    fun `returns "publish now" when post does not have the date and is draft`() {
        // arrange
        val expectedTextResource = R.string.prepublishing_nudges_home_publish_button

        postModel.setStatus(PostStatus.DRAFT.toString())
        postModel.setDateCreated("")
        // act
        val textResource = getPublishButtonLabelUseCase.getLabel(editPostRepository)

        // assert
        assertThat(textResource).isEqualTo(expectedTextResource)
    }

    @Test
    fun `returns "publish now" when no condition is met`() {
        // arrange
        val expectedTextResource = R.string.prepublishing_nudges_home_publish_button

        postModel.setDateCreated("")

        // act
        val textResource = getPublishButtonLabelUseCase.getLabel(editPostRepository)

        // assert
        assertThat(textResource).isEqualTo(expectedTextResource)
    }
}
