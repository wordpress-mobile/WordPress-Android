package org.wordpress.android.ui.stories

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.SavePostToDbUseCase

@ExperimentalCoroutinesApi
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
        whenever(postStore.instantiatePostModel(anyOrNull(), any(), anyOrNull(), anyOrNull())).thenReturn(PostModel())
    }

    @Test
    fun `if saveInitialPost is called then the PostModel should get set with a PostStatus of DRAFT`() {
        // arrange
        val expectedPostStatus = PostStatus.DRAFT

        // act
        saveInitialPostUseCase.saveInitialPost(editPostRepository, site)

        // assert
        assertThat(editPostRepository.status).isEqualTo(expectedPostStatus)
    }

    @Test
    fun `if saveInitialPost is called and the site is not null then savePostToDbUseCase is invoked`() {
        // arrange
        val nonNullSite: SiteModel? = mock()

        // act
        saveInitialPostUseCase.saveInitialPost(editPostRepository, nonNullSite)

        // assert
        verify(savePostToDbUseCase, times(1)).savePostToDb(any(), any())
    }

    @Test
    fun `if saveInitialPost is called and the site is null then savePostToDbUseCase is not invoked`() {
        // arrange
        val nullSite: SiteModel? = null

        // act
        saveInitialPostUseCase.saveInitialPost(editPostRepository, nullSite)

        // assert
        verify(savePostToDbUseCase, times(0)).savePostToDb(any(), any())
    }
}
