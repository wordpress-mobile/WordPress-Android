package org.wordpress.android.ui.stories.usecase

import android.content.Context
import org.wordpress.android.BaseUnitTest

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.ui.stories.StoryRepositoryWrapper

class SetUntitledStoryTitleIfTitleEmptyUseCaseTest : BaseUnitTest() {
    private lateinit var setUntitledStoryTitleIfTitleEmptyUseCase: SetUntitledStoryTitleIfTitleEmptyUseCase
    @Mock lateinit var storyRepositoryWrapper: StoryRepositoryWrapper
    @Mock lateinit var updateStoryPostTitleUseCase: UpdateStoryPostTitleUseCase
    @Mock lateinit var context: Context

    @Before
    fun setup() {
        setUntitledStoryTitleIfTitleEmptyUseCase = SetUntitledStoryTitleIfTitleEmptyUseCase(
                storyRepositoryWrapper,
                updateStoryPostTitleUseCase,
                context
        )
    }
}
