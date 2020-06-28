package org.wordpress.android.ui.stories

import com.wordpress.stories.compose.story.StoryRepository
import javax.inject.Inject

class StoryRepositoryWrapper @Inject constructor() {
    fun setCurrentStoryTitle(title: String) = StoryRepository.setCurrentStoryTitle(title)
    fun getCurrentStoryThumbnailUrl() = StoryRepository.getCurrentStoryThumbnailUrl()
}
