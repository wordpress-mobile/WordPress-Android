package org.wordpress.android.ui.bloggingprompts

import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType

object BloggingPromptsPostTagProvider {
    const val BLOGGING_PROMPT_TAG = "dailyprompt"
    private const val BLOGGING_PROMPT_ID_TAG = "dailyprompt-%s"

    fun promptIdTag(promptId: Int): String = BLOGGING_PROMPT_ID_TAG.format(promptId)

    fun promptIdSearchReaderTag(promptId: Int): ReaderTag = ReaderTag(
        promptIdTag(promptId),
        promptIdTag(promptId),
        promptIdTag(promptId),
        "",
        ReaderTagType.SEARCH,
    )
}
