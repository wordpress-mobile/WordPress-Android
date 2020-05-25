package org.wordpress.android.ui.posts.prepublishing.home.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostSettingsUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

class GetPublishDateLabelUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: GetPublishDateLabelUseCase
    @Mock lateinit var editPostRepository: EditPostRepository

    @Mock lateinit var postSettingsUtils: PostSettingsUtils

    @Before
    fun setup() {
        useCase = GetPublishDateLabelUseCase(postSettingsUtils)
    }

    @Test
    fun `verify that when PostStatus is PRIVATE then publish date label is Immediately`() {
        // arrange
        whenever(editPostRepository.status).thenReturn(PRIVATE)

        // act
        val label = useCase.getLabel(editPostRepository)

        // assert
        assertThat((label as UiStringRes).stringRes).isEqualTo(R.string.immediately)
    }

    @Test
    fun `verify that when PostModel is not null a label is returned based on that PostModel`() {
        // arrange
        val expectedLabel = "label"
        whenever(editPostRepository.getPost()).thenReturn(PostModel())
        whenever(postSettingsUtils.getPublishDateLabel(any())).thenReturn(expectedLabel)

        // act
        val label = useCase.getLabel(editPostRepository)

        // assert
        assertThat((label as UiStringText).text).isEqualTo(expectedLabel)
    }

    @Test
    fun `verify that when label from PostSettingsUtils is empty the publish date label is Immediately`() {
        // arrange
        val emptyLabel = ""
        whenever(editPostRepository.getPost()).thenReturn(PostModel())
        whenever(postSettingsUtils.getPublishDateLabel(any())).thenReturn(emptyLabel)

        // act
        val label = useCase.getLabel(editPostRepository)

        // assert
        assertThat((label as UiStringRes).stringRes).isEqualTo(R.string.immediately)
    }

    @Test(expected = NullPointerException::class)
    fun `verify that when PostModel is null then an Exception is thrown`() {
        // arrange
        whenever(editPostRepository.getPost()).thenReturn(null)

        // act
        useCase.getLabel(editPostRepository)
    }
}
