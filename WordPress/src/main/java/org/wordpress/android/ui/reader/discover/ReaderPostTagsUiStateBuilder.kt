package org.wordpress.android.ui.reader.discover

import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.discover.interests.TagUiState
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@Reusable
class ReaderPostTagsUiStateBuilder @Inject constructor(
    private val contextProvider: ContextProvider,
    private val resourceProvider: ResourceProvider
) {
    private val maxWidthForChip: Int
        get() {
            val width = DisplayUtils.getDisplayPixelWidth(contextProvider.getContext()) -
                    resourceProvider.getDimensionPixelSize(R.dimen.reader_card_margin) * 2
            return (width * MAX_WIDTH_FACTOR).toInt()
        }

    fun mapPostTagsToTagUiStates(
        post: ReaderPost
    ): List<TagUiState> {
        return post.tags.map {
            TagUiState(
                title = it.tagTitle,
                slug = it.tagSlug,
                maxWidth = maxWidthForChip
            )
        }
    }

    companion object {
        private const val MAX_WIDTH_FACTOR = 0.75
    }
}
