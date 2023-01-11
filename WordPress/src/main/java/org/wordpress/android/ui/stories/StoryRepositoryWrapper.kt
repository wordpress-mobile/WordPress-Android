package org.wordpress.android.ui.stories

import com.wordpress.stories.compose.story.StoryFrameItem
import com.wordpress.stories.compose.story.StoryIndex
import com.wordpress.stories.compose.story.StoryRepository
import javax.inject.Inject

class StoryRepositoryWrapper @Inject constructor() {
    fun setCurrentStoryTitle(title: String) = StoryRepository.setCurrentStoryTitle(title)
    fun getCurrentStoryThumbnailUrl() = StoryRepository.getCurrentStoryThumbnailUrl()
    fun getCurrentStoryTitle() = StoryRepository.getCurrentStoryTitle()
    fun getCurrentStoryIndex(): StoryIndex = StoryRepository.currentStoryIndex
    fun loadStory(storyIndex: StoryIndex) = StoryRepository.loadStory(storyIndex)
    fun addStoryFrameItemToCurrentStory(item: StoryFrameItem) =
        StoryRepository.addStoryFrameItemToCurrentStory(item)

    fun getStoryAtIndex(index: StoryIndex) = StoryRepository.getStoryAtIndex(index)
    fun getImmutableStories() = StoryRepository.getImmutableStories()
    fun getCurrentStorySaveProgress(storyIndex: StoryIndex, oneItemActualProgress: Float = 0.0F) =
        StoryRepository.getCurrentStorySaveProgress(storyIndex, oneItemActualProgress)

    fun findStoryContainingStoryFrameItemsByIds(ids: ArrayList<String>) =
        StoryRepository.findStoryContainingStoryFrameItemsByIds(ids)
}
