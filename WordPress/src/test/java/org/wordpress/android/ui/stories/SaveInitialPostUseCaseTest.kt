package org.wordpress.android.ui.stories

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.SavePostToDbUseCase

class SaveInitialPostUseCaseTest : BaseUnitTest() {
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var saveInitialPostUseCase: SaveInitialPostUseCase
    @Mock lateinit var site: SiteModel
    @Mock lateinit var savePostToDbUseCase: SavePostToDbUseCase
    @Mock lateinit var postStore: PostStore

    @InternalCoroutinesApi
    @Before
    fun setup() {
        saveInitialPostUseCase = SaveInitialPostUseCase(postStore, savePostToDbUseCase)
        editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
    }

    @Test
    fun `if saveInitialPost is called then the PostModel should have a PostStatus of DRAFT`() {
        // arrange
        val expectedPostStatus = PostStatus.DRAFT
        whenever(postStore.instantiatePostModel(any(), any(), any(), any())).thenReturn(PostModel())

        // act
        saveInitialPostUseCase.saveInitialPost(editPostRepository, site)

        // assert
        assertThat(editPostRepository.status).isEqualTo(expectedPostStatus)
    }
}
