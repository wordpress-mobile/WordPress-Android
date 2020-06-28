package org.wordpress.android.ui.stories

import com.wordpress.stories.compose.story.StoryRepository

class StoryRepositoryWrapper {
    fun setCurrentStoryTitle(title: String) = StoryRepository.setCurrentStoryTitle(title)
    fun getStoryFrameThumbnailUrl(index: Int) = StoryRepository.getStoryFrameThumbnailUrl(index)
}
