package org.wordpress.android.ui.posts.prepublishing.home.usecases

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostSettingsUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes

class GetPublishDateLabelUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: GetPublishDateLabelUseCase
    private lateinit var editPostRepository: EditPostRepository

    @Mock lateinit var postSettingsUtils: PostSettingsUtils

    @InternalCoroutinesApi
    @Before
    fun setup() {
        useCase = GetPublishDateLabelUseCase(postSettingsUtils)
        editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
        editPostRepository.set { PostModel() }
    }

    @Test
    fun `verify that when PostStatus is PRIVATE the publish date label is Immediately`() {
        editPostRepository.set { PostModel().apply { setStatus(PostStatus.PRIVATE.toString()) } }

        val label = useCase.getLabel(editPostRepository)

        assertThat((label as UiStringRes).stringRes).isEqualTo(R.string.immediately)
    }
}
