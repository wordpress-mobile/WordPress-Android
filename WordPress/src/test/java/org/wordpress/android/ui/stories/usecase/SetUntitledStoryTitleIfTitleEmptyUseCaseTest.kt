package org.wordpress.android.ui.stories.usecase

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.stories.StoryRepositoryWrapper

@ExperimentalCoroutinesApi
class SetUntitledStoryTitleIfTitleEmptyUseCaseTest : BaseUnitTest() {
    private lateinit var setUntitledStoryTitleIfTitleEmptyUseCase: SetUntitledStoryTitleIfTitleEmptyUseCase

    @Mock
    lateinit var storyRepositoryWrapper: StoryRepositoryWrapper

    @Mock
    lateinit var editPostRepository: EditPostRepository

    @Mock
    lateinit var updateStoryPostTitleUseCase: UpdateStoryPostTitleUseCase

    @Mock
    lateinit var context: Context

    @Before
    fun setup() {
        setUntitledStoryTitleIfTitleEmptyUseCase = SetUntitledStoryTitleIfTitleEmptyUseCase(
            storyRepositoryWrapper,
            updateStoryPostTitleUseCase,
            context
        )
    }

    @Test
    fun `if Post title is empty then set Untitled as the story title with the storyRepositoryWrapper`() {
        // arrange
        val expectedPostTitle = "Untitled"
        whenever(editPostRepository.title).thenReturn("")
        whenever(context.resources).thenReturn(mock())
        whenever(context.resources.getString(R.string.untitled)).thenReturn(expectedPostTitle)

        // act
        setUntitledStoryTitleIfTitleEmptyUseCase.setUntitledStoryTitleIfTitleEmpty(editPostRepository)

        // assert
        verify(storyRepositoryWrapper).setCurrentStoryTitle(eq(expectedPostTitle))
    }

    @Test
    fun `if Post title is empty then set Untitled as the story title with the updateStoryPostTitleUseCase`() {
        // arrange
        val expectedPostTitle = "Untitled"
        whenever(editPostRepository.title).thenReturn("")
        whenever(context.resources).thenReturn(mock())
        whenever(context.resources.getString(R.string.untitled)).thenReturn(expectedPostTitle)

        // act
        setUntitledStoryTitleIfTitleEmptyUseCase.setUntitledStoryTitleIfTitleEmpty(editPostRepository)

        // assert
        verify(updateStoryPostTitleUseCase).updateStoryTitle(eq(expectedPostTitle), any())
    }

    @Test
    fun `if Post title is not empty then storyRepositoryWrapper is not called`() {
        // arrange
        whenever(editPostRepository.title).thenReturn("Story Title")

        // act
        setUntitledStoryTitleIfTitleEmptyUseCase.setUntitledStoryTitleIfTitleEmpty(editPostRepository)

        // assert
        verify(storyRepositoryWrapper, times(0)).setCurrentStoryTitle(any())
    }

    @Test
    fun `if Post title is not empty then updateStoryPostTitleUseCase is not called`() {
        // arrange
        whenever(editPostRepository.title).thenReturn("Story Title")

        // act
        setUntitledStoryTitleIfTitleEmptyUseCase.setUntitledStoryTitleIfTitleEmpty(editPostRepository)

        // assert
        verify(updateStoryPostTitleUseCase, times(0)).updateStoryTitle(any(), any())
    }
}
