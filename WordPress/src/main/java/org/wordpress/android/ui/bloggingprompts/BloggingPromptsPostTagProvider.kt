package org.wordpress.android.ui.bloggingprompts

import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.services.post.ReaderPostLogic
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import javax.inject.Inject

class BloggingPromptsPostTagProvider @Inject constructor(
    private val readerUtilsWrapper: ReaderUtilsWrapper,
) {
    fun promptIdTag(
        tagUrl: String
    ): String = readerUtilsWrapper.getTagFromTagUrl(tagUrl)
        .takeIf { it.isNotBlank() }
        ?: BLOGGING_PROMPT_TAG

    fun promptIdSearchReaderTag(
        tagUrl: String
    ): ReaderTag {
        val promptIdTag = promptIdTag(tagUrl)
        return ReaderTag(
            promptIdTag,
            promptIdTag,
            promptIdTag,
            ReaderPostLogic.formatFullEndpointForTag(promptIdTag),
            ReaderTagType.FOLLOWED,
        )
    }

    companion object {
        const val BLOGGING_PROMPT_TAG = "dailyprompt"
        const val BLOGANUARY_TAG = "bloganuary"
    }
}
