package org.wordpress.android.ui.bloggingprompts.promptslist.mapper

import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.ui.bloggingprompts.promptslist.model.BloggingPromptsListItemModel
import org.wordpress.android.util.LocaleManagerWrapper
import java.text.SimpleDateFormat
import javax.inject.Inject

class BloggingPromptsListItemModelMapper @Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper
) {
    private val dateFormat = SimpleDateFormat("MMM d", localeManagerWrapper.getLocale())

    fun toUiModel(domainModel: BloggingPromptModel) = BloggingPromptsListItemModel(
            id = domainModel.id,
            text = domainModel.text,
            date = domainModel.date,
            formattedDate = dateFormat.format(domainModel.date),
            isAnswered = domainModel.isAnswered,
            answersCount = domainModel.respondentsCount,
    )
}
