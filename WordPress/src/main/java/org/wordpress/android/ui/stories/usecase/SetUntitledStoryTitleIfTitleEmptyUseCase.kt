package org.wordpress.android.ui.stories.usecase

import android.content.Context
import org.wordpress.android.R
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.stories.StoryRepositoryWrapper
import javax.inject.Inject

class SetUntitledStoryTitleIfTitleEmptyUseCase @Inject constructor(
    private val storyRepositoryWrapper: StoryRepositoryWrapper,
    private val updateStoryPostTitleUseCase: UpdateStoryPostTitleUseCase,
    private val context: Context
) {
    fun setUntitledStoryTitleIfTitleEmpty(editPostRepository: EditPostRepository) {
        if (editPostRepository.title.isEmpty()) {
            val untitledStoryTitle = context.resources.getString(R.string.untitled)
            storyRepositoryWrapper.setCurrentStoryTitle(untitledStoryTitle)
            updateStoryPostTitleUseCase.updateStoryTitle(untitledStoryTitle, editPostRepository)
        }
    }
}
