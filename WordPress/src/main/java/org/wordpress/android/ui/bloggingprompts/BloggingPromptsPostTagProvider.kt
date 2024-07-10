package org.wordpress.android.ui.bloggingprompts

import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import javax.inject.Inject

class BloggingPromptsPostTagProvider @Inject constructor(
    private val readerUtilsWrapper: ReaderUtilsWrapper,
) {
    fun promptIdTag(
        id: Int
    ): String = "$BLOGGING_PROMPT_ID_TAG_PREFIX$id"
        .takeIf { id > 0 }
        ?: BLOGGING_PROMPT_TAG

    fun promptSearchReaderTag(
        tagUrl: String
    ): ReaderTag {
        val promptIdTag = promptTagFromUrl(tagUrl)
        return ReaderTag(
            promptIdTag,
            promptIdTag,
            promptIdTag,
            ReaderPostRepository.formatFullEndpointForTag(promptIdTag),
            ReaderTagType.FOLLOWED,
        )
    }

    private fun promptTagFromUrl(
        tagUrl: String
    ): String = readerUtilsWrapper.getTagFromTagUrl(tagUrl)
        .takeIf { it.isNotBlank() }
        ?: BLOGGING_PROMPT_TAG

    companion object {
        const val BLOGGING_PROMPT_TAG = "dailyprompt"
        const val BLOGGING_PROMPT_ID_TAG_PREFIX = "dailyprompt-"
        const val BLOGANUARY_TAG = "bloganuary"
    }
}
